package com.jnapanam.sportszone.payment.dto;
import jakarta.validation.constraints.*;
/** Amount and deduction are in paise. A deduction is retained from the refundable balance. */
public record RefundRequest(@Positive Long amountPaise, @NotBlank @Size(max=300) String reason,
                            @PositiveOrZero Long deductionPaise) {
    public RefundRequest(Long amountPaise, String reason) { this(amountPaise, reason, null); }
}
