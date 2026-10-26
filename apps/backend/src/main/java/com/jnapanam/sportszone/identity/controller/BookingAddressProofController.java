package com.jnapanam.sportszone.identity.controller;

import com.jnapanam.sportszone.common.exception.BadRequestException;
import java.util.*;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/profile/address-proof")
public class BookingAddressProofController {
 private final JdbcTemplate jdbc;
 public BookingAddressProofController(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public record Status(String documentType,String status,boolean uploaded){}
 @GetMapping
 public Status status(@org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt){
  UUID id=UUID.fromString(jwt.getSubject());
  var rows=jdbc.queryForList("select document_type,status from booking_address_proofs where user_id=? order by uploaded_at desc limit 1",id);
  return rows.isEmpty()?new Status(null,"NOT_SUBMITTED",false):new Status((String)rows.get(0).get("document_type"),(String)rows.get(0).get("status"),true);
 }
 @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 public Status upload(@org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt,
  @RequestParam("type") String type,@RequestPart("file") MultipartFile file) throws java.io.IOException {
  if(!Set.of("DL","VOTER_ID","PASSPORT").contains(type))throw new BadRequestException("Select DL, Voter ID or Passport");
  String mime=file.getContentType();
  if(mime==null||!Set.of("image/jpeg","image/png","application/pdf").contains(mime))throw new BadRequestException("Only JPG, PNG or PDF is accepted");
  if(file.isEmpty()||file.getSize()>2*1024*1024)throw new BadRequestException("Document must be between 1 byte and 2 MB");
  byte[] bytes=file.getBytes();
  boolean jpeg=bytes.length>3&&(bytes[0]&255)==255&&(bytes[1]&255)==216&&(bytes[2]&255)==255;
  boolean png=bytes.length>7&&(bytes[0]&255)==137&&bytes[1]==80&&bytes[2]==78&&bytes[3]==71;
  boolean pdf=bytes.length>4&&bytes[0]==37&&bytes[1]==80&&bytes[2]==68&&bytes[3]==70;
  if(!("image/jpeg".equals(mime)&&jpeg||"image/png".equals(mime)&&png||"application/pdf".equals(mime)&&pdf))throw new BadRequestException("File content does not match its type");
  UUID id=UUID.fromString(jwt.getSubject());
  jdbc.update("delete from booking_address_proofs where user_id=?",id);
  jdbc.update("insert into booking_address_proofs(id,user_id,document_type,content_type,original_filename,document_bytes,status) values(?,?,?,?,?,?,'PENDING')",
   UUID.randomUUID(),id,type,mime,"address-proof",bytes);
  return new Status(type,"PENDING",true);
 }
}
