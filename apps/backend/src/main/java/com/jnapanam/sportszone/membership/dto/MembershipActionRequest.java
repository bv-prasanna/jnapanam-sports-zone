package com.jnapanam.sportszone.membership.dto; import com.jnapanam.sportszone.membership.model.MembershipStatus; import jakarta.validation.constraints.*; import java.time.LocalDate;
public record MembershipActionRequest(@NotNull MembershipStatus status,LocalDate startDate,@Size(max=250) String reason){}
