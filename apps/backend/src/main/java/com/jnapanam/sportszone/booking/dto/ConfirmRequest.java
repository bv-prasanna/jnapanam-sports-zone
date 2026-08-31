package com.jnapanam.sportszone.booking.dto;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
public record ConfirmRequest(@NotNull UUID holdId){}
