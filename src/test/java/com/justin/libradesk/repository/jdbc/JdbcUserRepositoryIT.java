package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.enumtype.UserRole;
import com.justin.libradesk.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcUserRepositoryIT extends AbstractRepositoryIT {

    private JdbcUserRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JdbcUserRepository(databaseManager);
    }

    private User newUser(String username, boolean mustChange) {
        User user = new User(null, username, "hash", "Full Name", UserRole.ADMIN, true, FIXED);
        user.setMustChangePassword(mustChange);
        return user;
    }

    @Test
    void mustChangePasswordRoundTrips() {
        User saved = repository.save(newUser("admin", true));
        assertTrue(repository.findById(saved.getId()).orElseThrow().isMustChangePassword());

        saved.setMustChangePassword(false);
        repository.save(saved);
        assertFalse(repository.findById(saved.getId()).orElseThrow().isMustChangePassword());
    }

    @Test
    void findByUsernameReturnsTheUser() {
        repository.save(newUser("alice", false));

        assertTrue(repository.findByUsername("alice").isPresent());
        assertTrue(repository.findByUsername("ghost").isEmpty());
    }
}
