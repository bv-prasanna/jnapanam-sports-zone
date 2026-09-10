package com.jnapanam.sportszone.admin.service;

import com.jnapanam.sportszone.booking.dto.BookingResponse;
import com.jnapanam.sportszone.catalog.model.CatalogueStatus;
import com.jnapanam.sportszone.catalog.repository.FacilityRepository;
import com.jnapanam.sportszone.common.exception.ConflictException;
import com.jnapanam.sportszone.common.exception.NotFoundException;
import com.jnapanam.sportszone.identity.model.RoleName;
import com.jnapanam.sportszone.identity.model.User;
import com.jnapanam.sportszone.identity.repository.UserRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperatorAssignmentService {
    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final FacilityRepository facilities;

    public OperatorAssignmentService(JdbcTemplate jdbc, UserRepository users, FacilityRepository facilities) {
        this.jdbc = jdbc;
        this.users = users;
        this.facilities = facilities;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> assignments(UUID operatorId) {
        requireOperator(operatorId);
        return jdbc.query("select f.id facility_id,f.code facility_code,f.name facility_name,s.code sport_code,s.name sport_name "
                        + "from operator_facility_assignments a join facilities f on f.id=a.facility_id "
                        + "join sports s on s.id=f.sport_id where a.operator_id=? order by s.name,f.name",
                (rs, row) -> {
                    Map<String, Object> value = new LinkedHashMap<>();
                    value.put("facilityId", rs.getObject("facility_id", UUID.class));
                    value.put("facilityCode", rs.getString("facility_code"));
                    value.put("facilityName", rs.getString("facility_name"));
                    value.put("sportCode", rs.getString("sport_code"));
                    value.put("sportName", rs.getString("sport_name"));
                    return value;
                }, operatorId);
    }

    @Transactional
    public List<Map<String, Object>> replace(UUID operatorId, List<UUID> facilityIds) {
        requireOperator(operatorId);
        Set<UUID> unique = new LinkedHashSet<>(facilityIds == null ? List.of() : facilityIds);
        for (UUID facilityId : unique) {
            var facility = facilities.findById(facilityId)
                    .orElseThrow(() -> new NotFoundException("Facility not found"));
            if (facility.getStatus() != CatalogueStatus.ACTIVE) {
                throw new ConflictException("Only active facilities can be assigned to an operator");
            }
        }
        jdbc.update("delete from operator_facility_assignments where operator_id=?", operatorId);
        for (UUID facilityId : unique) {
            jdbc.update("insert into operator_facility_assignments(operator_id,facility_id) values(?,?)",
                    operatorId, facilityId);
        }
        return assignments(operatorId);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> filterBookings(UUID actorId, List<BookingResponse> bookings) {
        if (isAdministrator(actorId)) return bookings;
        requireOperator(actorId);
        Set<UUID> allowed = assignedFacilityIds(actorId);
        if (allowed.isEmpty()) return List.of();
        List<BookingResponse> result = new ArrayList<>();
        for (BookingResponse booking : bookings) {
            if (allowed.contains(booking.facilityId())) result.add(booking);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public void assertBookingAllowed(UUID actorId, UUID bookingId) {
        if (isAdministrator(actorId)) return;
        requireOperator(actorId);
        Integer count = jdbc.queryForObject("select count(*) from bookings b join booking_slots s on s.id=b.slot_id "
                        + "join operator_facility_assignments a on a.facility_id=s.facility_id "
                        + "where b.id=? and a.operator_id=?", Integer.class, bookingId, actorId);
        if (count == null || count == 0) throw new NotFoundException("Booking not found");
    }

    @Transactional(readOnly = true)
    public void assertBookingAllowed(UUID actorId, String bookingNumber) {
        if (isAdministrator(actorId)) return;
        requireOperator(actorId);
        Integer count = jdbc.queryForObject("select count(*) from bookings b join booking_slots s on s.id=b.slot_id "
                        + "join operator_facility_assignments a on a.facility_id=s.facility_id "
                        + "where lower(b.booking_number)=lower(?) and a.operator_id=?", Integer.class,
                bookingNumber, actorId);
        if (count == null || count == 0) throw new NotFoundException("Booking not found");
    }

    private Set<UUID> assignedFacilityIds(UUID operatorId) {
        return new LinkedHashSet<>(jdbc.query("select facility_id from operator_facility_assignments where operator_id=?",
                (rs, row) -> rs.getObject(1, UUID.class), operatorId));
    }

    private User requireOperator(UUID id) {
        User user = users.findById(id).orElseThrow(() -> new NotFoundException("Operator not found"));
        boolean operator = user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.OPERATIONS);
        if (!operator) throw new ConflictException("User does not have the Operations role");
        return user;
    }

    private boolean isAdministrator(UUID id) {
        User user = users.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
        return user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ADMIN || r.getName() == RoleName.SUPER_ADMIN);
    }
}
