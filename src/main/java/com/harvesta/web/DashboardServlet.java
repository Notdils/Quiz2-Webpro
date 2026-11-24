package com.harvesta.web;

import com.harvesta.Db;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** F-01 Dashboard: farm statistics, upcoming care, recent harvests, and 12-month charts (finance for admins). */
@WebServlet("/dashboard")
public class DashboardServlet extends BaseServlet {

    @Override
    protected void index(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
        LocalDate chartStart = monthStart.minusMonths(11);

        Map<String, Object> statuses = new HashMap<>();
        long totalTrees = 0;
        for (Map<String, Object> row : Db.list("select status, count(*) as n from trees where deleted_at is null group by status")) {
            statuses.put((String) row.get("status"), row.get("n"));
            totalTrees += ((Number) row.get("n")).longValue();
        }
        req.setAttribute("statuses", statuses);
        req.setAttribute("totalTrees", totalTrees);
        req.setAttribute("blocksCount", Db.count("select count(*) from blocks"));
        req.setAttribute("monthHarvest", Db.one("select "
                + "coalesce(sum(case when unit = 'kg' then quantity end), 0) as kg, "
                + "coalesce(sum(case when unit = 'pcs' then quantity end), 0) as pcs "
                + "from harvests where harvested_on between ? and ?", monthStart, monthEnd));
        req.setAttribute("monthLabel", today.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)));

        String upcoming = "from care_logs cl join trees t on t.id = cl.tree_id and t.deleted_at is null "
                + "where cl.next_due_on is not null and cl.next_due_on <= ?";
        req.setAttribute("upcomingCount", Db.count("select count(*) " + upcoming, today.plusDays(7)));
        req.setAttribute("upcomingCare", Db.list("select cl.id, cl.activity_type, cl.next_due_on, t.id as tree_id, "
                + "t.code as tree_code " + upcoming + " order by cl.next_due_on, cl.id limit 8", today.plusDays(7)));
        req.setAttribute("recentHarvests", Db.list("select h.*, t.code as tree_code, tt.name as type_name, "
                + "b.name as block_name from harvests h join trees t on t.id = h.tree_id and t.deleted_at is null "
                + "join tree_types tt on tt.id = t.tree_type_id join blocks b on b.id = t.block_id "
                + "order by h.harvested_on desc, h.id desc limit 5"));

        List<String> keys = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            LocalDate month = chartStart.plusMonths(i);
            keys.add(month.toString().substring(0, 7));
            labels.add(month.format(DateTimeFormatter.ofPattern("MMM yy", Locale.ENGLISH)));
        }
        Map<String, Object> harvestChart = new LinkedHashMap<>();
        harvestChart.put("labels", labels);
        for (String unit : new String[]{"kg", "pcs"}) {
            harvestChart.put(unit, series(keys, Db.list("select to_char(harvested_on, 'YYYY-MM') as k, sum(quantity) as v "
                    + "from harvests where harvested_on >= ? and unit = ? group by 1", chartStart, unit)));
        }
        req.setAttribute("harvestChart", harvestChart);

        if (isAdmin(req)) {
            BigDecimal sales = (BigDecimal) Db.value("select coalesce(sum(total_amount), 0) from sales "
                    + "where sold_on between ? and ?", monthStart, monthEnd);
            BigDecimal expenses = (BigDecimal) Db.value("select coalesce(sum(amount), 0) from expenses "
                    + "where spent_on between ? and ?", monthStart, monthEnd);
            req.setAttribute("finance", Map.of("sales", sales, "expenses", expenses, "net", sales.subtract(expenses)));
            Map<String, Object> financeChart = new LinkedHashMap<>();
            financeChart.put("labels", labels);
            financeChart.put("sales", series(keys, Db.list("select to_char(sold_on, 'YYYY-MM') as k, sum(total_amount) as v "
                    + "from sales where sold_on >= ? group by 1", chartStart)));
            financeChart.put("expenses", series(keys, Db.list("select to_char(spent_on, 'YYYY-MM') as k, sum(amount) as v "
                    + "from expenses where spent_on >= ? group by 1", chartStart)));
            req.setAttribute("financeChart", financeChart);
        }
        render(req, resp, "dashboard", "Dashboard");
    }

    /** One value per month key, 0 where the query returned nothing. */
    private static List<Object> series(List<String> keys, List<Map<String, Object>> rows) {
        Map<String, Object> byKey = new HashMap<>();
        rows.forEach(r -> byKey.put((String) r.get("k"), r.get("v")));
        List<Object> values = new ArrayList<>();
        keys.forEach(k -> values.add(byKey.getOrDefault(k, BigDecimal.ZERO)));
        return values;
    }
}
