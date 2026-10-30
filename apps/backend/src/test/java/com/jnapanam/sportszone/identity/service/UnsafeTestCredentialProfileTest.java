package com.jnapanam.sportszone.identity.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.jnapanam.sportszone.identity.repository.PasswordResetTokenRepository;
import com.jnapanam.sportszone.identity.repository.RefreshTokenRepository;
import com.jnapanam.sportszone.identity.repository.UserRepository;
import com.jnapanam.sportszone.notification.provider.NotificationProviderRegistry;
import com.jnapanam.sportszone.notification.provider.Fast2SmsWhatsAppOtpSender;
import com.jnapanam.sportszone.notification.service.NotificationService;
import com.jnapanam.sportszone.security.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;

class UnsafeTestCredentialProfileTest {
    @Test
    void fixedOtpCannotBeEnabledUnderProduction() {
        MockEnvironment production = new MockEnvironment();
        production.setActiveProfiles("production");
        assertThrows(IllegalStateException.class, () -> otp(true, production));
    }

    @Test
    void productionCannotBeBypassedByAddingUatProfile() {
        MockEnvironment mixed = new MockEnvironment();
        mixed.setActiveProfiles("uat", "production");
        assertThrows(IllegalStateException.class, () -> otp(true, mixed));
        assertThrows(IllegalStateException.class, () -> passwordRecovery(true, mixed));
    }

    @Test
    void productionCanRunWhenUnsafeTestFeaturesAreDisabled() {
        MockEnvironment production = new MockEnvironment();
        production.setActiveProfiles("prod");
        assertDoesNotThrow(() -> otp(false, production));
        assertDoesNotThrow(() -> passwordRecovery(false, production));
    }

    @Test
    void unsafeTestFeaturesRequireExplicitNonProductionProfile() {
        MockEnvironment missingProfile = new MockEnvironment();
        assertThrows(IllegalStateException.class, () -> otp(true, missingProfile));
        assertThrows(IllegalStateException.class, () -> passwordRecovery(true, missingProfile));

        MockEnvironment test = new MockEnvironment();
        test.setActiveProfiles("test");
        assertDoesNotThrow(() -> otp(true, test));
        assertDoesNotThrow(() -> passwordRecovery(true, test));
    }

    private SmsOtpService otp(boolean enabled, MockEnvironment environment) {
        return new SmsOtpService(mock(JdbcTemplate.class), mock(PasswordEncoder.class),
                mock(NotificationProviderRegistry.class), mock(Fast2SmsWhatsAppOtpSender.class),
                "sms", enabled, "071977", "9000000000", environment);
    }

    private PasswordRecoveryService passwordRecovery(boolean expose, MockEnvironment environment) {
        return new PasswordRecoveryService(
                mock(UserRepository.class), mock(PasswordResetTokenRepository.class),
                mock(RefreshTokenRepository.class), mock(TokenService.class),
                mock(PasswordEncoder.class), mock(NotificationService.class),
                mock(SmsOtpService.class), expose, environment);
    }
}
