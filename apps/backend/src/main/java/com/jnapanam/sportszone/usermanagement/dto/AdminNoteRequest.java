package com.jnapanam.sportszone.usermanagement.dto;import jakarta.validation.constraints.*;public record AdminNoteRequest(@NotBlank@Size(max=1000)String note){}
