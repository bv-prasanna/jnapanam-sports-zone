package com.jnapanam.sportszone.tournament.model;
import com.jnapanam.sportszone.identity.model.User;import jakarta.persistence.*;import java.time.Instant;import java.util.*;
@Entity@Table(name="tournament_teams")public class TournamentTeam{
 @Id private UUID id;@ManyToOne(fetch=FetchType.LAZY,optional=false)@JoinColumn(name="tournament_id")private Tournament tournament;@ManyToOne(fetch=FetchType.LAZY,optional=false)@JoinColumn(name="captain_user_id")private User captain;
 @Column(nullable=false,length=120)private String name;@Enumerated(EnumType.STRING)@Column(nullable=false,length=20)private TeamStatus status;@Column(name="registered_at",nullable=false)private Instant registeredAt;@Column(name="payment_expires_at")private Instant paymentExpiresAt;@Column(name="payment_completed_at")private Instant paymentCompletedAt;
 protected TournamentTeam(){}
 public TournamentTeam(Tournament tournament,User captain,String name,boolean paymentRequired,Instant paymentExpiry){id=UUID.randomUUID();this.tournament=tournament;this.captain=captain;this.name=name.trim();registeredAt=Instant.now();status=paymentRequired?TeamStatus.PENDING_PAYMENT:TeamStatus.REGISTERED;paymentExpiresAt=paymentRequired?paymentExpiry:null;if(!paymentRequired)paymentCompletedAt=registeredAt;}
 public void paid(){if(status==TeamStatus.PENDING_PAYMENT){status=TeamStatus.REGISTERED;paymentCompletedAt=Instant.now();paymentExpiresAt=null;}}
 public void expirePayment(){if(status==TeamStatus.PENDING_PAYMENT)status=TeamStatus.PAYMENT_EXPIRED;}
 public void withdraw(){status=TeamStatus.WITHDRAWN;}
 public UUID getId(){return id;}public Tournament getTournament(){return tournament;}public User getCaptain(){return captain;}public String getName(){return name;}public TeamStatus getStatus(){return status;}public Instant getRegisteredAt(){return registeredAt;}public Instant getPaymentExpiresAt(){return paymentExpiresAt;}public Instant getPaymentCompletedAt(){return paymentCompletedAt;}
}
