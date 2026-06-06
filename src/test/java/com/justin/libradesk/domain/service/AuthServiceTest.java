package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.enumtype.UserRole;
import com.justin.libradesk.domain.model.User;
import com.justin.libradesk.repository.UserRepository;
import com.justin.libradesk.util.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository);
    }

    private User user(String passwordHash, boolean active) {
        return new User(1L, "alice", passwordHash, "Alice", UserRole.LIBRARIAN, active, LocalDateTime.now());
    }

    @Test
    void legacyHashAuthenticatesAndIsUpgradedToBcrypt() throws Exception {
        User legacyUser = user(sha256Hex("pw"), true);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(legacyUser));

        assertTrue(authService.authenticate("alice", "pw").isPresent());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertTrue(captor.getValue().getPasswordHash().startsWith("$2"));
    }

    @Test
    void bcryptHashIsNotResavedOnLogin() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user(PasswordHasher.hash("pw"), true)));

        assertTrue(authService.authenticate("alice", "pw").isPresent());
        verify(userRepository, never()).save(any());
    }

    @Test
    void wrongPasswordIsRejected() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user(PasswordHasher.hash("pw"), true)));

        assertTrue(authService.authenticate("alice", "nope").isEmpty());
    }

    @Test
    void inactiveAccountIsRejected() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user(PasswordHasher.hash("pw"), false)));

        assertTrue(authService.authenticate("alice", "pw").isEmpty());
    }

    @Test
    void unknownUserIsRejected() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertFalse(authService.authenticate("ghost", "pw").isPresent());
    }

    private static String sha256Hex(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }
}
