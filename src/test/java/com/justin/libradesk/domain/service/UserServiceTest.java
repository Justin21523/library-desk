package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.enumtype.UserRole;
import com.justin.libradesk.domain.model.User;
import com.justin.libradesk.repository.UserRepository;
import com.justin.libradesk.util.PasswordHasher;
import com.justin.libradesk.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditLogService auditLogService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(LocalDateTime.of(2026, 1, 1, 9, 0).atZone(ZoneId.of("UTC")).toInstant(),
                ZoneId.of("UTC"));
        userService = new UserService(userRepository, auditLogService, clock);
    }

    private User userWithPassword(String raw) {
        return new User(1L, "alice", PasswordHasher.hash(raw), "Alice", UserRole.LIBRARIAN,
                true, LocalDateTime.now());
    }

    @Test
    void changePasswordStoresNewHashWhenCurrentMatches() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(userWithPassword("oldpass")));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.changePassword(1L, "oldpass", "newpass", "alice");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertTrue(PasswordHasher.matches("newpass", captor.getValue().getPasswordHash()));
        verify(auditLogService).record("alice", "PASSWORD_CHANGED", "User", 1L, null);
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(userWithPassword("oldpass")));

        assertThrows(ValidationException.class,
                () -> userService.changePassword(1L, "wrong", "newpass", "alice"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePasswordRejectsWeakNewPassword() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(userWithPassword("oldpass")));

        assertThrows(ValidationException.class,
                () -> userService.changePassword(1L, "oldpass", "123", "alice"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createStaffRejectsDuplicateUsername() {
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(userWithPassword("x")));

        assertThrows(ValidationException.class,
                () -> userService.createStaff("bob", "Bob", UserRole.ASSISTANT, "password", "admin"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createStaffStoresHashedPassword() {
        when(userRepository.findByUsername("bob")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.createStaff("bob", "Bob", UserRole.ASSISTANT, "password", "admin");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertTrue(PasswordHasher.matches("password", captor.getValue().getPasswordHash()));
    }
}
