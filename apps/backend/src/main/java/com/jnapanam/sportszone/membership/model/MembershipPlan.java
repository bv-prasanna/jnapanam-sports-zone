package com.jnapanam.sportszone.membership.model;
import com.jnapanam.sportszone.catalog.model.Sport; import jakarta.persistence.*; import java.math.BigDecimal; import java.time.Instant; import java.util.*;
@Entity @Table(name="membership_plans") public class MembershipPlan{
 @Id private UUID id; @Column(nullable=false,unique=true,length=40) private String code; @Column(nullable=false,length=120) private String name; @Column(length=600) private String description;
 @Column(name="duration_days",nullable=false) private int durationDays; @Column(nullable=false,precision=12,scale=2) private BigDecimal price;
 @Column(name="daily_booking_limit") private Integer dailyBookingLimit; @Column(name="monthly_booking_limit") private Integer monthlyBookingLimit;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private MembershipPlanStatus status;
 @ManyToMany @JoinTable(name="membership_plan_sports",joinColumns=@JoinColumn(name="plan_id"),inverseJoinColumns=@JoinColumn(name="sport_id")) private Set<Sport> sports=new LinkedHashSet<>();
 @Column(name="created_at",nullable=false) private Instant createdAt; @Column(name="updated_at",nullable=false) private Instant updatedAt;
 protected MembershipPlan(){} public MembershipPlan(String code,String name,String description,int days,BigDecimal price,Integer daily,Integer monthly,Collection<Sport> sports){id=UUID.randomUUID();this.code=code.trim().toUpperCase(Locale.ROOT);update(name,description,days,price,daily,monthly,sports);status=MembershipPlanStatus.ACTIVE;createdAt=Instant.now();updatedAt=createdAt;}
 public void update(String name,String description,int days,BigDecimal price,Integer daily,Integer monthly,Collection<Sport> sports){this.name=name.trim();this.description=description==null||description.isBlank()?null:description.trim();durationDays=days;this.price=price;dailyBookingLimit=daily;monthlyBookingLimit=monthly;this.sports.clear();this.sports.addAll(sports);updatedAt=Instant.now();}
 public void status(MembershipPlanStatus value){status=value;updatedAt=Instant.now();}
 public UUID getId(){return id;} public String getCode(){return code;} public String getName(){return name;} public String getDescription(){return description;} public int getDurationDays(){return durationDays;} public BigDecimal getPrice(){return price;} public Integer getDailyBookingLimit(){return dailyBookingLimit;} public Integer getMonthlyBookingLimit(){return monthlyBookingLimit;} public MembershipPlanStatus getStatus(){return status;} public Set<Sport> getSports(){return sports;}
}
