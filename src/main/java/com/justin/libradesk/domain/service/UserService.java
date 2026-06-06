package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.enumtype.UserRole;
import com.justin.libradesk.domain.model.User;
import com.justin.libradesk.repository.UserRepository;
import com.justin.libradesk.util.PasswordHasher;
import com.justin.libradesk.validation.ValidationException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Manages staff accounts: changing one's own password and creating new staff
 * (so the shared default admin does not have to be reused). Passwords are only
 * ever stored hashed (see {@link PasswordHasher}).
 */
public class UserService {

    private static final int MIN_PASSWORD_LENGTH = 6;

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final Clock clock;

    public UserService(UserRepository userRepository, AuditLogService auditLogService, Clock clock) {
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.clock = clock;
    }

    /**
     * Changes a user's password after verifying the current one.
     *
     * @throws ValidationException if the current password is wrong or the new
     *                             one is too short
     */
    public void changePassword(Long userId, String currentPassword, String newPassword, String actor) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("User not found: " + userId));
        if (!PasswordHasher.matches(currentPassword, user.getPasswordHash())) {
            throw new ValidationException("Current password is incorrect");
        }
        requireStrong(newPassword);
        user.setPasswordHash(PasswordHasher.hash(newPassword));
        userRepository.save(user);
        auditLogService.record(actor, "PASSWORD_CHANGED", "User", userId, null);
    }

    /**
     * Creates a new staff account.
     *
     * @throws ValidationException if the username is taken or the password is weak
     */
    public User createStaff(String username, String fullName, UserRole role,
                            String rawPassword, String actor) {
        if (username == null || username.isBlank()) {
            throw new ValidationException("Username is required");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new ValidationException("Full name is required");
        }
        requireStrong(rawPassword);
        userRepository.findByUsername(username.trim()).ifPresent(existing -> {
            throw new ValidationException("Username already exists: " + username);
        });
        User user = new User(null, username.trim(), PasswordHasher.hash(rawPassword), fullName,
                role, true, LocalDateTime.now(clock));
        User saved = userRepository.save(user);
        auditLogService.record(actor, "USER_CREATED", "User", saved.getId(), username + " (" + role + ")");
        return saved;
    }

    public List<User> listUsers() {
        return userRepository.findAll();
    }

    private void requireStrong(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
    }
}
