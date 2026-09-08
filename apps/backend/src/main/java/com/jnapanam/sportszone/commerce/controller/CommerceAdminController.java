package com.jnapanam.sportszone.commerce.controller;

import static com.jnapanam.sportszone.commerce.dto.CommerceDtos.*;

import com.jnapanam.sportszone.commerce.service.CommerceAdminService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/commerce")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','OPERATIONS')")
public class CommerceAdminController {
    private final CommerceAdminService service;

    public CommerceAdminController(CommerceAdminService service) {
        this.service = service;
    }

    @GetMapping("/settings")
    public ShopSettings settings() { return service.settings(); }

    @PutMapping("/settings")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> settings(@Valid @RequestBody ShopSettingsRequest request) {
        service.updateSettings(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/categories")
    public List<Category> categories() { return service.categories(); }

    @GetMapping("/products")
    public List<Product> products() { return service.products(); }

    @GetMapping("/orders")
    public List<Map<String, Object>> orders() { return service.orders(); }

    @PostMapping("/categories")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Map<String, UUID>> category(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(201).body(Map.of("id", service.category(request)));
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Void> category(@PathVariable UUID id,
            @Valid @RequestBody CategoryRequest request) {
        service.updateCategory(id, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/products")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Map<String, UUID>> product(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(201).body(Map.of("id", service.product(request)));
    }

    @PutMapping("/products/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Void> product(@PathVariable UUID id,
            @Valid @RequestBody ProductRequest request) {
        service.updateProduct(id, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/products/{id}/variants")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Map<String, UUID>> createVariant(@PathVariable UUID id,
            @Valid @RequestBody VariantRequest request) {
        return ResponseEntity.status(201).body(Map.of("id", service.variant(id, request)));
    }

    @PutMapping("/variants/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Void> updateVariant(@PathVariable UUID id,
            @Valid @RequestBody VariantRequest request) {
        service.updateVariant(id, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/variants/{id}/stock")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Void> stock(Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody StockRequest request) {
        service.stock(UUID.fromString(authentication.getName()), id, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/orders/{id}/status")
    public ResponseEntity<Void> status(@PathVariable UUID id,
            @Valid @RequestBody StatusRequest request) {
        service.orderStatus(id, request.status());
        return ResponseEntity.noContent().build();
    }
}
