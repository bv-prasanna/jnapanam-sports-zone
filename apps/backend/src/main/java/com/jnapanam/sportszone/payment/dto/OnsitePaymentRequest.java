package com.jnapanam.sportszone.payment.dto;

import com.jnapanam.sportszone.booking.model.OnsitePaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OnsitePaymentRequest(
        @NotNull OnsitePaymentMethod paymentMethod,
        @Size(max = 120) String reference) {}
