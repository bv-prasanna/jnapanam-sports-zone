package com.jnapanam.sportszone.identity.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    @Id private UUID id;
    @Column(name = "full_name", nullable = false, length = 120) private String fullName;
    @Column(nullable = false, unique = true, length = 180) private String email;
    @Column(unique = true, length = 20) private String mobile;
    @Column(name = "password_hash", nullable = false) private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private UserStatus status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    protected User() {}
    public User(String fullName, String email, String mobile, String passwordHash, Role role) {
        this.id = UUID.randomUUID(); this.fullName = fullName; this.email = email;
        this.mobile = mobile; this.passwordHash = passwordHash; this.status = UserStatus.ACTIVE;
        this.createdAt = Instant.now(); this.updatedAt = this.createdAt; this.roles.add(role);
    }
    public UUID getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getMobile() { return mobile; }
    public String getPasswordHash() { return passwordHash; }
    public UserStatus getStatus() { return status; }
    public Set<Role> getRoles() { return roles; }
    public boolean isActive() { return status == UserStatus.ACTIVE; }
}
