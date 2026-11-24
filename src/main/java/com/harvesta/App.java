package com.harvesta;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/** Application bootstrap: connect, create the schema, seed an empty database, expose option sets to JSP. */
@WebListener
public class App implements ServletContextListener {

    /** Never write the app name as a literal; use this (or ${appName} in JSP). */
    public static final String NAME = Env.get("APP_NAME", "Harvesta");

    @Override
    public void contextInitialized(ServletContextEvent event) {
        Db.init();
        Db.script(resource("/schema.sql"));
        Seeder.run();
        Options.ALL.forEach(event.getServletContext()::setAttribute);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        Db.close();
    }

    static String resource(String name) {
        try (InputStream in = App.class.getResourceAsStream(name)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
