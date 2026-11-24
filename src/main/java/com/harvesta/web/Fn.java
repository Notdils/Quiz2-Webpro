package com.harvesta.web;

import com.harvesta.Options;
import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

/**
 * JSP EL functions (prefix "h", see WEB-INF/harvesta.tld).
 * Display formats follow PRD 3.1: "21 Oct 2026", "1,250,000.50", "IDR 1,250,000".
 */
public final class Fn {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final DecimalFormatSymbols US = DecimalFormatSymbols.getInstance(Locale.US);

    private Fn() {
    }

    /** HTML-escapes any value. Use for every user-entered string. */
    public static String e(Object value) {
        if (value == null) {
            return "";
        }
        String s = value.toString();
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (char c : s.toCharArray()) {
            switch (c) {
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '&' -> out.append("&amp;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    public static String date(Object value) {
        if (value instanceof LocalDate d) {
            return d.format(DATE);
        }
        if (value instanceof LocalDateTime dt) {
            return dt.format(DATE);
        }
        return "—";
    }

    public static String number(Object value) {
        return format(value, "#,##0.00");
    }

    /** Whole numbers keep no decimals; fractional values show two. */
    public static String qty(Object value) {
        if (value == null) {
            return "—";
        }
        BigDecimal number = new BigDecimal(value.toString());
        return format(value, number.stripTrailingZeros().scale() <= 0 ? "#,##0" : "#,##0.00");
    }

    public static String integer(Object value) {
        return format(value, "#,##0");
    }

    public static String money(Object value) {
        return value == null ? "—" : "IDR " + format(value, "#,##0");
    }

    private static String format(Object value, String pattern) {
        if (value == null) {
            return "—";
        }
        return new DecimalFormat(pattern, US).format(new BigDecimal(value.toString()));
    }

    public static String label(Object value) {
        return e(Options.label(value));
    }

    /** "selected" when both values are equal as strings (ids may be Long or String). */
    public static String sel(Object a, Object b) {
        return a != null && b != null && a.toString().equals(b.toString()) ? "selected" : "";
    }

    /** The current URL with "page" replaced, keeping every active filter. */
    public static String pageUrl(HttpServletRequest request, int page) {
        StringJoiner query = new StringJoiner("&", "?", "");
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            if (!entry.getKey().equals("page")) {
                for (String v : entry.getValue()) {
                    query.add(enc(entry.getKey()) + "=" + enc(v));
                }
            }
        }
        query.add("page=" + page);
        Object uri = request.getAttribute("jakarta.servlet.forward.request_uri");
        return e((uri != null ? uri : request.getRequestURI()) + query.toString());
    }

    /** The current URL with "sort" replaced, keeping every active filter. */
    public static String sortUrl(HttpServletRequest request, String sort) {
        StringJoiner query = new StringJoiner("&", "?", "");
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            if (!entry.getKey().equals("sort") && !entry.getKey().equals("page")) {
                for (String v : entry.getValue()) {
                    query.add(enc(entry.getKey()) + "=" + enc(v));
                }
            }
        }
        query.add("sort=" + enc(sort));
        Object uri = request.getAttribute("jakarta.servlet.forward.request_uri");
        return e((uri != null ? uri : request.getRequestURI()) + query.toString());
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    /** Minimal JSON for chart data (maps, collections, numbers, strings). Escape for HTML attributes in JSP. */
    public static String json(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Map<?, ?> map) {
            StringJoiner out = new StringJoiner(",", "{", "}");
            map.forEach((k, v) -> out.add(json(k.toString()) + ":" + json(v)));
            return out.toString();
        }
        if (value instanceof Collection<?> list) {
            StringJoiner out = new StringJoiner(",", "[", "]");
            list.forEach(v -> out.add(json(v)));
            return out.toString();
        }
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toString().toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }

    /** "Overdue by 2 days", "Due today", "Due tomorrow", "Due in 5 days". */
    public static String due(Object value) {
        if (!(value instanceof LocalDate date)) {
            return "";
        }
        long days = ChronoUnit.DAYS.between(LocalDate.now(), date);
        if (days < 0) {
            return "Overdue by " + -days + (days == -1 ? " day" : " days");
        }
        if (days == 0) {
            return "Due today";
        }
        return days == 1 ? "Due tomorrow" : "Due in " + days + " days";
    }

    public static String dueTone(Object value) {
        if (!(value instanceof LocalDate date)) {
            return "neutral";
        }
        long days = ChronoUnit.DAYS.between(LocalDate.now(), date);
        return days < 0 ? "danger" : days <= 2 ? "warning" : "info";
    }

    /** Inline SVG icons (Lucide shapes, ISC license). */
    private static final Map<String, String> ICONS = Map.ofEntries(
            Map.entry("dashboard", "<rect width='7' height='9' x='3' y='3' rx='1'/><rect width='7' height='5' x='14' y='3' rx='1'/><rect width='7' height='9' x='14' y='12' rx='1'/><rect width='7' height='5' x='3' y='16' rx='1'/>"),
            Map.entry("tree", "<path d='M8 19a4 4 0 0 1-2.24-7.32A3.5 3.5 0 0 1 9 6.03V6a3 3 0 1 1 6 0v.04a3.5 3.5 0 0 1 3.24 5.65A4 4 0 0 1 16 19Z'/><path d='M12 19v3'/>"),
            Map.entry("sprout", "<path d='M7 20h10'/><path d='M10 20c5.5-2.5.8-6.4 3-10'/><path d='M9.5 9.4c1.1.8 1.8 2.2 2.3 3.7-2 .4-3.5.4-4.8-.3-1.2-.6-2.3-1.9-3-4.2 2.8-.5 4.4 0 5.5.8z'/><path d='M14.1 6a7 7 0 0 0-1.1 4c1.9-.1 3.3-.6 4.3-1.4 1-1 1.6-2.3 1.7-4.6-2.7.1-4 1-4.9 2z'/>"),
            Map.entry("basket", "<path d='m15 11-1 9'/><path d='m19 11-4-7'/><path d='M2 11h20'/><path d='m3.5 11 1.6 7.4a2 2 0 0 0 2 1.6h9.8a2 2 0 0 0 2-1.6l1.7-7.4'/><path d='M4.5 15.5h15'/><path d='m5 11 4-7'/><path d='m9 11 1 9'/>"),
            Map.entry("tag", "<path d='M12.586 2.586A2 2 0 0 0 11.172 2H4a2 2 0 0 0-2 2v7.172a2 2 0 0 0 .586 1.414l8.704 8.704a2.426 2.426 0 0 0 3.42 0l6.58-6.58a2.426 2.426 0 0 0 0-3.42z'/><circle cx='7.5' cy='7.5' r='.5'/>"),
            Map.entry("layers", "<path d='M12.83 2.18a2 2 0 0 0-1.66 0L2.6 6.08a1 1 0 0 0 0 1.83l8.58 3.91a2 2 0 0 0 1.66 0l8.58-3.9a1 1 0 0 0 0-1.83Z'/><path d='m22 17.65-9.17 4.16a2 2 0 0 1-1.66 0L2 17.65'/><path d='m22 12.65-9.17 4.16a2 2 0 0 1-1.66 0L2 12.65'/>"),
            Map.entry("map", "<path d='M14.106 5.553a2 2 0 0 0 1.788 0l3.659-1.83A1 1 0 0 1 21 4.619v12.764a1 1 0 0 1-.553.894l-4.553 2.277a2 2 0 0 1-1.788 0l-4.212-2.106a2 2 0 0 0-1.788 0l-3.659 1.83A1 1 0 0 1 3 19.381V6.618a1 1 0 0 1 .553-.894l4.553-2.277a2 2 0 0 1 1.788 0z'/><path d='M15 5.764v15'/><path d='M9 3.236v15'/>"),
            Map.entry("wallet", "<path d='M19 7V4a1 1 0 0 0-1-1H5a2 2 0 0 0 0 4h15a1 1 0 0 1 1 1v4h-3a2 2 0 0 0 0 4h3a1 1 0 0 0 1-1v-2a1 1 0 0 0-1-1'/><path d='M3 5v14a2 2 0 0 0 2 2h15a1 1 0 0 0 1-1v-4'/>"),
            Map.entry("cash", "<rect width='20' height='12' x='2' y='6' rx='2'/><circle cx='12' cy='12' r='2'/><path d='M6 12h.01M18 12h.01'/>"),
            Map.entry("chart", "<path d='M3 3v16a2 2 0 0 0 2 2h16'/><path d='M18 17V9'/><path d='M13 17V5'/><path d='M8 17v-3'/>"),
            Map.entry("users", "<path d='M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2'/><circle cx='9' cy='7' r='4'/><path d='M22 21v-2a4 4 0 0 0-3-3.87'/><path d='M16 3.13a4 4 0 0 1 0 7.75'/>"),
            Map.entry("user", "<path d='M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2'/><circle cx='12' cy='7' r='4'/>"),
            Map.entry("logout", "<path d='M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4'/><path d='m16 17 5-5-5-5'/><path d='M21 12H9'/>"),
            Map.entry("plus", "<path d='M5 12h14'/><path d='M12 5v14'/>"),
            Map.entry("edit", "<path d='M21.174 6.812a1 1 0 0 0-3.986-3.987L3.842 16.174a2 2 0 0 0-.5.83l-1.321 4.352a.5.5 0 0 0 .623.622l4.353-1.32a2 2 0 0 0 .83-.497z'/><path d='m15 5 4 4'/>"),
            Map.entry("trash", "<path d='M3 6h18'/><path d='M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6'/><path d='M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2'/>"),
            Map.entry("eye", "<path d='M2.062 12.348a1 1 0 0 1 0-.696 10.75 10.75 0 0 1 19.876 0 1 1 0 0 1 0 .696 10.75 10.75 0 0 1-19.876 0'/><circle cx='12' cy='12' r='3'/>"),
            Map.entry("search", "<circle cx='11' cy='11' r='8'/><path d='m21 21-4.3-4.3'/>"),
            Map.entry("download", "<path d='M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4'/><path d='m7 10 5 5 5-5'/><path d='M12 15V3'/>"),
            Map.entry("calendar", "<rect width='18' height='18' x='3' y='4' rx='2'/><path d='M16 2v4M8 2v4M3 10h18'/>"),
            Map.entry("back", "<path d='m12 19-7-7 7-7'/><path d='M19 12H5'/>"),
            Map.entry("menu", "<path d='M4 6h16M4 12h16M4 18h16'/>"),
            Map.entry("check", "<circle cx='12' cy='12' r='10'/><path d='m9 12 2 2 4-4'/>"),
            Map.entry("alert", "<circle cx='12' cy='12' r='10'/><path d='M12 8v4M12 16h.01'/>"),
            Map.entry("leaf", "<path d='M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.48 19 2c1 2 2 4.18 2 8 0 5.5-4.78 10-10 10Z'/><path d='M2 21c0-3 1.85-5.36 5.08-6C9.5 14.52 12 13 13 12'/>"),
            Map.entry("image", "<rect width='18' height='18' x='3' y='3' rx='2'/><circle cx='9' cy='9' r='2'/><path d='m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21'/>"),
            Map.entry("trend", "<path d='m22 7-8.5 8.5-5-5L2 17'/><path d='M16 7h6v6'/>"),
            Map.entry("clock", "<circle cx='12' cy='12' r='10'/><path d='M12 6v6l4 2'/>"),
            Map.entry("sort", "<path d='m21 16-4 4-4-4'/><path d='M17 20V4'/><path d='m3 8 4-4 4 4'/><path d='M7 4v16'/>"));

    public static String icon(String name) {
        return "<svg class='icon' viewBox='0 0 24 24' aria-hidden='true'>" + ICONS.getOrDefault(name, "") + "</svg>";
    }

    public static String initials(Object name) {
        if (name == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (String part : name.toString().trim().split("\\s+")) {
            if (!part.isEmpty() && out.length() < 2) {
                out.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return e(out.toString());
    }
}
