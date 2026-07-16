package com.teamtask.service;

import com.teamtask.dto.LoginRequest;
import com.teamtask.dto.SignupRequest;
import com.teamtask.exception.DuplicateEmailException;
import com.teamtask.exception.InvalidCredentialsException;
import com.teamtask.model.Role;
import com.teamtask.model.User;
import com.teamtask.repository.UserRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Business logic for accounts.
 *
 * BCrypt in two lines:
 *  - BcryptUtil.bcryptHash(password)      -> one-way hash to STORE
 *  - BcryptUtil.matches(password, hash)   -> check a login attempt
 * BCrypt is deliberately SLOW and salted, which makes stolen hashes very
 * expensive to crack. That is why it is the standard for passwords.
 */
@ApplicationScoped
public class AuthService {

    private final UserRepository users;

    public AuthService(UserRepository users) {
        this.users = users;
    }

    /**
     * Register a new account. Always a MEMBER - clients can never pick a role.
     */
    @Transactional
    public User signup(SignupRequest request) {
        if (users.findByEmail(request.email) != null) {
            throw new DuplicateEmailException(request.email); // -> 409
        }

        User user = new User();
        user.setEmail(request.email);
        user.setPasswordHash(BcryptUtil.bcryptHash(request.password));
        user.setDisplayName(request.displayName);
        user.setRole(Role.MEMBER);

        users.persist(user);
        return user;
    }

    /**
     * Verify credentials. Note we do NOT say which part was wrong (see
     * InvalidCredentialsException) - both failures return the same 401.
     */
    @Transactional
    public User login(LoginRequest request) {
        User user = users.findByEmail(request.email);
        if (user == null || !BcryptUtil.matches(request.password, user.getPasswordHash())) {
            throw new InvalidCredentialsException(); // -> 401
        }
        return user;
    }
}
