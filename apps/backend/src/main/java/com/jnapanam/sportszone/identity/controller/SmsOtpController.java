package com.jnapanam.sportszone.identity.controller;
import com.jnapanam.sportszone.identity.dto.*;import com.jnapanam.sportszone.identity.service.*;import jakarta.validation.Valid;import jakarta.validation.constraints.*;import org.springframework.http.*;import org.springframework.web.bind.annotation.*;
@RestController@RequestMapping("/api/v1/auth/otp")public class SmsOtpController{private final SmsOtpService otp;private final AuthService auth;private final PasswordRecoveryService recovery;private final RegistrationEmailOtpService emailOtp;public SmsOtpController(SmsOtpService o,AuthService a,PasswordRecoveryService r,RegistrationEmailOtpService e){otp=o;auth=a;recovery=r;emailOtp=e;}
 @PostMapping("/registration/request")@ResponseStatus(HttpStatus.ACCEPTED)public void registration(@Valid@RequestBody OtpRequest r){otp.registrationOtp(r.mobile());}
 @PostMapping("/registration/verify")public ResponseEntity<TokenResponse> verify(@Valid@RequestBody RegistrationOtpVerifyRequest r){otp.verifyRegistration(r.mobile(),r.otp());RegisterRequest payload=new RegisterRequest(r.fullName(),r.email(),r.mobile(),r.password());return ResponseEntity.status(HttpStatus.CREATED).body(auth.register(payload,r.deviceSignature(),r.deviceName()));}
 @PostMapping("/registration/email/request")@ResponseStatus(HttpStatus.ACCEPTED)
 public void emailRequest(@Valid@RequestBody EmailOtpRequest r){emailOtp.request(r.email());}
 @PostMapping("/registration/email/verify")
 public ResponseEntity<TokenResponse> emailVerify(@Valid@RequestBody EmailRegistrationVerifyRequest r){
  emailOtp.verify(r.email(),r.otp());
  RegisterRequest payload=new RegisterRequest(r.fullName(),r.email(),r.mobile(),r.password());
  return ResponseEntity.status(HttpStatus.CREATED).body(auth.register(payload,r.deviceSignature(),r.deviceName()));
 }
 public record EmailOtpRequest(@Email@NotBlank String email){}
 public record EmailRegistrationVerifyRequest(@NotBlank String fullName,@Email@NotBlank String email,String mobile,@Size(min=8) String password,@Pattern(regexp="\\d{6}") String otp,String deviceSignature,String deviceName){}
 @PostMapping("/password/request")@ResponseStatus(HttpStatus.ACCEPTED)public void password(@Valid@RequestBody OtpRequest r){recovery.requestSmsOtp(r.mobile());}
 @PostMapping("/password/reset")@ResponseStatus(HttpStatus.NO_CONTENT)public void reset(@Valid@RequestBody SmsPasswordResetRequest r){recovery.resetWithSmsOtp(r);}
 public record RegistrationOtpVerifyRequest(@NotBlank String fullName,@Email@NotBlank String email,@NotBlank String mobile,@Size(min=8) String password,@Pattern(regexp="\\d{4,6}") String otp,String deviceSignature,String deviceName){}
}
