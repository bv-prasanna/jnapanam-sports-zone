package com.jnapanam.sportszone.booking.dto;
import com.jnapanam.sportszone.booking.model.PaymentOption;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;
/** Existing two-argument constructor is retained for Java callers. */
public record ConfirmRequest(@NotNull UUID holdId, PaymentOption paymentOption,
        @PositiveOrZero Long expectedTotalPaise, Boolean acceptedTerms) {
    public ConfirmRequest(UUID holdId,PaymentOption paymentOption){this(holdId,paymentOption,null,null);}
    public ConfirmRequest(UUID holdId, PaymentOption paymentOption, Long expectedTotalPaise) {
        this(holdId, paymentOption, expectedTotalPaise, null);
    }
}
