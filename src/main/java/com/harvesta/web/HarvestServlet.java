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

/**
 * F-07 Harvests: staff create records and edit only their own; admins can do everything.
 * A harvest with sales cannot be deleted, and its quantity cannot drop below what was sold.
 */
@WebServlet("/harvests/*")
public class HarvestServlet extends BaseServlet {

    private static final String FROM = "from harvests h join trees t on t.id = h.tree_id and t.deleted_at is null "
            + "join tree_types tt on tt.id = t.tree_type_id join blocks b on b.id = t.block_id "
            + "left join users u on u.id = h.user_id";

    @Override
    protected void index(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Query q = new Query()
                .filter(longParam(req, "tree_id"), "h.tree_id = ?")
                .filter(longParam(req, "block_id"), "t.block_id = ?")
                .filter(longParam(req, "tree_type_id"), "t.tree_type_id = ?")
                .filter(param(req, "grade"), "h.grade = ?")
                .filter(dateParam(req, "from"), "h.harvested_on >= ?")
                .filter(dateParam(req, "to"), "h.harvested_on <= ?");
        req.setAttribute("page", q.page(req, "h.*, t.code as tree_code, tt.name as type_name, b.name as block_name, "
                + "u.name as user_name, (select coalesce(sum(s.quantity), 0) from sales s where s.harvest_id = h.id) as sold",
                FROM, "h.harvested_on desc, h.id desc"));
        req.setAttribute("totals", Db.one("select "
                + "coalesce(sum(case when h.unit = 'kg' then h.quantity end), 0) as kg, "
                + "coalesce(sum(case when h.unit = 'pcs' then h.quantity end), 0) as pcs " + FROM + q.sql(), q.args()));
        req.setAttribute("trees", CareLogServlet.trees(false));
        req.setAttribute("blocks", Db.list("select id, name from blocks order by name"));
        req.setAttribute("treeTypes", Db.list("select id, name from tree_types order by name"));
        render(req, resp, "harvests/index", "Harvests");
    }

    @Override
    protected void create(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        requireWriter(req);
        Map<String, Object> f = new HashMap<>();
        f.put("tree_id", param(req, "tree_id"));
        f.put("harvested_on", LocalDate.now());
        f.put("unit", "kg");
        f.put("grade", "good");
        req.setAttribute("f", f);
        req.setAttribute("trees", CareLogServlet.trees(true));
        render(req, resp, "harvests/form", "New Harvest");
    }

    @Override
    protected void edit(HttpServletRequest req, HttpServletResponse resp, long id)
            throws ServletException, IOException {
        Map<String, Object> row = findOr404("select * from harvests where id = ?", id);
        requireOwnerOrAdmin(req, row);
        req.setAttribute("f", row);
        req.setAttribute("trees", CareLogServlet.trees(true));
        render(req, resp, "harvests/form", "Edit Harvest");
    }

    @Override
    protected void store(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        requireWriter(req);
        save(req, resp, null, null);
    }

    @Override
    protected void update(HttpServletRequest req, HttpServletResponse resp, long id)
            throws ServletException, IOException {
        Map<String, Object> row = findOr404("select * from harvests where id = ?", id);
        requireOwnerOrAdmin(req, row);
        save(req, resp, id, row.get("tree_id"));
    }

    private void save(HttpServletRequest req, HttpServletResponse resp, Long id, Object currentTreeId)
            throws ServletException, IOException {
        Form form = new Form(req);
        Long treeId = form.id("tree_id", "tree", true, "trees");
        LocalDate harvestedOn = form.date("harvested_on", "harvest date", true, true);
        BigDecimal quantity = form.decimal("quantity", "quantity", true, true);
        String unit = form.option("unit", "unit", Options.HarvestUnit.values(), true);
        String grade = form.option("grade", "grade", Options.HarvestGrade.values(), true);
        String notes = form.text("notes", "notes", false, 5000);
        if (treeId != null && !treeId.toString().equals(String.valueOf(currentTreeId)) && CareLogServlet.isDead(treeId)) {
            form.error("tree_id", "This tree is dead and cannot receive new harvests.");
        }
        if (id != null && quantity != null) {
            BigDecimal sold = (BigDecimal) Db.value("select coalesce(sum(quantity), 0) from sales where harvest_id = ?", id);
            if (quantity.compareTo(sold) < 0) {
                form.error("quantity", "The quantity cannot be less than the amount already sold (" + Fn.qty(sold)
                        + " " + Options.label(unit) + ").");
            }
        }
        if (!form.ok()) {
            req.setAttribute("trees", CareLogServlet.trees(true));
            invalid(req, resp, form, id, "harvests/form", id == null ? "New Harvest" : "Edit Harvest");
            return;
        }
        if (id == null) {
            Db.update("insert into harvests (tree_id, harvested_on, quantity, unit, grade, notes, user_id) "
                    + "values (?, ?, ?, ?, ?, ?, ?)", treeId, harvestedOn, quantity, unit, grade, notes, userId(req));
            flash(req, "The harvest was created successfully.");
        } else {
            Db.update("update harvests set tree_id = ?, harvested_on = ?, quantity = ?, unit = ?, grade = ?, notes = ?, "
                    + "updated_at = now() where id = ?", treeId, harvestedOn, quantity, unit, grade, notes, id);
            flash(req, "The harvest was updated successfully.");
        }
        String back = req.getParameter("back");
        redirect(req, resp, back != null && back.matches("/trees/\\d+") ? back : "/harvests");
    }

    @Override
    protected void destroy(HttpServletRequest req, HttpServletResponse resp, long id) throws IOException {
        requireAdmin(req);
        findOr404("select id from harvests where id = ?", id);
        if (Db.count("select count(*) from sales where harvest_id = ?", id) > 0) {
            flashError(req, "This harvest cannot be deleted because it already has sales.");
        } else {
            Db.update("delete from harvests where id = ?", id);
            flash(req, "The harvest was deleted successfully.");
        }
        redirect(req, resp, "/harvests");
    }
}
