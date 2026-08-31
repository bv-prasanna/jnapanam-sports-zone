package com.jnapanam.sportszone.security;

import com.jnapanam.sportszone.identity.dto.TokenResponse;
import com.jnapanam.sportszone.identity.model.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

@Service
public class TokenService {
    private final JwtEncoder encoder;
    private final SecurityProperties properties;
    public TokenService(JwtEncoder encoder, SecurityProperties properties) { this.encoder = encoder; this.properties = properties; }

    public AccessToken createAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.accessTokenMinutes(), ChronoUnit.MINUTES);
        var roles = user.getRoles().stream().map(r -> r.getName().name()).toList();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("jnapanam-sports-zone").issuedAt(now).expiresAt(expiry)
                .subject(user.getId().toString()).claim("email", user.getEmail()).claim("name", user.getFullName()).claim("roles", roles).build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return new AccessToken(encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue(), expiry);
    }

    public String newRefreshToken() { return UUID.randomUUID() + "." + UUID.randomUUID(); }
    public String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception ex) { throw new IllegalStateException("Cannot hash token", ex); }
    }
    public Instant refreshExpiry() { return Instant.now().plus(properties.refreshTokenDays(), ChronoUnit.DAYS); }
    public TokenResponse response(AccessToken access, String refresh) { return new TokenResponse("Bearer", access.value(), access.expiresAt(), refresh); }
    public record AccessToken(String value, Instant expiresAt) {}

    static SecretKey secretKey(String base64) {
        byte[] bytes = Base64.getDecoder().decode(base64);
        if (bytes.length < 32) throw new IllegalArgumentException("JWT_SECRET must decode to at least 32 bytes");
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
}
