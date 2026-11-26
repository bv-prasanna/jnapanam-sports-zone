package com.jnapanam.sportszone.release1.controller;

import com.jnapanam.sportszone.admin.service.OperatorAssignmentService;
import com.jnapanam.sportszone.common.exception.NotFoundException;
import com.jnapanam.sportszone.release1.service.Release1OperationsService;
import com.jnapanam.sportszone.tournament.service.TournamentService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/release1/ops")
public class Release1OperationsController {
    private final Release1OperationsService service;
    private final OperatorAssignmentService assignments;
    private final TournamentService tournaments;

    public Release1OperationsController(Release1OperationsService service, OperatorAssignmentService assignments,
            TournamentService tournaments) {
        this.service = service;
        this.assignments = assignments;
        this.tournaments = tournaments;
    }

    @PostMapping("/support/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','OPERATIONS')")
    public Map<String, Object> support(Authentication auth, @PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        return service.support(uid(auth), id, str(body, "action"), str(body, "details"),
                body.get("assigneeId") == null ? null : UUID.fromString(str(body, "assigneeId")));
    }

    @GetMapping("/equipment-inventory")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','OPERATIONS')")
    public List<Map<String, Object>> inventory(Authentication auth) {
        UUID actorId = uid(auth);
        if (assignments.isAdministratorActor(actorId)) return service.inventory();
        List<UUID> facilityIds = assignments.facilitiesForActor(actorId).stream()
                .map(facility -> (UUID) facility.get("facilityId"))
                .collect(Collectors.toList());
        return service.inventoryForFacilities(facilityIds);
    }

    @PostMapping("/equipment-inventory")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','OPERATIONS')")
    public Map<String, Object> inventory(Authentication auth, @RequestBody Map<String, Object> body) {
        UUID actorId = uid(auth);
        UUID facilityId = body.get("facilityId") == null ? null : UUID.fromString(str(body, "facilityId"));
        if (!assignments.isAdministratorActor(actorId)) {
            if (facilityId == null) throw new NotFoundException("Facility not found");
            assignments.assertFacilityAllowed(actorId, facilityId);
        }
        return service.inventory(facilityId, str(body, "equipmentName"), num(body, "totalQuantity"),
                Long.parseLong(String.valueOf(body.getOrDefault("depositPaise", 0))));
    }

    @PostMapping("/memberships/{id}/family")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public Map<String, Object> family(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        return service.family(id, UUID.fromString(str(body, "dependentId")));
    }

    @PostMapping("/training/{id}/installments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public Map<String, Object> installment(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        return service.installment(id, num(body, "installmentNo"),
                Long.parseLong(str(body, "amountPaise")), str(body, "dueDate"));
    }

    @GetMapping("/tournaments/{id}/standings")
    public List<Map<String, Object>> standings(@PathVariable UUID id) {
        return service.standings(id);
    }

    @PutMapping("/tournaments/{id}/standings/{team}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','ORGANIZER')")
    public Map<String, Object> standing(Authentication auth, @PathVariable UUID id, @PathVariable UUID team,
            @RequestBody Map<String, Object> body) {
        UUID actorId = uid(auth);
        if (!assignments.isAdministratorActor(actorId) && tournaments.managedBy(actorId).stream()
                .noneMatch(tournament -> tournament.id().equals(id))) {
            throw new NotFoundException("Tournament not found");
        }
        return service.standing(id, team, num(body, "played"), num(body, "won"), num(body, "drawn"),
                num(body, "lost"), num(body, "points"), num(body, "scoreFor"), num(body, "scoreAgainst"),
                Double.parseDouble(String.valueOf(body.getOrDefault("tieBreakValue", 0))));
    }

    private static UUID uid(Authentication auth) { return UUID.fromString(auth.getName()); }
    private static String str(Map<String, Object> body, String key) {
        Object value = body.get(key);
        return value == null ? null : value.toString();
    }
    private static int num(Map<String, Object> body, String key) {
        return Integer.parseInt(String.valueOf(body.getOrDefault(key, 0)));
    }
}
