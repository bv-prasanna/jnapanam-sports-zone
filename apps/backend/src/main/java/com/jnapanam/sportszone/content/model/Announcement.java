package com.jnapanam.sportszone.content.model;
import jakarta.persistence.*;import java.time.Instant;import java.util.UUID;
@Entity @Table(name="announcements")
public class Announcement {
 @Id private UUID id; @Column(nullable=false,length=180) private String title; @Column(name="title_kn",length=180) private String titleKn;
 @Column(nullable=false,columnDefinition="TEXT") private String body; @Column(name="body_kn",columnDefinition="TEXT") private String bodyKn;
 @Column(nullable=false) private int priority; @Column(name="publish_from",nullable=false) private Instant publishFrom; @Column(name="publish_until") private Instant publishUntil;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private ContentStatus status; @Column(name="created_at",nullable=false) private Instant createdAt; @Column(name="updated_at",nullable=false) private Instant updatedAt;
 protected Announcement(){}
 public Announcement(String t,String tKn,String b,String bKn,int p,Instant from,Instant until){id=UUID.randomUUID();update(t,tKn,b,bKn,p,from,until);status=ContentStatus.DRAFT;createdAt=Instant.now();updatedAt=createdAt;}
 public void update(String t,String tKn,String b,String bKn,int p,Instant from,Instant until){title=t.trim();titleKn=clean(tKn);body=b.trim();bodyKn=clean(bKn);priority=p;publishFrom=from;publishUntil=until;updatedAt=Instant.now();}
 public void status(ContentStatus s){status=s;updatedAt=Instant.now();}
 public UUID getId(){return id;} public String getTitle(){return title;} public String getTitleKn(){return titleKn;} public String getBody(){return body;} public String getBodyKn(){return bodyKn;} public int getPriority(){return priority;} public Instant getPublishFrom(){return publishFrom;} public Instant getPublishUntil(){return publishUntil;} public ContentStatus getStatus(){return status;}
 private static String clean(String v){return v==null||v.isBlank()?null:v.trim();}
}
