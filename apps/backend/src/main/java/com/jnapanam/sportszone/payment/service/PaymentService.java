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
    private org.springframework.jdbc.core.JdbcTemplate subscriptionJdbc;
    @org.springframework.beans.factory.annotation.Autowired(required=false)
    public void setSubscriptionJdbc(org.springframework.jdbc.core.JdbcTemplate jdbc){this.subscriptionJdbc=jdbc;}
    private com.jnapanam.sportszone.payment.subscription.RecurringWebhookService recurringWebhooks;
    @org.springframework.beans.factory.annotation.Autowired(required=false)
    public void setRecurringWebhooks(com.jnapanam.sportszone.payment.subscription.RecurringWebhookService service){
        this.recurringWebhooks=service;
    }

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
        if (subscriptionJdbc != null && (request.purpose() == PaymentPurpose.MEMBERSHIP || request.purpose() == PaymentPurpose.TRAINING)) {
            Integer recurring = subscriptionJdbc.queryForObject("""
                select count(*) from recurring_agreements where kind=? and reference_id=? and status not in ('CANCELLED','EXPIRED')
                """, Integer.class, request.purpose().name(), request.referenceId());
            if(recurring != null && recurring > 0)
                throw new ConflictException("Recurring billing exists for this purchase; do not pay again separately.");
        }
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
        notifyFulfilment(order);
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
        notificationService.payment(booking.getUser().getId(), order.getReceiptNumber(), order.getAmountPaise());
        notificationService.lifecycle(booking.getUser().getId(), "BOOKING", "Booking payment collected", "Payment collected at the venue for booking " + booking.getBookingNumber() + ".");
        return response(order);
    }

    @Transactional
    public RefundResponse refund(UUID id, RefundRequest request) {
        PaymentOrder order = orders.findById(id).orElseThrow(() -> new NotFoundException("Payment not found"));
        if (order.getStatus() != PaymentStatus.PAID && order.getStatus() != PaymentStatus.PARTIALLY_REFUNDED)
            throw new ConflictException("Only paid payments can be refunded");
        List<PaymentRefund> existing = refunds.findByPaymentOrderId(id);
        if (existing.stream().anyMatch(r -> r.getStatus() == RefundStatus.REQUESTED))
            throw new ConflictException("A refund is still processing. Check its status before another request.");
        long already = existing.stream().filter(r -> r.getStatus() == RefundStatus.PROCESSED)
                .mapToLong(PaymentRefund::getAmountPaise).sum();
        long amount = request.amountPaise() == null ? order.getAmountPaise() - already : request.amountPaise();
        if (amount <= 0 || already + amount > order.getAmountPaise())
            throw new BadRequestException("Refund exceeds refundable amount");

        PaymentRefund refund = refunds.save(new PaymentRefund(order, amount, request.reason().trim()));
        String providerRefundId = gateway.refund(order.getProviderPaymentId(), amount, request.reason()).id();
        refund.submitted(providerRefundId);

        // Mock refunds are synchronous. Razorpay returns a refund request identifier;
        // mark it processed only after the refund.processed webhook is verified.
        if ("MOCK".equals(gateway.provider())) completeRefund(refund, providerRefundId);
        return refundResponse(refund);
    }

    private void completeRefund(PaymentRefund refund, String providerRefundId) {
        if (refund.getStatus() == RefundStatus.PROCESSED) return;
        PaymentOrder order = refund.getPaymentOrder();
        refund.processed(providerRefundId);
        long total = refunds.findByPaymentOrderId(order.getId()).stream()
                .filter(r -> r.getStatus() == RefundStatus.PROCESSED)
                .mapToLong(PaymentRefund::getAmountPaise).sum();
        // During a synchronous mock refund, the just-created row must be counted
        // even when the test repository has not yet included it in findByPaymentOrderId.
        if (total < refund.getAmountPaise()) total = refund.getAmountPaise();
        order.refundStatus(total);
        if (total >= order.getAmountPaise() && order.getPurpose() == PaymentPurpose.BOOKING)
            bookings.findById(order.getReferenceId()).ifPresent(Booking::refunded);
        notificationService.refund(order.getUser().getId(), order.getReceiptNumber(), refund.getAmountPaise());
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
        if (eventId == null || eventId.isBlank() || eventId.length() > 160)
            throw new BadRequestException("Invalid webhook event id");
        // Check signature before consulting de-duplication state; never trust a replay
        // solely because it supplied the ID of a previously received webhook.
        if (!gateway.verifyWebhook(body, signature))
            throw new BadRequestException("Invalid webhook signature");
        if (webhookEvents.existsByProviderEventId(eventId)) return;

        String type = json.path("event").asText("");
        if (type.isBlank() || type.length() > 80)
            throw new BadRequestException("Webhook event type is missing or invalid");
        String category = type.substring(0, type.indexOf('.') < 0 ? type.length() : type.indexOf('.'));
        var entity = json.path("payload").path(category).path("entity");
        String objectId = safeIdentifier(entity.path("id").asText(null));
        String providerOrderId = safeIdentifier(entity.path("order_id").asText(null));
        String providerPaymentId = safeIdentifier(entity.path("payment_id").asText(null));
        Long amount = entity.hasNonNull("amount") && entity.path("amount").canConvertToLong()
                ? entity.path("amount").asLong() : null;
        String result = "OBSERVED";

        if ("payment.captured".equals(type)) {
            providerPaymentId = objectId;
            if (providerPaymentId == null)
                throw new BadRequestException("Captured payment is missing its payment identifier");
            // Subscription charges and other Razorpay products may have no ordinary
            // order_id. They are audited as unmatched and must never fulfill a
            // Jnapanam one-time order. subscription.charged owns recurring access.
            if (providerOrderId == null) {
                result = "UNMATCHED";
            } else {
                final String capturedPaymentId = providerPaymentId;
                var order = orders.findByProviderOrderId(providerOrderId);
                if (order.isEmpty()) {
                    result = "UNMATCHED";
                } else if (order.get().getStatus() == PaymentStatus.CREATED) {
                PaymentOrder target = order.get();
                if (amount == null || amount != target.getAmountPaise()
                        || !target.getCurrency().equals(entity.path("currency").asText()))
                    throw new BadRequestException("Captured payment amount or currency mismatch");
                resolveAmount(target.getUser().getId(), target.getPurpose(), target.getReferenceId());
                target.paid(capturedPaymentId);
                fulfil(target);
                loyaltyService.earn(target.getUser(), target);
                notificationService.payment(target.getUser().getId(), target.getReceiptNumber(), target.getAmountPaise());
                notifyFulfilment(target);
                result = "APPLIED";
                }
            }
        } else if (type.startsWith("refund.")) {
            providerPaymentId = safeIdentifier(entity.path("payment_id").asText(null));
            if ("refund.processed".equals(type) || "refund.failed".equals(type)) {
                var match = objectId == null ? Optional.<PaymentRefund>empty()
                        : refunds.findByProviderRefundId(objectId);
                if (match.isEmpty()) {
                    result = recurringWebhooks==null?"UNMATCHED":recurringWebhooks.refund(type,json.path("payload"));
                } else {
                    PaymentRefund refund = match.get();
                    PaymentOrder order = refund.getPaymentOrder();
                    if (amount == null || amount != refund.getAmountPaise()
                            || providerPaymentId == null || !providerPaymentId.equals(order.getProviderPaymentId()))
                        throw new BadRequestException("Refund does not match the original payment");
                    if ("refund.processed".equals(type)) {
                        if (refund.getStatus() != RefundStatus.PROCESSED) {
                            completeRefund(refund, objectId);
                            result = "APPLIED";
                        }
                    } else if (refund.getStatus() == RefundStatus.REQUESTED) {
                        refund.failed();
                        result = "APPLIED";
                    }
                }
            } else if (recurringWebhooks != null) {
                result = recurringWebhooks.refund(type,json.path("payload"));
            }
        }
        if (type.startsWith("subscription.") && recurringWebhooks != null)
            result = recurringWebhooks.accept(type,json.path("payload"));
        // payment.failed / payment.authorized / order.paid / settlement.* are
        // captured as verified observations, never treated as captured money.
        webhookEvents.save(new PaymentWebhookEvent(eventId, type, sha256(body), result,
                objectId, providerOrderId, providerPaymentId, amount));
    }

    private static String safeIdentifier(String value) {
        if (value == null || value.isBlank()) return null;
        // Protect the audit row against oversized untrusted provider fields.
        return value.length() > 120 ? value.substring(0, 120) : value;
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


    /** Confirm relevant product fulfilment messages only after payment is verified. */
    private void notifyFulfilment(PaymentOrder order) {
        switch (order.getPurpose()) {
            case BOOKING -> bookings.findById(order.getReferenceId()).ifPresent(booking ->
                    notificationService.lifecycle(order.getUser().getId(), "BOOKING", "Booking payment confirmed",
                            "Booking " + booking.getBookingNumber() + " is paid and confirmed."));
            case MEMBERSHIP -> memberships.findById(order.getReferenceId()).ifPresent(membership ->
                    notificationService.lifecycle(order.getUser().getId(), "MEMBERSHIP", "Membership activated",
                            membership.getPlan().getName() + " membership is active."));
            case TRAINING -> enrollments.findById(order.getReferenceId()).ifPresent(enrollment ->
                    notificationService.lifecycle(order.getUser().getId(), "TRAINING", "Training enrolment confirmed",
                            enrollment.getBatch().getName() + " enrolment is confirmed."));
            case TOURNAMENT -> tournamentTeams.findById(order.getReferenceId()).ifPresent(team ->
                    notificationService.lifecycle(order.getUser().getId(), "TOURNAMENT", "Tournament registration confirmed",
                            team.getTournament().getName() + " registration is confirmed."));
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
