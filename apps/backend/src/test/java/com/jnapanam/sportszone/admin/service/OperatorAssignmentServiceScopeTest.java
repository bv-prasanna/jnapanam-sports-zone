package com.jnapanam.sportszone.admin.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.jnapanam.sportszone.catalog.repository.FacilityRepository;
import com.jnapanam.sportszone.common.exception.ConflictException;
import com.jnapanam.sportszone.common.exception.NotFoundException;
import com.jnapanam.sportszone.identity.model.Role;
import com.jnapanam.sportszone.identity.model.RoleName;
import com.jnapanam.sportszone.identity.model.User;
import com.jnapanam.sportszone.identity.repository.UserRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class OperatorAssignmentServiceScopeTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final UserRepository users = mock(UserRepository.class);
    private final FacilityRepository facilities = mock(FacilityRepository.class);
    private final OperatorAssignmentService service = new OperatorAssignmentService(jdbc, users, facilities);
    private final UUID actorId = UUID.randomUUID();
    private final UUID slotId = UUID.randomUUID();
    private final UUID facilityId = UUID.randomUUID();
    private final User operator = mock(User.class);
    private final Role role = mock(Role.class);

    @BeforeEach
    void setUp() {
        when(users.findById(actorId)).thenReturn(Optional.of(operator));
        when(operator.getRoles()).thenReturn(Set.of(role));
        when(role.getName()).thenReturn(RoleName.OPERATIONS);
    }

    @Test
    void operatorCannotCreateWalkInForUnassignedSlot() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(slotId), eq(actorId))).thenReturn(0);

        assertThrows(NotFoundException.class, () -> service.assertSlotAllowed(actorId, slotId));
    }

    @Test
    void operatorCanCreateWalkInForAssignedSlot() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(slotId), eq(actorId))).thenReturn(1);

        assertDoesNotThrow(() -> service.assertSlotAllowed(actorId, slotId));
    }

    @Test
    void operatorCannotCloseUnassignedFacility() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(facilityId), eq(actorId))).thenReturn(0);

        assertThrows(NotFoundException.class, () -> service.assertFacilityAllowed(actorId, facilityId));
    }

    @Test
    void operatorCanCloseAssignedFacility() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(facilityId), eq(actorId))).thenReturn(1);

        assertDoesNotThrow(() -> service.assertFacilityAllowed(actorId, facilityId));
    }

    @Test
    void nonOperatorIsRejectedBeforeAssignmentQuery() {
        when(role.getName()).thenReturn(RoleName.CUSTOMER);

        assertThrows(ConflictException.class, () -> service.assertSlotAllowed(actorId, slotId));
        verify(jdbc, never()).queryForObject(anyString(), eq(Integer.class), any(), any());
    }
}
