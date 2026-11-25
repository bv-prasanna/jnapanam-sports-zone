package com.jnapanam.sportszone.payment.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.jnapanam.sportszone.booking.repository.BookingRepository;
import com.jnapanam.sportszone.booking.service.BookingService;
import com.jnapanam.sportszone.identity.model.User;
import com.jnapanam.sportszone.identity.repository.UserRepository;
import com.jnapanam.sportszone.loyalty.service.LoyaltyService;
import com.jnapanam.sportszone.membership.repository.CustomerMembershipRepository;
import com.jnapanam.sportszone.notification.service.NotificationService;
import com.jnapanam.sportszone.payment.config.PaymentProperties;
import com.jnapanam.sportszone.payment.dto.CreatePaymentRequest;
import com.jnapanam.sportszone.payment.gateway.PaymentGateway;
import com.jnapanam.sportszone.payment.model.*;
import com.jnapanam.sportszone.payment.repository.*;
import com.jnapanam.sportszone.promotion.service.PromotionService;
import com.jnapanam.sportszone.tournament.repository.TournamentTeamRepository;
import com.jnapanam.sportszone.training.repository.TrainingEnrollmentRepository;
import com.jnapanam.sportszone.common.exception.ConflictException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentIdempotencyKeyTest {
    private final PaymentOrderRepository orders=mock(PaymentOrderRepository.class);
    private final PaymentGateway gateway=mock(PaymentGateway.class);
    private final PaymentService service=new PaymentService(orders,mock(PaymentRefundRepository.class),
        mock(UserRepository.class),mock(BookingRepository.class),mock(CustomerMembershipRepository.class),
        mock(TrainingEnrollmentRepository.class),gateway,
        new PaymentProperties("razorpay","rzp_test_dummy","testsecret","webhooksecret","INR"),
        mock(PaymentWebhookEventRepository.class),mock(NotificationService.class),
        mock(TournamentTeamRepository.class),mock(BookingService.class),mock(PromotionService.class),
        mock(LoyaltyService.class));
    private final UUID userId=UUID.randomUUID();
    private final UUID reference=UUID.randomUUID();
    private PaymentOrder prior;

    @BeforeEach void setup(){
        User user=mock(User.class);
        when(user.getId()).thenReturn(userId);
        prior=new PaymentOrder(user,PaymentPurpose.MEMBERSHIP,reference,8500,"INR","RAZORPAY","same-key");
        prior.pricing(10000,1000,"SAVE10",10,500);
        when(orders.findByUserIdAndIdempotencyKey(userId,"same-key")).thenReturn(Optional.of(prior));
        when(gateway.publicKey()).thenReturn("rzp_test_dummy");
    }

    @Test void sameKeyAndSameRequestReturnsTheOriginalOrder(){
        var response=service.create(userId,"same-key",
            new CreatePaymentRequest(PaymentPurpose.MEMBERSHIP,reference," save10 ",10L));
        assertEquals(prior.getId(),response.id());
        verify(orders,never()).save(any());
        verify(gateway).publicKey();
        verify(gateway,never()).createOrder(anyString(),anyLong(),anyString());
    }

    @Test void sameKeyCannotBeReusedForAnotherPurchase(){
        assertThrows(ConflictException.class,()->service.create(userId,"same-key",
            new CreatePaymentRequest(PaymentPurpose.TRAINING,reference,"SAVE10",10L)));
        verify(orders,never()).save(any());
        verifyNoInteractions(gateway);
    }

    @Test void sameKeyCannotChangeCouponOrLoyaltyRedemption(){
        assertThrows(ConflictException.class,()->service.create(userId,"same-key",
            new CreatePaymentRequest(PaymentPurpose.MEMBERSHIP,reference,"OTHER",10L)));
        assertThrows(ConflictException.class,()->service.create(userId,"same-key",
            new CreatePaymentRequest(PaymentPurpose.MEMBERSHIP,reference,"SAVE10",11L)));
        verify(orders,never()).save(any());
        verifyNoInteractions(gateway);
    }
}
