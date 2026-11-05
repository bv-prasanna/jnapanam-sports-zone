package com.jnapanam.sportszone.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerSafeMessagesTest {
    private final GlobalExceptionHandler advice=new GlobalExceptionHandler();
    @Test void unexpectedErrorDoesNotRevealCredentials(){
        var result=advice.unexpected(new IllegalStateException("password=SECRET SQL failure"));
        assertEquals(500,result.getStatusCode().value());
        assertFalse(result.getBody().getDetail().contains("SECRET"));
        assertEquals("SERVER_ERROR",result.getBody().getProperties().get("code"));
    }
    @Test void mfaChallengeRemainsDistinctFromSessionExpiry(){
        var result=advice.mfaChallenge(new MfaChallengeRequiredException());
        assertEquals(401,result.getStatusCode().value());
        assertTrue(result.getBody().getDetail().contains("MFA code sent"));
    }
    @Test void responseReasonCannotExposeSecrets(){
        var result=advice.status(new ResponseStatusException(HttpStatus.BAD_REQUEST,"password secret"));
        assertEquals(400,result.getStatusCode().value());
        assertFalse(result.getBody().getDetail().contains("password"));
    }
    @Test void referenceAndSafeErrorCategoryAreAssociatedWithRequest(){
        var request=new org.springframework.mock.web.MockHttpServletRequest("GET","/api/v1/bookings");
        request.setAttribute("jnapanam.correlationId","support-ref-1234");
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(
                new org.springframework.web.context.request.ServletRequestAttributes(request));
        try{
            var result=advice.unexpected(new IllegalStateException("password=SECRET"));
            assertEquals("support-ref-1234",result.getBody().getProperties().get("requestId"));
            assertEquals("UNHANDLED_ILLEGALSTATEEXCEPTION",request.getAttribute("jnapanam.errorCode"));
            assertFalse(result.getBody().getDetail().contains("SECRET"));
        }finally{
            org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
        }
    }
}
