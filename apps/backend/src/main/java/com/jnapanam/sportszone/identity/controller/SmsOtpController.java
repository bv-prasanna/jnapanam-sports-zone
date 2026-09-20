package com.jnapanam.sportszone.identity.controller;
import com.jnapanam.sportszone.identity.dto.*;import com.jnapanam.sportszone.identity.service.*;import jakarta.validation.Valid;import org.springframework.http.*;import org.springframework.web.bind.annotation.*;
@RestController@RequestMapping("/api/v1/auth/otp")public class SmsOtpController{private final SmsOtpService otp;private final AuthService auth;private final PasswordRecoveryService recovery;public SmsOtpController(SmsOtpService o,AuthService a,PasswordRecoveryService r){otp=o;auth=a;recovery=r;}
 @PostMapping("/registration/request")@ResponseStatus(HttpStatus.ACCEPTED)public void registration(@Valid@RequestBody RegisterRequest r){otp.registrationOtp(r);}
 @PostMapping("/registration/verify")public ResponseEntity<TokenResponse> verify(@Valid@RequestBody OtpVerifyRequest r){RegisterRequest payload=otp.verifyRegistration(r.mobile(),r.otp());return ResponseEntity.status(HttpStatus.CREATED).body(auth.register(payload));}
 @PostMapping("/password/request")@ResponseStatus(HttpStatus.ACCEPTED)public void password(@Valid@RequestBody OtpRequest r){recovery.requestSmsOtp(r.mobile());}
 @PostMapping("/password/reset")@ResponseStatus(HttpStatus.NO_CONTENT)public void reset(@Valid@RequestBody SmsPasswordResetRequest r){recovery.resetWithSmsOtp(r);}
}
