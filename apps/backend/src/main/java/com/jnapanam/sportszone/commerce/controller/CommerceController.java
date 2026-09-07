package com.jnapanam.sportszone.commerce.controller;
import static com.jnapanam.sportszone.commerce.dto.CommerceDtos.*;import com.jnapanam.sportszone.commerce.service.CommerceService;import jakarta.validation.Valid;import java.util.*;import org.springframework.http.*;import org.springframework.security.core.Authentication;import org.springframework.web.bind.annotation.*;
@RestController@RequestMapping("/api/v1/commerce")public class CommerceController{private final CommerceService service;public CommerceController(CommerceService service){this.service=service;}
 private UUID user(Authentication a){if(a==null||!a.isAuthenticated())throw new org.springframework.security.access.AccessDeniedException("Login required");return UUID.fromString(a.getName());}
 @GetMapping("/categories")public List<Category> categories(){return service.categories();}
 @GetMapping("/products")public List<Product> products(@RequestParam(required=false)String category){return service.products(category);}
 @GetMapping("/products/{id}")public Product product(@PathVariable UUID id){return service.product(id);}
 @GetMapping("/cart")public Cart cart(Authentication a){return service.cart(user(a));}
 @PutMapping("/cart/items")public Cart put(Authentication a,@Valid@RequestBody CartRequest r){return service.putCart(user(a),r);}
 @DeleteMapping("/cart/items/{variantId}")public Cart remove(Authentication a,@PathVariable UUID variantId){return service.removeCart(user(a),variantId);}
 @PostMapping("/checkout")public ResponseEntity<Order> checkout(Authentication a,@Valid@RequestBody CheckoutRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(service.checkout(user(a),r));}
 @GetMapping("/orders")public List<Order> orders(Authentication a){return service.orders(user(a));}
 @PostMapping("/orders/{id}/cancel")public Order cancel(Authentication a,@PathVariable UUID id,@Valid@RequestBody CancelRequest r){return service.cancel(user(a),id,r.reason());}
}
