package com.jnapanam.sportszone.admin.controller;

import com.jnapanam.sportszone.admin.service.AdminReportingService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/admin/errors")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminErrorLogController {
    private final AdminReportingService reports;
    public AdminErrorLogController(AdminReportingService reports){this.reports=reports;}
    @GetMapping public List<AdminReportingService.ErrorLog> errors(
            @RequestParam(defaultValue="100")int limit,@RequestParam(required=false)Integer status){
        return reports.errors(limit,status);
    }
}
