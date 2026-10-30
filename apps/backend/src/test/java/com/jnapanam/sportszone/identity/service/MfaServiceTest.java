package com.jnapanam.sportszone.identity.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.jnapanam.sportszone.common.exception.MfaChallengeRequiredException;
import com.jnapanam.sportszone.common.exception.UnauthorizedException;
import com.jnapanam.sportszone.identity.model.Role;
import com.jnapanam.sportszone.identity.model.RoleName;
import com.jnapanam.sportszone.identity.model.User;
import com.jnapanam.sportszone.notification.model.Notification;
import com.jnapanam.sportszone.notification.model.NotificationChannel;
import com.jnapanam.sportszone.notification.provider.NotificationProvider;
import com.jnapanam.sportszone.notification.provider.NotificationProviderRegistry;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

class MfaServiceTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final NotificationProviderRegistry registry = mock(NotificationProviderRegistry.class);
    private final NotificationProvider mail = mock(NotificationProvider.class);
    private MfaService service;
    private User admin;

    @BeforeEach
    void setUp() {
        service = new MfaService(jdbc, encoder, registry, false, new org.springframework.mock.env.MockEnvironment());
        service.required = true;
        Role role = mock(Role.class);
        when(role.getName()).thenReturn(RoleName.SUPER_ADMIN);
        admin = new User("Admin", "admin@example.com", null, "hash", role);
        when(registry.provider(NotificationChannel.EMAIL)).thenReturn(mail);
        when(encoder.encode(anyString())).thenReturn("otp-hash");
        when(jdbc.update(anyString(), any(), any(), any())).thenReturn(1);
    }

    @Test
    void consoleMfaCannotBeEnabledOutsideLocalProfile() {
        assertThrows(IllegalStateException.class, () ->
                new MfaService(jdbc, encoder, registry, true, new org.springframework.mock.env.MockEnvironment()));
        var prod = new org.springframework.mock.env.MockEnvironment();
        prod.setActiveProfiles("local", "production");
        assertThrows(IllegalStateException.class, () ->
                new MfaService(jdbc, encoder, registry, true, prod));
    }

    @Test
    void newAdminChallengeSendsEmailImmediatelyAndPersistsHash() {
        when(jdbc.queryForList(anyString(), eq(admin.getId()))).thenReturn(List.of());
        assertThrows(MfaChallengeRequiredException.class, () -> service.verifyOrChallenge(admin, ""));
        verify(mail).send(argThat(n -> n.getChannel() == NotificationChannel.EMAIL
                && n.getMessage().contains("expires in 10 minutes")));
        verify(jdbc).update(startsWith("update privileged_mfa set challenge_hash="),
                eq("otp-hash"), any(Timestamp.class), eq(admin.getId()));
    }

    @Test
    void smtpFailureDoesNotPersistNewChallenge() {
        when(jdbc.queryForList(anyString(), eq(admin.getId()))).thenReturn(List.of(
                Map.of("enabled", true, "challenge_hash", "oldhash",
                        "challenge_expires_at", Timestamp.from(Instant.now().plusSeconds(300)))));
        when(mail.send(any(Notification.class))).thenThrow(new IllegalStateException("SMTP unavailable"));
        assertThrows(IllegalStateException.class, () -> service.verifyOrChallenge(admin, ""));
        verify(jdbc, never()).update(startsWith("update privileged_mfa set challenge_hash="),
                any(), any(), any());
    }

    @Test
    void validCodeClearsChallenge() {
        when(jdbc.queryForList(anyString(), eq(admin.getId()))).thenReturn(List.of(
                Map.of("enabled", true, "challenge_hash", "otp-hash",
                        "challenge_expires_at", Timestamp.from(Instant.now().plusSeconds(300)))));
        when(encoder.matches("123456", "otp-hash")).thenReturn(true);
        assertDoesNotThrow(() -> service.verifyOrChallenge(admin, "123456"));
        verify(jdbc).update(startsWith("update privileged_mfa set challenge_hash=null"), eq(admin.getId()));
        verifyNoInteractions(mail);
    }

    @Test
    void invalidCodeDoesNotIssueNewEmailOrClearChallenge() {
        when(jdbc.queryForList(anyString(), eq(admin.getId()))).thenReturn(List.of(
                Map.of("enabled", true, "challenge_hash", "otp-hash",
                        "challenge_expires_at", Timestamp.from(Instant.now().plusSeconds(300)))));
        assertThrows(UnauthorizedException.class, () -> service.verifyOrChallenge(admin, "000000"));
        verifyNoInteractions(mail);
        verify(jdbc, never()).update(startsWith("update privileged_mfa set challenge_hash=null"), eq(admin.getId()));
    }

    @Test
    void expiredCodeIsRejected() {
        when(jdbc.queryForList(anyString(), eq(admin.getId()))).thenReturn(List.of(
                Map.of("enabled", true, "challenge_hash", "otp-hash",
                        "challenge_expires_at", Timestamp.from(Instant.now().minusSeconds(5)))));
        assertThrows(UnauthorizedException.class, () -> service.verifyOrChallenge(admin, "123456"));
        verifyNoInteractions(mail);
    }

    @Test
    void customerDoesNotNeedPrivilegedMfa() {
        Role customerRole = mock(Role.class);
        when(customerRole.getName()).thenReturn(RoleName.CUSTOMER);
        User customer = new User("Customer", "customer@example.com", null, "hash", customerRole);
        assertDoesNotThrow(() -> service.verifyOrChallenge(customer, ""));
        verifyNoInteractions(jdbc, mail);
    }
}
