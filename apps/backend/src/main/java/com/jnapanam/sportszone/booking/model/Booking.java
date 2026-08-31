package com.jnapanam.sportszone.booking.model;
import com.jnapanam.sportszone.identity.model.User;import jakarta.persistence.*;import java.time.Instant;import java.util.UUID;
@Entity@Table(name="bookings")public class Booking{
 @Id private UUID id;@Column(name="booking_number",nullable=false,unique=true,length=30)private String bookingNumber;
 @ManyToOne(fetch=FetchType.LAZY,optional=false)@JoinColumn(name="user_id")private User user;@ManyToOne(fetch=FetchType.LAZY,optional=false)@JoinColumn(name="slot_id")private BookingSlot slot;
 @Column(nullable=false)private int quantity;@Enumerated(EnumType.STRING)@Column(nullable=false,length=20)private BookingStatus status;
 @Column(name="confirmed_at",nullable=false)private Instant confirmedAt;@Column(name="cancelled_at")private Instant cancelledAt;@Column(name="checked_in_at")private Instant checkedInAt;
 @ManyToOne(fetch=FetchType.LAZY)@JoinColumn(name="checked_in_by")private User checkedInBy;
 @Column(name="payment_expires_at")private Instant paymentExpiresAt;@Column(name="payment_completed_at")private Instant paymentCompletedAt;@Column(name="status_reason",length=300)private String statusReason;
 protected Booking(){}
 public Booking(User user,BookingSlot slot,int quantity,boolean paymentRequired,Instant paymentExpiry){id=UUID.randomUUID();bookingNumber="JNZ-"+UUID.randomUUID().toString().substring(0,8).toUpperCase();this.user=user;this.slot=slot;this.quantity=quantity;confirmedAt=Instant.now();status=paymentRequired?BookingStatus.PENDING_PAYMENT:BookingStatus.CONFIRMED;paymentExpiresAt=paymentRequired?paymentExpiry:null;if(!paymentRequired)paymentCompletedAt=confirmedAt;}
 public void paid(){if(status==BookingStatus.PENDING_PAYMENT){status=BookingStatus.CONFIRMED;paymentCompletedAt=Instant.now();paymentExpiresAt=null;}}
 public void expirePayment(){if(status==BookingStatus.PENDING_PAYMENT){status=BookingStatus.PAYMENT_EXPIRED;statusReason="Payment window expired";}}
 public void cancel(String reason){status=BookingStatus.CANCELLED;cancelledAt=Instant.now();statusReason=reason;}
 public void checkIn(User operator){if(checkedInAt==null){checkedInAt=Instant.now();checkedInBy=operator;}}
 public void undoCheckIn(){checkedInAt=null;checkedInBy=null;}
 public void noShow(String reason){if(status==BookingStatus.CONFIRMED){status=BookingStatus.NO_SHOW;statusReason=reason;}}
 public UUID getId(){return id;}public String getBookingNumber(){return bookingNumber;}public User getUser(){return user;}public BookingSlot getSlot(){return slot;}public int getQuantity(){return quantity;}public BookingStatus getStatus(){return status;}public Instant getConfirmedAt(){return confirmedAt;}public Instant getCancelledAt(){return cancelledAt;}public Instant getCheckedInAt(){return checkedInAt;}public User getCheckedInBy(){return checkedInBy;}public Instant getPaymentExpiresAt(){return paymentExpiresAt;}public Instant getPaymentCompletedAt(){return paymentCompletedAt;}public String getStatusReason(){return statusReason;}
}
