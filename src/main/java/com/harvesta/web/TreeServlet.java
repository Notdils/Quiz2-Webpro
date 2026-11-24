package com.harvesta.web;

import com.harvesta.Db;
import com.harvesta.Options;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * F-05 Trees: staff create/view/edit, admins also delete (soft delete).
 * One photo per tree (JPG/PNG/WebP, max 2 MB), stored in the database so it survives
 * on hosts without persistent disk, and served from /trees/{id}/photo.
 */
@WebServlet("/trees/*")
// Container limits are generous so oversized files reach our own validation message.
@MultipartConfig(maxFileSize = 10 * 1024 * 1024, maxRequestSize = 12 * 1024 * 1024)
public class TreeServlet extends BaseServlet {

    private static final int MAX_PHOTO = 2 * 1024 * 1024;

    private static final Map<String, String> SORTS = Map.of(
            "code", "t.code", "-code", "t.code desc",
            "planted_on", "t.planted_on, t.code", "-planted_on", "t.planted_on desc, t.code");

    @Override
    protected void index(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Query q = new Query().where("t.deleted_at is null")
                .search(param(req, "search"), "t.code", "coalesce(t.notes, '')")
                .filter(longParam(req, "block_id"), "t.block_id = ?")
                .filter(longParam(req, "tree_type_id"), "t.tree_type_id = ?")
                .filter(longParam(req, "category_id"), "tt.category_id = ?")
                .filter(param(req, "status"), "t.status = ?");
        String sort = SORTS.getOrDefault(String.valueOf(param(req, "sort")), "t.code");
        req.setAttribute("page", q.page(req,
                "t.id, t.code, t.status, t.planted_on, t.height_cm, t.photo is not null as has_photo, "
                        + "tt.name as type_name, c.name as category_name, b.name as block_name, b.id as block_id",
                "from trees t join tree_types tt on tt.id = t.tree_type_id join categories c on c.id = tt.category_id "
                        + "join blocks b on b.id = t.block_id", sort));
        lookups(req);
        req.setAttribute("categories", Db.list("select id, name from categories order by name"));
        render(req, resp, "trees/index", "Trees");
    }

    @Override
    protected void show(HttpServletRequest req, HttpServletResponse resp, long id)
            throws ServletException, IOException {
        Map<String, Object> tree = findOr404("select t.id, t.code, t.status, t.planted_on, t.height_cm, t.notes, "
                + "t.photo is not null as has_photo, t.created_at, t.block_id, tt.name as type_name, "
                + "tt.scientific_name, c.name as category_name, b.name as block_name, u.name as creator_name "
                + "from trees t join tree_types tt on tt.id = t.tree_type_id join categories c on c.id = tt.category_id "
                + "join blocks b on b.id = t.block_id left join users u on u.id = t.created_by "
                + "where t.id = ? and t.deleted_at is null", id);
        req.setAttribute("tree", tree);
        req.setAttribute("careLogs", Db.list("select cl.*, u.name as user_name from care_logs cl "
                + "left join users u on u.id = cl.user_id where cl.tree_id = ? "
                + "order by cl.performed_on desc, cl.id desc limit 50", id));
        req.setAttribute("harvests", Db.list("select h.*, u.name as user_name, "
                + "(select coalesce(sum(s.quantity), 0) from sales s where s.harvest_id = h.id) as sold "
                + "from harvests h left join users u on u.id = h.user_id where h.tree_id = ? "
                + "order by h.harvested_on desc, h.id desc limit 50", id));
        req.setAttribute("totals", Db.one("select "
                + "coalesce(sum(case when unit = 'kg' then quantity end), 0) as kg, "
                + "coalesce(sum(case when unit = 'pcs' then quantity end), 0) as pcs, count(*) as harvests "
                + "from harvests where tree_id = ?", id));
        render(req, resp, "trees/show", (String) tree.get("code"));
    }

    @Override
    protected void other(HttpServletRequest req, HttpServletResponse resp, long id, String action, boolean post)
            throws IOException {
        if (!action.equals("photo") || post) {
            throw HttpError.notFound();
        }
        Map<String, Object> row = findOr404("select photo, photo_type from trees where id = ? and photo is not null", id);
        byte[] photo = (byte[]) row.get("photo");
        resp.setContentType((String) row.get("photo_type"));
        resp.setContentLength(photo.length);
        resp.setHeader("Cache-Control", "private, max-age=300");
        resp.getOutputStream().write(photo);
    }

    @Override
    protected void create(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        requireWriter(req);
        Map<String, Object> f = new HashMap<>();
        f.put("status", "seedling");
        f.put("planted_on", LocalDate.now());
        f.put("block_id", param(req, "block_id"));
        req.setAttribute("f", f);
        lookups(req);
        render(req, resp, "trees/form", "New Tree");
    }

