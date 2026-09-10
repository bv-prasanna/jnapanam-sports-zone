package com.jnapanam.sportszone.commerce.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ShopCounterDtos {
    private ShopCounterDtos() {}

    public record SaleItemRequest(@NotNull UUID variantId, @Min(1) int quantity) {}

    public record WalkInSaleRequest(
            UUID customerUserId,
            @Size(max = 150) String customerName,
            @Size(max = 20) String mobile,
            @NotBlank String paymentMethod,
            @Size(max = 120) String paymentReference,
            @NotEmpty List<@Valid SaleItemRequest> items) {}

    public record WalkInSaleResponse(
            UUID id, String saleNumber, String customerName, String mobile, String paymentMethod,
            String paymentStatus, long subtotalPaise, long taxPaise, long totalPaise,
            UUID createdBy, Instant createdAt) {}

    public record CounterPaymentRequest(
            @NotBlank String paymentMethod,
            @Size(max = 120) String paymentReference) {}

    public record ReturnRequest(
            @NotBlank String sourceType,
            @NotNull UUID sourceId,
            @NotNull UUID variantId,
            @Min(1) int quantity,
            @NotBlank String resolution,
            UUID replacementVariantId,
            @NotBlank @Size(max = 300) String reason) {}

    public record ReturnReviewRequest(@Size(max = 300) String note) {}

    public record ReturnResponse(
            UUID id, String sourceType, UUID sourceId, UUID variantId, int quantity,
            String resolution, UUID replacementVariantId, String reason, long amountPaise,
            String status, UUID requestedBy, UUID approvedBy, Instant requestedAt,
            Instant reviewedAt, Instant completedAt) {}
}
