package com.jnapanam.sportszone.booking.repository;
import com.jnapanam.sportszone.booking.model.*;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface BookingHoldRepository extends JpaRepository<BookingHold,UUID>{
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select h from BookingHold h where h.id=:id") Optional<BookingHold> findLocked(@Param("id") UUID id);
 List<BookingHold> findByStatusAndExpiresAtLessThanEqual(HoldStatus status,Instant now);
}
