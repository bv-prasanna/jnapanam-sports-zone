package com.jnapanam.sportszone.payment.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Provider-side event metadata only; never store card details or customer contact data. */
@Entity
@Table(name="payment_webhook_events")
public class PaymentWebhookEvent {
    @Id private UUID id;
    @Column(name="provider_event_id",nullable=false,unique=true,length=160)
    private String providerEventId;
    @Column(name="event_type",nullable=false,length=80)
    private String eventType;
    @Column(name="payload_hash",nullable=false,length=64)
    private String payloadHash;
    @Column(nullable=false,length=20)
    private String status;
    @Column(name="processed_at",nullable=false)
    private Instant processedAt;
    @Column(name="provider_object_id",length=120)
    private String providerObjectId;
    @Column(name="provider_order_id",length=120)
    private String providerOrderId;
    @Column(name="provider_payment_id",length=120)
    private String providerPaymentId;
    @Column(name="amount_paise")
    private Long amountPaise;

    protected PaymentWebhookEvent() {}
    public PaymentWebhookEvent(String eventId,String type,String hash) {
        this(eventId,type,hash,"PROCESSED",null,null,null,null);
    }
    public PaymentWebhookEvent(String eventId,String type,String hash,String status,
             String objectId,String orderId,String paymentId,Long amount) {
        this.id=UUID.randomUUID();
        this.providerEventId=eventId;
        this.eventType=type;
        this.payloadHash=hash;
        this.status=status;
        this.processedAt=Instant.now();
        this.providerObjectId=objectId;
        this.providerOrderId=orderId;
        this.providerPaymentId=paymentId;
        this.amountPaise=amount;
    }
    public String getEventType(){return eventType;}
    public String getStatus(){return status;}
    public String getProviderObjectId(){return providerObjectId;}
    public String getProviderPaymentId(){return providerPaymentId;}
    public String getProviderOrderId(){return providerOrderId;}
}
