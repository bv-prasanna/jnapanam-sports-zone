package com.jnapanam.sportszone.notification.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Reports only bookings for explicitly assigned facilities. Never infer a facility's
 * sport from its name; the sports catalog is the source of that relationship.
 */
public final class DailyBookingDigestReport {
    private DailyBookingDigestReport() {}
    public record Facility(UUID id, String sport, String name) {}
    public record Booking(UUID facilityId, String sport, String facility, String start, String end,
                          String reference, String customerName, String customerMobile,
                          String bookingStatus, String paymentStatus) {}

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public static List<Booking> visibleBookings(List<Booking> bookings, List<Facility> accessible) {
        var ids = accessible.stream().map(Facility::id).collect(java.util.stream.Collectors.toSet());
        return bookings.stream().filter(b -> ids.contains(b.facilityId()))
                .sorted(Comparator.comparing(Booking::facility, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Booking::start).thenComparing(Booking::reference)).toList();
    }

    private static List<Facility> sorted(List<Facility> facilities) {
        List<Facility> out = new ArrayList<>(facilities);
        out.sort(Comparator.comparing(Facility::name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Facility::id));
        return out;
    }
    private static List<Booking> forFacility(UUID id,List<Booking> bookings) {
        return bookings.stream().filter(b -> id.equals(b.facilityId()))
                .sorted(Comparator.comparing(Booking::start).thenComparing(Booking::reference)).toList();
    }
    private static String time(String text) {
        if (text == null) return "—";
        try { return LocalTime.parse(text).format(CLOCK); }
        catch (RuntimeException ex) { return text; }
    }
    private static String status(String text) { return text==null ? "—" : text.replace('_',' '); }
    private static String safe(String text) { return text==null||text.isBlank() ? "—" : text.strip(); }
    private static String escape(String text) {
        return safe(text).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                .replace("\"","&quot;").replace("'","&#39;");
    }
    private static String paymentColor(String text) {
        return "PAID".equals(text) ? "#1d6a3c" : "#9b491d";
    }
    public static String compose(LocalDate date,List<Facility> facilities,List<Booking> bookings) {
        StringBuilder body=new StringBuilder();
        body.append("JNAPANAM SPORTS ZONE — NEXT-DAY BOOKINGS\n")
            .append("Date: ").append(date.format(DAY)).append(" (IST)\n")
            .append("Facilities: ").append(facilities.size()).append(" | Bookings: ").append(bookings.size()).append("\n")
            .append("Booking and payment statuses are a snapshot at dispatch.\n");
        if (facilities.isEmpty())return body.append("\nNo facilities are assigned to your account.\n").toString();
        int number=0;
        for(Facility facility:sorted(facilities)) {
            List<Booking> rows=forFacility(facility.id(),bookings);
            body.append("\n").append("--------------------------------------------------\n")
                .append(++number).append(". ").append(safe(facility.name()))
                .append("  |  SPORT: ").append(safe(facility.sport()))
                .append("  (").append(rows.size()).append(rows.size()==1?" booking)":" bookings)").append("\n");
            if(rows.isEmpty()){body.append("   No bookings for this facility.\n");continue;}
            for(Booking b:rows) {
                body.append("\n   ").append(time(b.start())).append(" – ").append(time(b.end()))
                    .append("  |  ").append(safe(b.reference())).append("\n")
                    .append("   Person: ").append(safe(b.customerName())).append("\n")
                    .append("   Mobile: ").append(safe(b.customerMobile())).append("\n")
                    .append("   Booking: ").append(status(b.bookingStatus()))
                    .append("   |   Payment: ").append(status(b.paymentStatus())).append("\n");
            }
        }
        return body.append("\nConfidential — authorized Jnapanam staff only.\n").toString();
    }

