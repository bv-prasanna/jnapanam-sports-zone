package com.jnapanam.sportszone.common.exception;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BadRequestException.class) ResponseEntity<ProblemDetail> badRequest(BadRequestException ex) { return problem(HttpStatus.BAD_REQUEST, ex.getMessage()); }
    @ExceptionHandler(ConflictException.class) ResponseEntity<ProblemDetail> conflict(ConflictException ex) { return problem(HttpStatus.CONFLICT, ex.getMessage()); }
    @ExceptionHandler(NotFoundException.class) ResponseEntity<ProblemDetail> notFound(NotFoundException ex) { return problem(HttpStatus.NOT_FOUND, ex.getMessage()); }
    @ExceptionHandler({UnauthorizedException.class, AuthenticationException.class}) ResponseEntity<ProblemDetail> unauthorized(RuntimeException ex) { return problem(HttpStatus.UNAUTHORIZED, "Invalid credentials or token"); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        detail.setType(URI.create("https://jnapanamsportszone.in/problems/validation"));
        Map<String,String> errors = ex.getBindingResult().getFieldErrors().stream().collect(Collectors.toMap(e -> e.getField(), e -> e.getDefaultMessage() == null ? "invalid" : e.getDefaultMessage(), (a,b) -> a));
        detail.setProperty("errors", errors); return ResponseEntity.badRequest().body(detail);
    }
    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String message) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setType(URI.create("https://jnapanamsportszone.in/problems/" + status.value()));
        return ResponseEntity.status(status).body(detail);
    }
}
