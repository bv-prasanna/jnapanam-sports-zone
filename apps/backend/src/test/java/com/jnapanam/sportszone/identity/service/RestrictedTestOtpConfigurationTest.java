package com.jnapanam.sportszone.identity.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.jnapanam.sportszone.notification.provider.NotificationProviderRegistry;

class RestrictedTestOtpConfigurationTest {
    private SmsOtpService service(boolean enabled,String otp,String phones,String profile){
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles(profile);
        return new SmsOtpService(mock(JdbcTemplate.class),mock(PasswordEncoder.class),
          mock(NotificationProviderRegistry.class),mock(com.jnapanam.sportszone.notification.provider.Fast2SmsWhatsAppOtpSender.class),"sms",enabled,otp,phones,env);
    }
    @Test void neverPermitFixedOtpInProduction(){
        assertThrows(IllegalStateException.class,()->service(true,"071977","9000000000","prod"));
    }
    @Test void rejectUnrestrictedFixedOtp(){
        assertThrows(IllegalStateException.class,()->service(true,"071977","","uat"));
    }
    @Test void allowExplicitlyScopedUatOtp(){
        assertNotNull(service(true,"071977","9000000000","uat"));
    }
    @Test void requireSixDigits(){
        assertThrows(IllegalStateException.class,()->service(true,"1977","9000000000","uat"));
    }
}
