package com.jnapanam.sportszone.booking.dto;
import com.jnapanam.sportszone.booking.model.PaymentOption;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
public record ConfirmRequest(@NotNull UUID holdId, PaymentOption paymentOption){}
