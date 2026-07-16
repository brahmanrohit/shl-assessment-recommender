package com.teamtask.security;

import com.teamtask.model.Role;
import com.teamtask.model.User;
import com.teamtask.repository.UserRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Creates the first ADMIN account at application startup if it doesn't exist.
 *
 * Why this exists: signups are always MEMBER (so nobody can self-promote),
 * which raises the chicken-and-egg question "where does the first admin come
 * from?". Answer: the system creates it, with credentials from configuration
 * (environment variables in production - see application.properties).
 *
 * @Observes StartupEvent = "run this method once, when the app boots".
 */
@ApplicationScoped
public class AdminBootstrap {

    private final UserRepository users;

    @ConfigProperty(name = "app.admin.email")
    String adminEmail;

    @ConfigProperty(name = "app.admin.password")
    String adminPassword;

    @ConfigProperty(name = "app.admin.display-name")
    String adminDisplayName;

    public AdminBootstrap(UserRepository users) {
        this.users = users;
    }

    @Transactional
    void onStart(@Observes StartupEvent event) {
        if (users.findByEmail(adminEmail) != null) {
            return; // admin already exists, nothing to do
        }

        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setPasswordHash(BcryptUtil.bcryptHash(adminPassword));
        admin.setDisplayName(adminDisplayName);
        admin.setRole(Role.ADMIN);
        users.persist(admin);

        Log.infof("Bootstrap admin account created: %s", adminEmail);
    }
}
