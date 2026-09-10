package com.jnapanam.sportszone.tournament.controller;

import com.jnapanam.sportszone.tournament.dto.*;
import com.jnapanam.sportszone.tournament.service.TournamentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/tournaments")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
public class AdminTournamentController {
    private final TournamentService service;

    public AdminTournamentController(TournamentService service) {
        this.service = service;
    }

    @GetMapping public List<TournamentResponse> all() { return service.all(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public TournamentResponse create(@Valid @RequestBody TournamentRequest request) { return service.create(request); }
    @PutMapping("/{id}") public TournamentResponse update(@PathVariable UUID id, @Valid @RequestBody TournamentRequest request) { return service.update(id, request); }
    @PatchMapping("/{id}/status") public TournamentResponse status(@PathVariable UUID id, @Valid @RequestBody TournamentStatusRequest request) { return service.status(id, request); }
    @GetMapping("/{id}/teams") public List<TeamResponse> teams(@PathVariable UUID id) { return service.teams(id); }
    @PostMapping("/{id}/fixtures/generate") public List<FixtureResponse> generate(@PathVariable UUID id) { return service.generate(id); }
    @PatchMapping("/fixtures/{id}/schedule") public FixtureResponse schedule(@PathVariable UUID id, @RequestBody FixtureScheduleRequest request) { return service.schedule(id, request); }
    @PatchMapping("/fixtures/{id}/result") public FixtureResponse result(@PathVariable UUID id, @Valid @RequestBody FixtureResultRequest request) { return service.result(id, request); }
    @GetMapping("/organizer-applications/approved") public List<OrganizerOptionResponse> approved() { return service.approvedOrganizers(); }
    @GetMapping("/organizer-applications") public List<OrganizerApplicationResponse> applications() { return service.applications(); }
    @PatchMapping("/organizer-applications/{id}") public OrganizerApplicationResponse review(@PathVariable UUID id, @Valid @RequestBody OrganizerReviewRequest request) { return service.review(id, request); }
}
