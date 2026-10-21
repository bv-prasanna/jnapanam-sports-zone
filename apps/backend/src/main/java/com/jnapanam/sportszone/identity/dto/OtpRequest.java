package com.jnapanam.sportszone.identity.dto;import jakarta.validation.constraints.*;public record OtpRequest(@NotBlank@Pattern(regexp="^[0-9+ -]{10,20}$")String mobile){}
