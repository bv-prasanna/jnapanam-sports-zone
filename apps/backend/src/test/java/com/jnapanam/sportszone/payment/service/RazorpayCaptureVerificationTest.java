package com.jnapanam.sportszone.payment.service;
import com.jnapanam.sportszone.booking.model.*;
import com.jnapanam.sportszone.booking.repository.BookingRepository;
import com.jnapanam.sportszone.booking.service.BookingService;
import com.jnapanam.sportszone.common.exception.*;
import com.jnapanam.sportszone.identity.model.User;
import com.jnapanam.sportszone.identity.repository.UserRepository;
import com.jnapanam.sportszone.loyalty.service.LoyaltyService;
import com.jnapanam.sportszone.membership.repository.CustomerMembershipRepository;
import com.jnapanam.sportszone.notification.service.NotificationService;
import com.jnapanam.sportszone.payment.config.PaymentProperties;
import com.jnapanam.sportszone.payment.dto.VerifyPaymentRequest;
import com.jnapanam.sportszone.payment.gateway.PaymentGateway;
import com.jnapanam.sportszone.payment.model.*;
import com.jnapanam.sportszone.payment.repository.*;
import com.jnapanam.sportszone.promotion.service.PromotionService;
import com.jnapanam.sportszone.tournament.repository.TournamentTeamRepository;
import com.jnapanam.sportszone.training.repository.TrainingEnrollmentRepository;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RazorpayCaptureVerificationTest {
 final PaymentOrderRepository orders=mock(PaymentOrderRepository.class);
 final BookingRepository bookings=mock(BookingRepository.class);
 final PaymentGateway gateway=mock(PaymentGateway.class);
 final NotificationService notifications=mock(NotificationService.class);
 final LoyaltyService loyalty=mock(LoyaltyService.class);
 final User user=mock(User.class);
 final Booking booking=mock(Booking.class);
 final UUID userId=UUID.randomUUID(),bookingId=UUID.randomUUID();
 final PaymentOrder order=new PaymentOrder(user,PaymentPurpose.BOOKING,bookingId,10000,"INR","RAZORPAY","key");
 final PaymentService service=new PaymentService(orders,mock(PaymentRefundRepository.class),
    mock(UserRepository.class),bookings,mock(CustomerMembershipRepository.class),
    mock(TrainingEnrollmentRepository.class),gateway,
    new PaymentProperties("razorpay","rzp_live_dummy","secret","webhook","INR"),
    mock(PaymentWebhookEventRepository.class),notifications,mock(TournamentTeamRepository.class),
    mock(BookingService.class),mock(PromotionService.class),loyalty);
 final VerifyPaymentRequest callback=new VerifyPaymentRequest(order.getId(),"pay_abc123","sig");
 @BeforeEach void setup(){
  when(user.getId()).thenReturn(userId);
  order.providerOrder("order_abc123");
  when(orders.findById(order.getId())).thenReturn(Optional.of(order));
  when(bookings.findById(bookingId)).thenReturn(Optional.of(booking));
  when(booking.getUser()).thenReturn(user);
  when(booking.getPaymentStatus()).thenReturn(BookingPaymentStatus.UNPAID);
  when(booking.getStatus()).thenReturn(BookingStatus.PENDING_PAYMENT);
  when(booking.getPriceTotalPaise()).thenReturn(10000L);
  when(gateway.provider()).thenReturn("RAZORPAY");
  when(gateway.verifyPayment("order_abc123","pay_abc123","sig")).thenReturn(true);
 }
 @Test void signatureWithoutCaptureCannotFulfil(){
  when(gateway.fetchPayment("pay_abc123")).thenReturn(
    new PaymentGateway.CapturedPayment("order_abc123",10000L,"INR",false));
  assertThrows(ConflictException.class,()->service.verify(userId,callback));
  assertEquals(PaymentStatus.CREATED,order.getStatus());
  verify(booking,never()).paid();
  verifyNoInteractions(notifications,loyalty);
 }
 @Test void wrongOrderAmountOrCurrencyCannotFulfil(){
  for(var state:List.of(
    new PaymentGateway.CapturedPayment("order_other",10000L,"INR",true),
    new PaymentGateway.CapturedPayment("order_abc123",100L,"INR",true),
    new PaymentGateway.CapturedPayment("order_abc123",10000L,"USD",true))){
   when(gateway.fetchPayment("pay_abc123")).thenReturn(state);
   assertThrows(BadRequestException.class,()->service.verify(userId,callback));
   assertEquals(PaymentStatus.CREATED,order.getStatus());
  }
  verify(booking,never()).paid();
 }
 @Test void invalidSignatureDoesNotFetchPayment(){
  when(gateway.verifyPayment(anyString(),anyString(),anyString())).thenReturn(false);
  assertThrows(BadRequestException.class,()->service.verify(userId,callback));
  verify(gateway,never()).fetchPayment(anyString());
 }
 @Test void exactCapturedPaymentIsFulfilled(){
  when(gateway.fetchPayment("pay_abc123")).thenReturn(
    new PaymentGateway.CapturedPayment("order_abc123",10000L,"INR",true));
  assertEquals(PaymentStatus.PAID,service.verify(userId,callback).status());
  verify(booking).paid();
  verify(loyalty).earn(user,order);
 }
}