package com.jnapanam.sportszone.release1.dto;
import jakarta.validation.constraints.*;import java.time.LocalDate;
public record DependentRequest(@NotBlank @Size(max=160)String fullName,@NotNull @Past LocalDate dateOfBirth,@NotBlank @Size(max=60)String relationship,boolean guardianConsent,@Size(max=40)String waiverVersion){}
