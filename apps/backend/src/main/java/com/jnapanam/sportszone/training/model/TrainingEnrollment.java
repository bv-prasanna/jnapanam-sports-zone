package com.jnapanam.sportszone.training.model;
import com.jnapanam.sportszone.identity.model.User;import jakarta.persistence.*;import java.time.Instant;import java.util.UUID;
@Entity@Table(name="training_enrollments")public class TrainingEnrollment{
 @Id private UUID id;@ManyToOne(fetch=FetchType.LAZY,optional=false)@JoinColumn(name="batch_id")private TrainingBatch batch;@ManyToOne(fetch=FetchType.LAZY,optional=false)@JoinColumn(name="user_id")private User user;
 @Column(name="participant_name",nullable=false,length=120)private String participantName;@Column(name="participant_age",nullable=false)private int participantAge;@Enumerated(EnumType.STRING)@Column(nullable=false,length=20)private EnrollmentStatus status;
 @Column(name="enrolled_at",nullable=false)private Instant enrolledAt;@Column(name="cancelled_at")private Instant cancelledAt;@Column(name="payment_expires_at")private Instant paymentExpiresAt;@Column(name="payment_completed_at")private Instant paymentCompletedAt;
 protected TrainingEnrollment(){}
 public TrainingEnrollment(TrainingBatch batch,User user,String name,int age,boolean paymentRequired,Instant paymentExpiry){id=UUID.randomUUID();this.batch=batch;this.user=user;participantName=name.trim();participantAge=age;enrolledAt=Instant.now();status=paymentRequired?EnrollmentStatus.PENDING_PAYMENT:EnrollmentStatus.ENROLLED;paymentExpiresAt=paymentRequired?paymentExpiry:null;if(!paymentRequired)paymentCompletedAt=enrolledAt;}
 public void paid(){if(status==EnrollmentStatus.PENDING_PAYMENT){status=EnrollmentStatus.ENROLLED;paymentCompletedAt=Instant.now();paymentExpiresAt=null;}}
 public void expirePayment(){if(status==EnrollmentStatus.PENDING_PAYMENT)status=EnrollmentStatus.PAYMENT_EXPIRED;}
 public void cancel(){status=EnrollmentStatus.CANCELLED;cancelledAt=Instant.now();}
 public UUID getId(){return id;}public TrainingBatch getBatch(){return batch;}public User getUser(){return user;}public String getParticipantName(){return participantName;}public int getParticipantAge(){return participantAge;}public EnrollmentStatus getStatus(){return status;}public Instant getEnrolledAt(){return enrolledAt;}public Instant getCancelledAt(){return cancelledAt;}public Instant getPaymentExpiresAt(){return paymentExpiresAt;}public Instant getPaymentCompletedAt(){return paymentCompletedAt;}
}
