package com.jnapanam.sportszone.admin.controller;

import com.jnapanam.sportszone.admin.dto.OperatorAssignmentRequest;
import com.jnapanam.sportszone.admin.service.OperatorAssignmentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/operator-assignments")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
public class OperatorAssignmentController {
    private final OperatorAssignmentService service;

    public OperatorAssignmentController(OperatorAssignmentService service) {
        this.service = service;
    }

    @GetMapping("/{operatorId}")
    public List<Map<String, Object>> get(@PathVariable UUID operatorId) {
        return service.assignments(operatorId);
    }

    @PutMapping("/{operatorId}")
    public List<Map<String, Object>> replace(@PathVariable UUID operatorId,
            @Valid @RequestBody OperatorAssignmentRequest request) {
        return service.replace(operatorId, request.facilityIds());
    }
}
