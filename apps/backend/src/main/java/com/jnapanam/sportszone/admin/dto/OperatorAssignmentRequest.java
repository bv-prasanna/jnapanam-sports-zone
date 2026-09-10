package com.jnapanam.sportszone.admin.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record OperatorAssignmentRequest(@NotNull List<UUID> facilityIds) {}
