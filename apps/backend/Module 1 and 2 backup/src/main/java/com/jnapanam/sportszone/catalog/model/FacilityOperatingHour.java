package com.jnapanam.sportszone.catalog.model;

import jakarta.persistence.*;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "facility_operating_hours", uniqueConstraints = @UniqueConstraint(name = "uq_facility_day", columnNames = {"facility_id", "day_of_week"}))
public class FacilityOperatingHour {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "facility_id") private Facility facility;
    @Enumerated(EnumType.STRING) @Column(name = "day_of_week", nullable = false, length = 10) private DayOfWeek dayOfWeek;
    @Column(name = "open_time") private LocalTime openTime;
    @Column(name = "close_time") private LocalTime closeTime;
    @Column(nullable = false) private boolean closed;

    protected FacilityOperatingHour() {}

    public FacilityOperatingHour(Facility facility, DayOfWeek dayOfWeek, LocalTime openTime, LocalTime closeTime, boolean closed) {
        this.id = UUID.randomUUID();
        this.facility = facility;
        this.dayOfWeek = dayOfWeek;
        change(openTime, closeTime, closed);
    }

    public void change(LocalTime openTime, LocalTime closeTime, boolean closed) {
        this.closed = closed;
        this.openTime = closed ? null : openTime;
        this.closeTime = closed ? null : closeTime;
    }

    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public LocalTime getOpenTime() { return openTime; }
    public LocalTime getCloseTime() { return closeTime; }
    public boolean isClosed() { return closed; }
}

