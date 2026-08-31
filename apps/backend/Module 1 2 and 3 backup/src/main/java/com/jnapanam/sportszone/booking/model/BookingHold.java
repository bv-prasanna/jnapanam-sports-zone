package com.jnapanam.sportszone.booking.model;
import com.jnapanam.sportszone.identity.model.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="booking_holds")
public class BookingHold {
 @Id private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id") private User user;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="slot_id") private BookingSlot slot;
 @Column(nullable=false) private int quantity;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private HoldStatus status;
 @Column(name="expires_at",nullable=false) private Instant expiresAt;
 @Column(name="created_at",nullable=false) private Instant createdAt;
 @Column(name="updated_at",nullable=false) private Instant updatedAt;
 protected BookingHold() {}
 public BookingHold(User user,BookingSlot slot,int quantity,Instant expiresAt){this.id=UUID.randomUUID();this.user=user;this.slot=slot;this.quantity=quantity;this.status=HoldStatus.ACTIVE;this.expiresAt=expiresAt;this.createdAt=Instant.now();this.updatedAt=createdAt;}
 public void change(HoldStatus value){status=value;updatedAt=Instant.now();}
 public boolean expired(Instant now){return !expiresAt.isAfter(now);}
 public UUID getId(){return id;} public User getUser(){return user;} public BookingSlot getSlot(){return slot;} public int getQuantity(){return quantity;} public HoldStatus getStatus(){return status;} public Instant getExpiresAt(){return expiresAt;}
}
