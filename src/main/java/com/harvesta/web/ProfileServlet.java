package com.harvesta.web;

import com.harvesta.Db;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * F-12 Profile: change name/email, change password, delete own account
 * (not allowed for the last admin).
 * GET /profile, POST /profile, POST /profile/password, POST /profile/delete
 */
@WebServlet("/profile/*")
public class ProfileServlet extends BaseServlet {

    @Override
    protected void dispatch(HttpServletRequest req, HttpServletResponse resp, String[] parts, boolean post)
            throws ServletException, IOException {
        String action = parts.length == 0 ? "" : parts.length == 1 ? parts[0] : null;
        if (action == null || !post && !action.isEmpty()) {
            throw HttpError.notFound();
        }
        long id = userId(req);
        if (!post) {
            show(req, resp, new HashMap<>(user(req)), new HashMap<>());
            return;
        }
        Form form = new Form(req);
        switch (action) {
            case "" -> {
                String name = form.text("name", "name", true, 255);
                String email = form.email("email");
                form.unique("email", "email", email, "users", "email", id, null, null);
                if (!form.ok()) {
                    show(req, resp, form.values, form.errors);
                    return;
                }
                Db.update("update users set name = ?, email = ?, updated_at = now() where id = ?", name, email, id);
                flash(req, "Your profile was updated successfully.");
            }
            case "password" -> {
                checkCurrentPassword(form, id, "current_password");
                String password = form.password("password", true);
                if (!form.ok()) {
                    show(req, resp, new HashMap<>(user(req)), form.errors);
                    return;
                }
                Db.update("update users set password = ?, updated_at = now() where id = ?",
                        BCrypt.hashpw(password, BCrypt.gensalt()), id);
                flash(req, "Your password was updated successfully.");
            }
            case "delete" -> {
                checkCurrentPassword(form, id, "delete_password");
                if (UserServlet.isLastAdmin(id)) {
                    form.error("delete_password", "The last admin cannot delete their account.");
                }
                if (!form.ok()) {
                    show(req, resp, new HashMap<>(user(req)), form.errors);
                    return;
                }
                Db.update("delete from users where id = ?", id);
                req.getSession().invalidate();
                redirect(req, resp, "/login");
                return;
            }
            default -> throw HttpError.notFound();
        }
        redirect(req, resp, "/profile");
    }

    private void show(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> f, Map<String, String> errors)
            throws ServletException, IOException {
        req.setAttribute("f", f);
        req.setAttribute("errors", errors);
        if (!errors.isEmpty()) {
            resp.setStatus(422);
        }
        render(req, resp, "profile", "Profile");
    }

    private static void checkCurrentPassword(Form form, long id, String field) {
        String given = form.raw(field);
        String hash = (String) Db.value("select password from users where id = ?", id);
        if (given == null || !BCrypt.checkpw(given, hash)) {
            form.error(field, "The current password is incorrect.");
        }
    }
}
