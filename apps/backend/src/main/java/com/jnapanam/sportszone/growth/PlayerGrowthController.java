package com.jnapanam.sportszone.growth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/growth")
public class PlayerGrowthController {
    private final JdbcTemplate jdbc;
    public PlayerGrowthController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping("/me")
    public Map<String,Object> me(@AuthenticationPrincipal Jwt jwt) {
        return detail(UUID.fromString(jwt.getSubject()));
    }

    @GetMapping("/players/{playerId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','COACH')")
    public Map<String,Object> player(@PathVariable UUID playerId,@AuthenticationPrincipal Jwt jwt) {
        requireCoachAssignment(jwt,playerId);
        return detail(playerId);
    }

    private Map<String,Object> detail(UUID playerId) {
        var assessments = jdbc.queryForList("""
           select a.id,a.sport_id,s.name as sport_name,a.skill,a.score,a.remarks,
                  a.assessed_on,a.created_at
           from player_growth_assessments a join sports s on s.id=a.sport_id
           where a.player_id=? order by a.assessed_on desc,a.created_at desc limit 200
           """, playerId);
        var goals = jdbc.queryForList("""
           select g.id,g.sport_id,s.name as sport_name,g.title,g.target_score,g.due_on,g.status
           from player_growth_goals g join sports s on s.id=g.sport_id
           where g.player_id=? order by g.created_at desc limit 100
           """,playerId);
        var attendance=jdbc.queryForList("""
           select p.name as sport_name,count(*) as sessions,
             count(*) filter (where ta.present) as attended
           from training_attendance ta
           join training_enrollments te on te.id=ta.enrollment_id
           join training_batches tb on tb.id=te.batch_id
           join training_programs p on p.id=tb.program_id
           where te.user_id=? group by p.name order by p.name
           """,playerId);
        var competitions=jdbc.queryForList("""
           select t.name as tournament_name,tm.name as team_name,f.round_number,
              f.home_score,f.away_score,f.status as match_status,
              case when f.winner_team_id=tm.id then 'WIN'
                when f.winner_team_id is not null then 'LOSS' else 'PENDING' end as outcome
           from tournament_teams tm
           join tournaments t on t.id=tm.tournament_id
           join tournament_fixtures f on (f.home_team_id=tm.id or f.away_team_id=tm.id)
           where tm.captain_user_id=?
           order by t.start_date desc,f.round_number desc limit 100
           """,playerId);
        Map<String,Object> output=new LinkedHashMap<>();
        output.put("assessments",assessments);output.put("goals",goals);
        output.put("attendance",attendance);output.put("competitions",competitions);
        return output;
    }

    private void requireCoachAssignment(Jwt jwt,UUID playerId) {
        Object roles=jwt.getClaim("roles");
        if(roles instanceof Collection<?> entries && (entries.contains("SUPER_ADMIN") || entries.contains("ADMIN"))) return;
        Integer count=jdbc.queryForObject("""
          select count(*) from training_enrollments e join training_batches b on b.id=e.batch_id
          where e.user_id=? and b.coach_user_id=? and e.status <> 'CANCELLED'
          """,Integer.class,playerId,UUID.fromString(jwt.getSubject()));
        if(count==null || count==0) throw new org.springframework.security.access.AccessDeniedException("Player is not assigned to this coach");
    }

    public record AssessmentRequest(@NotNull UUID playerId,@NotNull UUID sportId,
        @NotBlank @Size(max=100) String skill,@NotNull @Min(0) @Max(100) Integer score,
        @Size(max=500) String remarks,@NotNull LocalDate assessedOn) {}

    @PostMapping("/assessments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','COACH')")
    public Map<String,Object> assess(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody AssessmentRequest req) {
        requireCoachAssignment(jwt,req.playerId());
        UUID id=UUID.randomUUID();
        jdbc.update("""
          insert into player_growth_assessments(id,player_id,sport_id,assessor_id,skill,score,remarks,assessed_on)
          values (?,?,?,?,?,?,?,?)
          """,id,req.playerId(),req.sportId(),UUID.fromString(jwt.getSubject()),
              req.skill().trim(),req.score(),req.remarks(),req.assessedOn());
        return Map.of("id",id);
    }

    public record GoalRequest(@NotNull UUID playerId,@NotNull UUID sportId,
       @NotBlank @Size(max=140) String title,@Min(0) @Max(100) Integer targetScore,LocalDate dueOn) {}

    @PostMapping("/goals")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','COACH')")
    public Map<String,Object> goal(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody GoalRequest req) {
        UUID id=UUID.randomUUID();
        jdbc.update("""
          insert into player_growth_goals(id,player_id,sport_id,created_by,title,target_score,due_on)
          values (?,?,?,?,?,?,?)
          """,id,req.playerId(),req.sportId(),UUID.fromString(jwt.getSubject()),
              req.title().trim(),req.targetScore(),req.dueOn());
        return Map.of("id",id);
    }
}
