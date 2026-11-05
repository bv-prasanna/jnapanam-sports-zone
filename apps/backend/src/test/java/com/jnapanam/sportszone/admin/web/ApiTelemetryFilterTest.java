package com.jnapanam.sportszone.admin.web;

import com.jnapanam.sportszone.admin.model.AuditLog;
import com.jnapanam.sportszone.admin.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiTelemetryFilterTest {
    @Test void failedGetIsRecordedWithoutQuerySecrets() throws Exception {
        var repo=mock(AuditLogRepository.class);
        var request=new MockHttpServletRequest("GET","/api/v1/profile/me");
        request.setQueryString("password=private&refreshToken=secret");
        request.setAttribute(ApiRequestTelemetryFilter.CORRELATION_ATTRIBUTE,"valid-reference-123");
        var response=new MockHttpServletResponse();
        new AdminAuditFilter(repo).doFilterInternal(request,response,
                (req,res)->((jakarta.servlet.http.HttpServletResponse)res).setStatus(401));
        var captor=ArgumentCaptor.forClass(AuditLog.class);
        verify(repo).save(captor.capture());
        assertEquals(401,captor.getValue().getResponseStatus());
        assertNull(captor.getValue().getQueryString());
        assertEquals("valid-reference-123",response.getHeader("X-Correlation-ID"));
    }
    @Test void successfulReadsAreNotAudited() throws Exception {
        var repo=mock(AuditLogRepository.class);
        new AdminAuditFilter(repo).doFilterInternal(
                new MockHttpServletRequest("GET","/api/v1/public/sports"),new MockHttpServletResponse(),(a,b)->{});
        verifyNoInteractions(repo);
    }
    @Test void earlyDeniedRequestsAreLoggedAndUntrustedCorrelationIdsAreDiscarded() throws Exception {
        var repo=mock(AuditLogRepository.class);
        var request=new MockHttpServletRequest("GET","/api/v1/admin/errors");
        request.addHeader("X-Correlation-ID","unsafe-value!");
        var response=new MockHttpServletResponse();
        new ApiRequestTelemetryFilter(repo).doFilterInternal(request,response,
                (a,b)->((jakarta.servlet.http.HttpServletResponse)b).setStatus(403));
        assertTrue(response.getHeader("X-Correlation-ID").matches("[a-f0-9-]{36}"));
        var captor=ArgumentCaptor.forClass(AuditLog.class);
        verify(repo).save(captor.capture());
        assertEquals(403,captor.getValue().getResponseStatus());
        assertNull(captor.getValue().getQueryString());
    }
    @Test void nestedAuditIsNotDuplicated() throws Exception {
        var repo=mock(AuditLogRepository.class);
        var outer=new ApiRequestTelemetryFilter(repo);var inner=new AdminAuditFilter(repo);
        outer.doFilterInternal(new MockHttpServletRequest("POST","/api/v1/bookings"),
                new MockHttpServletResponse(),(a,b)->inner.doFilterInternal(
                        (jakarta.servlet.http.HttpServletRequest)a,(jakarta.servlet.http.HttpServletResponse)b,
                        (c,d)->((jakarta.servlet.http.HttpServletResponse)d).setStatus(409)));
        verify(repo,times(1)).save(any(AuditLog.class));
    }
}
