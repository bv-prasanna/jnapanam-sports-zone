package com.jnapanam.sportszone.commerce.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class CommerceDtos {
    private CommerceDtos() {}

    public record ShopSettings(
            boolean enabled, String shopName, String shopNameKn, String heroTitle, String heroTitleKn,
            String heroSubtitle, String heroSubtitleKn, String logoUrl, String primaryColor,
            String secondaryColor, String accentColor, String backgroundColor, String textColor,
            double loyaltyMaxPercent, long loyaltyPointValuePaise, boolean deliveryEnabled,
            long deliveryFeePaise, boolean payAtFacilityEnabled, String contactPhone, String contactEmail) {}

    public record ShopSettingsRequest(
            boolean enabled, @NotBlank String shopName, String shopNameKn, @NotBlank String heroTitle,
            String heroTitleKn, String heroSubtitle, String heroSubtitleKn, String logoUrl,
            @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String primaryColor,
            @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String secondaryColor,
            @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String accentColor,
            @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String backgroundColor,
            @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String textColor,
            @DecimalMin("0") @DecimalMax("100") double loyaltyMaxPercent,
            @Min(1) long loyaltyPointValuePaise, boolean deliveryEnabled,
            @Min(0) long deliveryFeePaise, boolean payAtFacilityEnabled,
            String contactPhone, @Email String contactEmail) {}

    public record Category(
            UUID id, String code, String name, String nameKn, String description, String imageUrl,
            int displayOrder, String status, Double loyaltyMaxPercent) {}
    public record Variant(
            UUID id, String sku, String label, long pricePaise, int stockQuantity,
            int lowStockThreshold, String status) {}
    public record Product(
            UUID id, UUID categoryId, String categoryCode, String sku, String name, String nameKn,
            String description, String descriptionKn, String imageUrl, double taxPercent,
            String status, Double loyaltyMaxPercent, List<Variant> variants) {}
    public record CartItem(
            UUID id, UUID variantId, String productName, String variantLabel, String imageUrl,
            long unitPricePaise, int quantity, long lineTotalPaise, int availableStock) {}
    public record Cart(List<CartItem> items, long subtotalPaise) {}
    public record LoyaltyQuote(
            long balancePoints, long maximumRedeemablePoints, long pointValuePaise,
            long maximumDiscountPaise) {}
    public record CartRequest(@NotNull UUID variantId, @Min(1) @Max(99) int quantity) {}
    public record CheckoutRequest(
            @NotBlank String fulfilmentType, @NotBlank String paymentMethod,
            @NotBlank String recipientName, @NotBlank String mobile, String addressLine,
            String city, String state, String postalCode, @Min(0) Long loyaltyPoints) {}
    public record OrderItem(
            String sku, String productName, String variantLabel, long unitPricePaise,
            int quantity, long taxPaise, long lineTotalPaise) {}
    public record Order(
            UUID id, String orderNumber, String status, String fulfilmentType, String paymentMethod,
            String paymentStatus, long subtotalPaise, long taxPaise, long deliveryFeePaise,
            long originalTotalPaise, long loyaltyPointsUsed, long loyaltyDiscountPaise,
            long totalPaise, String recipientName, String mobile, String addressLine, String city,
            String state, String postalCode, Instant createdAt, List<OrderItem> items) {}
    public record CancelRequest(@NotBlank @Size(max = 300) String reason) {}
    public record CategoryRequest(
            @NotBlank String code, @NotBlank String name, String nameKn, String description,
            String imageUrl, @Min(0) int displayOrder,
            @DecimalMin("0") @DecimalMax("100") Double loyaltyMaxPercent, String status) {}
    public record ProductRequest(
            @NotNull UUID categoryId, @NotBlank String sku, @NotBlank String name,
            String nameKn, String description, String descriptionKn, String imageUrl,
            @DecimalMin("0") @DecimalMax("100") double taxPercent, String status,
            @DecimalMin("0") @DecimalMax("100") Double loyaltyMaxPercent) {}
    public record VariantRequest(
            @NotBlank String sku, @NotBlank String label, @Min(0) long pricePaise,
            @Min(0) int stockQuantity, @Min(0) int lowStockThreshold, String status) {}
    public record StockRequest(int quantityDelta, @NotBlank String reason) {}
    public record StatusRequest(@NotBlank String status) {}
}
