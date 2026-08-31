package com.jnapanam.sportszone.catalog.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "facility_closures", uniqueConstraints = @UniqueConstraint(name = "uq_facility_closure_date", columnNames = {"facility_id", "closure_date"}))
public class FacilityClosure {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "facility_id") private Facility facility;
    @Column(name = "closure_date", nullable = false) private LocalDate closureDate;
    @Column(nullable = false, length = 250) private String reason;

    protected FacilityClosure() {}
    public FacilityClosure(Facility facility, LocalDate closureDate, String reason) {
        this.id = UUID.randomUUID(); this.facility = facility; this.closureDate = closureDate; this.reason = reason.trim();
    }
    public UUID getId() { return id; }
    public Facility getFacility() { return facility; }
    public LocalDate getClosureDate() { return closureDate; }
    public String getReason() { return reason; }
}
