package com.harvesta.web;

import com.harvesta.Db;
import com.harvesta.Options;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/** F-08 Expenses (admins only). An empty block means a general expense. */
@WebServlet("/expenses/*")
public class ExpenseServlet extends BaseServlet {

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
        Query q = new Query()
                .search(param(req, "search"), "coalesce(e.description, '')")
                .filter(param(req, "category"), "e.category = ?")
                .filter(dateParam(req, "from"), "e.spent_on >= ?")
                .filter(dateParam(req, "to"), "e.spent_on <= ?");
        String block = param(req, "block_id");
        if ("general".equals(block)) {
            q.where("e.block_id is null");
        } else {
            q.filter(longParam(req, "block_id"), "e.block_id = ?");
        }
        String from = "from expenses e left join blocks b on b.id = e.block_id left join users u on u.id = e.user_id";
        req.setAttribute("page", q.page(req, "e.*, b.name as block_name, u.name as user_name", from,
                "e.spent_on desc, e.id desc"));
        req.setAttribute("total", Db.value("select coalesce(sum(e.amount), 0) " + from + q.sql(), q.args()));
        req.setAttribute("blocks", Db.list("select id, name from blocks order by name"));
        render(req, resp, "expenses/index", "Expenses");
    }

    @Override
    protected void create(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, Object> f = new HashMap<>();
        f.put("spent_on", LocalDate.now());
        req.setAttribute("f", f);
        req.setAttribute("blocks", Db.list("select id, name from blocks order by name"));
        render(req, resp, "expenses/form", "New Expense");
    }

    @Override
    protected void edit(HttpServletRequest req, HttpServletResponse resp, long id)
            throws ServletException, IOException {
        req.setAttribute("f", findOr404("select * from expenses where id = ?", id));
        req.setAttribute("blocks", Db.list("select id, name from blocks order by name"));
        render(req, resp, "expenses/form", "Edit Expense");
    }

    @Override
    protected void store(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        save(req, resp, null);
    }

    @Override
    protected void update(HttpServletRequest req, HttpServletResponse resp, long id)
            throws ServletException, IOException {
        findOr404("select id from expenses where id = ?", id);
        save(req, resp, id);
    }

    private void save(HttpServletRequest req, HttpServletResponse resp, Long id) throws ServletException, IOException {
        Form form = new Form(req);
        Long blockId = form.id("block_id", "block", false, "blocks");
        String category = form.option("category", "category", Options.ExpenseCategory.values(), true);
        BigDecimal amount = form.decimal("amount", "amount", true, true);
        LocalDate spentOn = form.date("spent_on", "date", true, true);
        String description = form.text("description", "description", false, 255);
        if (!form.ok()) {
            req.setAttribute("blocks", Db.list("select id, name from blocks order by name"));
            invalid(req, resp, form, id, "expenses/form", id == null ? "New Expense" : "Edit Expense");
            return;
        }
        if (id == null) {
            Db.update("insert into expenses (block_id, category, amount, spent_on, description, user_id) "
                    + "values (?, ?, ?, ?, ?, ?)", blockId, category, amount, spentOn, description, userId(req));
            flash(req, "The expense was created successfully.");
        } else {
            Db.update("update expenses set block_id = ?, category = ?, amount = ?, spent_on = ?, description = ?, "
                    + "updated_at = now() where id = ?", blockId, category, amount, spentOn, description, id);
            flash(req, "The expense was updated successfully.");
        }
        redirect(req, resp, "/expenses");
    }

    @Override
    protected void destroy(HttpServletRequest req, HttpServletResponse resp, long id) throws IOException {
        findOr404("select id from expenses where id = ?", id);
        Db.update("delete from expenses where id = ?", id);
        flash(req, "The expense was deleted successfully.");
        redirect(req, resp, "/expenses");
    }
}
