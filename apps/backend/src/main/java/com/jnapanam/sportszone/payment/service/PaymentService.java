package com.jnapanam.sportszone.payment.service;

import com.jnapanam.sportszone.booking.model.*;
import com.jnapanam.sportszone.booking.repository.BookingRepository;
import com.jnapanam.sportszone.booking.service.BookingService;
import com.jnapanam.sportszone.common.exception.*;
import com.jnapanam.sportszone.identity.model.User;
import com.jnapanam.sportszone.identity.repository.UserRepository;
import com.jnapanam.sportszone.membership.model.*;
import com.jnapanam.sportszone.membership.repository.CustomerMembershipRepository;
import com.jnapanam.sportszone.payment.config.PaymentProperties;
import com.jnapanam.sportszone.payment.dto.*;
import com.jnapanam.sportszone.payment.gateway.PaymentGateway;
import com.jnapanam.sportszone.payment.model.*;
import com.jnapanam.sportszone.payment.repository.*;
import com.jnapanam.sportszone.training.model.*;
import com.jnapanam.sportszone.training.repository.TrainingEnrollmentRepository;
import com.jnapanam.sportszone.notification.service.NotificationService;
import com.jnapanam.sportszone.tournament.model.*;
import com.jnapanam.sportszone.tournament.repository.TournamentTeamRepository;
import com.jnapanam.sportszone.loyalty.service.LoyaltyService;
import com.jnapanam.sportszone.promotion.service.PromotionService;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    private final PaymentOrderRepository orders;
    private final PaymentRefundRepository refunds;
    private final UserRepository users;
    private final BookingRepository bookings;
    private final CustomerMembershipRepository memberships;
    private final TrainingEnrollmentRepository enrollments;
    private final PaymentGateway gateway;
    private final PaymentProperties properties;
    private final PaymentWebhookEventRepository webhookEvents;
    private final NotificationService notificationService;
    private final TournamentTeamRepository tournamentTeams;
    private final BookingService bookingService;
    private final PromotionService promotionService;
    private final LoyaltyService loyaltyService;

    public PaymentService(PaymentOrderRepository o, PaymentRefundRepository r, UserRepository u, BookingRepository b,
            CustomerMembershipRepository m, TrainingEnrollmentRepository e, PaymentGateway g, PaymentProperties p,
            PaymentWebhookEventRepository w, NotificationService n, TournamentTeamRepository tt,
            BookingService bookingService, PromotionService promotionService, LoyaltyService loyaltyService) {
        orders = o;
        refunds = r;
        users = u;
        bookings = b;
        memberships = m;
        enrollments = e;
        gateway = g;
        properties = p;
        webhookEvents = w;
        notificationService = n;
        tournamentTeams = tt;
        this.bookingService = bookingService;
        this.promotionService = promotionService;
        this.loyaltyService = loyaltyService;
    }

    @Transactional
    public PaymentResponse create(UUID userId, String key, CreatePaymentRequest request) {
        if (key == null || key.isBlank() || key.length() > 100)
            throw new BadRequestException("A valid Idempotency-Key header is required");
        Optional<PaymentOrder> existing = orders.findByUserIdAndIdempotencyKey(userId, key);
        if (existing.isPresent())
            return response(existing.get());
        if (orders.existsByPurposeAndReferenceIdAndStatus(request.purpose(), request.referenceId(), PaymentStatus.PAID))
            throw new ConflictException("This item is already paid");
        User user = users.findById(userId).orElseThrow(() -> new UnauthorizedException("User not found"));
        long original = resolveAmount(userId, request.purpose(), request.referenceId());
        if (original <= 0)
            throw new ConflictException("Payment amount is not configured");
        var applied = promotionService.choose(request.couponCode(), original, request.purpose(), user);
        long requestedPoints = request.loyaltyPoints() == null ? 0 : request.loyaltyPoints();
        long points = loyaltyService.redeem(user, requestedPoints, original - applied.discountPaise());
        long loyaltyValue = points * loyaltyService.pointValue();
        PaymentOrder order = new PaymentOrder(user, request.purpose(), request.referenceId(), original,
                properties.currency(), gateway.provider(), key);
        order.pricing(original, applied.discountPaise(), applied.code(), points, loyaltyValue);
        orders.save(order);
        promotionService.redeem(applied, user, order);
        loyaltyService.recordRedemption(user, order, points);
        order.providerOrder(
                gateway.createOrder(order.getReceiptNumber(), order.getAmountPaise(), order.getCurrency()).id());
        return response(orders.save(order));
    }

    @Transactional
    public PaymentResponse verify(UUID userId, VerifyPaymentRequest request) {
        PaymentOrder order = own(userId, request.paymentOrderId());
        if (order.getStatus() == PaymentStatus.PAID)
            return response(order);
        if (order.getStatus() != PaymentStatus.CREATED)
            throw new ConflictException("Only a pending payment can be completed");
        resolveAmount(userId, order.getPurpose(), order.getReferenceId());
        if (!gateway.verifyPayment(order.getProviderOrderId(), request.providerPaymentId(), request.signature()))
            throw new BadRequestException("Invalid payment signature");
        order.paid(request.providerPaymentId());
        fulfil(order);
        loyaltyService.earn(order.getUser(), order);
        notificationService.payment(userId, order.getReceiptNumber(), order.getAmountPaise());
        return response(order);
    }

    @Transactional
    public PaymentResponse voidPayment(UUID id, VoidPaymentRequest request) {
        PaymentOrder order = orders.findById(id).orElseThrow(() -> new NotFoundException("Payment not found"));
        if (order.getStatus() != PaymentStatus.CREATED)
            throw new ConflictException("Only a pending payment can be voided");
        if (order.getPurpose() != PaymentPurpose.BOOKING)
            throw new ConflictException("Only booking payments can be voided");
        Booking booking = bookings.findById(order.getReferenceId())
                .orElseThrow(() -> new NotFoundException("Booking not found"));
        if (booking.getStatus() != BookingStatus.NO_SHOW && !booking.getSlot().getDate().isAfter(LocalDate.now()))
            throw new ConflictException("Void is available only for no-show or future bookings");
        promotionService.reverseRedemption(order);
        loyaltyService.reverseRedemption(order.getUser(), order);
        order.voidOrder(request.reason().trim());
        return response(order);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> mine(UUID userId) {
        return orders.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponse one(UUID userId, UUID id) {
        return response(own(userId, id));
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> all() {
        return orders.findAllByOrderByCreatedAtDesc().stream().map(this::response).toList();
    }

    @Transactional
    public PaymentResponse collectOnsite(UUID operatorId, UUID bookingId, OnsitePaymentRequest request) {
        Booking booking = bookings.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        if (booking.getStatus() != BookingStatus.CONFIRMED)
            throw new ConflictException("Only confirmed bookings can receive onsite payment");
        if (booking.getPaymentStatus() != BookingPaymentStatus.UNPAID)
            throw new ConflictException("Booking does not have an outstanding payment");
        User operator = users.findById(operatorId).orElseThrow(() -> new UnauthorizedException("Operator not found"));
        long amount = resolveAmount(booking.getUser().getId(), PaymentPurpose.BOOKING, bookingId);
        String key = "onsite-" + bookingId;
        PaymentOrder order = new PaymentOrder(booking.getUser(), PaymentPurpose.BOOKING, bookingId, amount,
                properties.currency(), "ONSITE", key);
        orders.save(order);
        order.providerOrder("onsite_order_" + order.getId());
        String reference = request.reference() == null || request.reference().isBlank() ? UUID.randomUUID().toString()
                : request.reference().trim();
        order.paid("onsite_" + bookingId + "_" + reference);
        booking.collectOnsite(operator, request.paymentMethod(), request.reference());
        loyaltyService.earn(booking.getUser(), order);
        return response(order);
    }

    @Transactional
    public RefundResponse refund(UUID id, RefundRequest request) {
        PaymentOrder order = orders.findById(id).orElseThrow(() -> new NotFoundException("Payment not found"));
        if (order.getStatus() != PaymentStatus.PAID && order.getStatus() != PaymentStatus.PARTIALLY_REFUNDED)
            throw new ConflictException("Only paid payments can be refunded");
        long already = refunds.findByPaymentOrderId(id).stream().filter(r -> r.getStatus() == RefundStatus.PROCESSED)
                .mapToLong(PaymentRefund::getAmountPaise).sum();
        long amount = request.amountPaise() == null ? order.getAmountPaise() - already : request.amountPaise();
        if (amount <= 0 || already + amount > order.getAmountPaise())
            throw new BadRequestException("Refund exceeds refundable amount");
        PaymentRefund refund = refunds.save(new PaymentRefund(order, amount, request.reason().trim()));
        refund.processed(gateway.refund(order.getProviderPaymentId(), amount, request.reason()).id());
        order.refundStatus(already + amount);
        if (already + amount >= order.getAmountPaise()
        && order.getPurpose() == PaymentPurpose.BOOKING) {
    bookings.findById(order.getReferenceId())
            .ifPresent(Booking::refunded);
}
        notificationService.refund(order.getUser().getId(), order.getReceiptNumber(), amount);
        return refundResponse(refund);
    }

    @Transactional
    public BookingCancellationRefundResponse cancelBookingAndRefund(UUID bookingId, String reason) {
        String safe = reason == null || reason.isBlank() ? "Cancelled and refunded by operations" : reason.trim();
        var booking = bookingService.adminCancel(bookingId, safe);
        PaymentOrder order = orders
                .findFirstByPurposeAndReferenceIdAndStatusInOrderByCreatedAtDesc(PaymentPurpose.BOOKING, bookingId,
                        List.of(PaymentStatus.PAID, PaymentStatus.PARTIALLY_REFUNDED))
                .orElseThrow(() -> new ConflictException("No refundable payment exists for this booking"));
        return new BookingCancellationRefundResponse(booking, refund(order.getId(), new RefundRequest(null, safe)));
    }

    @Transactional
    public void webhook(String eventId, String signature, String body, com.fasterxml.jackson.databind.JsonNode json) {
        if (eventId == null || eventId.isBlank())
            throw new BadRequestException("Missing webhook event id");
        if (webhookEvents.existsByProviderEventId(eventId))
            return;
        if (!gateway.verifyWebhook(body, signature))
            throw new BadRequestException("Invalid webhook signature");
        String type = json.path("event").asText();
        if ("payment.captured".equals(type)) {
            var payment = json.path("payload").path("payment").path("entity");
            String orderId = payment.path("order_id").asText();
            String paymentId = payment.path("id").asText();
            orders.findByProviderOrderId(orderId).ifPresent(o -> {
                if (o.getStatus() == PaymentStatus.CREATED) {
                    resolveAmount(o.getUser().getId(), o.getPurpose(), o.getReferenceId());
                    o.paid(paymentId);
                    fulfil(o);
                    loyaltyService.earn(o.getUser(), o);
                    notificationService.payment(o.getUser().getId(), o.getReceiptNumber(), o.getAmountPaise());
                }
            });
        }
        webhookEvents.save(new PaymentWebhookEvent(eventId, type, sha256(body)));
    }

    private long resolveAmount(UUID userId, PaymentPurpose purpose, UUID ref) {
        BigDecimal rupees = switch (purpose) {
            case BOOKING -> {
                Booking b = bookings.findById(ref).orElseThrow(() -> new NotFoundException("Booking not found"));
                owner(userId, b.getUser());
                if (b.getPaymentStatus() != BookingPaymentStatus.UNPAID
                        || (b.getStatus() != BookingStatus.PENDING_PAYMENT && b.getStatus() != BookingStatus.CONFIRMED)
                        || (b.getStatus() == BookingStatus.PENDING_PAYMENT && expired(b.getPaymentExpiresAt())))
                    throw new ConflictException("Booking is not awaiting payment");
                yield b.getSlot().getFacility().getBookingPrice().multiply(BigDecimal.valueOf(b.getQuantity()));
            }
            case MEMBERSHIP -> {
                CustomerMembership m = memberships.findById(ref)
                        .orElseThrow(() -> new NotFoundException("Membership not found"));
                owner(userId, m.getUser());
                if (m.getStatus() != MembershipStatus.PENDING_PAYMENT || expired(m.getPaymentExpiresAt()))
                    throw new ConflictException("Membership is not awaiting payment");
                yield m.getPlan().getPrice();
            }
            case TRAINING -> {
                TrainingEnrollment e = enrollments.findById(ref)
                        .orElseThrow(() -> new NotFoundException("Enrollment not found"));
                owner(userId, e.getUser());
                if (e.getStatus() != EnrollmentStatus.PENDING_PAYMENT || expired(e.getPaymentExpiresAt()))
                    throw new ConflictException("Training enrollment is not awaiting payment");
                yield e.getBatch().getFee();
            }
            case TOURNAMENT -> {
                TournamentTeam team = tournamentTeams.findById(ref)
                        .orElseThrow(() -> new NotFoundException("Tournament team not found"));
                owner(userId, team.getCaptain());
                if (team.getStatus() != TeamStatus.PENDING_PAYMENT || expired(team.getPaymentExpiresAt()))
                    throw new ConflictException("Tournament team is not awaiting payment");
                yield team.getTournament().getEntryFee();
            }
        };
        try {
            return rupees.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
        } catch (ArithmeticException e) {
            throw new BadRequestException("Invalid configured payment amount");
        }
    }

    private void fulfil(PaymentOrder o) {
        switch (o.getPurpose()) {
            case BOOKING -> bookings.findById(o.getReferenceId()).orElseThrow().paid();
            case MEMBERSHIP -> {
                CustomerMembership m = memberships.findById(o.getReferenceId()).orElseThrow();
                if (m.getStatus() == MembershipStatus.PENDING_PAYMENT)
                    m.activate(LocalDate.now());
            }
            case TRAINING -> enrollments.findById(o.getReferenceId()).orElseThrow().paid();
            case TOURNAMENT -> tournamentTeams.findById(o.getReferenceId()).orElseThrow().paid();
        }
    }

    private PaymentOrder own(UUID userId, UUID id) {
        PaymentOrder o = orders.findById(id).orElseThrow(() -> new NotFoundException("Payment not found"));
        owner(userId, o.getUser());
        return o;
    }

    private void owner(UUID id, User u) {
        if (!u.getId().equals(id))
            throw new NotFoundException("Payment reference not found");
    }

    private PaymentResponse response(PaymentOrder o) {
        return new PaymentResponse(o.getId(), o.getPurpose(), o.getReferenceId(), o.getReceiptNumber(),
                o.getOriginalAmountPaise(), o.getDiscountPaise(), o.getPromotionCode(), o.getLoyaltyPointsUsed(),
                o.getAmountPaise(), o.getCurrency(), o.getProvider(), o.getProviderOrderId(), o.getProviderPaymentId(),
                o.getStatus(), "ONSITE".equals(o.getProvider()) ? null : gateway.publicKey(),
                o.getLoyaltyPointsEarned(), o.getPaidAt(), o.getVoidedAt(), o.getVoidReason(), o.getCreatedAt());
    }

    private RefundResponse refundResponse(PaymentRefund r) {
        return new RefundResponse(r.getId(), r.getAmountPaise(), r.getReason(), r.getStatus(), r.getProviderRefundId());
    }

    private static String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean expired(Instant value) {
        return value != null && !value.isAfter(Instant.now());
    }
}
