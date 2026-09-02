package com.jnapanam.sportszone.content.model;
import com.jnapanam.sportszone.catalog.model.Sport;import jakarta.persistence.*;import java.time.Instant;import java.util.UUID;
@Entity @Table(name="gallery_items")
public class GalleryItem {
 @Id private UUID id; @Column(nullable=false,length=180) private String title; @Column(name="title_kn",length=180) private String titleKn; @Enumerated(EnumType.STRING) @Column(name="media_type",nullable=false,length=20) private MediaType mediaType;
 @Column(name="media_url",nullable=false,length=500) private String mediaUrl; @Column(name="thumbnail_url",length=500) private String thumbnailUrl; @Column(name="alt_text",length=250) private String altText; @Column(name="alt_text_kn",length=250) private String altTextKn;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="sport_id") private Sport sport; @Column(nullable=false) private boolean published; @Column(name="display_order",nullable=false) private int displayOrder; @Column(name="created_at",nullable=false) private Instant createdAt;
 protected GalleryItem(){}
 public GalleryItem(String title,String titleKn,MediaType type,String url,String thumbnail,String alt,String altKn,Sport sport,boolean published,int order){id=UUID.randomUUID();this.title=title.trim();this.titleKn=clean(titleKn);mediaType=type;mediaUrl=url.trim();thumbnailUrl=clean(thumbnail);altText=clean(alt);altTextKn=clean(altKn);this.sport=sport;this.published=published;displayOrder=order;createdAt=Instant.now();}
 public UUID getId(){return id;} public String getTitle(){return title;} public String getTitleKn(){return titleKn;} public MediaType getMediaType(){return mediaType;} public String getMediaUrl(){return mediaUrl;} public String getThumbnailUrl(){return thumbnailUrl;} public String getAltText(){return altText;} public String getAltTextKn(){return altTextKn;} public Sport getSport(){return sport;} public boolean isPublished(){return published;} public int getDisplayOrder(){return displayOrder;}
 private static String clean(String v){return v==null||v.isBlank()?null:v.trim();}
}
