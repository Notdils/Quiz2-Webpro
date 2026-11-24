package com.harvesta;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Thin JDBC layer. Every query uses bound parameters (no string-built values).
 * Rows come back as maps keyed by column label so JSP EL can read them directly
 * (${tree.code}); SQL dates become LocalDate and timestamps LocalDateTime.
 */
public final class Db {

    private static HikariDataSource dataSource;
    private static final ThreadLocal<Connection> TX = new ThreadLocal<>();

    private Db() {
    }

    public static void init() {
        String url = Env.get("DB_URL", null);
        if (url == null) {
            url = "jdbc:postgresql://" + Env.get("DB_HOST", "127.0.0.1") + ":" + Env.get("DB_PORT", "5432")
                    + "/" + Env.get("DB_DATABASE", "harvesta") + "?sslmode=" + Env.get("DB_SSLMODE", "prefer");
        }
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(Env.get("DB_USERNAME", "postgres"));
        config.setPassword(Env.get("DB_PASSWORD", ""));
        config.setMaximumPoolSize(Integer.parseInt(Env.get("DB_POOL_SIZE", "5")));
        dataSource = new HikariDataSource(config);
    }

    public static void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    /** Runs the work in one transaction; any exception rolls it back. */
    public static <T> T tx(Supplier<T> work) {
        if (TX.get() != null) {
            return work.get();
        }
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            TX.set(connection);
            try {
                T result = work.get();
                connection.commit();
                return result;
            } catch (RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                TX.remove();
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DbException(e);
        }
    }

    public static List<Map<String, Object>> list(String sql, Object... params) {
        return run(sql, params, statement -> {
            List<Map<String, Object>> rows = new ArrayList<>();
            try (ResultSet rs = statement.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        row.put(meta.getColumnLabel(i), convert(rs.getObject(i)));
                    }
                    rows.add(row);
                }
            }
            return rows;
        });
    }

    public static List<Map<String, Object>> list(String sql, List<Object> params) {
        return list(sql, params.toArray());
    }

    /** First row, or null. */
    public static Map<String, Object> one(String sql, Object... params) {
        List<Map<String, Object>> rows = list(sql, params);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** First column of the first row, or null. */
    public static Object value(String sql, Object... params) {
        Map<String, Object> row = one(sql, params);
        return row == null ? null : row.values().iterator().next();
    }

    public static long count(String sql, Object... params) {
        Object value = value(sql, params);
        return value == null ? 0 : ((Number) value).longValue();
    }

    public static int update(String sql, Object... params) {
        return run(sql, params, PreparedStatement::executeUpdate);
    }

    /** Executes an INSERT ... RETURNING id and returns the new id. */
    public static long insert(String sql, Object... params) {
        return ((Number) value(sql, params)).longValue();
    }

    public static void script(String sql) {
        // Comments are removed first so a ';' inside a comment cannot split a statement.
        for (String statement : sql.replaceAll("(?m)--.*$", "").split(";")) {
            String trimmed = statement.trim();
            if (!trimmed.isEmpty()) {
                run(trimmed, new Object[0], s -> s.execute());
            }
        }
    }

    private interface StatementWork<T> {
        T apply(PreparedStatement statement) throws SQLException;
    }

    private static <T> T run(String sql, Object[] params, StatementWork<T> work) {
        Connection shared = TX.get();
        try {
            Connection connection = shared != null ? shared : dataSource.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    statement.setObject(i + 1, params[i]);
                }
                return work.apply(statement);
            } finally {
                if (shared == null) {
                    connection.close();
                }
            }
        } catch (SQLException e) {
            throw new DbException(e);
        }
    }

    private static Object convert(Object value) {
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return value;
    }

    /** Unchecked wrapper so callers can test the SQL state (e.g. 23505 unique, 23503 foreign key). */
    public static final class DbException extends RuntimeException {
        public DbException(SQLException cause) {
            super(cause.getMessage(), cause);
        }

        public String sqlState() {
            return ((SQLException) getCause()).getSQLState();
        }
    }
}
