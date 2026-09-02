package com.jnapanam.sportszone.loyalty.model;

import com.jnapanam.sportszone.common.exception.ConflictException;
import com.jnapanam.sportszone.identity.model.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "loyalty_wallets")
public class LoyaltyWallet {
    @Id
    @Column(name = "user_id")
    private java.util.UUID userId;
    
    @Column(name = "points_balance", nullable = false)
    private long pointsBalance;
    @Column(name = "lifetime_earned", nullable = false)
    private long lifetimeEarned;
    @Column(name = "lifetime_redeemed", nullable = false)
    private long lifetimeRedeemed;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private long version;

    protected LoyaltyWallet() {
    }
public LoyaltyWallet(User u) {
    userId = u.getId();
    updatedAt = Instant.now();
}

    public void earn(long p) {
        if (p > 0) {
            pointsBalance += p;
            lifetimeEarned += p;
            updatedAt = Instant.now();
        }
    }

    public void redeem(long p) {
        if (p < 0 || p > pointsBalance)
            throw new ConflictException("Insufficient loyalty points");
        pointsBalance -= p;
        lifetimeRedeemed += p;
        updatedAt = Instant.now();
    }

    public void restoreRedemption(long p) {
        if (p > 0) { pointsBalance += p; lifetimeRedeemed = Math.max(0, lifetimeRedeemed - p); updatedAt = Instant.now(); }
    }

    public void adjust(long p) {
        if (pointsBalance + p < 0)
            throw new ConflictException("Adjustment would make loyalty balance negative");
        pointsBalance += p;
        if (p > 0)
            lifetimeEarned += p;
        else
            lifetimeRedeemed += -p;
        updatedAt = Instant.now();
    }

    public long getPointsBalance() {
        return pointsBalance;
    }

    public long getLifetimeEarned() {
        return lifetimeEarned;
    }

    public long getLifetimeRedeemed() {
        return lifetimeRedeemed;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
