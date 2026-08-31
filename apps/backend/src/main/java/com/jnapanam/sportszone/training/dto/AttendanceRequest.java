package com.jnapanam.sportszone.training.dto;import jakarta.validation.constraints.NotNull;import java.time.LocalDate;public record AttendanceRequest(@NotNull LocalDate sessionDate,boolean present){}