    @Override
    protected void edit(HttpServletRequest req, HttpServletResponse resp, long id)
            throws ServletException, IOException {
        requireWriter(req);
        req.setAttribute("f", findOr404("select id, code, tree_type_id, block_id, planted_on, status, height_cm, notes, "
                + "photo is not null as has_photo from trees where id = ? and deleted_at is null", id));
        lookups(req);
        render(req, resp, "trees/form", "Edit Tree");
    }

    @Override
    protected void store(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        save(req, resp, null);
    }

    @Override
    protected void update(HttpServletRequest req, HttpServletResponse resp, long id)
            throws ServletException, IOException {
        findOr404("select id from trees where id = ? and deleted_at is null", id);
        save(req, resp, id);
    }

    private void save(HttpServletRequest req, HttpServletResponse resp, Long id) throws ServletException, IOException {
        requireWriter(req);
        Form form = new Form(req);
        String code = form.text("code", "code", false, 30);
        Long typeId = form.id("tree_type_id", "tree type", true, "tree_types");
        Long blockId = form.id("block_id", "block", true, "blocks");
        LocalDate plantedOn = form.date("planted_on", "planted on date", true, true);
        String status = form.option("status", "status", Options.TreeStatus.values(), true);
        BigDecimal height = form.decimal("height_cm", "height", false, false);
        String notes = form.text("notes", "notes", false, 5000);
        form.unique("code", "code", code, "trees", "code", id, null, null);

        String contentType = req.getContentType();
        Part part = contentType != null && contentType.startsWith("multipart/") ? req.getPart("photo") : null;
        byte[] photo = null;
        String photoType = null;
        if (part != null && part.getSize() > 0) {
            try (InputStream in = part.getInputStream()) {
                photo = in.readAllBytes();
            }
            photoType = imageType(photo);
            if (photo.length > MAX_PHOTO) {
                form.error("photo", "The photo must not be greater than 2 MB.");
            } else if (photoType == null) {
                form.error("photo", "The photo must be a JPG, PNG, or WebP image.");
            }
        }

        if (!form.ok()) {
            if (id != null) {
                form.values.put("has_photo", Db.count("select count(*) from trees where id = ? and photo is not null", id) > 0);
            }
            lookups(req);
            invalid(req, resp, form, id, "trees/form", id == null ? "New Tree" : "Edit Tree");
            return;
        }

        boolean removePhoto = req.getParameter("remove_photo") != null;
        byte[] newPhoto = photo;
        String newType = photoType;
        long treeId = Db.tx(() -> {
            if (id == null) {
                return Db.insert("insert into trees (code, tree_type_id, block_id, planted_on, status, height_cm, notes, "
                                + "photo, photo_type, created_by) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) returning id",
                        code == null ? nextCode() : code, typeId, blockId, plantedOn, status, height, notes,
                        newPhoto, newType, userId(req));
            }
            Db.update("update trees set code = coalesce(?, code), tree_type_id = ?, block_id = ?, planted_on = ?, "
                            + "status = ?, height_cm = ?, notes = ?, updated_at = now() where id = ?",
                    code, typeId, blockId, plantedOn, status, height, notes, id);
            if (newType != null || removePhoto) {
                // The old photo is replaced in place, so nothing is left behind.
                Db.update("update trees set photo = ?, photo_type = ? where id = ?", newPhoto, newType, id);
            }
            return (long) id;
        });
        flash(req, id == null ? "The tree was created successfully." : "The tree was updated successfully.");
        redirect(req, resp, "/trees/" + treeId);
    }

    @Override
    protected void destroy(HttpServletRequest req, HttpServletResponse resp, long id) throws IOException {
        requireAdmin(req);
        findOr404("select id from trees where id = ? and deleted_at is null", id);
        Db.update("update trees set deleted_at = now() where id = ?", id);
        flash(req, "The tree was deleted successfully.");
        redirect(req, resp, "/trees");
    }

    /** Next free sequential code: T-0001, T-0002, ... (soft-deleted trees included). */
    static String nextCode() {
        Object last = Db.value("select code from trees where code ~ '^T-[0-9]+$' order by id desc limit 1");
        int number = last == null ? 1 : Integer.parseInt(last.toString().substring(2)) + 1;
        String code;
        do {
            code = String.format("T-%04d", number++);
        } while (Db.count("select count(*) from trees where code = ?", code) > 0);
        return code;
    }

    /** Detects the image type from the file's magic bytes; the client's content type is not trusted. */
    public static String imageType(byte[] b) {
        if (b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "image/png";
        }
        if (b.length > 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    private static void lookups(HttpServletRequest req) {
        req.setAttribute("treeTypes", Db.list("select tt.id, tt.name, c.name as category_name from tree_types tt "
                + "join categories c on c.id = tt.category_id order by c.name, tt.name"));
        req.setAttribute("blocks", Db.list("select id, name from blocks order by name"));
    }
}
