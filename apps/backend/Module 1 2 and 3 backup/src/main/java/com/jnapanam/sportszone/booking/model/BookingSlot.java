package com.jnapanam.sportszone.booking.model;

import com.jnapanam.sportszone.catalog.model.Facility;
import com.jnapanam.sportszone.common.exception.ConflictException;
import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity @Table(name="booking_slots", uniqueConstraints=@UniqueConstraint(name="uq_booking_slot", columnNames={"facility_id","slot_date","start_time"}))
public class BookingSlot {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="facility_id") private Facility facility;
    @Column(name="slot_date", nullable=false) private LocalDate date;
    @Column(name="start_time", nullable=false) private LocalTime startTime;
    @Column(name="end_time", nullable=false) private LocalTime endTime;
    @Column(nullable=false) private int capacity;
    @Column(name="held_count", nullable=false) private int heldCount;
    @Column(name="booked_count", nullable=false) private int bookedCount;
    @Version private long version;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    protected BookingSlot() {}
    public BookingSlot(Facility facility, LocalDate date, LocalTime start, LocalTime end, int capacity) {
        this.id=UUID.randomUUID(); this.facility=facility; this.date=date; this.startTime=start; this.endTime=end;
        this.capacity=capacity; this.createdAt=Instant.now();
    }
    public void hold(int quantity) { if (available()<quantity) throw new ConflictException("Requested capacity is no longer available"); heldCount+=quantity; }
    public void releaseHold(int quantity) { heldCount=Math.max(0, heldCount-quantity); }
    public void confirm(int quantity) { releaseHold(quantity); bookedCount+=quantity; }
    public void cancel(int quantity) { bookedCount=Math.max(0, bookedCount-quantity); }
    public int available() { return capacity-heldCount-bookedCount; }
    public UUID getId(){return id;} public Facility getFacility(){return facility;} public LocalDate getDate(){return date;}
    public LocalTime getStartTime(){return startTime;} public LocalTime getEndTime(){return endTime;} public int getCapacity(){return capacity;}
    public int getHeldCount(){return heldCount;} public int getBookedCount(){return bookedCount;}
}
