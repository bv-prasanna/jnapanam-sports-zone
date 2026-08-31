package com.jnapanam.sportszone.booking.dto;
import jakarta.validation.constraints.*;
import java.util.UUID;
public record HoldRequest(@NotNull UUID slotId,@Min(1) @Max(50) int quantity){}
