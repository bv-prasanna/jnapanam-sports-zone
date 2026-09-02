package com.jnapanam.sportszone.content.model;
import jakarta.persistence.*;import java.time.Instant;import java.util.*;
@Entity @Table(name="content_pages")
public class ContentPage {
 @Id private UUID id; @Column(nullable=false,unique=true,length=80) private String slug; @Column(nullable=false,length=180) private String title; @Column(name="title_kn",length=180) private String titleKn;
 @Column(length=500) private String summary; @Column(name="summary_kn",length=500) private String summaryKn; @Column(nullable=false,columnDefinition="TEXT") private String body; @Column(name="body_kn",columnDefinition="TEXT") private String bodyKn;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private ContentStatus status; @Column(name="updated_at",nullable=false) private Instant updatedAt;
 protected ContentPage(){}
 public ContentPage(String slug,String title,String titleKn,String summary,String summaryKn,String body,String bodyKn){id=UUID.randomUUID();this.slug=slug.trim().toLowerCase(Locale.ROOT);update(title,titleKn,summary,summaryKn,body,bodyKn,ContentStatus.DRAFT);}
 public void update(String title,String titleKn,String summary,String summaryKn,String body,String bodyKn,ContentStatus status){this.title=title.trim();this.titleKn=clean(titleKn);this.summary=clean(summary);this.summaryKn=clean(summaryKn);this.body=body.trim();this.bodyKn=clean(bodyKn);this.status=status;updatedAt=Instant.now();}
 public UUID getId(){return id;} public String getSlug(){return slug;} public String getTitle(){return title;} public String getTitleKn(){return titleKn;} public String getSummary(){return summary;} public String getSummaryKn(){return summaryKn;} public String getBody(){return body;} public String getBodyKn(){return bodyKn;} public ContentStatus getStatus(){return status;} public Instant getUpdatedAt(){return updatedAt;}
 private static String clean(String v){return v==null||v.isBlank()?null:v.trim();}
}
