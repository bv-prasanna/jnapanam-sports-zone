package com.jnapanam.sportszone.booking.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BookingAddressProofPolicyTest {
 @Test void missingOrRejectedBlocksBookingEvenForExistingCustomers(){
   assertFalse(BookingService.bookingProofAllowsBooking("NOT_SUBMITTED"));
   assertFalse(BookingService.bookingProofAllowsBooking("REJECTED"));
   assertFalse(BookingService.bookingProofAllowsBooking(null));
   assertTrue(BookingService.bookingProofMessage("REJECTED").contains("Upload a corrected"));
 }
 @Test void uploadedAwaitingReviewIsPendingButNotMissing(){
   assertTrue(BookingService.bookingProofAllowsBooking("PENDING"));
   assertTrue(BookingService.bookingProofAllowsBooking("APPROVED"));
 }
}
