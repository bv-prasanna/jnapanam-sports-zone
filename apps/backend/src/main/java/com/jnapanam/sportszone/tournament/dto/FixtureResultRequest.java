package com.jnapanam.sportszone.tournament.dto;import jakarta.validation.constraints.Min;public record FixtureResultRequest(@Min(0)int homeScore,@Min(0)int awayScore){}
