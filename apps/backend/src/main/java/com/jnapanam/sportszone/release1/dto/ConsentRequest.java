package com.jnapanam.sportszone.release1.dto;
import jakarta.validation.constraints.*;
public record ConsentRequest(@NotBlank @Size(max=40)String consentType,@NotBlank @Size(max=40)String documentVersion,boolean accepted){}
