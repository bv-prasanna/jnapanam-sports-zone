package com.jnapanam.sportszone.tournament.service;

import com.jnapanam.sportszone.catalog.model.Facility;
import com.jnapanam.sportszone.catalog.model.Sport;
import com.jnapanam.sportszone.catalog.repository.FacilityRepository;
import com.jnapanam.sportszone.catalog.repository.SportRepository;
import com.jnapanam.sportszone.common.exception.BadRequestException;
import com.jnapanam.sportszone.common.exception.ConflictException;
import com.jnapanam.sportszone.common.exception.NotFoundException;
import com.jnapanam.sportszone.identity.model.RoleName;
import com.jnapanam.sportszone.identity.model.User;
import com.jnapanam.sportszone.identity.repository.RoleRepository;
import com.jnapanam.sportszone.identity.repository.UserRepository;
import com.jnapanam.sportszone.notification.service.NotificationService;
import com.jnapanam.sportszone.tournament.dto.*;
import com.jnapanam.sportszone.tournament.model.*;
import com.jnapanam.sportszone.tournament.repository.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TournamentService {
    private final OrganizerApplicationRepository applications;
    private final TournamentRepository tournaments;
    private final TournamentTeamRepository teams;
    private final TournamentTeamMemberRepository members;
    private final TournamentFixtureRepository fixtures;
    private final UserRepository users;
    private final RoleRepository roles;
    private final SportRepository sports;
    private final FacilityRepository facilities;
    private final NotificationService notifications;
    private final long paymentMinutes;

    public TournamentService(OrganizerApplicationRepository applications, TournamentRepository tournaments,
            TournamentTeamRepository teams, TournamentTeamMemberRepository members,
            TournamentFixtureRepository fixtures, UserRepository users, RoleRepository roles,
            SportRepository sports, FacilityRepository facilities, NotificationService notifications,
            @Value("${app.payment.pending-minutes:15}") long paymentMinutes) {
        this.applications = applications;
        this.tournaments = tournaments;
        this.teams = teams;
        this.members = members;
        this.fixtures = fixtures;
        this.users = users;
        this.roles = roles;
        this.sports = sports;
        this.facilities = facilities;
        this.notifications = notifications;
        this.paymentMinutes = paymentMinutes;
    }

    @Transactional
    public OrganizerApplicationResponse apply(UUID userId, OrganizerApplicationRequest request) {
        if (applications.existsByUserIdAndStatusIn(userId, List.of(OrganizerStatus.PENDING, OrganizerStatus.APPROVED))) {
            throw new ConflictException("An open or approved organizer application already exists");
        }
        return OrganizerApplicationResponse.from(applications.save(new OrganizerApplication(user(userId),
                request.organizationName(), request.experience(), request.contactNumber())));
    }

    @Transactional(readOnly = true)
    public List<OrganizerApplicationResponse> myApplications(UUID userId) {
        return applications.findByUserIdOrderByAppliedAtDesc(userId).stream()
                .map(OrganizerApplicationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<OrganizerOptionResponse> approvedOrganizers() {
        Map<UUID, OrganizerOptionResponse> options = new LinkedHashMap<>();
        for (OrganizerApplication application : applications.findByStatusOrderByAppliedAtDesc(OrganizerStatus.APPROVED)) {
            User user = application.getUser();
            options.put(user.getId(), new OrganizerOptionResponse(user.getId(), user.getFullName(), "APPROVED_ORGANIZER"));
        }
        for (User user : users.findAll()) {
            if (user.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ADMIN || role.getName() == RoleName.SUPER_ADMIN)) {
                options.putIfAbsent(user.getId(), new OrganizerOptionResponse(user.getId(), user.getFullName(), "ADMINISTRATOR"));
            }
        }
        return options.values().stream().toList();
    }

    @Transactional(readOnly = true)
    public List<OrganizerApplicationResponse> applications() {
        return applications.findAllByOrderByAppliedAtDesc().stream().map(OrganizerApplicationResponse::from).toList();
    }

    @Transactional
    public OrganizerApplicationResponse review(UUID id, OrganizerReviewRequest request) {
        if (request.status() == OrganizerStatus.PENDING) throw new BadRequestException("Review must approve or reject the application");
        OrganizerApplication application = applications.findById(id)
                .orElseThrow(() -> new NotFoundException("Organizer application not found"));
        if (application.getStatus() != OrganizerStatus.PENDING) throw new ConflictException("Application is already reviewed");
        application.review(request.status(), request.reviewNote());
        User applicant = application.getUser();
        if (request.status() == OrganizerStatus.APPROVED) {
            var organizerRole = roles.findByName(RoleName.ORGANIZER)
                    .orElseThrow(() -> new ConflictException("Organizer role is not configured"));
            applicant.getRoles().add(organizerRole);
            notifications.notify(applicant.getId(), "ORGANIZER_APPROVED", "Organizer access approved",
                    "Your organizer application has been approved. You can now create and manage tournaments.");
        } else {
            notifications.notify(applicant.getId(), "ORGANIZER_REJECTED", "Organizer application reviewed",
                    "Your organizer application was not approved. " + cleanReviewNote(request.reviewNote()));
        }
        return OrganizerApplicationResponse.from(application);
    }

    @Transactional
    public TournamentResponse create(TournamentRequest request) {
        return createFor(request.organizerUserId(), request);
    }

    @Transactional
    public TournamentResponse createByOrganizer(UUID organizerId, TournamentRequest request) {
        User organizer = user(organizerId);
        if (!organizer.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ORGANIZER)) {
            throw new ConflictException("Organizer access is required");
        }
        if (!applications.existsByUserIdAndStatus(organizerId, OrganizerStatus.APPROVED)) {
            throw new ConflictException("Organizer approval is required");
        }
        return createFor(organizerId, request);
    }

    private TournamentResponse createFor(UUID organizerId, TournamentRequest request) {
        validateDates(request);
        if (tournaments.existsByCodeIgnoreCase(request.code())) throw new ConflictException("Tournament code already exists");
        User organizer = user(organizerId);
        if (!eligibleOrganizer(organizer)) throw new ConflictException("Select an approved organizer, administrator, or super administrator");
        Sport sport = sports.findByCodeIgnoreCase(request.sportCode())
                .orElseThrow(() -> new NotFoundException("Sport not found"));
        Tournament tournament = new Tournament(request.code(), request.name(), request.description(), sport, organizer,
                request.format(), request.registrationOpens(), request.registrationCloses(), request.startDate(),
                request.endDate(), request.teamSize(), request.maxTeams(), request.entryFee());
        return response(tournaments.save(tournament));
    }

    @Transactional
    public TournamentResponse update(UUID id, TournamentRequest request) {
        validateDates(request);
        Tournament tournament = tournament(id);
        if (!tournament.getCode().equalsIgnoreCase(request.code())) throw new BadRequestException("Tournament code cannot be changed");
        User organizer = user(request.organizerUserId());
        if (!eligibleOrganizer(organizer)) throw new ConflictException("Select an approved organizer, administrator, or super administrator");
        Sport sport = sports.findByCodeIgnoreCase(request.sportCode()).orElseThrow(() -> new NotFoundException("Sport not found"));
        long registered = teams.countByTournamentIdAndStatus(tournament.getId(), TeamStatus.REGISTERED);
        if (request.maxTeams() < registered) throw new BadRequestException("Maximum teams cannot be lower than registered teams");
        tournament.update(request.name(), request.description(), sport, organizer, request.format(), request.registrationOpens(),
                request.registrationCloses(), request.startDate(), request.endDate(), request.teamSize(), request.maxTeams(), request.entryFee());
        return response(tournament);
    }

    @Transactional
    public TournamentResponse status(UUID id, TournamentStatusRequest request) {
        Tournament tournament = tournament(id);
        tournament.status(request.status());
        return response(tournament);
    }

    @Transactional
    public TournamentResponse statusByOrganizer(UUID organizerId, UUID id, TournamentStatusRequest request) {
        Tournament tournament = tournament(id);
        if (!tournament.getOrganizer().getId().equals(organizerId)) throw new NotFoundException("Tournament not found");
        if (!SetHolder.ORGANIZER_STATUSES.contains(request.status())) throw new ConflictException("Organizer cannot set this tournament status");
        tournament.status(request.status());
        return response(tournament);
    }

    @Transactional(readOnly = true)
    public List<TournamentResponse> managedBy(UUID organizerId) {
        return tournaments.findAllByOrderByStartDateDesc().stream()
                .filter(t -> t.getOrganizer().getId().equals(organizerId)).map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public List<TournamentResponse> publicList() {
        return tournaments.findByStatusInOrderByStartDateAsc(List.of(TournamentStatus.PUBLISHED,
                TournamentStatus.REGISTRATION_OPEN, TournamentStatus.REGISTRATION_CLOSED,
                TournamentStatus.IN_PROGRESS, TournamentStatus.COMPLETED)).stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public List<TournamentResponse> all() {
        return tournaments.findAllByOrderByStartDateDesc().stream().map(this::response).toList();
    }

    @Transactional
    public TeamResponse register(UUID userId, UUID tournamentId, TeamRegistrationRequest request) {
        Tournament tournament = tournament(tournamentId);
        if (tournament.getStatus() != TournamentStatus.REGISTRATION_OPEN) throw new ConflictException("Tournament registration is not open");
        LocalDate now = LocalDate.now();
        if (now.isBefore(tournament.getRegistrationOpens()) || now.isAfter(tournament.getRegistrationCloses())) {
            throw new ConflictException("Tournament registration window is closed");
        }
        if (request.members().size() != tournament.getTeamSize()) throw new BadRequestException("Team must contain exactly " + tournament.getTeamSize() + " members");
        List<TeamStatus> active = List.of(TeamStatus.REGISTERED, TeamStatus.PENDING_PAYMENT);
        if (teams.countByTournamentIdAndStatusIn(tournamentId, active) >= tournament.getMaxTeams()) throw new ConflictException("Tournament is full");
        if (teams.existsByTournamentIdAndNameIgnoreCaseAndStatusIn(tournamentId, request.teamName(), active)) throw new ConflictException("Team name already exists");
        if (teams.existsByTournamentIdAndCaptainIdAndStatusIn(tournamentId, userId, active)) throw new ConflictException("Captain already registered a team");
        boolean paymentRequired = tournament.getEntryFee().signum() > 0;
        TournamentTeam team = teams.save(new TournamentTeam(tournament, user(userId), request.teamName(), paymentRequired,
                Instant.now().plusSeconds(paymentMinutes * 60)));
        for (var member : request.members()) members.save(new TournamentTeamMember(team, member.name(), member.email(), member.mobile()));
        return team(team);
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> mine(UUID userId) {
        return teams.findByCaptainIdOrderByRegisteredAtDesc(userId).stream().map(this::team).toList();
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> teams(UUID tournamentId) {
        return teams.findByTournamentIdAndStatusOrderByRegisteredAtAsc(tournamentId, TeamStatus.REGISTERED).stream().map(this::team).toList();
    }

    @Transactional
    public List<FixtureResponse> generate(UUID tournamentId) {
        Tournament tournament = tournament(tournamentId);
        if (fixtures.existsByTournamentId(tournamentId)) throw new ConflictException("Fixtures already exist");
        List<TournamentTeam> registered = teams.findByTournamentIdAndStatusOrderByRegisteredAtAsc(tournamentId, TeamStatus.REGISTERED);
        if (registered.size() < 2) throw new ConflictException("At least two teams are required");
        int match = 1;
        if (tournament.getFormat() == TournamentFormat.LEAGUE) {
            for (int i = 0; i < registered.size(); i++) for (int j = i + 1; j < registered.size(); j++) {
                fixtures.save(new TournamentFixture(tournament, 1, match++, registered.get(i), registered.get(j)));
            }
        } else {
            for (int i = 0; i < registered.size(); i += 2) {
                fixtures.save(new TournamentFixture(tournament, 1, match++, registered.get(i),
                        i + 1 < registered.size() ? registered.get(i + 1) : null));
            }
        }
        tournament.status(TournamentStatus.REGISTRATION_CLOSED);
        return fixtureList(tournamentId);
    }

    @Transactional
    public FixtureResponse schedule(UUID id, FixtureScheduleRequest request) {
        TournamentFixture fixture = fixture(id);
        Facility facility = request.facilityId() == null ? null : facilities.findById(request.facilityId())
                .orElseThrow(() -> new NotFoundException("Facility not found"));
        fixture.schedule(facility, request.scheduledAt());
        return FixtureResponse.from(fixture);
    }

    @Transactional
    public FixtureResponse result(UUID id, FixtureResultRequest request) {
        TournamentFixture fixture = fixture(id);
        if (fixture.getTournament().getFormat() == TournamentFormat.SINGLE_ELIMINATION
                && request.homeScore() == request.awayScore()) throw new BadRequestException("Elimination match cannot end in a draw");
        fixture.result(request.homeScore(), request.awayScore());
        return FixtureResponse.from(fixture);
    }

    @Transactional(readOnly = true)
    public List<FixtureResponse> fixtureList(UUID tournamentId) {
        tournament(tournamentId);
        return fixtures.findByTournamentIdOrderByRoundNumberAscMatchNumberAsc(tournamentId).stream()
                .map(FixtureResponse::from).toList();
    }

    @Transactional
    public int cleanupUnpaid() {
        int count = 0;
        for (TournamentTeam team : teams.findByStatusAndPaymentExpiresAtLessThanEqual(TeamStatus.PENDING_PAYMENT, Instant.now())) {
            team.expirePayment();
            count++;
        }
        return count;
    }

    private TournamentResponse response(Tournament tournament) {
        return TournamentResponse.from(tournament, teams.countByTournamentIdAndStatus(tournament.getId(), TeamStatus.REGISTERED));
    }
    private TeamResponse team(TournamentTeam team) { return TeamResponse.from(team, members.findByTeamIdOrderByCreatedAtAsc(team.getId())); }
    private Tournament tournament(UUID id) { return tournaments.findById(id).orElseThrow(() -> new NotFoundException("Tournament not found")); }
    private TournamentFixture fixture(UUID id) { return fixtures.findById(id).orElseThrow(() -> new NotFoundException("Fixture not found")); }
    private User user(UUID id) { return users.findById(id).orElseThrow(() -> new NotFoundException("User not found")); }

    private boolean eligibleOrganizer(User user) {
        return applications.existsByUserIdAndStatus(user.getId(), OrganizerStatus.APPROVED)
                || user.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ADMIN || role.getName() == RoleName.SUPER_ADMIN);
    }

    private static String cleanReviewNote(String value) {
        return value == null || value.isBlank() ? "Please contact the administrator if you need more information." : value.trim();
    }

    private static void validateDates(TournamentRequest request) {
        if (request.registrationCloses().isBefore(request.registrationOpens())
                || request.startDate().isBefore(request.registrationCloses())
                || request.endDate().isBefore(request.startDate())) throw new BadRequestException("Tournament dates are invalid");
    }

    private static final class SetHolder {
        private static final java.util.Set<TournamentStatus> ORGANIZER_STATUSES = java.util.Set.of(
                TournamentStatus.DRAFT, TournamentStatus.PUBLISHED, TournamentStatus.REGISTRATION_OPEN,
                TournamentStatus.REGISTRATION_CLOSED, TournamentStatus.CANCELLED);
    }
}
