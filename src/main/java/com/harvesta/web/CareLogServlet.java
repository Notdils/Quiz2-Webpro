package com.harvesta.web;

import com.harvesta.Db;
import com.harvesta.Options;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * F-06 Care Logs: staff create records and edit only their own; admins can do everything.
 * Dead trees do not accept new care logs.
 */
@WebServlet("/care-logs/*")
public class CareLogServlet extends BaseServlet {

    static final String FROM = "from care_logs cl join trees t on t.id = cl.tree_id and t.deleted_at is null "
            + "join tree_types tt on tt.id = t.tree_type_id join blocks b on b.id = t.block_id "
            + "left join users u on u.id = cl.user_id";

    @Override
    protected void index(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Query q = new Query()
                .filter(longParam(req, "tree_id"), "cl.tree_id = ?")
                .filter(longParam(req, "block_id"), "t.block_id = ?")
                .filter(param(req, "activity_type"), "cl.activity_type = ?")
                .filter(dateParam(req, "from"), "cl.performed_on >= ?")
                .filter(dateParam(req, "to"), "cl.performed_on <= ?");
        req.setAttribute("page", q.page(req, "cl.*, t.code as tree_code, tt.name as type_name, b.name as block_name, "
                + "u.name as user_name", FROM, "cl.performed_on desc, cl.id desc"));
        req.setAttribute("trees", trees(false));
        req.setAttribute("blocks", Db.list("select id, name from blocks order by name"));
        render(req, resp, "care-logs/index", "Care Logs");
    }

    @Override
    protected void create(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        requireWriter(req);
        Map<String, Object> f = new HashMap<>();
        f.put("tree_id", param(req, "tree_id"));
        f.put("performed_on", LocalDate.now());
        req.setAttribute("f", f);
        req.setAttribute("trees", trees(true));
        render(req, resp, "care-logs/form", "New Care Log");
    }

    @Override
    protected void edit(HttpServletRequest req, HttpServletResponse resp, long id)
            throws ServletException, IOException {
        Map<String, Object> row = findOr404("select * from care_logs where id = ?", id);
        requireOwnerOrAdmin(req, row);
        req.setAttribute("f", row);
        req.setAttribute("trees", trees(true));
        render(req, resp, "care-logs/form", "Edit Care Log");
    }

    @Override
    protected void store(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        requireWriter(req);
        save(req, resp, null, null);
    }

    @Override
    protected void update(HttpServletRequest req, HttpServletResponse resp, long id)
            throws ServletException, IOException {
        Map<String, Object> row = findOr404("select * from care_logs where id = ?", id);
        requireOwnerOrAdmin(req, row);
        save(req, resp, id, row.get("tree_id"));
    }

    private void save(HttpServletRequest req, HttpServletResponse resp, Long id, Object currentTreeId)
            throws ServletException, IOException {
        Form form = new Form(req);
        Long treeId = form.id("tree_id", "tree", true, "trees");
        String activity = form.option("activity_type", "activity type", Options.ActivityType.values(), true);
        LocalDate performedOn = form.date("performed_on", "performed on date", true, true);
        LocalDate nextDueOn = form.date("next_due_on", "next due date", false, false);
        String notes = form.text("notes", "notes", false, 5000);
        if (performedOn != null && nextDueOn != null && nextDueOn.isBefore(performedOn)) {
            form.error("next_due_on", "The next due date must be on or after the performed on date.");
        }
        // Moving a record to a dead tree (or adding one) is blocked; existing records may still be edited.
        if (treeId != null && !treeId.toString().equals(String.valueOf(currentTreeId)) && isDead(treeId)) {
            form.error("tree_id", "This tree is dead and cannot receive new care logs.");
        }
        if (!form.ok()) {
            req.setAttribute("trees", trees(true));
            invalid(req, resp, form, id, "care-logs/form", id == null ? "New Care Log" : "Edit Care Log");
            return;
        }
        if (id == null) {
            Db.update("insert into care_logs (tree_id, activity_type, performed_on, next_due_on, notes, user_id) "
                    + "values (?, ?, ?, ?, ?, ?)", treeId, activity, performedOn, nextDueOn, notes, userId(req));
            flash(req, "The care log was created successfully.");
        } else {
            Db.update("update care_logs set tree_id = ?, activity_type = ?, performed_on = ?, next_due_on = ?, "
                    + "notes = ?, updated_at = now() where id = ?", treeId, activity, performedOn, nextDueOn, notes, id);
            flash(req, "The care log was updated successfully.");
        }
        String back = req.getParameter("back");
        redirect(req, resp, back != null && back.matches("/trees/\\d+") ? back : "/care-logs");
    }

    @Override
    protected void destroy(HttpServletRequest req, HttpServletResponse resp, long id) throws IOException {
        requireAdmin(req);
        findOr404("select id from care_logs where id = ?", id);
        Db.update("delete from care_logs where id = ?", id);
        flash(req, "The care log was deleted successfully.");
        redirect(req, resp, "/care-logs");
    }

    static boolean isDead(long treeId) {
        return Db.count("select count(*) from trees where id = ? and status = 'dead'", treeId) > 0;
    }

    /** Trees for select boxes; forms show living trees only. */
    static Object trees(boolean livingOnly) {
        return Db.list("select t.id, t.code, tt.name as type_name from trees t "
                + "join tree_types tt on tt.id = t.tree_type_id where t.deleted_at is null"
                + (livingOnly ? " and t.status <> 'dead'" : "") + " order by t.code");
    }
}
