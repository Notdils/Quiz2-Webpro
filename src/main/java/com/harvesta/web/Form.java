package com.harvesta.web;

import com.harvesta.Db;
import com.harvesta.Options;
import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Server-side validation for one submitted form. Each method validates a field,
 * records an English error message, and returns the parsed value (or null).
 * {@link #values} keeps the raw input so the form can be re-rendered.
 */
public final class Form {

    private final HttpServletRequest request;
    public final Map<String, String> errors = new LinkedHashMap<>();
    public final Map<String, Object> values = new HashMap<>();

    public Form(HttpServletRequest request) {
        this.request = request;
        request.getParameterMap().forEach((name, value) -> values.put(name, value[0]));
    }

    public String raw(String name) {
        String value = request.getParameter(name);
        return value == null || value.isBlank() ? null : value.trim();
    }

    public String text(String name, String label, boolean required, int max) {
        String value = raw(name);
        if (value == null) {
            if (required) {
                error(name, "The " + label + " field is required.");
            }
            return null;
        }
        if (max > 0 && value.length() > max) {
            error(name, "The " + label + " must not be greater than " + max + " characters.");
        }
        return value;
    }

    public LocalDate date(String name, String label, boolean required, boolean notFuture) {
        String value = raw(name);
        if (value == null) {
            if (required) {
                error(name, "The " + label + " field is required.");
            }
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(value);
            if (notFuture && date.isAfter(LocalDate.now())) {
                error(name, "The " + label + " must not be in the future.");
            }
            return date;
        } catch (DateTimeParseException e) {
            error(name, "The " + label + " must be a valid date.");
            return null;
        }
    }

    /** A decimal; with {@code positive} it must be > 0, otherwise >= 0. */
    public BigDecimal decimal(String name, String label, boolean required, boolean positive) {
        String value = raw(name);
        if (value == null) {
            if (required) {
                error(name, "The " + label + " field is required.");
            }
            return null;
        }
        try {
            BigDecimal number = new BigDecimal(value.replace(",", ""));
            int sign = number.signum();
            if (positive ? sign <= 0 : sign < 0) {
                error(name, "The " + label + " must be greater than " + (positive ? "0." : "or equal to 0."));
            } else if (number.compareTo(new BigDecimal("999999999999")) > 0) {
                error(name, "The " + label + " is too large.");
            }
            return number;
        } catch (NumberFormatException e) {
            error(name, "The " + label + " must be a number.");
            return null;
        }
    }

    /** An id that must exist in {@code table} (soft-deleted trees excluded). */
    public Long id(String name, String label, boolean required, String table) {
        String value = raw(name);
        if (value == null) {
            if (required) {
                error(name, "The " + label + " field is required.");
            }
            return null;
        }
        try {
            long id = Long.parseLong(value);
            String extra = table.equals("trees") ? " and deleted_at is null" : "";
            if (Db.count("select count(*) from " + table + " where id = ?" + extra, id) == 0) {
                error(name, "The selected " + label + " is invalid.");
            }
            return id;
        } catch (NumberFormatException e) {
            error(name, "The selected " + label + " is invalid.");
            return null;
        }
    }

    public String option(String name, String label, Options.Option[] set, boolean required) {
        String value = raw(name);
        if (value == null) {
            if (required) {
                error(name, "The " + label + " field is required.");
            }
            return null;
        }
        if (!Options.valid(set, value)) {
            error(name, "The selected " + label + " is invalid.");
        }
        return value;
    }

    public String email(String name) {
        String value = text(name, "email", true, 255);
        if (value != null && !value.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            error(name, "The email must be a valid email address.");
        }
        return value == null ? null : value.toLowerCase();
    }

    /** Password rules: at least 8 characters, one number, one uppercase letter, confirmed. */
    public String password(String name, boolean required) {
        String value = request.getParameter(name);
        if (value == null || value.isEmpty()) {
            if (required) {
                error(name, "The password field is required.");
            }
            return null;
        }
        if (value.length() < 8) {
            error(name, "The password must be at least 8 characters.");
        } else if (!value.matches(".*\\d.*")) {
            error(name, "The password must contain at least one number.");
        } else if (!value.matches(".*\\p{Lu}.*")) {
            error(name, "The password must contain at least one uppercase letter.");
        } else if (!value.equals(request.getParameter(name + "_confirmation"))) {
            error(name, "The password confirmation does not match.");
        }
        return value;
    }

    /** The field must not be used by another row (case-insensitive). */
    public void unique(String name, String label, Object value, String table, String column, Long ignoreId,
                       String scopeColumn, Object scopeValue) {
        if (value == null || errors.containsKey(name)) {
            return;
        }
        String sql = "select count(*) from " + table + " where lower(" + column + ") = lower(?)"
                + (scopeColumn != null ? " and " + scopeColumn + " = ?" : "")
                + (ignoreId != null ? " and id <> ?" : "");
        Object[] params = scopeColumn != null
                ? (ignoreId != null ? new Object[]{value, scopeValue, ignoreId} : new Object[]{value, scopeValue})
                : (ignoreId != null ? new Object[]{value, ignoreId} : new Object[]{value});
        if (Db.count(sql, params) > 0) {
            error(name, "The " + label + " has already been taken.");
        }
    }

    public void error(String name, String message) {
        errors.putIfAbsent(name, message);
    }

    public boolean ok() {
        return errors.isEmpty();
    }
}
