package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.model.User;
import com.justin.libradesk.repository.UserRepository;
import com.justin.libradesk.util.PasswordHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Authenticates staff accounts for login. Returns an empty result for any
 * failure (unknown user, wrong password, or deactivated account) so callers
 * cannot distinguish between them.
 */
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> authenticate(String username, String rawPassword) {
        if (username == null || username.isBlank() || rawPassword == null) {
            return Optional.empty();
        }
        Optional<User> user = userRepository.findByUsername(username.trim());
        if (user.isEmpty() || !user.get().isActive()) {
            log.debug("Authentication failed for username '{}'", username);
            return Optional.empty();
        }
        if (!PasswordHasher.matches(rawPassword, user.get().getPasswordHash())) {
            log.debug("Wrong password for username '{}'", username);
            return Optional.empty();
        }
        log.info("User '{}' authenticated", username);
        return user;
    }
}
