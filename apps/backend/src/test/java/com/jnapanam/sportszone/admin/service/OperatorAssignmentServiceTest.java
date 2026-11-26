package com.jnapanam.sportszone.admin.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.jnapanam.sportszone.catalog.repository.FacilityRepository;
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

class OperatorAssignmentServiceTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final UserRepository users = mock(UserRepository.class);
    private final FacilityRepository facilities = mock(FacilityRepository.class);
    private final OperatorAssignmentService service = new OperatorAssignmentService(jdbc, users, facilities);
    private final UUID operatorId = UUID.randomUUID();
    private final UUID facilityId = UUID.randomUUID();
    private final Role operationsRole = mock(Role.class);
    private final User operator = mock(User.class);

    @BeforeEach
    void setUp() {
        when(operationsRole.getName()).thenReturn(RoleName.OPERATIONS);
        when(operator.getRoles()).thenReturn(Set.of(operationsRole));
        when(users.findById(operatorId)).thenReturn(Optional.of(operator));
    }

    @Test
    void assignedOperatorCanUseFacility() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(facilityId), eq(operatorId))).thenReturn(1);
        assertDoesNotThrow(() -> service.assertFacilityAllowed(operatorId, facilityId));
    }

    @Test
    void unassignedOperatorCannotUseFacility() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(facilityId), eq(operatorId))).thenReturn(0);
        assertThrows(NotFoundException.class, () -> service.assertFacilityAllowed(operatorId, facilityId));
    }

    @Test
    void operatorWithNoAssignmentCannotUseFacility() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(facilityId), eq(operatorId))).thenReturn(null);
        assertThrows(NotFoundException.class, () -> service.assertFacilityAllowed(operatorId, facilityId));
    }

    @Test
    void administratorCanUseAnyFacilityWithoutAssignmentLookup() {
        Role adminRole = mock(Role.class);
        when(adminRole.getName()).thenReturn(RoleName.ADMIN);
        User admin = mock(User.class);
        UUID adminId = UUID.randomUUID();
        when(admin.getRoles()).thenReturn(Set.of(adminRole));
        when(users.findById(adminId)).thenReturn(Optional.of(admin));

        assertDoesNotThrow(() -> service.assertFacilityAllowed(adminId, facilityId));
        verifyNoInteractions(jdbc);
    }

    @Test
    void missingOperatorIsRejected() {
        UUID unknown = UUID.randomUUID();
        when(users.findById(unknown)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.assertFacilityAllowed(unknown, facilityId));
    }
}
