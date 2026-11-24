package com.harvesta.web;

import com.harvesta.App;
import com.harvesta.Db;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;

/**
 * Runs before every request: loads the signed-in user, sends guests to /login,
 * issues the CSRF token, moves flash messages into the request, and sets security headers.
 */
@WebFilter("/*")
public class AuthFilter implements Filter {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        req.setCharacterEncoding("UTF-8");
        String ctx = req.getContextPath();
        String path = req.getRequestURI().substring(ctx.length());

        if (path.startsWith("/assets/")) {
            chain.doFilter(req, resp);
            return;
        }

        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Referrer-Policy", "same-origin");

        HttpSession session = req.getSession();
        if (session.getAttribute("csrf") == null) {
            byte[] token = new byte[32];
            RANDOM.nextBytes(token);
            session.setAttribute("csrf", HexFormat.of().formatHex(token));
        }
        req.setAttribute("csrf", session.getAttribute("csrf"));
        req.setAttribute("ctx", ctx);
        req.setAttribute("appName", App.NAME);
        req.setAttribute("path", path);

        Object userId = session.getAttribute("userId");
        Map<String, Object> user = userId == null ? null
                : Db.one("select id, name, email, role from users where id = ?", userId);
        boolean guestPage = path.equals("/login") || path.equals("/register");

        if (user == null) {
            session.removeAttribute("userId");
            if (!guestPage) {
                resp.sendRedirect(ctx + "/login");
                return;
            }
        } else {
            if (guestPage || path.equals("/") || path.isEmpty()) {
                resp.sendRedirect(ctx + "/dashboard");
                return;
            }
            req.setAttribute("user", user);
            req.setAttribute("isAdmin", "admin".equals(user.get("role")));
            req.setAttribute("canWrite", !"viewer".equals(user.get("role")));
        }

        for (String key : new String[]{"flash", "flashError"}) {
            Object message = session.getAttribute(key);
            if (message != null) {
                req.setAttribute(key, message);
                session.removeAttribute(key);
            }
        }

        chain.doFilter(req, resp);
    }
}
