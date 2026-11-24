package com.harvesta.web;

import com.harvesta.Db;
import com.harvesta.Options;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * F-10 Reports (admins only): harvest summary, financial summary, and care activity for a period,
 * with charts and a streamed CSV export of each report (nothing is written to disk).
 */
@WebServlet("/reports/*")
public class ReportServlet extends BaseServlet {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    @Override
    protected void dispatch(HttpServletRequest req, HttpServletResponse resp, String[] parts, boolean post)
            throws ServletException, IOException {
        requireAdmin(req);
        if (post || parts.length > 1) {
            throw HttpError.notFound();
        }
        LocalDate to = dateParam(req, "to") != null ? dateParam(req, "to") : LocalDate.now();
        LocalDate from = dateParam(req, "from") != null ? dateParam(req, "from")
                : LocalDate.now().withDayOfMonth(1).minusMonths(11);
        if (from.isAfter(to)) {
            LocalDate swap = from;
            from = to;
            to = swap;
        }
        String report = param(req, "report");
        report = "financial".equals(report) || "care".equals(report) ? report : "harvest";
        String groupBy = param(req, "group_by");
        groupBy = "tree_type".equals(groupBy) || "month".equals(groupBy) ? groupBy : "block";

        if (parts.length == 1) {
            if (!parts[0].equals("export")) {
                throw HttpError.notFound();
            }
            export(resp, report, from, to, groupBy);
            return;
        }

        req.setAttribute("from", from);
        req.setAttribute("to", to);
        req.setAttribute("report", report);
        req.setAttribute("groupBy", groupBy);
        List<Map<String, Object>> harvest = harvestSummary(from, to, groupBy);
        List<Map<String, Object>> financial = financialByMonth(from, to);
        List<Map<String, Object>> care = careByActivity(from, to);
        req.setAttribute("harvest", harvest);
        req.setAttribute("financial", financial);
        req.setAttribute("financialBlocks", financialByBlock(from, to));
        req.setAttribute("care", care);
        req.setAttribute("harvestChart", Map.of("labels", column(harvest, "label"), "kg", column(harvest, "kg"),
                "pcs", column(harvest, "pcs")));
        req.setAttribute("financeChart", Map.of("labels", column(financial, "label"),
                "sales", column(financial, "sales"), "expenses", column(financial, "expenses")));
        req.setAttribute("careChart", Map.of("labels", column(care, "label"), "values", column(care, "total")));
        req.setAttribute("totals", Map.of(
                "kg", sum(harvest, "kg"), "pcs", sum(harvest, "pcs"), "harvests", sum(harvest, "harvests"),
                "sales", sum(financial, "sales"), "expenses", sum(financial, "expenses"),
                "net", sum(financial, "net"), "care", sum(care, "total")));
        render(req, resp, "reports/index", "Reports");
    }

    private static List<Object> column(List<Map<String, Object>> rows, String key) {
        return rows.stream().map(r -> r.get(key)).toList();
    }

    private static BigDecimal sum(List<Map<String, Object>> rows, String key) {
        return rows.stream().map(r -> new BigDecimal(r.get(key).toString())).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    static List<Map<String, Object>> harvestSummary(LocalDate from, LocalDate to, String groupBy) {
        String bucket = switch (groupBy) {
            case "month" -> "to_char(h.harvested_on, 'YYYY-MM')";
            case "tree_type" -> "tt.name";
            default -> "b.name";
        };
        List<Map<String, Object>> rows = Db.list("select " + bucket + " as label, count(*) as harvests, "
                + "coalesce(sum(case when h.unit = 'kg' then h.quantity end), 0) as kg, "
                + "coalesce(sum(case when h.unit = 'pcs' then h.quantity end), 0) as pcs "
                + "from harvests h join trees t on t.id = h.tree_id join tree_types tt on tt.id = t.tree_type_id "
                + "join blocks b on b.id = t.block_id where h.harvested_on between ? and ? "
                + "group by 1 order by 1", from, to);
        if (groupBy.equals("month")) {
            rows.forEach(r -> r.put("label", LocalDate.parse(r.get("label") + "-01").format(MONTH)));
        }
        return rows;
    }

    static List<Map<String, Object>> financialByMonth(LocalDate from, LocalDate to) {
        Map<String, Object> sales = byKey(Db.list("select to_char(sold_on, 'YYYY-MM') as k, sum(total_amount) as v "
                + "from sales where sold_on between ? and ? group by 1", from, to));
        Map<String, Object> expenses = byKey(Db.list("select to_char(spent_on, 'YYYY-MM') as k, sum(amount) as v "
                + "from expenses where spent_on between ? and ? group by 1", from, to));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (LocalDate month = from.withDayOfMonth(1); !month.isAfter(to); month = month.plusMonths(1)) {
            String key = month.toString().substring(0, 7);
            rows.add(row(month.format(MONTH), sales.get(key), expenses.get(key)));
        }
        return rows;
    }

    static List<Map<String, Object>> financialByBlock(LocalDate from, LocalDate to) {
        Map<String, Object> sales = byKey(Db.list("select coalesce(b.name, '') as k, sum(s.total_amount) as v "
                + "from sales s join harvests h on h.id = s.harvest_id join trees t on t.id = h.tree_id "
                + "join blocks b on b.id = t.block_id where s.sold_on between ? and ? group by 1", from, to));
        Map<String, Object> expenses = byKey(Db.list("select coalesce(b.name, '') as k, sum(e.amount) as v "
                + "from expenses e left join blocks b on b.id = e.block_id where e.spent_on between ? and ? group by 1",
                from, to));
        List<String> keys = new ArrayList<>(sales.keySet());
        expenses.keySet().stream().filter(k -> !keys.contains(k)).forEach(keys::add);
        keys.sort((a, b) -> a.isEmpty() ? 1 : b.isEmpty() ? -1 : a.compareTo(b));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String key : keys) {
            rows.add(row(key.isEmpty() ? "General (no block)" : key, sales.get(key), expenses.get(key)));
        }
        return rows;
    }

