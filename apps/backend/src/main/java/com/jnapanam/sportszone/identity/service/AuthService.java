package com.jnapanam.sportszone.identity.service;

import com.jnapanam.sportszone.common.exception.*;
import com.jnapanam.sportszone.identity.dto.*;
import com.jnapanam.sportszone.identity.model.*;
import com.jnapanam.sportszone.identity.repository.*;
import com.jnapanam.sportszone.security.TokenService;
import com.jnapanam.sportszone.notification.service.NotificationService;
import java.util.Locale;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users; private final RoleRepository roles; private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder; private final AuthenticationManager authenticationManager; private final TokenService tokens; private final MfaService mfa; private final NotificationService notifications;
    public AuthService(UserRepository users, RoleRepository roles, RefreshTokenRepository refreshTokens, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, TokenService tokens, MfaService mfa, NotificationService notifications) {
        this.users=users; this.roles=roles; this.refreshTokens=refreshTokens; this.passwordEncoder=passwordEncoder; this.authenticationManager=authenticationManager; this.tokens=tokens; this.mfa=mfa; this.notifications=notifications;
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) throw new ConflictException("An account already exists for this email");
        String mobile = request.mobile() == null || request.mobile().isBlank() ? null : request.mobile().replaceAll("[ -]", "");
        if (mobile != null && users.existsByMobile(mobile)) throw new ConflictException("An account already exists for this mobile number");
        Role customer = roles.findByName(RoleName.CUSTOMER).orElseThrow(() -> new IllegalStateException("CUSTOMER role is missing"));
        User user = users.save(new User(request.fullName().trim(), email, mobile, passwordEncoder.encode(request.password()), customer));
        notifications.welcome(user.getId(), user.getFullName());
        return issueTokens(user,null,null);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        String identifier = request.email().trim().toLowerCase(Locale.ROOT);
        User user = (identifier.matches("[0-9]{10}") ? users.findByMobile(identifier) : users.findByEmailIgnoreCase(identifier))
            .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(user.getEmail(), request.password()));
        mfa.verifyOrChallenge(user, request.mfaCode());
        return issueTokens(user,request.deviceSignature(),request.deviceName());
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        RefreshToken stored = refreshTokens.findByTokenHash(tokens.hash(request.refreshToken())).orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        if (!stored.isUsable() || !stored.getUser().isActive()) throw new UnauthorizedException("Refresh token expired or revoked");
        if(stored.getDeviceSignatureHash()!=null){if(request.deviceSignature()==null||!stored.getDeviceSignatureHash().equals(tokens.hash(request.deviceSignature())))throw new UnauthorizedException("Device verification failed");}
        stored.revoke();
        return issueTokens(stored.getUser(),request.deviceSignature(),stored.getDeviceName());
    }

    @Transactional
    public void logout(RefreshRequest request) { refreshTokens.findByTokenHash(tokens.hash(request.refreshToken())).ifPresent(RefreshToken::revoke); }

    private TokenResponse issueTokens(User user,String deviceSignature,String deviceName) {
        String rawRefresh = tokens.newRefreshToken();
        refreshTokens.save(new RefreshToken(user,tokens.hash(rawRefresh),tokens.refreshExpiry(),deviceSignature==null||deviceSignature.isBlank()?null:tokens.hash(deviceSignature),deviceName));
        return tokens.response(tokens.createAccessToken(user), rawRefresh);
    }
}
