package com.jnapanam.sportszone.identity.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/uat-otp")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class UatOtpNumberController {
 private final JdbcTemplate jdbc;
 private final boolean enabled;
 public UatOtpNumberController(JdbcTemplate jdbc,
   @Value("${app.security.test-otp-enabled:false}") boolean enabled) {
  this.jdbc=jdbc;this.enabled=enabled;
 }
 @GetMapping("/settings")
 public Map<String,Object> settings() {return Map.of("enabled",enabled,"scope","AUTHORIZED_NUMBERS_ONLY");}
 @GetMapping("/numbers")
 public List<Map<String,Object>> list() {
  return jdbc.queryForList("select mobile,active,note,created_at,updated_at from uat_otp_allowed_mobiles order by created_at desc");
 }
 public record NumberRequest(@Pattern(regexp="^[0-9]{10}$") String mobile,@Size(max=200) String note) {}
 @PostMapping("/numbers")
 @ResponseStatus(HttpStatus.CREATED)
 public Map<String,String> add(@Valid @RequestBody NumberRequest input,@AuthenticationPrincipal Jwt jwt) {
  jdbc.update("""
    insert into uat_otp_allowed_mobiles(mobile,active,note,created_by) values (?,true,?,?)
    on conflict (mobile) do update set active=true,note=excluded.note,updated_at=now()
    """,input.mobile(),input.note(),UUID.fromString(jwt.getSubject()));
  return Map.of("mobile",input.mobile(),"status","APPROVED");
 }
 @DeleteMapping("/numbers/{mobile}")
 @ResponseStatus(HttpStatus.NO_CONTENT)
 public void deactivate(@PathVariable String mobile) {
  if (!mobile.matches("[0-9]{10}")) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid mobile number");
  jdbc.update("update uat_otp_allowed_mobiles set active=false,updated_at=now() where mobile=?",mobile);
 }
}
