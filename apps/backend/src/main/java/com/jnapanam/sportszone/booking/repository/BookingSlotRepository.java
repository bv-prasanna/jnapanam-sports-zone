package com.jnapanam.sportszone.booking.repository;
import com.jnapanam.sportszone.booking.model.BookingSlot;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface BookingSlotRepository extends JpaRepository<BookingSlot,UUID>{
 List<BookingSlot> findByFacilityIdAndDateOrderByStartTimeAsc(UUID facilityId,LocalDate date);
 // Public pricing accesses slot.facility.sport while open-in-view=false. Load both
 // associations before the repository transaction closes, including for batch quotes.
 @Query("select s from BookingSlot s join fetch s.facility f join fetch f.sport where s.id in :ids")
 List<BookingSlot> findAllForPricing(@Param("ids") Collection<UUID> ids);
 @Query("select s from BookingSlot s join fetch s.facility f join fetch f.sport where s.id = :id")
 Optional<BookingSlot> findForPricingById(@Param("id") UUID id);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from BookingSlot s where s.id=:id") Optional<BookingSlot> findLocked(@Param("id") UUID id);
}
