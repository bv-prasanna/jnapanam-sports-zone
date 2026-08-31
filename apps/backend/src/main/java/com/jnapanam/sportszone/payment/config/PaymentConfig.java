package com.jnapanam.sportszone.payment.config;
import com.fasterxml.jackson.databind.ObjectMapper;import org.springframework.context.annotation.*;
@Configuration public class PaymentConfig{@Bean ObjectMapper paymentObjectMapper(){return new ObjectMapper();}}
