package com.harvesta.web;

import com.harvesta.Db;
import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Builds a WHERE clause from optional filters, always with bound parameters. */
public final class Query {

    public static final int PER_PAGE = 15;

    private final StringBuilder where = new StringBuilder();
    public final List<Object> params = new ArrayList<>();

    public Query where(String condition, Object... values) {
        where.append(where.isEmpty() ? " where " : " and ").append(condition);
        params.addAll(Arrays.asList(values));
        return this;
    }

    /** Adds the condition only when the value is present. */
    public Query filter(Object value, String condition) {
        return value == null ? this : where(condition, value);
    }

    /** Case-insensitive "contains" across one or more columns. */
    public Query search(String term, String... columns) {
        if (term == null) {
            return this;
        }
        List<String> parts = new ArrayList<>();
        for (String column : columns) {
            parts.add(column + " ilike ?");
            params.add("%" + term.replace("%", "\\%").replace("_", "\\_") + "%");
        }
        where.append(where.isEmpty() ? " where " : " and ").append("(").append(String.join(" or ", parts)).append(")");
        return this;
    }

    public String sql() {
        return where.toString();
    }

    public Object[] args() {
        return params.toArray();
    }

    /** Runs a paginated select: {@code select} is the column list, {@code from} the FROM/JOIN part. */
    public Page page(HttpServletRequest request, String select, String from, String orderBy) {
        long total = Db.count("select count(*) " + from + sql(), args());
        int pages = (int) Math.max(1, (total + PER_PAGE - 1) / PER_PAGE);
        int page = 1;
        try {
            page = Math.min(pages, Math.max(1, Integer.parseInt(request.getParameter("page"))));
        } catch (NumberFormatException e) {
            // Missing or invalid page: show the first page.
        }
        List<Object> all = new ArrayList<>(params);
        all.add(PER_PAGE);
        all.add((page - 1) * PER_PAGE);
        List<Map<String, Object>> items = Db.list(
                "select " + select + " " + from + sql() + " order by " + orderBy + " limit ? offset ?", all);
        return new Page(items, page, pages, total);
    }

    public record Page(List<Map<String, Object>> items, int page, int pages, long total) {
        // Bean getters: Jakarta EL 5 (JSP 3.1) does not read record components.
        public List<Map<String, Object>> getItems() {
            return items;
        }

        public int getPage() {
            return page;
        }

        public int getPages() {
            return pages;
        }

        public long getTotal() {
            return total;
        }

        public long getFrom() {
            return total == 0 ? 0 : (long) (page - 1) * PER_PAGE + 1;
        }

        public long getTo() {
            return Math.min(total, (long) page * PER_PAGE);
        }
    }
}
