package com.jnapanam.sportszone.admin.dto;import jakarta.validation.constraints.Size;public record OperationalReasonRequest(@Size(max=300)String reason){}
