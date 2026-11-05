package com.jnapanam.sportszone.common.exception;

import java.net.URI;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.security.access.AccessDeniedException;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOG=LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(BadRequestException.class) ResponseEntity<ProblemDetail> badRequest(BadRequestException ex) { return problem(HttpStatus.BAD_REQUEST, ex.getMessage()); }
    @ExceptionHandler(ConflictException.class) ResponseEntity<ProblemDetail> conflict(ConflictException ex) { return problem(HttpStatus.CONFLICT, ex.getMessage()); }
    @ExceptionHandler(NotFoundException.class) ResponseEntity<ProblemDetail> notFound(NotFoundException ex) { return problem(HttpStatus.NOT_FOUND, ex.getMessage()); }
    @ExceptionHandler(com.jnapanam.sportszone.common.exception.MfaChallengeRequiredException.class)
    ResponseEntity<ProblemDetail> mfaChallenge(com.jnapanam.sportszone.common.exception.MfaChallengeRequiredException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "MFA code sent. Enter the 6-digit code to continue.");
    }
    @ExceptionHandler({UnauthorizedException.class, AuthenticationException.class}) ResponseEntity<ProblemDetail> unauthorized(RuntimeException ex) { return problem(HttpStatus.UNAUTHORIZED, "Invalid credentials or token"); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException ex) {
        Map<String,String> errors = ex.getBindingResult().getFieldErrors().stream().collect(Collectors.toMap(e -> e.getField(), e -> e.getDefaultMessage() == null ? "invalid" : e.getDefaultMessage(), (a,b) -> a));
        String summary = errors.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue()).collect(Collectors.joining("; "));
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, summary.isBlank() ? "Please correct the highlighted fields" : summary);
        detail.setType(URI.create("https://jnapanamsportszone.in/problems/validation"));
        detail.setProperty("errors", errors); return ResponseEntity.badRequest().body(detail);
    }
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> forbidden(AccessDeniedException ex){
        return problem(HttpStatus.FORBIDDEN,"You do not have permission to perform this action.");
    }
    @ExceptionHandler({HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class})
    ResponseEntity<ProblemDetail> malformed(Exception ex){
        return problem(HttpStatus.BAD_REQUEST,"Please check the information entered and try again.");
    }
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ProblemDetail> status(ResponseStatusException ex){
        HttpStatus status=HttpStatus.resolve(ex.getStatusCode().value());
        if(status==null)status=HttpStatus.INTERNAL_SERVER_ERROR;
        String reason=ex.getReason();
        String safe=reason!=null && reason.length()<=160
                && !reason.matches("(?is).*(?:exception|stacktrace|password|secret|token|sql|jdbc).*")
                ? reason : status.getReasonPhrase();
        return problem(status,safe);
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> unexpected(Exception ex){
        // Exception messages may contain credential/payment data: do not write them to logs.
        var context=RequestContextHolder.getRequestAttributes();
        String reference="unknown";
        if(context instanceof ServletRequestAttributes servlet){
            HttpServletRequest request=servlet.getRequest();
            Object id=request.getAttribute("jnapanam.correlationId");
            if(id instanceof String safeId)reference=safeId;
            request.setAttribute("jnapanam.errorCode",
                    "UNHANDLED_"+ex.getClass().getSimpleName().replaceAll("[^A-Za-z0-9]","").toUpperCase(java.util.Locale.ROOT));
        }
        LOG.error("Unhandled API exception class={} correlationId={}",ex.getClass().getSimpleName(),reference);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong. Please try again or contact support.");
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String message) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setType(URI.create("https://jnapanamsportszone.in/problems/" + status.value()));
        String code=status.value()>=500?"SERVER_ERROR":status==HttpStatus.UNAUTHORIZED?"UNAUTHORIZED":status==HttpStatus.FORBIDDEN?"FORBIDDEN":"REQUEST_ERROR";
        if(status==HttpStatus.UNAUTHORIZED && message.contains("MFA code sent"))code="MFA_REQUIRED";
        detail.setProperty("code",code);
        var attrs=RequestContextHolder.getRequestAttributes();
        if(attrs instanceof ServletRequestAttributes servlet){
            HttpServletRequest request=servlet.getRequest();
            if(request.getAttribute("jnapanam.errorCode")==null)request.setAttribute("jnapanam.errorCode",code);
            Object id=request.getAttribute("jnapanam.correlationId");
            if(id instanceof String reference)detail.setProperty("requestId",reference);
        }
        return ResponseEntity.status(status).body(detail);
    }
}