    /** Safe, table-based HTML alternative for Gmail/Outlook/mobile clients. */
    public static String composeHtml(LocalDate date,List<Facility> facilities,List<Booking> bookings) {
        StringBuilder out=new StringBuilder("""
            <!doctype html><html><head><meta charset="UTF-8"></head>
            <body style="margin:0;background:#fff9ef;color:#20251c;font-family:Arial,Helvetica,sans-serif;">
            <div style="max-width:760px;margin:0 auto;padding:24px 16px;">
              <div style="background:#28330f;color:white;border-radius:12px 12px 0 0;padding:22px;">
                <div style="font-size:13px;letter-spacing:1px;">JNAPANAM SPORTS ZONE</div>
                <h1 style="font-size:24px;margin:9px 0 5px;color:#fff;">Next-day bookings</h1>
            """);
        out.append("<div style=\"font-size:14px;color:#eee6cf;\">").append(escape(date.format(DAY)))
           .append(" · IST</div></div>");
        out.append("<div style=\"background:#fffdf8;padding:18px;border:1px solid #e4d6be;\">")
           .append("<strong>Facilities: ").append(facilities.size()).append("</strong>")
           .append(" &nbsp;&nbsp; <strong>Bookings: ").append(bookings.size()).append("</strong>")
           .append("<p style=\"font-size:12px;color:#66705d;\">Includes confirmed and awaiting-payment reservations. Statuses can change after this report is sent.</p></div>");
        if(facilities.isEmpty())out.append("<p>No facilities are assigned to your account.</p>");
        int count=0;
        for(Facility facility:sorted(facilities)){
            List<Booking> rows=forFacility(facility.id(),bookings);
            out.append("<section style=\"margin-top:17px;background:#fff;border:1px solid #e4d6be;border-radius:10px;overflow:hidden;\">")
               .append("<div style=\"background:#f3e7d2;padding:13px 16px;\"><strong style=\"font-size:17px;color:#28330f;\">")
               .append(++count).append(". ").append(escape(facility.name())).append("</strong>")
               .append(" <span style=\"color:#66705d;font-size:13px;\">Sport: ").append(escape(facility.sport())).append(" · ").append(rows.size()).append(rows.size()==1?" booking":" bookings").append(")</span></div>");
            if(rows.isEmpty())out.append("<p style=\"color:#66705d;margin:15px;\">No bookings scheduled.</p>");
            else {
                out.append("<div style=\"overflow-x:auto;\"><table role=\"table\" style=\"width:100%;border-collapse:collapse;font-size:13px;text-align:left;\">")
                   .append("<thead><tr style=\"color:#28330f;background:#fff7e9;\">")
                   .append("<th style=\"padding:11px;\">Slot (IST)</th><th style=\"padding:11px;\">Booked person</th>")
                   .append("<th style=\"padding:11px;\">Booking no.</th><th style=\"padding:11px;\">Booking</th>")
                   .append("<th style=\"padding:11px;\">Payment</th></tr></thead><tbody>");
                for(Booking b:rows) {
                    out.append("<tr style=\"border-top:1px solid #e4d6be;\"><td style=\"padding:11px;white-space:nowrap;\">")
                       .append(escape(time(b.start()))).append(" – ").append(escape(time(b.end())))
                       .append("</td><td style=\"padding:11px;word-break:break-word;\"><strong>")
                       .append(escape(b.customerName())).append("</strong><div style=\"color:#66705d;margin-top:4px;\">")
                       .append(escape(b.customerMobile())).append("</div></td><td style=\"padding:11px;\">")
                       .append(escape(b.reference())).append("</td><td style=\"padding:11px;\">")
                       .append(escape(status(b.bookingStatus()))).append("</td><td style=\"padding:11px;font-weight:700;color:")
                       .append(paymentColor(b.paymentStatus())).append(";\">")
                       .append(escape(status(b.paymentStatus()))).append("</td></tr>");
                }
                out.append("</tbody></table></div>");
            }
            out.append("</section>");
        }
        return out.append("<p style=\"font-size:12px;color:#66705d;margin:24px 0;\">Confidential — authorized Jnapanam staff only.</p>")
            .append("</div></body></html>").toString();
    }
}
