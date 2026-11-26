package com.jnapanam.sportszone.release1.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.jnapanam.sportszone.admin.service.OperatorAssignmentService;
import com.jnapanam.sportszone.common.exception.NotFoundException;
import com.jnapanam.sportszone.release1.service.Release1OperationsService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

class Release1OperationsControllerInventoryScopeTest {
    private final Release1OperationsService service = mock(Release1OperationsService.class);
    private final OperatorAssignmentService assignments = mock(OperatorAssignmentService.class);
    private final Release1OperationsController controller =
            new Release1OperationsController(service, assignments);
    private final UUID operatorId = UUID.randomUUID();
    private final UUID assignedFacilityId = UUID.randomUUID();
    private final Authentication authentication = mock(Authentication.class);

    @Test
    void operatorInventoryIsFilteredToAssignedFacilities() {
        when(authentication.getName()).thenReturn(operatorId.toString());
        when(assignments.isAdministratorActor(operatorId)).thenReturn(false);
        when(assignments.facilitiesForActor(operatorId)).thenReturn(
                List.of(Map.of("facilityId", assignedFacilityId)));
        List<Map<String, Object>> expected = List.of(Map.of("equipmentName", "Badminton racket"));
        when(service.inventoryForFacilities(List.of(assignedFacilityId))).thenReturn(expected);

        assertEquals(expected, controller.inventory(authentication));
        verify(service).inventoryForFacilities(List.of(assignedFacilityId));
        verify(service, never()).inventory();
    }

    @Test
    void operatorCannotCreateGlobalInventoryWithoutFacility() {
        when(authentication.getName()).thenReturn(operatorId.toString());
        when(assignments.isAdministratorActor(operatorId)).thenReturn(false);

        assertThrows(NotFoundException.class,
                () -> controller.inventory(authentication, Map.of("equipmentName", "Shuttlecock",
                        "totalQuantity", 20)));
        verifyNoInteractions(service);
        verify(assignments, never()).assertFacilityAllowed(any(), any());
    }

    @Test
    void operatorCannotCreateInventoryForUnassignedFacility() {
        when(authentication.getName()).thenReturn(operatorId.toString());
        when(assignments.isAdministratorActor(operatorId)).thenReturn(false);
        doThrow(new NotFoundException("Facility not found"))
                .when(assignments).assertFacilityAllowed(operatorId, assignedFacilityId);

        assertThrows(NotFoundException.class,
                () -> controller.inventory(authentication, Map.of("facilityId", assignedFacilityId.toString(),
                        "equipmentName", "Shuttlecock", "totalQuantity", 20)));
        verifyNoInteractions(service);
    }

    @Test
    void administratorCanManageGlobalInventory() {
        when(authentication.getName()).thenReturn(operatorId.toString());
        when(assignments.isAdministratorActor(operatorId)).thenReturn(true);
        Map<String, Object> expected = Map.of("equipmentName", "General stock");
        when(service.inventory(null, "General stock", 10, 0L)).thenReturn(expected);

        assertEquals(expected, controller.inventory(authentication,
                Map.of("equipmentName", "General stock", "totalQuantity", 10)));
        verify(assignments, never()).assertFacilityAllowed(any(), any());
    }
}
