package com.jnapanam.sportszone.booking.repository;
import com.jnapanam.sportszone.booking.model.BookingSlot;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface BookingSlotRepository extends JpaRepository<BookingSlot,UUID>{
 List<BookingSlot> findByFacilityIdAndDateOrderByStartTimeAsc(UUID facilityId,LocalDate date);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from BookingSlot s where s.id=:id") Optional<BookingSlot> findLocked(@Param("id") UUID id);
}
