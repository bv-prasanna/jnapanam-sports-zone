package com.jnapanam.sportszone.payment.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.jnapanam.sportszone.booking.model.Booking;
import com.jnapanam.sportszone.booking.model.BookingStatus;
import com.jnapanam.sportszone.booking.repository.BookingRepository;
import com.jnapanam.sportszone.booking.service.BookingService;
import com.jnapanam.sportszone.common.exception.BadRequestException;
import com.jnapanam.sportszone.common.exception.ConflictException;
import com.jnapanam.sportszone.identity.model.User;
import com.jnapanam.sportszone.identity.repository.UserRepository;
import com.jnapanam.sportszone.loyalty.service.LoyaltyService;
import com.jnapanam.sportszone.membership.repository.CustomerMembershipRepository;
import com.jnapanam.sportszone.notification.service.NotificationService;
import com.jnapanam.sportszone.payment.config.PaymentProperties;
import com.jnapanam.sportszone.payment.dto.RefundRequest;
import com.jnapanam.sportszone.payment.gateway.PaymentGateway;
import com.jnapanam.sportszone.payment.gateway.RefundGatewayUncertainException;
import com.jnapanam.sportszone.payment.model.*;
import com.jnapanam.sportszone.payment.repository.*;
import com.jnapanam.sportszone.promotion.service.PromotionService;
import com.jnapanam.sportszone.tournament.repository.TournamentTeamRepository;
import com.jnapanam.sportszone.training.repository.TrainingEnrollmentRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AdminRefundDeductionTest {
 final PaymentOrderRepository orders=mock(PaymentOrderRepository.class);
 final PaymentRefundRepository refunds=mock(PaymentRefundRepository.class);
 final BookingRepository bookings=mock(BookingRepository.class);
 final PaymentGateway gateway=mock(PaymentGateway.class);
 final PaymentService service=new PaymentService(orders,refunds,mock(UserRepository.class),bookings,
    mock(CustomerMembershipRepository.class),mock(TrainingEnrollmentRepository.class),gateway,
    new PaymentProperties("razorpay","rzp_test_dummy","testsecret","webhooksecret","INR"),
    mock(PaymentWebhookEventRepository.class),mock(NotificationService.class),
    mock(TournamentTeamRepository.class),mock(BookingService.class),mock(PromotionService.class),
    mock(LoyaltyService.class));
 PaymentOrder order;
 Booking booking;

 @BeforeEach void setup() {
    User customer=mock(User.class);
    order=new PaymentOrder(customer,PaymentPurpose.BOOKING,UUID.randomUUID(),15000L,"INR","RAZORPAY","refund-test");
    order.providerOrder("order_1234");
    order.paid("pay_1234");
    booking=mock(Booking.class);
    when(booking.getStatus()).thenReturn(BookingStatus.CANCELLED);
    when(bookings.findById(order.getReferenceId())).thenReturn(Optional.of(booking));
    when(orders.findLockedById(order.getId())).thenReturn(Optional.of(order));
    when(refunds.findByPaymentOrderId(order.getId())).thenReturn(List.of());
    when(refunds.save(any(PaymentRefund.class))).thenAnswer(invocation->invocation.getArgument(0));
    when(gateway.provider()).thenReturn("RAZORPAY");
    when(gateway.refund(eq("pay_1234"),anyLong(),anyString(),anyString()))
      .thenReturn(new PaymentGateway.Refund("rfnd_1234"));
 }

 @Test void cancelledBookingCanRefundNetAfterRetainingPlatformCharge() {
    var result=service.refund(order.getId(),new RefundRequest(13000L,"Customer cancellation and venue fee",2000L,"Platform processing charge"));
    assertEquals(13000L,result.amountPaise());
    assertEquals(2000L,result.deductionPaise());
    assertEquals("Platform processing charge",result.deductionDescription());
    assertEquals(RefundStatus.REQUESTED,result.status());
    assertNotNull(result.requestedAt());
    var key=ArgumentCaptor.forClass(String.class);
    verify(gateway).refund(eq("pay_1234"),eq(13000L),anyString(),key.capture());
    assertTrue(key.getValue().length()>=10);
 }

 @Test void pastNoShowRequiresGenuineExceptionReason() {
    when(booking.getStatus()).thenReturn(BookingStatus.NO_SHOW);
    assertThrows(BadRequestException.class,
      ()->service.refund(order.getId(),new RefundRequest(14000L,"no",1000L)));
    var result=service.refund(order.getId(),new RefundRequest(14000L,
      "Hospital emergency: approved partial no-show refund",1000L,"Venue cancellation fee"));
    assertEquals(14000L,result.amountPaise());
    verify(gateway).refund(eq("pay_1234"),eq(14000L),anyString(),anyString());
 }

 @Test void confirmedFutureBookingMustFirstBeCancelled() {
    when(booking.getStatus()).thenReturn(BookingStatus.CONFIRMED);
    assertThrows(ConflictException.class,
      ()->service.refund(order.getId(),new RefundRequest(13000L,"Change of plans",2000L)));
    verify(gateway,never()).refund(anyString(),anyLong(),anyString(),anyString());
 }

 @Test void deductionsCannotExceedRemainingAmountOrMismatchActualRefund() {
    assertThrows(BadRequestException.class,
      ()->service.refund(order.getId(),new RefundRequest(13000L,"Customer cancellation",15000L)));
    assertThrows(BadRequestException.class,
      ()->service.refund(order.getId(),new RefundRequest(10000L,"Customer cancellation",2000L)));
    assertThrows(BadRequestException.class,
      ()->service.refund(order.getId(),new RefundRequest(99L,"Customer cancellation",14901L)));
    verify(gateway,never()).refund(anyString(),anyLong(),anyString(),anyString());
 }

 @Test void deductionRequiresSeparateDescription() {
    assertThrows(BadRequestException.class,
      ()->service.refund(order.getId(),new RefundRequest(13000L,"Customer cancellation",2000L)));
    assertThrows(BadRequestException.class,
      ()->service.refund(order.getId(),new RefundRequest(13000L,"Customer cancellation",2000L,"  ")));
    assertThrows(BadRequestException.class,
      ()->service.refund(order.getId(),new RefundRequest(13000L,"Customer cancellation",2000L,"X")));
    verify(gateway,never()).refund(anyString(),anyLong(),anyString(),anyString());
 }

 @Test void refundWithoutDeductionNeedsNoDeductionDescription() {
    var result=service.refund(order.getId(),new RefundRequest(15000L,"Venue-approved full refund",0L));
    assertEquals(15000L,result.amountPaise());
    assertEquals(0L,result.deductionPaise());
    assertNull(result.deductionDescription());
 }

 @Test void pendingRefundBlocksSecondSubmission() {
    PaymentRefund pending=new PaymentRefund(order,13000L,"Pending",2000L);
    when(refunds.findByPaymentOrderId(order.getId())).thenReturn(List.of(pending));
    assertThrows(ConflictException.class,
      ()->service.refund(order.getId(),new RefundRequest(13000L,"Again",2000L)));
    verify(gateway,never()).refund(anyString(),anyLong(),anyString(),anyString());
 }

 @Test void onsitePaymentMustNotBeRefundedThroughRazorpay() {
    PaymentOrder onsite=new PaymentOrder(mock(User.class),PaymentPurpose.BOOKING,
      order.getReferenceId(),15000L,"INR","ONSITE","onsite-test");
    onsite.paid("onsite_1234");
    when(orders.findLockedById(onsite.getId())).thenReturn(Optional.of(onsite));
    assertThrows(ConflictException.class,
      ()->service.refund(onsite.getId(),new RefundRequest(null,"Cash payment")));
 }

 @Test void ambiguousNetworkResponseLeavesRequestedRecordForReconciliation() {
    when(gateway.refund(eq("pay_1234"),anyLong(),anyString(),anyString()))
      .thenThrow(new RefundGatewayUncertainException());
    assertThrows(RefundGatewayUncertainException.class,
      ()->service.refund(order.getId(),new RefundRequest(13000L,"Venue refund",2000L,"Administration fee")));
    var saved=ArgumentCaptor.forClass(PaymentRefund.class);
    verify(refunds).save(saved.capture());
    assertEquals(RefundStatus.REQUESTED,saved.getValue().getStatus());
    assertNull(saved.getValue().getProviderRefundId());
 }
}
