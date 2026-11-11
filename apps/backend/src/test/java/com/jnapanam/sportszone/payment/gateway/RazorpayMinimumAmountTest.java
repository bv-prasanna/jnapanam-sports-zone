package com.jnapanam.sportszone.payment.gateway;

import com.jnapanam.sportszone.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RazorpayMinimumAmountTest {
 @Test void rejectsAmountsBelowOneRupeeBeforeCallingGateway() {
  assertThrows(BadRequestException.class,()->RazorpayGateway.validateOrderAmount(0));
  assertThrows(BadRequestException.class,()->RazorpayGateway.validateOrderAmount(99));
  assertThrows(BadRequestException.class,()->RazorpayGateway.validateOrderAmount(-1));
  assertDoesNotThrow(()->RazorpayGateway.validateOrderAmount(100));
  assertDoesNotThrow(()->RazorpayGateway.validateOrderAmount(15000));
 }
}
