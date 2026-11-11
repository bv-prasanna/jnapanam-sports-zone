package com.jnapanam.sportszone.booking.repository;
import com.jnapanam.sportszone.booking.model.Booking;
import com.jnapanam.sportszone.booking.model.BookingStatus;
import java.time.LocalDate;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
public interface BookingRepository extends JpaRepository<Booking,UUID>{
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select b from Booking b where b.id=:id")
 Optional<Booking> findLockedById(@Param("id") UUID id);
 List<Booking> findByUserIdOrderByConfirmedAtDesc(UUID userId);
 Optional<Booking> findByBookingNumberIgnoreCase(String bookingNumber);
 List<Booking> findAllByOrderByConfirmedAtDesc();
 List<Booking> findBySlot_DateOrderByConfirmedAtDesc(LocalDate date);
 List<Booking> findByStatusAndPaymentExpiresAtLessThanEqual(BookingStatus status,Instant expiresAt);
}
