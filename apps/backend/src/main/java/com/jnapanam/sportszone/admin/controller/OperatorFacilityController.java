package com.jnapanam.sportszone.admin.controller;

import com.jnapanam.sportszone.admin.service.OperatorAssignmentService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/operations/facilities")
@PreAuthorize("hasAnyRole('OPERATIONS','ADMIN','SUPER_ADMIN')")
public class OperatorFacilityController {
    private final OperatorAssignmentService service;
    public OperatorFacilityController(OperatorAssignmentService service) { this.service = service; }
    @GetMapping
    public List<Map<String, Object>> mine(Authentication auth) {
        return service.facilitiesForActor(UUID.fromString(auth.getName()));
    }
}
