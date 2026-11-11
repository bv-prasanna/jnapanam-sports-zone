package com.jnapanam.sportszone.identity.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.jnapanam.sportszone.common.exception.BadRequestException;
import com.jnapanam.sportszone.common.exception.UnauthorizedException;
import com.jnapanam.sportszone.identity.dto.ChangePasswordRequest;
import com.jnapanam.sportszone.identity.model.RefreshToken;
import com.jnapanam.sportszone.identity.model.Role;
import com.jnapanam.sportszone.identity.model.User;
import com.jnapanam.sportszone.identity.model.UserStatus;
import com.jnapanam.sportszone.identity.repository.RefreshTokenRepository;
import com.jnapanam.sportszone.identity.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class ChangePasswordServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final RefreshTokenRepository refresh = mock(RefreshTokenRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final ChangePasswordService service = new ChangePasswordService(users, refresh, encoder);

    private User activeUser() {
        var user = new User("Test Admin", "admin@example.test", null,
                encoder.encode("PreviousPassword123!"), mock(Role.class));
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    void changesPasswordAndRevokesAllRefreshSessions() {
        User user = activeUser();
        var tokenOne = mock(RefreshToken.class);
        var tokenTwo = mock(RefreshToken.class);
        when(refresh.findByUserIdAndRevokedAtIsNull(user.getId())).thenReturn(List.of(tokenOne, tokenTwo));
        service.change(user.getId(), new ChangePasswordRequest("PreviousPassword123!", "DifferentPassword456!"));
        assertTrue(encoder.matches("DifferentPassword456!", user.getPasswordHash()));
        assertFalse(encoder.matches("PreviousPassword123!", user.getPasswordHash()));
        assertFalse(user.isForcePasswordChange());
        verify(users).save(user);
        verify(tokenOne).revoke();
        verify(tokenTwo).revoke();
    }

    @Test
    void wrongCurrentPasswordDoesNotModifyAccount() {
        User user = activeUser();
        assertThrows(BadRequestException.class,
                () -> service.change(user.getId(), new ChangePasswordRequest("IncorrectCurrent123!", "DifferentPassword456!")));
        verify(users, never()).save(any());
        verifyNoInteractions(refresh);
    }

    @Test
    void samePasswordIsRejected() {
        User user = activeUser();
        assertThrows(BadRequestException.class,
                () -> service.change(user.getId(), new ChangePasswordRequest("PreviousPassword123!", "PreviousPassword123!")));
        verify(users, never()).save(any());
        verifyNoInteractions(refresh);
    }

    @Test
    void missingUserCannotChangePassword() {
        var id = java.util.UUID.randomUUID();
        when(users.findById(id)).thenReturn(Optional.empty());
        assertThrows(UnauthorizedException.class,
                () -> service.change(id, new ChangePasswordRequest("PreviousPassword123!", "DifferentPassword456!")));
        verifyNoInteractions(refresh);
    }

    @Test
    void inactiveAccountCannotChangePassword() {
        User user = activeUser();
        user.status(UserStatus.SUSPENDED);
        assertThrows(UnauthorizedException.class,
                () -> service.change(user.getId(), new ChangePasswordRequest("PreviousPassword123!", "DifferentPassword456!")));
        verify(users, never()).save(any());
    }

    @Test
    void rejectsPasswordBeyondBcryptByteLimit() {
        User user = activeUser();
        String longUnicodePassword = "ಕನ್ನಡ".repeat(20);
        assertThrows(BadRequestException.class,
                () -> service.change(user.getId(), new ChangePasswordRequest("PreviousPassword123!", longUnicodePassword)));
        verify(users, never()).save(any());
    }
}
