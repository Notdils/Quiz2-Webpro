package com.harvesta.web;

import com.harvesta.Db;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * Controller base class. A servlet mapped to "/things/*" gets resource-style routes:
 * <pre>
 * GET  /things            index      POST /things             store
 * GET  /things/create     create     GET  /things/{id}        show
 * GET  /things/{id}/edit  edit       POST /things/{id}        update
 * POST /things/{id}/delete destroy   anything else            other(...)
 * </pre>
 * Every POST must carry the session CSRF token in "_csrf".
 */
public abstract class BaseServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        boolean post = "POST".equalsIgnoreCase(req.getMethod());
        try {
            if (post) {
                verifyCsrf(req);
            }
            String path = req.getPathInfo() == null ? "" : req.getPathInfo().replaceAll("^/+|/+$", "");
            dispatch(req, resp, path.isEmpty() ? new String[0] : path.split("/"), post);
        } catch (HttpError e) {
            resp.sendError(e.status);
        }
    }

    protected void dispatch(HttpServletRequest req, HttpServletResponse resp, String[] parts, boolean post)
            throws ServletException, IOException {
        if (parts.length == 0) {
            if (post) {
                store(req, resp);
            } else {
                index(req, resp);
            }
            return;
        }
        if (parts.length == 1 && parts[0].equals("create") && !post) {
            create(req, resp);
            return;
        }
        if (parts.length > 2) {
            throw HttpError.notFound();
        }
        long id = parseId(parts[0]);
        String action = parts.length == 2 ? parts[1] : "";
        switch (action + (post ? ":post" : ":get")) {
            case ":get" -> show(req, resp, id);
            case "edit:get" -> edit(req, resp, id);
            case ":post" -> update(req, resp, id);
            case "delete:post" -> destroy(req, resp, id);
            default -> other(req, resp, id, action, post);
        }
    }

    protected void index(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        throw HttpError.notFound();
    }

    protected void create(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        throw HttpError.notFound();
    }

    protected void store(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        throw HttpError.notFound();
    }

    protected void show(HttpServletRequest req, HttpServletResponse resp, long id)
            throws ServletException, IOException {
        throw HttpError.notFound();
    }

    protected void edit(HttpServletRequest req, HttpServletResponse resp, long id)
            throws ServletException, IOException {
        throw HttpError.notFound();
    }

    protected void update(HttpServletRequest req, HttpServletResponse resp, long id)
            throws ServletException, IOException {
        throw HttpError.notFound();
    }

    protected void destroy(HttpServletRequest req, HttpServletResponse resp, long id)
            throws ServletException, IOException {
        throw HttpError.notFound();
    }

    protected void other(HttpServletRequest req, HttpServletResponse resp, long id, String action, boolean post)
            throws ServletException, IOException {
        throw HttpError.notFound();
    }

    // ---- Rendering -------------------------------------------------------------------------------

    protected void render(HttpServletRequest req, HttpServletResponse resp, String view, String title)
            throws ServletException, IOException {
        req.setAttribute("title", title);
        // "Back to tree" links: only a local tree path is accepted (no open redirect / javascript: URLs).
        String back = req.getParameter("back");
        req.setAttribute("back", back != null && back.matches("/trees/\\d+") ? back : null);
        req.getRequestDispatcher("/WEB-INF/views/" + view + ".jsp").forward(req, resp);
    }

    /** Re-renders a form with its validation errors and the submitted values. */
    protected void invalid(HttpServletRequest req, HttpServletResponse resp, Form form, Long id, String view,
                           String title) throws ServletException, IOException {
        if (id != null) {
            form.values.put("id", id);
        }
        req.setAttribute("f", form.values);
        req.setAttribute("errors", form.errors);
        resp.setStatus(422);
        render(req, resp, view, title);
    }

    protected void redirect(HttpServletRequest req, HttpServletResponse resp, String path) throws IOException {
        resp.sendRedirect(req.getContextPath() + path);
    }

    protected static void flash(HttpServletRequest req, String message) {
        req.getSession().setAttribute("flash", message);
    }

    protected static void flashError(HttpServletRequest req, String message) {
        req.getSession().setAttribute("flashError", message);
    }

    // ---- Current user and authorization ------------------------------------------------------------

    @SuppressWarnings("unchecked")
    protected static Map<String, Object> user(HttpServletRequest req) {
        return (Map<String, Object>) req.getAttribute("user");
    }

    protected static long userId(HttpServletRequest req) {
        return ((Number) user(req).get("id")).longValue();
    }

    protected static boolean isAdmin(HttpServletRequest req) {
        return "admin".equals(user(req).get("role"));
    }

    protected static boolean isViewer(HttpServletRequest req) {
        return "viewer".equals(user(req).get("role"));
    }

    protected static void requireAdmin(HttpServletRequest req) {
        if (!isAdmin(req)) {
            throw HttpError.forbidden();
        }
    }

    /** Staff and admins may write; viewers are read-only. */
    protected static void requireWriter(HttpServletRequest req) {
        if (isViewer(req)) {
            throw HttpError.forbidden();
        }
    }

    /** Admins edit everything; staff only the records they created. */
    protected static void requireOwnerOrAdmin(HttpServletRequest req, Map<String, Object> row) {
        requireWriter(req);
        Object owner = row.get("user_id");
        if (!isAdmin(req) && (owner == null || ((Number) owner).longValue() != userId(req))) {
            throw HttpError.forbidden();
        }
    }

    // ---- Input -----------------------------------------------------------------------------------

    protected static Map<String, Object> findOr404(String sql, long id) {
        Map<String, Object> row = Db.one(sql, id);
        if (row == null) {
            throw HttpError.notFound();
        }
        return row;
    }

    protected static String param(HttpServletRequest req, String name) {
        String value = req.getParameter(name);
        return value == null || value.isBlank() ? null : value.trim();
    }

    protected static Long longParam(HttpServletRequest req, String name) {
        try {
            String value = param(req, name);
            return value == null ? null : Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected static LocalDate dateParam(HttpServletRequest req, String name) {
        try {
            String value = param(req, name);
            return value == null ? null : LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static long parseId(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw HttpError.notFound();
        }
    }

    private static void verifyCsrf(HttpServletRequest req) {
        Object expected = req.getSession().getAttribute("csrf");
        String given = req.getParameter("_csrf");
        if (expected == null || given == null || !MessageDigest.isEqual(
                expected.toString().getBytes(StandardCharsets.UTF_8), given.getBytes(StandardCharsets.UTF_8))) {
            throw new HttpError(419);
        }
    }
}
