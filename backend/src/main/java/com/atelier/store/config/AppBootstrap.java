package com.atelier.store.config;

import com.atelier.store.dao.UserDao;
import com.atelier.store.util.Passwords;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class AppBootstrap implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent event) {
        String email = Database.setting("ADMIN_EMAIL", "admin@atelier.local");
        String password = Database.setting("ADMIN_PASSWORD", "Admin@123");
        try {
            UserDao users = new UserDao();
            if (users.findByEmail(email).isEmpty()) {
                users.create("STYLE SPHERE Admin", email, Passwords.hash(password), "ADMIN");
            }
        } catch (Exception exception) {
            event.getServletContext().log("Admin bootstrap skipped. Verify MySQL schema and DB credentials.", exception);
        }
    }
}
