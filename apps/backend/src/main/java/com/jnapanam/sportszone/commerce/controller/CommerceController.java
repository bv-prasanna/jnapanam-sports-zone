package com.jnapanam.sportszone.commerce.controller;

import static com.jnapanam.sportszone.commerce.dto.CommerceDtos.*;

import com.jnapanam.sportszone.commerce.service.CommerceService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/commerce")
public class CommerceController {
    private final CommerceService service;

    public CommerceController(CommerceService service) {
        this.service = service;
    }

    private UUID user(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.security.access.AccessDeniedException("Login required");
        }
        return UUID.fromString(authentication.getName());
    }

    @GetMapping("/settings")
    public ShopSettings settings() { return service.settings(); }

    @GetMapping("/categories")
    public List<Category> categories() { return service.categories(); }

    @GetMapping("/products")
    public List<Product> products(@RequestParam(required = false) String category) {
        return service.products(category);
    }

    @GetMapping("/products/{id}")
    public Product product(@PathVariable UUID id) { return service.product(id); }

    @GetMapping("/cart")
    public Cart cart(Authentication authentication) { return service.cart(user(authentication)); }

    @GetMapping("/cart/loyalty-quote")
    public LoyaltyQuote loyaltyQuote(Authentication authentication) {
        return service.loyaltyQuote(user(authentication));
    }

    @PutMapping("/cart/items")
    public Cart put(Authentication authentication, @Valid @RequestBody CartRequest request) {
        return service.putCart(user(authentication), request);
    }

    @DeleteMapping("/cart/items/{variantId}")
    public Cart remove(Authentication authentication, @PathVariable UUID variantId) {
        return service.removeCart(user(authentication), variantId);
    }

    @PostMapping("/checkout")
    public ResponseEntity<Order> checkout(Authentication authentication,
            @Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.checkout(user(authentication), request));
    }

    @GetMapping("/orders")
    public List<Order> orders(Authentication authentication) {
        return service.orders(user(authentication));
    }

    @PostMapping("/orders/{id}/cancel")
    public Order cancel(Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody CancelRequest request) {
        return service.cancel(user(authentication), id, request.reason());
    }
}
