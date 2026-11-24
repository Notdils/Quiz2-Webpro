package com.harvesta.web;

import com.harvesta.Db;
import com.harvesta.Options;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** F-11 User Management (admins only). The last admin can be neither demoted nor deleted. */
@WebServlet("/users/*")
public class UserServlet extends BaseServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            requireAdmin(req);
        } catch (HttpError e) {
            resp.sendError(e.status);
            return;
        }
        super.service(req, resp);
    }

    @Override
    protected void index(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Query q = new Query().search(param(req, "search"), "u.name", "u.email").filter(param(req, "role"), "u.role = ?");
        req.setAttribute("page", q.page(req, "u.id, u.name, u.email, u.role, u.created_at", "from users u", "u.name, u.id"));
        req.setAttribute("adminCount", Db.count("select count(*) from users where role = 'admin'"));
        render(req, resp, "users/index", "Users");
    }

    @Override
    protected void create(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, Object> f = new HashMap<>();
        f.put("role", "staff");
        req.setAttribute("f", f);
        render(req, resp, "users/form", "New User");
    }

    @Override
    protected void edit(HttpServletRequest req, HttpServletResponse resp, long id)
            throws ServletException, IOException {
        req.setAttribute("f", findOr404("select id, name, email, role from users where id = ?", id));
        render(req, resp, "users/form", "Edit User");
    }

    @Override
    protected void store(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        save(req, resp, null);
    }

    @Override
    protected void update(HttpServletRequest req, HttpServletResponse resp, long id)
            throws ServletException, IOException {
        findOr404("select id from users where id = ?", id);
        save(req, resp, id);
    }

    private void save(HttpServletRequest req, HttpServletResponse resp, Long id) throws ServletException, IOException {
        Form form = new Form(req);
        String name = form.text("name", "name", true, 255);
        String email = form.email("email");
        String role = form.option("role", "role", Options.UserRole.values(), true);
        String password = form.password("password", id == null);
        form.unique("email", "email", email, "users", "email", id, null, null);
        if (id != null && role != null && !role.equals("admin") && isLastAdmin(id)) {
            form.error("role", "The last admin cannot be given another role.");
        }
        if (!form.ok()) {
            invalid(req, resp, form, id, "users/form", id == null ? "New User" : "Edit User");
            return;
        }
        if (id == null) {
            Db.update("insert into users (name, email, password, role) values (?, ?, ?, ?)",
                    name, email, BCrypt.hashpw(password, BCrypt.gensalt()), role);
            flash(req, "The user was created successfully.");
        } else {
            Db.update("update users set name = ?, email = ?, role = ?, updated_at = now() where id = ?", name, email, role, id);
            if (password != null) {
                Db.update("update users set password = ? where id = ?", BCrypt.hashpw(password, BCrypt.gensalt()), id);
            }
            flash(req, "The user was updated successfully.");
        }
        redirect(req, resp, "/users");
    }

    @Override
    protected void destroy(HttpServletRequest req, HttpServletResponse resp, long id) throws IOException {
        findOr404("select id from users where id = ?", id);
        if (id == userId(req)) {
            flashError(req, "You cannot delete your own account here. Use your profile instead.");
        } else if (isLastAdmin(id)) {
            flashError(req, "The last admin cannot be deleted.");
        } else {
            Db.update("delete from users where id = ?", id);
            flash(req, "The user was deleted successfully.");
        }
        redirect(req, resp, "/users");
    }

    static boolean isLastAdmin(long id) {
        return Db.count("select count(*) from users where id = ? and role = 'admin'", id) > 0
                && Db.count("select count(*) from users where role = 'admin'") == 1;
    }
}
