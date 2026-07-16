package com.teamtask.repository;

import com.teamtask.model.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Data access for users. Same Panache pattern as TaskRepository.
 */
@ApplicationScoped
public class UserRepository implements PanacheRepository<User> {

    /**
     * Find a user by email, or null if none exists.
     * Panache turns this into: SELECT * FROM users WHERE email = ?
     */
    public User findByEmail(String email) {
        return find("email", email).firstResult();
    }
}
