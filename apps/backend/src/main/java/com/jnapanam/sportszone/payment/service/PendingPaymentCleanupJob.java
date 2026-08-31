package com.jnapanam.sportszone.payment.service;
import com.jnapanam.sportszone.booking.service.BookingService;import com.jnapanam.sportszone.membership.service.MembershipService;import com.jnapanam.sportszone.training.service.TrainingService;import com.jnapanam.sportszone.tournament.service.TournamentService;import org.springframework.scheduling.annotation.Scheduled;import org.springframework.stereotype.Component;
@Component public class PendingPaymentCleanupJob{
 private final BookingService bookings;private final MembershipService memberships;private final TrainingService training;private final TournamentService tournaments;
 public PendingPaymentCleanupJob(BookingService bookings,MembershipService memberships,TrainingService training,TournamentService tournaments){this.bookings=bookings;this.memberships=memberships;this.training=training;this.tournaments=tournaments;}
 @Scheduled(fixedDelayString="${app.payment.cleanup-delay-ms:60000}")public void cleanup(){bookings.cleanupUnpaid();memberships.cleanupUnpaid();training.cleanupUnpaid();tournaments.cleanupUnpaid();}
}
