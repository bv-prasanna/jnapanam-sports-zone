package com.jnapanam.sportszone.identity.dto;import jakarta.validation.constraints.*;public record OtpVerifyRequest(@NotBlank String mobile,@NotBlank@Pattern(regexp="^[0-9]{4,8}$")String otp){}
