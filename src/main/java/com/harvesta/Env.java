package com.harvesta;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration from environment variables, falling back to a local ".env" file
 * in the working directory (used by "mvn jetty:run"; production uses real env vars).
 */
public final class Env {

    private static final Map<String, String> FILE = load(Path.of(System.getProperty("user.dir"), ".env"));

    private Env() {
    }

    public static String get(String key, String fallback) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            value = FILE.get(key);
        }
        return value == null || value.isBlank() ? fallback : value;
    }

    public static boolean flag(String key, boolean fallback) {
        String value = get(key, null);
        return value == null ? fallback : value.equalsIgnoreCase("true") || value.equals("1");
    }

    public static boolean production() {
        return "production".equalsIgnoreCase(get("APP_ENV", "local"));
    }

    static Map<String, String> load(Path path) {
        Map<String, String> values = new HashMap<>();
        if (!Files.isRegularFile(path)) {
            return values;
        }
        try {
            for (String line : Files.readAllLines(path)) {
                line = line.trim();
                int eq = line.indexOf('=');
                if (line.isEmpty() || line.startsWith("#") || eq < 1) {
                    continue;
                }
                String value = line.substring(eq + 1).trim();
                if (value.length() >= 2 && (value.startsWith("\"") && value.endsWith("\"")
                        || value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                values.put(line.substring(0, eq).trim(), value);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return values;
    }
}
