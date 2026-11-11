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
 @Column(name="original_amount_paise",nullable=false) private long originalAmountPaise;
 @Column(name="discount_paise",nullable=false) private long discountPaise;
 @Column(name="promotion_code",length=40) private String promotionCode;
 @Column(name="loyalty_points_used",nullable=false) private long loyaltyPointsUsed;
 @Column(name="loyalty_points_earned",nullable=false) private long loyaltyPointsEarned;
 @Column(name="billing_gross_paise")private Long billingGrossPaise;@Column(name="billing_taxable_paise")private Long billingTaxablePaise;@Column(name="billing_tax_paise")private Long billingTaxPaise;@Column(name="billing_rate_percent",precision=7,scale=3)private java.math.BigDecimal billingRatePercent;@Column(name="billing_loyalty_tender_paise")private Long billingLoyaltyTenderPaise;@Column(name="billing_promotion_treatment",length=30)private String billingPromotionTreatment;@Column(name="billing_loyalty_treatment",length=30)private String billingLoyaltyTreatment;@Column(name="billing_snapshot_at")private Instant billingSnapshotAt;
 @Column(nullable=false,length=3) private String currency;
 @Column(nullable=false,length=20) private String provider;
 @Column(name="provider_order_id",unique=true,length=120) private String providerOrderId;
 @Column(name="provider_payment_id",unique=true,length=120) private String providerPaymentId;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private PaymentStatus status;
 @Column(name="idempotency_key",nullable=false,length=100) private String idempotencyKey;
 @Column(name="paid_at") private Instant paidAt;
 @Column(name="voided_at") private Instant voidedAt;
 @Column(name="void_reason",length=300) private String voidReason;
 @Column(name="created_at",nullable=false) private Instant createdAt;
 @Column(name="updated_at",nullable=false) private Instant updatedAt;
 @Version private long version;
 protected PaymentOrder() {}
 public PaymentOrder(User user,PaymentPurpose purpose,UUID referenceId,long amountPaise,String currency,String provider,String idempotencyKey){
  id=UUID.randomUUID();this.user=user;this.purpose=purpose;this.referenceId=referenceId;this.amountPaise=amountPaise;
  this.originalAmountPaise=amountPaise;this.currency=currency;this.provider=provider;this.idempotencyKey=idempotencyKey;
  receiptNumber="JNZ-PAY-"+id.toString().substring(0,8).toUpperCase();status=PaymentStatus.CREATED;createdAt=Instant.now();updatedAt=createdAt;
 }
 public void pricing(long original,long discount,String promotion,long pointsUsed,long loyaltyValuePaise){originalAmountPaise=original;discountPaise=discount;promotionCode=promotion;loyaltyPointsUsed=pointsUsed;amountPaise=original-discount-loyaltyValuePaise;if(amountPaise<1)amountPaise=1;updatedAt=Instant.now();}
 public void billingSnapshot(long gross,long net,long tax,java.math.BigDecimal rate,long loyaltyTender,String couponMode,String loyaltyMode){if(billingSnapshotAt!=null)throw new IllegalStateException("Billing snapshot cannot change");if(gross<0||net<0||tax<0||loyaltyTender<0||gross!=net+tax)throw new IllegalArgumentException("Invalid invoice breakdown");billingGrossPaise=gross;billingTaxablePaise=net;billingTaxPaise=tax;billingRatePercent=rate;billingLoyaltyTenderPaise=loyaltyTender;billingPromotionTreatment=couponMode;billingLoyaltyTreatment=loyaltyMode;billingSnapshotAt=Instant.now();}
 public Long getBillingGrossPaise(){return billingGrossPaise;}public Long getBillingTaxablePaise(){return billingTaxablePaise;}public Long getBillingTaxPaise(){return billingTaxPaise;}public java.math.BigDecimal getBillingRatePercent(){return billingRatePercent;}public Long getBillingLoyaltyTenderPaise(){return billingLoyaltyTenderPaise;}public Instant getBillingSnapshotAt(){return billingSnapshotAt;}public String getBillingPromotionTreatment(){return billingPromotionTreatment;}public String getBillingLoyaltyTreatment(){return billingLoyaltyTreatment;}
 public void loyaltyEarned(long value){loyaltyPointsEarned=value;updatedAt=Instant.now();}
 public void providerOrder(String value){providerOrderId=value;updatedAt=Instant.now();}
 public void paid(String paymentId){providerPaymentId=paymentId;status=PaymentStatus.PAID;paidAt=Instant.now();updatedAt=paidAt;}
 public void voidOrder(String reason){if(status!=PaymentStatus.CREATED)throw new IllegalStateException("Only created payments can be voided");status=PaymentStatus.VOIDED;voidedAt=Instant.now();voidReason=reason.trim();updatedAt=voidedAt;}
 public void refundStatus(long refunded){status=refunded>=amountPaise?PaymentStatus.REFUNDED:PaymentStatus.PARTIALLY_REFUNDED;updatedAt=Instant.now();}
 public UUID getId(){return id;} public User getUser(){return user;} public PaymentPurpose getPurpose(){return purpose;} public UUID getReferenceId(){return referenceId;}
 public String getReceiptNumber(){return receiptNumber;} public long getAmountPaise(){return amountPaise;} public String getCurrency(){return currency;} public String getProvider(){return provider;}
 public String getProviderOrderId(){return providerOrderId;} public String getProviderPaymentId(){return providerPaymentId;} public PaymentStatus getStatus(){return status;}
 public String getIdempotencyKey(){return idempotencyKey;} public Instant getPaidAt(){return paidAt;} public Instant getVoidedAt(){return voidedAt;} public String getVoidReason(){return voidReason;} public Instant getCreatedAt(){return createdAt;}
 public long getOriginalAmountPaise(){return originalAmountPaise;}public long getDiscountPaise(){return discountPaise;}public String getPromotionCode(){return promotionCode;}public long getLoyaltyPointsUsed(){return loyaltyPointsUsed;}public long getLoyaltyPointsEarned(){return loyaltyPointsEarned;}
}
