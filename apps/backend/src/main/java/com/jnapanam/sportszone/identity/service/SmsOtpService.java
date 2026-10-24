package com.jnapanam.sportszone.identity.service;

import com.jnapanam.sportszone.common.exception.BadRequestException;
import com.jnapanam.sportszone.common.exception.ConflictException;
import com.jnapanam.sportszone.notification.provider.NotificationProviderRegistry;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SmsOtpService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final NotificationProviderRegistry providers;
    private final boolean testOtpEnabled;
    private final String testOtp;
    private final java.util.Set<String> testMobiles;

    public SmsOtpService(
            JdbcTemplate jdbc,
            PasswordEncoder encoder,
            NotificationProviderRegistry providers,
            @Value("${app.security.test-otp-enabled:false}") boolean testOtpEnabled,
            @Value("${app.security.test-otp:071977}") String testOtp,
            @Value("${app.security.test-otp-allowed-mobiles:}") String testMobiles,
            Environment environment) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.providers = providers;
        this.testOtpEnabled = testOtpEnabled;
        this.testOtp = testOtp;
        this.testMobiles = java.util.Arrays.stream(testMobiles.split(",")).map(String::trim).filter(v -> !v.isBlank()).collect(java.util.stream.Collectors.toUnmodifiableSet());
        // The fixed test OTP must never be usable on a production profile,
        // including when the deployment accidentally enables its property.
        if (testOtpEnabled && (!environment.acceptsProfiles(Profiles.of("test", "uat", "dev", "local"))
                || environment.acceptsProfiles(Profiles.of("prod", "production")))) {
            throw new IllegalStateException("Fixed OTP requires a non-production test, uat, dev or local profile");
        }
        if (testOtpEnabled && (testOtp == null || !testOtp.matches("\\d{6}"))) {
            throw new IllegalStateException("Test OTP must contain exactly 6 digits");
        }
        if (testOtpEnabled && testMobiles.isEmpty()) {
            throw new IllegalStateException("Fixed OTP requires an explicit allowlist of test mobile numbers");
        }
    }

    private boolean isTestMobile(String mobile) {
        return testOtpEnabled && testMobiles.contains(mobile);
    }

    @Transactional
    public void registrationOtp(String mobile) {
        issue(normalize(mobile), "REGISTRATION");
    }

    // Never roll back a failed-attempt counter when an invalid OTP is supplied.
    @Transactional(noRollbackFor = BadRequestException.class)
    public void verifyRegistration(String mobile, String otp) {
        verify(normalize(mobile), "REGISTRATION", otp);
    }

    @Transactional
    public void passwordResetOtp(String mobile) {
        issue(normalize(mobile), "PASSWORD_RESET");
    }

    @Transactional(noRollbackFor = BadRequestException.class)
    public void verifyPasswordReset(String mobile, String otp) {
        verify(normalize(mobile), "PASSWORD_RESET", otp);
    }

    private void issue(String mobile, String purpose) {
        if (mobile == null || mobile.length() != 10) {
            throw new BadRequestException("A valid 10 digit mobile number is required");
        }
        Integer recent = jdbc.queryForObject(
                "select count(*) from mobile_otp_challenges where mobile=? and purpose=? and created_at>now()-interval '60 seconds'",
                Integer.class, mobile, purpose);
        if (recent != null && recent > 0) {
            throw new ConflictException("Please wait before requesting another OTP");
        }
        Integer hourly = jdbc.queryForObject(
                "select count(*) from mobile_otp_challenges where mobile=? and purpose=? and created_at>now()-interval '1 hour'",
                Integer.class, mobile, purpose);
        if (hourly != null && hourly >= 5) {
            throw new ConflictException("OTP request limit reached. Please try again later.");
        }

        // Fixed OTP is strictly registration-only; never bypass SMS for password resets.
        boolean testRequest = "REGISTRATION".equals(purpose) && isTestMobile(mobile);
        String otp = testRequest ? testOtp : String.format("%06d", RANDOM.nextInt(1000000));
        jdbc.update(
                "insert into mobile_otp_challenges(id,purpose,mobile,otp_hash,expires_at) values(?,?,?,?,?)",
                UUID.randomUUID(), purpose, mobile, encoder.encode(otp),
                Timestamp.from(Instant.now().plusSeconds(300)));
        if (!testRequest) {
            providers.sms().sendOtp(mobile, otp);
        }
    }

    private void verify(String mobile, String purpose, String otp) {
        if (mobile == null || mobile.length() != 10 || otp == null
                || !otp.matches("\\d{6}")) {
            throw new BadRequestException("Invalid OTP");
        }
        List<Map<String, Object>> challenges = jdbc.queryForList(
                "select * from mobile_otp_challenges where mobile=? and purpose=? and verified_at is null order by created_at desc limit 1 for update",
                mobile, purpose);
        if (challenges.isEmpty()) {
            throw new BadRequestException("OTP not requested");
        }
        Map<String, Object> challenge = challenges.get(0);
        if (Instant.now().isAfter(((Timestamp) challenge.get("expires_at")).toInstant())) {
            throw new BadRequestException("OTP expired");
        }
        if (((Number) challenge.get("attempts")).intValue() >= 5) {
            throw new BadRequestException("Too many OTP attempts");
        }
        if (!encoder.matches(otp, (String) challenge.get("otp_hash"))) {
            jdbc.update("update mobile_otp_challenges set attempts=attempts+1 where id=?", challenge.get("id"));
            throw new BadRequestException("Invalid OTP");
        }
        jdbc.update("update mobile_otp_challenges set verified_at=now() where id=?", challenge.get("id"));
    }

    private String normalize(String mobile) {
        if (mobile == null) {
            return null;
        }
        String digits = mobile.replaceAll("\\D", "");
        return digits.startsWith("91") && digits.length() == 12 ? digits.substring(2) : digits;
    }
}
