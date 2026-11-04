package com.jnapanam.sportszone.catalog.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "facilities")
public class Facility {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "sport_id") private Sport sport;
    @Column(nullable = false, unique = true, length = 50) private String code;
    @Column(nullable = false, length = 120) private String name;
    @Column(length = 500) private String description;
    @Column(name = "location_label", length = 160) private String locationLabel;
    @Column(name = "slot_duration_minutes", nullable = false) private int slotDurationMinutes;
    @Column(nullable = false) private int capacity;
    @Column(name = "buffer_minutes", nullable = false) private int bufferMinutes;
    @Column(name = "terms_and_conditions", length = 3000) private String termsAndConditions;
    @Column(name = "terms_and_conditions_kn", length = 3000) private String termsAndConditionsKn;
    @Column(name = "booking_price", nullable = false, precision = 12, scale = 2) private BigDecimal bookingPrice;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private CatalogueStatus status;
    @Column(name = "suspended_from") private LocalDate suspendedFrom;
    @Column(name = "suspended_until") private LocalDate suspendedUntil;
    @Column(name = "status_reason", length = 250) private String statusReason;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected Facility() {}

    public Facility(Sport sport, String code, String name, String description, String locationLabel, int slotDurationMinutes, int capacity) {
        this(sport, code, name, description, locationLabel, slotDurationMinutes, capacity, BigDecimal.ZERO);
    }

    public Facility(Sport sport, String code, String name, String description, String locationLabel, int slotDurationMinutes, int capacity, BigDecimal bookingPrice) {
        this.id = UUID.randomUUID();
        this.sport = sport;
        this.code = code.trim().toUpperCase(java.util.Locale.ROOT);
        this.name = name.trim();
        this.description = trimToNull(description);
        this.locationLabel = trimToNull(locationLabel);
        this.slotDurationMinutes = slotDurationMinutes;
        this.capacity = capacity;
        this.bookingPrice = bookingPrice == null ? BigDecimal.ZERO : bookingPrice;
        this.status = CatalogueStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void update(Sport sport, String name, String description, String locationLabel, int slotDurationMinutes, int capacity, BigDecimal bookingPrice) {
        this.sport = sport;
        this.name = name.trim();
        this.description = trimToNull(description);
        this.locationLabel = trimToNull(locationLabel);
        this.slotDurationMinutes = slotDurationMinutes;
        this.capacity = capacity;
        this.bookingPrice = bookingPrice == null ? BigDecimal.ZERO : bookingPrice;
        this.updatedAt = Instant.now();
    }

    public void configureBookingRules(int bufferMinutes, String termsAndConditions) {
        configureBookingRules(bufferMinutes,termsAndConditions,null);
    }
    public void configureBookingRules(int bufferMinutes, String termsAndConditions, String termsAndConditionsKn) {
        if (bufferMinutes < 0 || bufferMinutes > 120) throw new IllegalArgumentException("Invalid buffer minutes");
        this.bufferMinutes = bufferMinutes;
        this.termsAndConditions = trimToNull(termsAndConditions) == null
            ? "Gutka, cigarettes, smoking and tobacco are not permitted. Follow the safety and hygiene instructions of the facility operator."
            : trimToNull(termsAndConditions);
        this.termsAndConditionsKn=trimToNull(termsAndConditionsKn)==null
          ? (this.termsAndConditions.equals("Gutka, cigarettes, smoking and tobacco are not permitted. Follow the safety and hygiene instructions of the facility operator.")
              ? "ಗುಟ್ಕಾ, ಸಿಗರೇಟ್, ಧೂಮಪಾನ ಮತ್ತು ತಂಬಾಕು ಬಳಕೆಯನ್ನು ನಿಷೇಧಿಸಲಾಗಿದೆ. ಸೌಲಭ್ಯದ ನಿರ್ವಾಹಕರ ಸುರಕ್ಷತೆ ಮತ್ತು ಸ್ವಚ್ಛತಾ ಸೂಚನೆಗಳನ್ನು ಪಾಲಿಸಿ." : null)
          : trimToNull(termsAndConditionsKn);
        this.updatedAt = Instant.now();
    }

    public void changeStatus(CatalogueStatus status, LocalDate from, LocalDate until, String reason) {
        this.status = status;
        this.suspendedFrom = status == CatalogueStatus.TEMPORARILY_SUSPENDED ? from : null;
        this.suspendedUntil = status == CatalogueStatus.TEMPORARILY_SUSPENDED ? until : null;
        this.statusReason = status == CatalogueStatus.ACTIVE ? null : trimToNull(reason);
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Sport getSport() { return sport; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getLocationLabel() { return locationLabel; }
    public int getSlotDurationMinutes() { return slotDurationMinutes; }
    public int getCapacity() { return capacity; }
    public int getBufferMinutes() { return bufferMinutes; }
    public String getTermsAndConditions() { return termsAndConditions; }
    public String getTermsAndConditionsKn() { return termsAndConditionsKn; }
    public BigDecimal getBookingPrice() { return bookingPrice; }
    public CatalogueStatus getStatus() { return status; }
    public LocalDate getSuspendedFrom() { return suspendedFrom; }
    public LocalDate getSuspendedUntil() { return suspendedUntil; }
    public String getStatusReason() { return statusReason; }
    public boolean isAvailableOn(LocalDate date) {
        if (!sport.isAvailableOn(date)) return false;
        if (status == CatalogueStatus.ACTIVE) return true;
        if (status == CatalogueStatus.INACTIVE) return false;
        return date.isBefore(suspendedFrom) || date.isAfter(suspendedUntil);
    }

    private static String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