    static List<Map<String, Object>> careByActivity(LocalDate from, LocalDate to) {
        List<Map<String, Object>> rows = Db.list("select activity_type, count(*) as total, max(performed_on) as last_performed "
                + "from care_logs where performed_on between ? and ? group by activity_type order by total desc, activity_type",
                from, to);
        rows.forEach(r -> r.put("label", Options.label(r.get("activity_type"))));
        return rows;
    }

    private static Map<String, Object> row(String label, Object sales, Object expenses) {
        BigDecimal s = sales == null ? BigDecimal.ZERO : (BigDecimal) sales;
        BigDecimal e = expenses == null ? BigDecimal.ZERO : (BigDecimal) expenses;
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("label", label);
        row.put("sales", s);
        row.put("expenses", e);
        row.put("net", s.subtract(e));
        return row;
    }

    private static Map<String, Object> byKey(List<Map<String, Object>> rows) {
        Map<String, Object> map = new LinkedHashMap<>();
        rows.forEach(r -> map.put((String) r.get("k"), r.get("v")));
        return map;
    }

    private static void export(HttpServletResponse resp, String report, LocalDate from, LocalDate to, String groupBy)
            throws IOException {
        resp.setContentType("text/csv; charset=UTF-8");
        resp.setHeader("Content-Disposition",
                "attachment; filename=\"" + report + "-report-" + from + "-to-" + to + ".csv\"");
        PrintWriter out = resp.getWriter();
        switch (report) {
            case "financial" -> {
                csv(out, "Group", "Name", "Sales (IDR)", "Expenses (IDR)", "Net (IDR)");
                financialByMonth(from, to).forEach(r -> csv(out, "Month", r.get("label"), r.get("sales"), r.get("expenses"), r.get("net")));
                financialByBlock(from, to).forEach(r -> csv(out, "Block", r.get("label"), r.get("sales"), r.get("expenses"), r.get("net")));
            }
            case "care" -> {
                csv(out, "Activity", "Care logs", "Last performed");
                careByActivity(from, to).forEach(r -> csv(out, r.get("label"), r.get("total"), r.get("last_performed")));
            }
            default -> {
                csv(out, groupBy.equals("tree_type") ? "Tree type" : groupBy.equals("month") ? "Month" : "Block",
                        "Harvests", "Quantity (kg)", "Quantity (pcs)");
                harvestSummary(from, to, groupBy).forEach(r -> csv(out, r.get("label"), r.get("harvests"), r.get("kg"), r.get("pcs")));
            }
        }
        out.flush();
    }

    /** One CSV line; text cells are quoted, and cells that look like formulas are neutralized. */
    static void csv(PrintWriter out, Object... cells) {
        List<String> values = new ArrayList<>();
        for (Object cell : cells) {
            if (cell instanceof Number) {
                values.add(cell instanceof BigDecimal d ? d.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() : cell.toString());
                continue;
            }
            String text = cell == null ? "" : cell.toString();
            if (!text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0) {
                text = "'" + text;
            }
            values.add("\"" + text.replace("\"", "\"\"") + "\"");
        }
        out.print(String.join(",", values) + "\r\n");
    }
}
