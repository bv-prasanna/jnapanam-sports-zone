package com.jnapanam.sportszone.payment.model;
import jakarta.persistence.*;import java.time.Instant;import java.util.UUID;
@Entity @Table(name="payment_refunds") public class PaymentRefund{
 @Id private UUID id;@ManyToOne(fetch=FetchType.LAZY,optional=false)@JoinColumn(name="payment_order_id")private PaymentOrder paymentOrder;
 @Column(name="provider_refund_id",unique=true,length=120)private String providerRefundId;@Column(name="amount_paise",nullable=false)private long amountPaise;
 @Column(nullable=false,length=300)private String reason;@Enumerated(EnumType.STRING)@Column(nullable=false,length=20)private RefundStatus status;
 @Column(name="requested_at",nullable=false)private Instant requestedAt;@Column(name="processed_at")private Instant processedAt;protected PaymentRefund(){}
 public PaymentRefund(PaymentOrder order,long amount,String reason){id=UUID.randomUUID();paymentOrder=order;amountPaise=amount;this.reason=reason;status=RefundStatus.REQUESTED;requestedAt=Instant.now();}
 public void submitted(String id){providerRefundId=id;}
 public void failed(){if(status!=RefundStatus.PROCESSED)status=RefundStatus.FAILED;}
 public void processed(String id){providerRefundId=id;status=RefundStatus.PROCESSED;processedAt=Instant.now();}
 public PaymentOrder getPaymentOrder(){return paymentOrder;}
 public UUID getId(){return id;}public long getAmountPaise(){return amountPaise;}public String getReason(){return reason;}public RefundStatus getStatus(){return status;}public String getProviderRefundId(){return providerRefundId;}
}
