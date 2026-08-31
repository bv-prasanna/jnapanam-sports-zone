package com.jnapanam.sportszone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.boot.context.properties.EnableConfigurationProperties(com.jnapanam.sportszone.payment.config.PaymentProperties.class)
public class SportsZoneApplication {
    public static void main(String[] args) {
        SpringApplication.run(SportsZoneApplication.class, args);
    }
}
