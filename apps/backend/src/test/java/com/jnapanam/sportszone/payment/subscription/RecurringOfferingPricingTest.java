package com.jnapanam.sportszone.payment.subscription;

import static org.junit.jupiter.api.Assertions.*;
import com.jnapanam.sportszone.common.exception.BadRequestException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RecurringOfferingPricingTest {
 @Test void threeInstallmentsMatchTrainingFeeWithinRounding() {
   assertDoesNotThrow(() -> RecurringSubscriptionService.requireInstallmentTotal(
       33334L,3,new BigDecimal("1000.00")));
 }
 @Test void threeInstallmentsCannotOverchargeByWholeFee() {
   assertThrows(BadRequestException.class, () -> RecurringSubscriptionService.requireInstallmentTotal(
       100000L,3,new BigDecimal("1000.00")));
 }
 @Test void threeInstallmentsCannotUnderchargeByHundreds() {
   assertThrows(BadRequestException.class, () -> RecurringSubscriptionService.requireInstallmentTotal(
       25000L,3,new BigDecimal("1000.00")));
 }
 @Test void zeroFeeNotRecurring() {
   assertThrows(BadRequestException.class, () -> RecurringSubscriptionService.requireInstallmentTotal(
       10000L,3,BigDecimal.ZERO));
 }
 @Test void subPaiseAmountRejected() {
   assertThrows(BadRequestException.class, () -> RecurringSubscriptionService.toPaise(
       new BigDecimal("100.001")));
 }
}
