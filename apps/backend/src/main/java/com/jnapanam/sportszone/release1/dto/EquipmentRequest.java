package com.jnapanam.sportszone.release1.dto;
import jakarta.validation.constraints.*;import java.util.UUID;
public record EquipmentRequest(UUID bookingId,UUID facilityId,UUID customerUserId,@NotBlank @Size(max=160)String equipmentName,@Min(1)int quantity,@Size(max=300)String conditionOut){}
