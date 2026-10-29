package com.jnapanam.sportszone.identity.service;

import com.jnapanam.sportszone.common.exception.MfaChallengeRequiredException;
import com.jnapanam.sportszone.common.exception.UnauthorizedException;
import com.jnapanam.sportszone.identity.model.RoleName;
import com.jnapanam.sportszone.identity.model.User;
import com.jnapanam.sportszone.notification.model.Notification;
import com.jnapanam.sportszone.notification.model.NotificationChannel;
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
public class MfaService {
    private static final SecureRandom RANDOM = new SecureRandom();
    @Value("${app.security.privileged-mfa-required:true}")
    boolean required;
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final NotificationProviderRegistry providers;
    private final boolean localConsoleMfa;


    public MfaService(JdbcTemplate jdbc, PasswordEncoder encoder, NotificationProviderRegistry providers,
            @Value("${app.security.local-console-mfa:false}") boolean localConsoleMfa,
            Environment environment) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.providers = providers;
        if (localConsoleMfa && (!environment.acceptsProfiles(Profiles.of("local"))
                || environment.acceptsProfiles(Profiles.of("prod", "production", "uat", "test")))) {
            throw new IllegalStateException("Console MFA is allowed only with the local profile");
        }
        this.localConsoleMfa = localConsoleMfa;
    }

    public boolean privileged(User user) {
        return user.getRoles().stream().anyMatch(role ->
                role.getName() == RoleName.ADMIN || role.getName() == RoleName.SUPER_ADMIN);
    }

    @Transactional(noRollbackFor = MfaChallengeRequiredException.class)
    public void verifyOrChallenge(User user, String code) {
        if (!required || !privileged(user)) return;
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select enabled,challenge_hash,challenge_expires_at from privileged_mfa where user_id=?",
                user.getId());
        if (rows.isEmpty()) {
            jdbc.update("insert into privileged_mfa(id,user_id,enabled) values(?,?,true)",
                    UUID.randomUUID(), user.getId());
            challenge(user);
            throw new MfaChallengeRequiredException();
        }

        Map<String, Object> row = rows.get(0);
        if (!Boolean.TRUE.equals(row.get("enabled"))) return;
        String hash = (String) row.get("challenge_hash");
        Object expiry = row.get("challenge_expires_at");
        Instant expiresAt = expiry instanceof Timestamp timestamp ? timestamp.toInstant()
                : expiry instanceof Instant instant ? instant : null;

        if (code == null || code.isBlank()) {
            challenge(user);
            throw new MfaChallengeRequiredException();
        }
        if (hash == null || expiresAt == null || Instant.now().isAfter(expiresAt))
            throw new UnauthorizedException("Invalid or expired MFA code");
        if (!code.matches("[0-9]{6}") || !encoder.matches(code, hash))
            throw new UnauthorizedException("Invalid MFA code");
        jdbc.update("update privileged_mfa set challenge_hash=null,challenge_expires_at=null,updated_at=now() where user_id=?",
                user.getId());
    }

    private void challenge(User user) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        String hash = encoder.encode(code);
        // Deliver immediately, just as registration verification does. A queued security
        // notification is not sufficient: this endpoint has no authenticated dispatcher.
        // If SMTP fails, do not persist an unusable challenge or claim the code was sent.
        Notification email = new Notification(user, NotificationChannel.EMAIL, "SECURITY",
                "Jnapanam sign-in verification code",
                "Your verification code is " + code + ". It expires in 10 minutes.");
        if (!localConsoleMfa) providers.provider(NotificationChannel.EMAIL).send(email);
        jdbc.update("update privileged_mfa set challenge_hash=?,challenge_expires_at=?,updated_at=now() where user_id=?",
                hash, Timestamp.from(Instant.now().plusSeconds(600)), user.getId());
        if (localConsoleMfa) System.out.println("[LOCAL TEST ONLY] MFA code for " + user.getEmail() + ": " + code);
    }
}
