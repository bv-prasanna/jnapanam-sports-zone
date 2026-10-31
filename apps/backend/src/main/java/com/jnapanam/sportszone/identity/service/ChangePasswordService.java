package com.jnapanam.sportszone.identity.service;

import com.jnapanam.sportszone.common.exception.BadRequestException;
import com.jnapanam.sportszone.common.exception.UnauthorizedException;
import com.jnapanam.sportszone.identity.dto.ChangePasswordRequest;
import com.jnapanam.sportszone.identity.repository.RefreshTokenRepository;
import com.jnapanam.sportszone.identity.repository.UserRepository;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChangePasswordService {
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;

    public ChangePasswordService(UserRepository users, RefreshTokenRepository refreshTokens,
                                 PasswordEncoder encoder) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.encoder = encoder;
    }

    @Transactional
    public void change(UUID authenticatedUserId, ChangePasswordRequest request) {
        var user = users.findById(authenticatedUserId)
                .filter(u -> u.isActive())
                .orElseThrow(() -> new UnauthorizedException("Account unavailable"));
        if (!encoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (encoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must be different from current password");
        }
        // Avoid BCrypt silently truncating passwords longer than 72 UTF-8 bytes.
        if (request.newPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new BadRequestException("Password must not exceed 72 UTF-8 bytes");
        }
        user.password(encoder.encode(request.newPassword()), false);
        users.save(user);
        // Require fresh sign-in on every device after password change.
        refreshTokens.findByUserIdAndRevokedAtIsNull(authenticatedUserId)
                .forEach(token -> token.revoke());
    }
}
