package com.jnapanam.sportszone.payment.model;

import com.jnapanam.sportszone.identity.model.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="payment_orders")
public class PaymentOrder {
 @Id private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id") private User user;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private PaymentPurpose purpose;
 @Column(name="reference_id",nullable=false) private UUID referenceId;
 @Column(name="receipt_number",nullable=false,unique=true,length=40) private String receiptNumber;
 @Column(name="amount_paise",nullable=false) private long amountPaise;
 @Column(nullable=false,length=3) private String currency;
 @Column(nullable=false,length=20) private String provider;
 @Column(name="provider_order_id",unique=true,length=120) private String providerOrderId;
 @Column(name="provider_payment_id",unique=true,length=120) private String providerPaymentId;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private PaymentStatus status;
 @Column(name="idempotency_key",nullable=false,length=100) private String idempotencyKey;
 @Column(name="paid_at") private Instant paidAt;
 @Column(name="created_at",nullable=false) private Instant createdAt;
 @Column(name="updated_at",nullable=false) private Instant updatedAt;
 @Version private long version;
 protected PaymentOrder() {}
 public PaymentOrder(User user,PaymentPurpose purpose,UUID referenceId,long amountPaise,String currency,String provider,String idempotencyKey){
  id=UUID.randomUUID();this.user=user;this.purpose=purpose;this.referenceId=referenceId;this.amountPaise=amountPaise;
  this.currency=currency;this.provider=provider;this.idempotencyKey=idempotencyKey;
  receiptNumber="JNZ-PAY-"+id.toString().substring(0,8).toUpperCase();status=PaymentStatus.CREATED;createdAt=Instant.now();updatedAt=createdAt;
 }
 public void providerOrder(String value){providerOrderId=value;updatedAt=Instant.now();}
 public void paid(String paymentId){providerPaymentId=paymentId;status=PaymentStatus.PAID;paidAt=Instant.now();updatedAt=paidAt;}
 public void refundStatus(long refunded){status=refunded>=amountPaise?PaymentStatus.REFUNDED:PaymentStatus.PARTIALLY_REFUNDED;updatedAt=Instant.now();}
 public UUID getId(){return id;} public User getUser(){return user;} public PaymentPurpose getPurpose(){return purpose;} public UUID getReferenceId(){return referenceId;}
 public String getReceiptNumber(){return receiptNumber;} public long getAmountPaise(){return amountPaise;} public String getCurrency(){return currency;} public String getProvider(){return provider;}
 public String getProviderOrderId(){return providerOrderId;} public String getProviderPaymentId(){return providerPaymentId;} public PaymentStatus getStatus(){return status;}
 public String getIdempotencyKey(){return idempotencyKey;} public Instant getPaidAt(){return paidAt;} public Instant getCreatedAt(){return createdAt;}
}
