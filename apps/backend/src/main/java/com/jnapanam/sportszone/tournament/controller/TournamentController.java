package com.jnapanam.sportszone.tournament.controller;

import com.jnapanam.sportszone.tournament.dto.*;
import com.jnapanam.sportszone.tournament.service.TournamentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tournaments")
public class TournamentController {
    private final TournamentService service;

    public TournamentController(TournamentService service) {
        this.service = service;
    }

    @GetMapping("/organizer-applications/me")
    public List<OrganizerApplicationResponse> myApplications(Authentication authentication) {
        return service.myApplications(user(authentication));
    }

    @PostMapping("/organizer-applications")
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizerApplicationResponse apply(Authentication authentication,
            @Valid @RequestBody OrganizerApplicationRequest request) {
        return service.apply(user(authentication), request);
    }

    @GetMapping("/organizer/me")
    @PreAuthorize("hasRole('ORGANIZER')")
    public List<TournamentResponse> managed(Authentication authentication) {
        return service.managedBy(user(authentication));
    }

    @PostMapping("/organizer")
    @PreAuthorize("hasRole('ORGANIZER')")
    @ResponseStatus(HttpStatus.CREATED)
    public TournamentResponse create(Authentication authentication, @Valid @RequestBody TournamentRequest request) {
        return service.createByOrganizer(user(authentication), request);
    }

    @PatchMapping("/organizer/{id}/status")
    @PreAuthorize("hasRole('ORGANIZER')")
    public TournamentResponse organizerStatus(Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody TournamentStatusRequest request) {
        return service.statusByOrganizer(user(authentication), id, request);
    }

    @PostMapping("/{id}/teams")
    @ResponseStatus(HttpStatus.CREATED)
    public TeamResponse register(Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody TeamRegistrationRequest request) {
        return service.register(user(authentication), id, request);
    }

    @GetMapping("/teams/me")
    public List<TeamResponse> mine(Authentication authentication) {
        return service.mine(user(authentication));
    }

    private static UUID user(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
