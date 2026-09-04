package com.jnapanam.sportszone.tournament.dto;

import java.util.UUID;

public record OrganizerOptionResponse(UUID userId, String applicantName, String eligibility) {}
