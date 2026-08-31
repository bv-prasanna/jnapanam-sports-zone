package com.jnapanam.sportszone.admin.controller;

import com.jnapanam.sportszone.admin.dto.*;
import com.jnapanam.sportszone.admin.service.AdminReportingService;
import com.jnapanam.sportszone.common.exception.BadRequestException;
import java.time.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','OPERATIONS')")
public class AdminReportingController {
    private final AdminReportingService service;
    public AdminReportingController(AdminReportingService service){this.service=service;}
    @GetMapping("/dashboard") public DashboardResponse dashboard(@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate from,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate to){return service.dashboard(from,to);}
    @GetMapping("/reconciliation") public ReconciliationResponse reconciliation(){return service.reconciliation();}
    @GetMapping("/reports/revenue") public List<Map<String,Object>> revenue(@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate from,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate to){return service.revenue(from,to);}
    @GetMapping("/reports/bookings") public List<Map<String,Object>> bookings(@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate from,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate to){return service.bookings(from,to);}
    @GetMapping("/reports/memberships") public List<Map<String,Object>> memberships(){return service.memberships();}
    @GetMapping("/reports/utilization") public List<Map<String,Object>> utilization(@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate from,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate to){return service.utilization(from,to);}
    @GetMapping("/reports/customers") public List<Map<String,Object>> customers(){return service.customers();}
    @GetMapping("/reports/training") public List<Map<String,Object>> training(@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate from,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate to){return service.training(from,to);}
    @GetMapping("/reports/tournaments") public List<Map<String,Object>> tournaments(@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate from,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate to){return service.tournaments(from,to);}

    @GetMapping("/reports/{report}/export")
    public ResponseEntity<byte[]> export(@PathVariable String report,@RequestParam(defaultValue="csv")String format,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate from,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate to){
        if(!"csv".equalsIgnoreCase(format))throw new BadRequestException("Only CSV export is supported in backend v1");
        return ResponseEntity.ok().contentType(new MediaType("text","csv")).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+report+"-report.csv\"").body(service.csv(report,from,to));
    }

    @GetMapping("/audit-logs") public List<AuditLogResponse> audit(@RequestParam(defaultValue="100")int limit){return service.audit(limit);}
    @GetMapping("/audit-logs/search") public List<Map<String,Object>> searchAudit(@RequestParam(required=false)UUID actorUserId,@RequestParam(required=false)String action,@RequestParam(required=false)String resourceType,@RequestParam(required=false)Boolean successful,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)Instant from,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)Instant to,@RequestParam(defaultValue="100")int limit){return service.searchAudit(actorUserId,action,resourceType,successful,from,to,limit);}
}
