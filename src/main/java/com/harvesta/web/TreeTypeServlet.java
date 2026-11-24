package com.harvesta.web;

import com.harvesta.Db;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;

/** F-03 Tree Types (master data): name unique per category; cannot be deleted while trees use it. */
@WebServlet("/tree-types/*")
public class TreeTypeServlet extends BaseServlet {

    @Override
    protected void index(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Query q = new Query()
                .search(param(req, "search"), "tt.name", "coalesce(tt.scientific_name, '')")
                .filter(longParam(req, "category_id"), "tt.category_id = ?");
        req.setAttribute("page", q.page(req,
                "tt.*, c.name as category_name, "
                        + "(select count(*) from trees t where t.tree_type_id = tt.id and t.deleted_at is null) as trees_count",
                "from tree_types tt join categories c on c.id = tt.category_id", "tt.name, c.name"));
        req.setAttribute("categories", categories());
        render(req, resp, "tree-types/index", "Tree Types");
    }

    @Override
    protected void create(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        requireAdmin(req);
        HashMap<String, Object> f = new HashMap<>();
        f.put("category_id", param(req, "category_id"));
        req.setAttribute("f", f);
        req.setAttribute("categories", categories());
        render(req, resp, "tree-types/form", "New Tree Type");
    }

    @Override
    protected void edit(HttpServletRequest req, HttpServletResponse resp, long id)
            throws ServletException, IOException {
        requireAdmin(req);
        req.setAttribute("f", findOr404("select * from tree_types where id = ?", id));
        req.setAttribute("categories", categories());
        render(req, resp, "tree-types/form", "Edit Tree Type");
    }

    @Override
    protected void store(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        save(req, resp, null);
    }

    @Override
    protected void update(HttpServletRequest req, HttpServletResponse resp, long id)
            throws ServletException, IOException {
        findOr404("select id from tree_types where id = ?", id);
        save(req, resp, id);
    }

    private void save(HttpServletRequest req, HttpServletResponse resp, Long id) throws ServletException, IOException {
        requireAdmin(req);
        Form form = new Form(req);
        Long categoryId = form.id("category_id", "category", true, "categories");
        String name = form.text("name", "name", true, 100);
        String scientificName = form.text("scientific_name", "scientific name", false, 150);
        String description = form.text("description", "description", false, 5000);
        form.unique("name", "name", name, "tree_types", "name", id, "category_id", categoryId);
        if (!form.ok()) {
            req.setAttribute("categories", categories());
            invalid(req, resp, form, id, "tree-types/form", id == null ? "New Tree Type" : "Edit Tree Type");
            return;
        }
        if (id == null) {
            Db.update("insert into tree_types (category_id, name, scientific_name, description) values (?, ?, ?, ?)",
                    categoryId, name, scientificName, description);
            flash(req, "The tree type was created successfully.");
        } else {
            Db.update("update tree_types set category_id = ?, name = ?, scientific_name = ?, description = ?, "
                    + "updated_at = now() where id = ?", categoryId, name, scientificName, description, id);
            flash(req, "The tree type was updated successfully.");
        }
        redirect(req, resp, "/tree-types");
    }

    @Override
    protected void destroy(HttpServletRequest req, HttpServletResponse resp, long id) throws IOException {
        requireAdmin(req);
        findOr404("select id from tree_types where id = ?", id);
        if (Db.count("select count(*) from trees where tree_type_id = ?", id) > 0) {
            flashError(req, "This tree type cannot be deleted because it is still used by trees.");
        } else {
            Db.update("delete from tree_types where id = ?", id);
            flash(req, "The tree type was deleted successfully.");
        }
        redirect(req, resp, "/tree-types");
    }

    private static Object categories() {
        return Db.list("select id, name from categories order by name");
    }
}
