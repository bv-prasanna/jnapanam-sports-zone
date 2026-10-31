package com.jnapanam.sportszone.notification.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class DailyBookingDigestReport {
    private DailyBookingDigestReport() {}

    public record Facility(UUID id, String sport, String name) {}
    public record Booking(UUID facilityId, String sport, String facility, String start, String end,
                          String reference, String customerName, String customerMobile,
                          String bookingStatus, String paymentStatus) {}

    public static List<Booking> visibleBookings(List<Booking> bookings, List<Facility> accessible) {
        var ids = accessible.stream().map(Facility::id).collect(java.util.stream.Collectors.toSet());
        return bookings.stream().filter(b -> ids.contains(b.facilityId()))
                .sorted(Comparator.comparing(Booking::sport).thenComparing(Booking::facility)
                        .thenComparing(Booking::start).thenComparing(Booking::reference))
                .toList();
    }

    public static String compose(LocalDate date, List<Facility> facilities, List<Booking> bookings) {
        StringBuilder body = new StringBuilder("JN﻿APANAM SPORTS ZONE — NEXT-DAY BOOKING REPORT\n".replace("\uFEFF",""));
        body.append("Date: ").append(date).append(" (Asia/Kolkata)\n")
            .append("Facilities in this report: ").append(facilities.size()).append("\n")
            .append("Scheduled bookings: ").append(bookings.size()).append("\n")
            .append("Includes CONFIRMED and PENDING_PAYMENT bookings as of report generation.\n")
            .append("Live bookings and payment statuses may change after this email.\n\n");
        if (facilities.isEmpty()) return body.append("No facilities are assigned to your account.\n").toString();

        List<Facility> sorted = new ArrayList<>(facilities);
        sorted.sort(Comparator.comparing(Facility::sport).thenComparing(Facility::name));
        String lastSport = null;
        for (Facility facility : sorted) {
            if (!Objects.equals(lastSport, facility.sport())) {
                body.append("\nSPORT: ").append(facility.sport()).append("\n");
                lastSport = facility.sport();
            }
            body.append("  FACILITY: ").append(facility.name()).append("\n");
            List<Booking> selected = bookings.stream().filter(b -> b.facilityId().equals(facility.id())).toList();
            if (selected.isEmpty()) {
                body.append("    No bookings scheduled for this facility.\n");
                continue;
            }
            for (Booking b : selected) {
                body.append("    ").append(b.start()).append("–").append(b.end())
                    .append(" | ").append(b.reference())
                    .append(" | ").append(b.customerName())
                    .append(" | ").append(b.customerMobile())
                    .append(" | Booking: ").append(b.bookingStatus())
                    .append(" | Payment: ").append(b.paymentStatus()).append("\n");
            }
        }
        return body.append("\nConfidential: authorized Jnapanam staff only.\n").toString();
    }
}
