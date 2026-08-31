package com.jnapanam.sportszone.catalog.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "facility_schedule_overrides")
public class FacilityScheduleOverride {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "facility_id") private Facility facility;
    @Column(name = "schedule_date", nullable = false) private LocalDate scheduleDate;
    @Column(name = "start_time", nullable = false) private LocalTime startTime;
    @Column(name = "end_time", nullable = false) private LocalTime endTime;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Availability availability;
    @Column(name = "slot_duration_minutes") private Integer slotDurationMinutes;
    @Column private Integer capacity;
    @Column(length = 250) private String reason;

    protected FacilityScheduleOverride() {}
    public FacilityScheduleOverride(Facility facility, LocalDate date, LocalTime start, LocalTime end, Availability availability, Integer duration, Integer capacity, String reason) {
        this.id = UUID.randomUUID(); this.facility = facility; this.scheduleDate = date; this.startTime = start; this.endTime = end;
        this.availability = availability; this.slotDurationMinutes = duration; this.capacity = capacity;
        this.reason = reason == null || reason.isBlank() ? null : reason.trim();
    }
    public UUID getId() { return id; }
    public Facility getFacility() { return facility; }
    public LocalDate getScheduleDate() { return scheduleDate; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public Availability getAvailability() { return availability; }
    public Integer getSlotDurationMinutes() { return slotDurationMinutes; }
    public Integer getCapacity() { return capacity; }
    public String getReason() { return reason; }
}
