package com.jnapanam.sportszone.membership.model;
import com.jnapanam.sportszone.identity.model.User; import jakarta.persistence.*; import java.time.*; import java.util.UUID;
@Entity @Table(name="customer_memberships") public class CustomerMembership{
 @Id private UUID id; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id") private User user; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="plan_id") private MembershipPlan plan;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private MembershipStatus status; @Column(name="start_date") private LocalDate startDate; @Column(name="end_date") private LocalDate endDate;
 @Column(name="subscribed_at",nullable=false) private Instant subscribedAt; @Column(name="activated_at") private Instant activatedAt; @Column(name="updated_at",nullable=false) private Instant updatedAt; @Column(name="status_reason",length=250) private String reason;
 @Column(name="payment_expires_at")private Instant paymentExpiresAt;
 protected CustomerMembership(){} public CustomerMembership(User user,MembershipPlan plan,boolean paymentRequired,Instant paymentExpiry){id=UUID.randomUUID();this.user=user;this.plan=plan;subscribedAt=Instant.now();updatedAt=subscribedAt;if(paymentRequired){status=MembershipStatus.PENDING_PAYMENT;paymentExpiresAt=paymentExpiry;}else activate(LocalDate.now());}
 public void activate(LocalDate start){startDate=start;endDate=start.plusDays(plan.getDurationDays()-1L);status=MembershipStatus.ACTIVE;activatedAt=Instant.now();updatedAt=activatedAt;paymentExpiresAt=null;reason=null;}
 public void change(MembershipStatus value,String reason){status=value;this.reason=reason==null||reason.isBlank()?null:reason.trim();updatedAt=Instant.now();}
 public UUID getId(){return id;} public User getUser(){return user;} public MembershipPlan getPlan(){return plan;} public MembershipStatus getStatus(){return status;} public LocalDate getStartDate(){return startDate;} public LocalDate getEndDate(){return endDate;} public Instant getSubscribedAt(){return subscribedAt;} public Instant getActivatedAt(){return activatedAt;} public String getReason(){return reason;}public Instant getPaymentExpiresAt(){return paymentExpiresAt;}
}
