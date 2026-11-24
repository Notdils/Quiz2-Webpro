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

/** Login, registration (new users are Staff), and logout. */
@WebServlet({"/login", "/register", "/logout"})
public class AuthServlet extends BaseServlet {

    // Compared against when the email is unknown, so both cases take the same time.
    private static final String DUMMY_HASH = BCrypt.hashpw("dummy-password", BCrypt.gensalt());

    @Override
    protected void dispatch(HttpServletRequest req, HttpServletResponse resp, String[] parts, boolean post)
            throws ServletException, IOException {
        String page = req.getServletPath().substring(1);
        if (page.equals("logout")) {
            if (!post) {
                throw HttpError.notFound();
            }
            req.getSession().invalidate();
            redirect(req, resp, "/login");
            return;
        }
        if (!post) {
            show(req, resp, page, new HashMap<>(), new HashMap<>());
            return;
        }
        Form form = new Form(req);
        if (page.equals("login")) {
            String email = form.email("email");
            String password = form.raw("password");
            if (password == null) {
                form.error("password", "The password field is required.");
            }
            Map<String, Object> user = form.ok() ? Db.one("select id, password from users where email = ?", email) : null;
            boolean valid = form.ok() && BCrypt.checkpw(password, user == null ? DUMMY_HASH : (String) user.get("password"))
                    && user != null;
            if (!valid) {
                form.error("email", "These credentials do not match our records.");
                form.values.remove("password");
                show(req, resp, page, form.values, form.errors);
                return;
            }
            login(req, user.get("id"));
        } else {
            String name = form.text("name", "name", true, 255);
            String email = form.email("email");
            String password = form.password("password", true);
            form.unique("email", "email", email, "users", "email", null, null, null);
            if (!form.ok()) {
                show(req, resp, page, form.values, form.errors);
                return;
            }
            long id = Db.insert("insert into users (name, email, password, role) values (?, ?, ?, 'staff') returning id",
                    name, email, BCrypt.hashpw(password, BCrypt.gensalt()));
            login(req, id);
            flash(req, "Welcome! Your account was created successfully.");
        }
        redirect(req, resp, "/dashboard");
    }

    private static void login(HttpServletRequest req, Object userId) {
        req.changeSessionId(); // prevents session fixation
        req.getSession().setAttribute("userId", ((Number) userId).longValue());
    }

    private void show(HttpServletRequest req, HttpServletResponse resp, String page, Map<String, Object> f,
                      Map<String, String> errors) throws ServletException, IOException {
        req.setAttribute("f", f);
        req.setAttribute("errors", errors);
        if (!errors.isEmpty()) {
            resp.setStatus(422);
        }
        render(req, resp, "auth/" + page, page.equals("login") ? "Log in" : "Create an account");
    }
}
