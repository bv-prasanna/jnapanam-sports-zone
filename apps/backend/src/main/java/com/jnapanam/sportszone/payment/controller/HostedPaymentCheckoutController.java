package com.jnapanam.sportszone.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnapanam.sportszone.common.exception.BadRequestException;
import com.jnapanam.sportszone.common.exception.NotFoundException;
import com.jnapanam.sportszone.payment.dto.PaymentResponse;
import com.jnapanam.sportszone.payment.dto.VerifyPaymentRequest;
import com.jnapanam.sportszone.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class HostedPaymentCheckoutController {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final JdbcTemplate jdbc;
    private final PaymentService payments;
    private final ObjectMapper json;

    public HostedPaymentCheckoutController(JdbcTemplate jdbc, PaymentService payments, ObjectMapper json) {
        this.jdbc = jdbc;
        this.payments = payments;
        this.json = json;
    }

    @PostMapping("/api/v1/payments/{id}/checkout-session")
    public Map<String, String> create(@PathVariable UUID id, Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        PaymentResponse order = payments.one(userId, id);
        if (!"RAZORPAY".equals(order.provider()) || !"CREATED".equals(order.status().name())
                || order.checkoutKey() == null || order.checkoutKey().isBlank()
                || order.providerOrderId() == null || order.providerOrderId().isBlank()) {
            throw new BadRequestException("This order is not eligible for Razorpay checkout");
        }
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        jdbc.update("INSERT INTO payment_checkout_sessions(token_hash,payment_order_id,user_id,expires_at) VALUES (?,?,?,?)",
                hash(token), id, userId, java.sql.Timestamp.from(Instant.now().plusSeconds(900)));
        return Map.of("path", "/api/v1/public/payments/checkout/" + token);
    }

    @GetMapping(value = "/api/v1/public/payments/checkout/{token}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> page(@PathVariable String token) {
        Session session = session(token);
        PaymentResponse order = payments.one(session.userId(), session.paymentOrderId());
        if (!"RAZORPAY".equals(order.provider()) || !"CREATED".equals(order.status().name())) {
            throw new BadRequestException("Payment is no longer pending");
        }
        String configuration;
        try {
            configuration = json.writeValueAsString(Map.of(
                "key", order.checkoutKey(),
                "amount", order.amountPaise(),
                "currency", order.currency(),
                "name", "Jnapanam Sports Zone",
                "description", order.purpose().name(),
                "order_id", order.providerOrderId()
            )).replace("<", "\\u003c");
        } catch (Exception e) {
            throw new IllegalStateException("Unable to initialize payment checkout", e);
        }
        String html = """
            <!doctype html><html lang="en"><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width,initial-scale=1">
            <title>Jnapanam secure checkout</title>
            <style>body{font-family:system-ui,sans-serif;background:#f7fafc;color:#063b63;
            max-width:460px;margin:10vh auto;padding:24px;text-align:center}
            button{background:#0877b9;color:white;border:0;border-radius:10px;padding:14px 26px;font-size:17px}
            button:disabled{opacity:.55}</style></head><body>
            <h2>Jnapanam Sports Zone</h2><p>Secure payment through Razorpay</p>
            <button id="pay" disabled>Preparing checkout…</button><p id="status"></p>
            <script src="https://checkout.razorpay.com/v1/checkout.js"></script>
            <script>
            const options = __OPTIONS__;
            const button = document.getElementById('pay');
            const status = document.getElementById('status');
            if (!window.Razorpay) {
              status.textContent='Payment gateway unavailable. Please retry.';
            } else {
              button.disabled=false; button.textContent='Pay securely';
            }
            const openCheckout=()=>{
              button.disabled=true;
              const checkout=new Razorpay({...options,
                handler:async response=>{
                  status.textContent='Verifying payment securely…';
                  try {
                    const result=await fetch(window.location.pathname+'/verify',{
                      method:'POST',headers:{'Content-Type':'application/json'},
                      body:JSON.stringify({
                        providerPaymentId:response.razorpay_payment_id,
                        signature:response.razorpay_signature
                      })
                    });
                    if(!result.ok) throw new Error('Verification was not confirmed');
                    status.textContent='Payment successful. Return to Jnapanam app.';
                    button.style.display='none';
                  } catch(e) {
                    status.textContent='Payment verification pending. Return to app and refresh; do not pay again until status is checked.';
                  }
                },
                modal:{ondismiss:()=>{button.disabled=false;status.textContent='Payment cancelled; no charge confirmed.';}}
              });
              checkout.on('payment.failed',()=>{
                button.disabled=false;
                status.textContent='Payment failed. You may retry.';
              });
              checkout.open();
            };
            button.addEventListener('click',openCheckout);
            </script></body></html>
            """.replace("__OPTIONS__", configuration);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("Referrer-Policy", "no-referrer")
                .header("X-Content-Type-Options", "nosniff")
                .body(html);
    }

    public record CheckoutVerification(String providerPaymentId, String signature) {}

    @PostMapping("/api/v1/public/payments/checkout/{token}/verify")
    public PaymentResponse verify(@PathVariable String token, @RequestBody CheckoutVerification request) {
        Session session = session(token);
        if (request == null || request.providerPaymentId() == null || request.providerPaymentId().isBlank()
                || request.signature() == null || request.signature().isBlank()) {
            throw new BadRequestException("Missing Razorpay verification fields");
        }
        PaymentResponse verified = payments.verify(session.userId(),
                new VerifyPaymentRequest(session.paymentOrderId(), request.providerPaymentId(), request.signature()));
        if ("PAID".equals(verified.status().name())) {
            jdbc.update("DELETE FROM payment_checkout_sessions WHERE token_hash=?", hash(token));
        }
        return verified;
    }

    private Session session(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) {
            throw new NotFoundException("Checkout session not found");
        }
        Session result = jdbc.query("SELECT payment_order_id,user_id FROM payment_checkout_sessions WHERE token_hash=? AND expires_at > now()",
                rs -> rs.next() ? new Session((UUID) rs.getObject(1), (UUID) rs.getObject(2)) : null, hash(token));
        return result == null ? missing() : result;
    }

    private static Session missing() {
        throw new NotFoundException("Checkout session expired or unavailable");
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private record Session(UUID paymentOrderId, UUID userId) {}
}
