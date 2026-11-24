package com.harvesta.web;

import com.harvesta.Db;
import com.harvesta.SaleService;
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
 * F-09 Sales (admins only). total_amount = quantity x unit price, computed here.
 * The sum of sales of one harvest may never exceed its quantity (checked under a row lock).
 */
@WebServlet("/sales/*")
public class SaleServlet extends BaseServlet {

    private static final String FROM = "from sales s join harvests h on h.id = s.harvest_id "
            + "join trees t on t.id = h.tree_id join tree_types tt on tt.id = t.tree_type_id "
            + "left join users u on u.id = s.user_id";

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
                .search(param(req, "search"), "s.buyer_name")
                .filter(longParam(req, "tree_type_id"), "t.tree_type_id = ?")
                .filter(dateParam(req, "from"), "s.sold_on >= ?")
                .filter(dateParam(req, "to"), "s.sold_on <= ?");
        req.setAttribute("page", q.page(req, "s.*, h.unit, h.harvested_on, t.id as tree_id, t.code as tree_code, "
                + "tt.name as type_name, u.name as user_name", FROM, "s.sold_on desc, s.id desc"));
        req.setAttribute("total", Db.value("select coalesce(sum(s.total_amount), 0) " + FROM + q.sql(), q.args()));
        req.setAttribute("treeTypes", Db.list("select id, name from tree_types order by name"));
        render(req, resp, "sales/index", "Sales");
    }

    @Override
    protected void create(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, Object> f = new HashMap<>();
        f.put("harvest_id", param(req, "harvest_id"));
        f.put("sold_on", LocalDate.now());
        req.setAttribute("f", f);
        req.setAttribute("harvests", harvests(null));
        render(req, resp, "sales/form", "New Sale");
    }

    @Override
    protected void edit(HttpServletRequest req, HttpServletResponse resp, long id)
            throws ServletException, IOException {
        req.setAttribute("f", findOr404("select * from sales where id = ?", id));
        req.setAttribute("harvests", harvests(id));
        render(req, resp, "sales/form", "Edit Sale");
    }

    @Override
    protected void store(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        save(req, resp, null);
    }

    @Override
    protected void update(HttpServletRequest req, HttpServletResponse resp, long id)
            throws ServletException, IOException {
        findOr404("select id from sales where id = ?", id);
        save(req, resp, id);
    }

    private void save(HttpServletRequest req, HttpServletResponse resp, Long id) throws ServletException, IOException {
        Form form = new Form(req);
        Long harvestId = form.id("harvest_id", "harvest", true, "harvests");
        String buyer = form.text("buyer_name", "buyer name", true, 150);
        LocalDate soldOn = form.date("sold_on", "sale date", true, true);
        BigDecimal quantity = form.decimal("quantity", "quantity", true, true);
        BigDecimal unitPrice = form.decimal("unit_price", "unit price", true, true);
        String notes = form.text("notes", "notes", false, 5000);

        boolean saved = form.ok() && Db.tx(() -> {
            String error = SaleService.check(harvestId, quantity, id);
            if (error != null) {
                form.error("quantity", error);
                return false;
            }
            BigDecimal total = SaleService.total(quantity, unitPrice);
            if (id == null) {
                Db.update("insert into sales (harvest_id, buyer_name, sold_on, quantity, unit_price, total_amount, notes, "
                                + "user_id) values (?, ?, ?, ?, ?, ?, ?, ?)",
                        harvestId, buyer, soldOn, quantity, unitPrice, total, notes, userId(req));
            } else {
                Db.update("update sales set harvest_id = ?, buyer_name = ?, sold_on = ?, quantity = ?, unit_price = ?, "
                                + "total_amount = ?, notes = ?, updated_at = now() where id = ?",
                        harvestId, buyer, soldOn, quantity, unitPrice, total, notes, id);
            }
            return true;
        });
        if (!saved) {
            req.setAttribute("harvests", harvests(id));
            invalid(req, resp, form, id, "sales/form", id == null ? "New Sale" : "Edit Sale");
            return;
        }
        flash(req, id == null ? "The sale was created successfully." : "The sale was updated successfully.");
        redirect(req, resp, "/sales");
    }

    @Override
    protected void destroy(HttpServletRequest req, HttpServletResponse resp, long id) throws IOException {
        findOr404("select id from sales where id = ?", id);
        Db.update("delete from sales where id = ?", id);
        flash(req, "The sale was deleted successfully.");
        redirect(req, resp, "/sales");
    }

    /** Harvests that still have stock left (plus the one of the sale being edited), with the remaining amount. */
    private static Object harvests(Long saleId) {
        return Db.list("select * from (select h.id, h.harvested_on, h.unit, t.code as tree_code, tt.name as type_name, "
                + "h.quantity - coalesce((select sum(s.quantity) from sales s where s.harvest_id = h.id and s.id <> ?), 0) "
                + "as remaining from harvests h join trees t on t.id = h.tree_id "
                + "join tree_types tt on tt.id = t.tree_type_id) x "
                + "where x.remaining > 0 or x.id = (select harvest_id from sales where id = ?) "
                + "order by x.harvested_on desc, x.id desc",
                saleId == null ? -1L : saleId, saleId == null ? -1L : saleId);
    }
}
