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
    @Column(name="address_line",length=250) private String addressLine;
    @Column(length=100) private String city;
    @Column(length=100) private String state;
    @Column(name="postal_code",length=12) private String postalCode;
    @Column(name="emergency_contact_name",length=120) private String emergencyContactName;
    @Column(name="emergency_contact_mobile",length=20) private String emergencyContactMobile;
    @Column(name="force_password_change",nullable=false) private boolean forcePasswordChange;
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
    public void update(String fullName,String email,String mobile,String addressLine,String city,String state,String postalCode,String emergencyName,String emergencyMobile){this.fullName=fullName.trim();this.email=email.trim().toLowerCase(java.util.Locale.ROOT);this.mobile=clean(mobile);this.addressLine=clean(addressLine);this.city=clean(city);this.state=clean(state);this.postalCode=clean(postalCode);this.emergencyContactName=clean(emergencyName);this.emergencyContactMobile=clean(emergencyMobile);updatedAt=Instant.now();}
    public void status(UserStatus value){status=value;updatedAt=Instant.now();}
    public void replaceRoles(Set<Role> value){roles.clear();roles.addAll(value);updatedAt=Instant.now();}
    public void password(String hash,boolean force){passwordHash=hash;forcePasswordChange=force;updatedAt=Instant.now();}
    public String getAddressLine(){return addressLine;}public String getCity(){return city;}public String getState(){return state;}public String getPostalCode(){return postalCode;}public String getEmergencyContactName(){return emergencyContactName;}public String getEmergencyContactMobile(){return emergencyContactMobile;}public boolean isForcePasswordChange(){return forcePasswordChange;}
    private static String clean(String v){return v==null||v.isBlank()?null:v.trim();}
}
