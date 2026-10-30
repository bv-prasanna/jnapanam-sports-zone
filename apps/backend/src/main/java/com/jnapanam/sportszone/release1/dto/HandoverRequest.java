package com.jnapanam.sportszone.release1.dto;
import jakarta.validation.constraints.*;import java.time.LocalDate;
public record HandoverRequest(@NotNull LocalDate shiftDate,@NotBlank @Size(max=4000)String notes){}
