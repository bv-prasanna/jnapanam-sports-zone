package com.jnapanam.sportszone.booking.service;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only capacity reconciliation. Neither changing a payment status nor changing
 * slot counters is safe until captured gateway transactions are reconciled.
 * In particular, a PENDING_PAYMENT booking legitimately occupies a slot until it
 * is paid, cancelled or expires; a separate short-lived ACTIVE hold also occupies it.
 */
@Service
public class BookingCapacityAuditService {
    private final JdbcTemplate jdbc;

    public BookingCapacityAuditService(JdbcTemplate jdbc) {this.jdbc=jdbc;}

    public record SlotAudit(UUID slotId,String facilityName,String sportName,LocalDate date,
        LocalTime startTime,LocalTime endTime,int capacity,int heldCount,int bookedCount,
        int available,long activeHolds,long activeBookings,long pendingPayment,
        long confirmed,long expiredBookings,boolean countersMismatch) {}

    @Transactional(readOnly=true)
    public List<SlotAudit> inspect(UUID facilityId,LocalDate date) {
        return jdbc.query("""
            SELECT s.id AS slot_id,f.name AS facility_name,sp.name AS sport_name,
                   s.slot_date,s.start_time,s.end_time,s.capacity,s.held_count,s.booked_count,
                   COALESCE((SELECT SUM(h.quantity) FROM booking_holds h
                       WHERE h.slot_id=s.id AND h.status='ACTIVE'
                       AND h.expires_at>CURRENT_TIMESTAMP),0) AS active_holds,
                   COALESCE((SELECT SUM(b.quantity) FROM bookings b
                       WHERE b.slot_id=s.id AND b.status IN ('PENDING_PAYMENT','CONFIRMED','NO_SHOW')),0)
                       AS active_bookings,
                   COALESCE((SELECT SUM(b.quantity) FROM bookings b
                       WHERE b.slot_id=s.id AND b.status='PENDING_PAYMENT'),0) AS pending_payment,
                   COALESCE((SELECT SUM(b.quantity) FROM bookings b
                       WHERE b.slot_id=s.id AND b.status IN ('CONFIRMED','NO_SHOW')),0) AS confirmed,
                   (SELECT COUNT(*) FROM bookings b
                       WHERE b.slot_id=s.id AND b.status='PAYMENT_EXPIRED') AS expired_bookings
            FROM booking_slots s
            JOIN facilities f ON f.id=s.facility_id
            JOIN sports sp ON sp.id=f.sport_id
            WHERE s.facility_id=? AND s.slot_date=?
            ORDER BY s.start_time
            """,(rs,n)->{
              int capacity=rs.getInt("capacity"),held=rs.getInt("held_count"),
                  booked=rs.getInt("booked_count");
              long holds=rs.getLong("active_holds"),bookings=rs.getLong("active_bookings");
              return new SlotAudit(rs.getObject("slot_id",UUID.class),
                rs.getString("facility_name"),rs.getString("sport_name"),
                rs.getDate("slot_date").toLocalDate(),
                rs.getTime("start_time").toLocalTime(),rs.getTime("end_time").toLocalTime(),
                capacity,held,booked,Math.max(0,capacity-held-booked),
                holds,bookings,rs.getLong("pending_payment"),rs.getLong("confirmed"),
                rs.getLong("expired_bookings"),held!=holds || booked!=bookings);
            },facilityId,Date.valueOf(date));
    }
}
