package com.harvesta.web;

import com.harvesta.Db;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;

/** F-04 Blocks: planting areas with a detail page listing their trees and harvest totals. */
@WebServlet("/blocks/*")
public class BlockServlet extends BaseServlet {

    @Override
    protected void index(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Query q = new Query().search(param(req, "search"), "b.name", "coalesce(b.location_note, '')");
        req.setAttribute("page", q.page(req,
                "b.*, (select count(*) from trees t where t.block_id = b.id and t.deleted_at is null) as trees_count",
                "from blocks b", "b.name"));
        render(req, resp, "blocks/index", "Blocks");
    }

    @Override
    protected void show(HttpServletRequest req, HttpServletResponse resp, long id)
            throws ServletException, IOException {
        req.setAttribute("block", findOr404("select * from blocks where id = ?", id));
        req.setAttribute("stats", Db.one("select "
                + "(select count(*) from trees where block_id = ? and deleted_at is null) as trees, "
                + "(select coalesce(sum(h.quantity), 0) from harvests h join trees t on t.id = h.tree_id "
                + " where t.block_id = ? and h.unit = 'kg') as kg, "
                + "(select coalesce(sum(h.quantity), 0) from harvests h join trees t on t.id = h.tree_id "
                + " where t.block_id = ? and h.unit = 'pcs') as pcs", id, id, id));
        req.setAttribute("page", new Query().where("t.block_id = ?", id).where("t.deleted_at is null").page(req,
                "t.id, t.code, t.status, t.planted_on, t.photo is not null as has_photo, tt.name as type_name",
                "from trees t join tree_types tt on tt.id = t.tree_type_id", "t.code"));
        render(req, resp, "blocks/show", "Block");
    }

    @Override
    protected void create(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        requireAdmin(req);
        req.setAttribute("f", new HashMap<>());
        render(req, resp, "blocks/form", "New Block");
    }

    @Override
    protected void edit(HttpServletRequest req, HttpServletResponse resp, long id)
            throws ServletException, IOException {
        requireAdmin(req);
        req.setAttribute("f", findOr404("select * from blocks where id = ?", id));
        render(req, resp, "blocks/form", "Edit Block");
    }

    @Override
    protected void store(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        save(req, resp, null);
    }

    @Override
    protected void update(HttpServletRequest req, HttpServletResponse resp, long id)
            throws ServletException, IOException {
        findOr404("select id from blocks where id = ?", id);
        save(req, resp, id);
    }

    private void save(HttpServletRequest req, HttpServletResponse resp, Long id) throws ServletException, IOException {
        requireAdmin(req);
        Form form = new Form(req);
        String name = form.text("name", "name", true, 100);
        BigDecimal area = form.decimal("area_sqm", "area", false, false);
        String location = form.text("location_note", "location note", false, 255);
        String description = form.text("description", "description", false, 5000);
        form.unique("name", "name", name, "blocks", "name", id, null, null);
        if (!form.ok()) {
            invalid(req, resp, form, id, "blocks/form", id == null ? "New Block" : "Edit Block");
            return;
        }
        if (id == null) {
            id = Db.insert("insert into blocks (name, area_sqm, location_note, description) values (?, ?, ?, ?) "
                    + "returning id", name, area, location, description);
            flash(req, "The block was created successfully.");
        } else {
            Db.update("update blocks set name = ?, area_sqm = ?, location_note = ?, description = ?, "
                    + "updated_at = now() where id = ?", name, area, location, description, id);
            flash(req, "The block was updated successfully.");
        }
        redirect(req, resp, "/blocks/" + id);
    }

    @Override
    protected void destroy(HttpServletRequest req, HttpServletResponse resp, long id) throws IOException {
        requireAdmin(req);
        findOr404("select id from blocks where id = ?", id);
        if (Db.count("select count(*) from trees where block_id = ?", id) > 0) {
            flashError(req, "This block cannot be deleted because it still has trees.");
            redirect(req, resp, "/blocks/" + id);
            return;
        }
        Db.update("delete from blocks where id = ?", id);
        flash(req, "The block was deleted successfully.");
        redirect(req, resp, "/blocks");
    }
}
