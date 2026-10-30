package com.jnapanam.sportszone.identity.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "revoked_at") private Instant revokedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;@Column(name="device_signature_hash",length=64)private String deviceSignatureHash;@Column(name="device_name",length=160)private String deviceName;

    protected RefreshToken() {}
    public RefreshToken(User user, String tokenHash, Instant expiresAt) {this(user,tokenHash,expiresAt,null,null);}public RefreshToken(User user,String tokenHash,Instant expiresAt,String deviceSignatureHash,String deviceName){
        this.id = UUID.randomUUID(); this.user = user; this.tokenHash = tokenHash;
        this.expiresAt = expiresAt; this.createdAt = Instant.now();this.deviceSignatureHash=deviceSignatureHash;this.deviceName=deviceName;
    }
    public User getUser() { return user; } public String getDeviceSignatureHash(){return deviceSignatureHash;}public String getDeviceName(){return deviceName;}
    public boolean isUsable() { return revokedAt == null && expiresAt.isAfter(Instant.now()); }
    public void revoke() { this.revokedAt = Instant.now(); }
}
