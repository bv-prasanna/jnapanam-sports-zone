package com.jnapanam.sportszone.booking.dto;
import com.jnapanam.sportszone.booking.model.PaymentOption;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;
/** Existing two-argument constructor is retained for Java callers. */
public record ConfirmRequest(@NotNull UUID holdId, PaymentOption paymentOption,
        @PositiveOrZero Long expectedTotalPaise) {
    public ConfirmRequest(UUID holdId,PaymentOption paymentOption){this(holdId,paymentOption,null);}
}
