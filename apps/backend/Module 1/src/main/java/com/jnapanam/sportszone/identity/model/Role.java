package com.jnapanam.sportszone.identity.model;

import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class Role {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, unique = true, length = 40)
    private RoleName name;

    protected Role() {}
    public Long getId() { return id; }
    public RoleName getName() { return name; }
}
