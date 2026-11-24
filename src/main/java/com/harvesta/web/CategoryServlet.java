package com.harvesta.web;

import com.harvesta.Db;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;

/** F-02 Categories (master data): everyone can view, only admins can change. */
@WebServlet("/categories/*")
public class CategoryServlet extends BaseServlet {

    @Override
    protected void index(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Query q = new Query().search(param(req, "search"), "c.name", "coalesce(c.description, '')");
        req.setAttribute("page", q.page(req,
                "c.*, (select count(*) from tree_types t where t.category_id = c.id) as tree_types_count",
                "from categories c", "c.name"));
        render(req, resp, "categories/index", "Categories");
    }

    @Override
    protected void create(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        requireAdmin(req);
        req.setAttribute("f", new HashMap<>());
        render(req, resp, "categories/form", "New Category");
    }

    @Override
    protected void edit(HttpServletRequest req, HttpServletResponse resp, long id)
            throws ServletException, IOException {
        requireAdmin(req);
        req.setAttribute("f", findOr404("select * from categories where id = ?", id));
        render(req, resp, "categories/form", "Edit Category");
    }

    @Override
    protected void store(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        save(req, resp, null);
    }

    @Override
    protected void update(HttpServletRequest req, HttpServletResponse resp, long id)
            throws ServletException, IOException {
        findOr404("select id from categories where id = ?", id);
        save(req, resp, id);
    }

    private void save(HttpServletRequest req, HttpServletResponse resp, Long id) throws ServletException, IOException {
        requireAdmin(req);
        Form form = new Form(req);
        String name = form.text("name", "name", true, 100);
        String description = form.text("description", "description", false, 5000);
        form.unique("name", "name", name, "categories", "name", id, null, null);
        if (!form.ok()) {
            invalid(req, resp, form, id, "categories/form", id == null ? "New Category" : "Edit Category");
            return;
        }
        if (id == null) {
            Db.update("insert into categories (name, description) values (?, ?)", name, description);
            flash(req, "The category was created successfully.");
        } else {
            Db.update("update categories set name = ?, description = ?, updated_at = now() where id = ?",
                    name, description, id);
            flash(req, "The category was updated successfully.");
        }
        redirect(req, resp, "/categories");
    }

    @Override
    protected void destroy(HttpServletRequest req, HttpServletResponse resp, long id) throws IOException {
        requireAdmin(req);
        findOr404("select id from categories where id = ?", id);
        if (Db.count("select count(*) from tree_types where category_id = ?", id) > 0) {
            flashError(req, "This category cannot be deleted because it is still used by tree types.");
        } else {
            Db.update("delete from categories where id = ?", id);
            flash(req, "The category was deleted successfully.");
        }
        redirect(req, resp, "/categories");
    }
}
