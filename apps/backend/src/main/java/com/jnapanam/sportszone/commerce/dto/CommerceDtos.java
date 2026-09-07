package com.jnapanam.sportszone.commerce.dto;
import jakarta.validation.constraints.*;import java.time.Instant;import java.util.*;
public final class CommerceDtos{private CommerceDtos(){}
 public record Category(UUID id,String code,String name,String nameKn,String description,String imageUrl,int displayOrder,String status){}
 public record Variant(UUID id,String sku,String label,long pricePaise,int stockQuantity,String status){}
 public record Product(UUID id,String categoryCode,String sku,String name,String nameKn,String description,String descriptionKn,String imageUrl,double taxPercent,String status,List<Variant> variants){}
 public record CartItem(UUID id,UUID variantId,String productName,String variantLabel,String imageUrl,long unitPricePaise,int quantity,long lineTotalPaise,int availableStock){}
 public record Cart(List<CartItem> items,long subtotalPaise){}
 public record CartRequest(@NotNull UUID variantId,@Min(1)@Max(99)int quantity){}
 public record CheckoutRequest(@NotBlank String fulfilmentType,@NotBlank String paymentMethod,@NotBlank String recipientName,@NotBlank String mobile,String addressLine,String city,String state,String postalCode){}
 public record OrderItem(String sku,String productName,String variantLabel,long unitPricePaise,int quantity,long taxPaise,long lineTotalPaise){}
 public record Order(UUID id,String orderNumber,String status,String fulfilmentType,String paymentMethod,String paymentStatus,long subtotalPaise,long taxPaise,long deliveryFeePaise,long totalPaise,String recipientName,String mobile,String addressLine,String city,String state,String postalCode,Instant createdAt,List<OrderItem> items){}
 public record CancelRequest(@NotBlank@Size(max=300)String reason){}
 public record CategoryRequest(@NotBlank String code,@NotBlank String name,String nameKn,String description,String imageUrl,@Min(0)int displayOrder){}
 public record ProductRequest(@NotNull UUID categoryId,@NotBlank String sku,@NotBlank String name,String nameKn,String description,String descriptionKn,String imageUrl,@DecimalMin("0")@DecimalMax("100")double taxPercent,String status){}
 public record VariantRequest(@NotBlank String sku,@NotBlank String label,@Min(0)long pricePaise,@Min(0)int stockQuantity,@Min(0)int lowStockThreshold){}
 public record StockRequest(int quantityDelta,@NotBlank String reason){}
 public record StatusRequest(@NotBlank String status){}
}
