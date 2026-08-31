package com.jnapanam.sportszone.booking.repository;
import com.jnapanam.sportszone.booking.model.Booking;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface BookingRepository extends JpaRepository<Booking,UUID>{
 List<Booking> findByUserIdOrderByConfirmedAtDesc(UUID userId);
 @Query("select b from Booking b where (:date is null or b.slot.date=:date) order by b.confirmedAt desc") List<Booking> adminList(@Param("date") LocalDate date);
}
