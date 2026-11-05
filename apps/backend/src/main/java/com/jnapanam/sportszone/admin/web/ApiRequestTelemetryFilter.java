package com.jnapanam.sportszone.admin.web;

import com.jnapanam.sportszone.admin.model.AuditLog;
import com.jnapanam.sportszone.admin.repository.AuditLogRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Wraps Spring Security to keep a reference for rejected authentication and permission requests. */
@Component @Order(-102)
public class ApiRequestTelemetryFilter extends OncePerRequestFilter {
    public static final String CORRELATION_ATTRIBUTE="jnapanam.correlationId";
    public static final String AUDITED_ATTRIBUTE="jnapanam.auditRecorded";
    private static final Logger LOG=LoggerFactory.getLogger(ApiRequestTelemetryFilter.class);
    private final AuditLogRepository logs;
    public ApiRequestTelemetryFilter(AuditLogRepository logs){this.logs=logs;}
    @Override protected boolean shouldNotFilter(HttpServletRequest request){
        return !request.getRequestURI().startsWith("/api/v1/") || "OPTIONS".equals(request.getMethod());
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
            throws ServletException,IOException {
        long start=System.nanoTime();
        String supplied=request.getHeader("X-Correlation-ID");
        String id=supplied!=null && supplied.matches("[A-Za-z0-9_-]{8,80}")
                ? supplied : UUID.randomUUID().toString();
        request.setAttribute(CORRELATION_ATTRIBUTE,id);
        response.setHeader("X-Correlation-ID",id);
        boolean unhandled=false;
        try {chain.doFilter(request,response);}
        catch(ServletException|IOException|RuntimeException ex){unhandled=true;throw ex;}
        finally {
            int status=unhandled?500:response.getStatus();
            if(status>=400 && !Boolean.TRUE.equals(request.getAttribute(AUDITED_ATTRIBUTE))) {
                try {
                    String path=AdminAuditFilter.safePath(request.getRequestURI());
                    String resource=status==401?"AUTH":status==403?"ACCESS":"API";
                    logs.save(new AuditLog(null,request.getMethod(),path,status,request.getRemoteAddr(),
                            "ERROR_"+resource,resource,null,null,id,null,(System.nanoTime()-start)/1_000_000));
                    if(status>=500)LOG.warn("API failure method={} status={} path={} correlationId={}",
                            request.getMethod(),status,path,id);
                }catch(Exception ignored){LOG.warn("API error audit unavailable correlationId={}",id);}
            }
        }
    }
}
