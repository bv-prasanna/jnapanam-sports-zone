package com.jnapanam.sportszone.booking.model;
import com.jnapanam.sportszone.identity.model.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="bookings")
public class Booking {
 @Id private UUID id;
 @Column(name="booking_number",nullable=false,unique=true,length=30) private String bookingNumber;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id") private User user;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="slot_id") private BookingSlot slot;
 @Column(nullable=false) private int quantity;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private BookingStatus status;
 @Column(name="confirmed_at",nullable=false) private Instant confirmedAt;
 @Column(name="cancelled_at") private Instant cancelledAt;
 @Column(name="checked_in_at") private Instant checkedInAt;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="checked_in_by") private User checkedInBy;
 protected Booking() {}
 public Booking(User user,BookingSlot slot,int quantity){this.id=UUID.randomUUID();this.bookingNumber="JNZ-"+UUID.randomUUID().toString().substring(0,8).toUpperCase();this.user=user;this.slot=slot;this.quantity=quantity;this.status=BookingStatus.CONFIRMED;this.confirmedAt=Instant.now();}
 public void cancel(){status=BookingStatus.CANCELLED;cancelledAt=Instant.now();}
 public void checkIn(User operator){if(checkedInAt==null){checkedInAt=Instant.now();checkedInBy=operator;}}
 public UUID getId(){return id;} public String getBookingNumber(){return bookingNumber;} public User getUser(){return user;} public BookingSlot getSlot(){return slot;} public int getQuantity(){return quantity;} public BookingStatus getStatus(){return status;} public Instant getConfirmedAt(){return confirmedAt;} public Instant getCancelledAt(){return cancelledAt;}public Instant getCheckedInAt(){return checkedInAt;}public User getCheckedInBy(){return checkedInBy;}
}
