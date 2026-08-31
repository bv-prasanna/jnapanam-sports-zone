package com.jnapanam.sportszone.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.payment")
public record PaymentProperties(String provider, String keyId, String keySecret,
                                String webhookSecret, String currency) {
    public PaymentProperties {
        provider = provider == null ? "mock" : provider;
        currency = currency == null ? "INR" : currency;
    }
}
