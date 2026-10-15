package com.jnapanam.sportszone.release1.controller;
import com.jnapanam.sportszone.release1.dto.*;import com.jnapanam.sportszone.release1.service.Release1Service;import jakarta.validation.Valid;import java.util.*;import org.springframework.security.access.prepost.PreAuthorize;import org.springframework.security.core.Authentication;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1")
public class Release1Controller{
 private final Release1Service service;public Release1Controller(Release1Service service){this.service=service;}
 @GetMapping("/dependents/me")public List<Map<String,Object>> dependents(Authentication a){return service.dependents(id(a));}
 @PostMapping("/dependents")public Map<String,Object> dependent(Authentication a,@Valid@RequestBody DependentRequest r){return service.addDependent(id(a),r);}
 @GetMapping("/consents/me")public List<Map<String,Object>> consents(Authentication a){return service.consents(id(a));}
 @PostMapping("/consents")public Map<String,Object> consent(Authentication a,@Valid@RequestBody ConsentRequest r){return service.consent(id(a),r);}
 @GetMapping("/support/me")public List<Map<String,Object>> myTickets(Authentication a){return service.myTickets(id(a));}
 @PostMapping("/support")public Map<String,Object> ticket(Authentication a,@Valid@RequestBody SupportTicketRequest r){return service.ticket(id(a),r);}
 @GetMapping("/admin/support")@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','OPERATIONS')")public List<Map<String,Object>> tickets(){return service.tickets();}
 @GetMapping("/operations/incidents")@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','OPERATIONS')")public List<Map<String,Object>> incidents(){return service.incidents();}
 @PostMapping("/operations/incidents")@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','OPERATIONS')")public Map<String,Object> incident(Authentication a,@Valid@RequestBody IncidentRequest r){return service.incident(id(a),r);}
 @GetMapping("/operations/handovers")@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','OPERATIONS')")public List<Map<String,Object>> handovers(){return service.handovers();}
 @PostMapping("/operations/handovers")@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','OPERATIONS')")public Map<String,Object> handover(Authentication a,@Valid@RequestBody HandoverRequest r){return service.handover(id(a),r);}
 @GetMapping("/operations/equipment")@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','OPERATIONS')")public List<Map<String,Object>> equipment(){return service.equipment();}
 @PostMapping("/operations/equipment")@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','OPERATIONS')")public Map<String,Object> issue(Authentication a,@Valid@RequestBody EquipmentRequest r){return service.issue(id(a),r);}
 @PostMapping("/operations/equipment/{id}/return")@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','OPERATIONS')")public Map<String,Object> returned(Authentication a,@PathVariable UUID id,@RequestParam(required=false)String condition,@RequestParam(required=false)String damage){return service.returnEquipment(id(a),id,condition,damage);}
 private static UUID id(Authentication a){return UUID.fromString(a.getName());}
}
