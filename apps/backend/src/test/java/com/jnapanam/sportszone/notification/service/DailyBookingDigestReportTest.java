package com.jnapanam.sportszone.notification.service;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DailyBookingDigestReportTest {
    private final UUID badminton=UUID.randomUUID(),cricket=UUID.randomUUID();
    private final DailyBookingDigestReport.Facility court=new DailyBookingDigestReport.Facility(badminton,"Badminton","Court 1");
    private final DailyBookingDigestReport.Facility ground=new DailyBookingDigestReport.Facility(cricket,"Cricket","Ground A");
    private final DailyBookingDigestReport.Booking booked=new DailyBookingDigestReport.Booking(badminton,"Badminton","Court 1",
        "07:00","08:00","JN-01","Test Customer","9000000000","CONFIRMED","PAID");
    private final DailyBookingDigestReport.Booking other=new DailyBookingDigestReport.Booking(cricket,"Cricket","Ground A",
        "09:00","10:00","JN-02","Private Customer","9888888888","PENDING_PAYMENT","UNPAID");

    @Test void adminSeesEverySportAndPayment() {
        var visible=DailyBookingDigestReport.visibleBookings(List.of(other,booked),List.of(court,ground));
        var text=DailyBookingDigestReport.compose(LocalDate.of(2026,11,1),List.of(court,ground),visible);
        assertEquals(2,visible.size());assertTrue(text.contains("Test Customer"));
        assertTrue(text.contains("Private Customer"));assertTrue(text.contains("9000000000"));
        assertTrue(text.contains("Payment: UNPAID"));assertTrue(text.contains("Payment: PAID"));
    }
    @Test void operatorDoesNotSeeUnassignedCustomers() {
        var visible=DailyBookingDigestReport.visibleBookings(List.of(other,booked),List.of(court));
        var text=DailyBookingDigestReport.compose(LocalDate.of(2026,11,1),List.of(court),visible);
        assertEquals(1,visible.size());assertFalse(text.contains("Private Customer"));
        assertFalse(text.contains("9888888888"));assertFalse(text.contains("Ground A"));
    }
    @Test void emptyScheduleAndNoAssignmentsExplained() {
        assertTrue(DailyBookingDigestReport.compose(LocalDate.now(),List.of(court),List.of()).contains("No bookings for this facility"));
        assertTrue(DailyBookingDigestReport.compose(LocalDate.now(),List.of(),List.of()).contains("No facilities are assigned"));
    }
    @Test void rowsSortBySlotTime() {
        var later=new DailyBookingDigestReport.Booking(badminton,"Badminton","Court 1","11:00","12:00",
                "JN-03","Later","9000000001","CONFIRMED","PAID");
        var values=DailyBookingDigestReport.visibleBookings(List.of(later,booked),List.of(court));
        assertEquals("JN-01",values.get(0).reference());
    }
    @Test void htmlEmailIsFacilityWiseWithReadableColumnsAndEscaping(){
        var html=DailyBookingDigestReport.composeHtml(LocalDate.of(2026,11,4),List.of(ground,court),List.of(other,booked));
        assertTrue(html.contains("<table role=\"table\""));
        assertTrue(html.contains("Booked person"));
        assertTrue(html.contains("Payment"));
        assertTrue(html.contains("Court 1"));
        assertTrue(html.contains("Ground A"));
        assertTrue(html.indexOf("Court 1")<html.indexOf("Ground A"));
        assertTrue(html.contains("Test Customer"));
    }
    @Test void userSuppliedNamesAreEscapedInHtml(){
        var injected=new DailyBookingDigestReport.Booking(badminton,"Badminton","Court 1","09:00","10:00",
           "JNZ-3","<script>alert(1)</script>","9449284747","CONFIRMED","UNPAID");
        var html=DailyBookingDigestReport.composeHtml(LocalDate.now(),List.of(court),List.of(injected));
        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&lt;script&gt;"));
    }
    @Test void wrongSportAssignmentRemainsVisibleWithoutGroupingOtherFacilities(){
        var incorrect=new DailyBookingDigestReport.Facility(UUID.randomUUID(),"Cricket","Swimming");
        var body=DailyBookingDigestReport.compose(LocalDate.now(),List.of(ground,incorrect),List.of());
        assertTrue(body.contains("Swimming"));
        assertTrue(body.contains("SPORT: Cricket"));
    }
    @Test void reportIncludesActualConfiguredSportPerFacility() {
        var misAssigned=new DailyBookingDigestReport.Facility(UUID.randomUUID(),"Cricket","Swimming");
        var body=DailyBookingDigestReport.compose(LocalDate.now(),List.of(misAssigned),List.of());
        var html=DailyBookingDigestReport.composeHtml(LocalDate.now(),List.of(misAssigned),List.of());
        assertTrue(body.contains("SPORT: Cricket"));
        assertTrue(html.contains("Sport: Cricket"));
        assertTrue(html.contains("Swimming"));
    }

}
