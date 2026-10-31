package com.jnapanam.sportszone.admin.controller;

import com.jnapanam.sportszone.common.exception.BadRequestException;
import com.jnapanam.sportszone.common.exception.ConflictException;
import com.jnapanam.sportszone.common.exception.NotFoundException;
import java.sql.Timestamp;
import java.util.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/verification")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminVerificationController {
 private final JdbcTemplate jdbc;
 public AdminVerificationController(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public record DocumentRow(UUID id,UUID userId,String customer,String email,String type,String status,java.time.Instant uploadedAt,java.time.Instant reviewedAt,String reviewNote){}
 public record Decision(String status,String note){}
 @GetMapping("/summary")
 public Map<String,Long> summary(){
  Long pending=jdbc.queryForObject("select count(*) from booking_address_proofs where status='PENDING'",Long.class);
  return Map.of("pendingDocuments",pending==null?0L:pending);
 }
 @GetMapping("/documents")
 public List<DocumentRow> documents(@RequestParam(defaultValue="PENDING") String status){
  if(!Set.of("PENDING","APPROVED","REJECTED","ALL").contains(status))throw new BadRequestException("Invalid verification status");
  String sql="select p.id,p.user_id,u.full_name,u.email,p.document_type,p.status,p.uploaded_at,p.reviewed_at,p.review_note from booking_address_proofs p join users u on u.id=p.user_id"
   +("ALL".equals(status)?"":" where p.status=?")+" order by p.uploaded_at desc limit 200";
  org.springframework.jdbc.core.RowMapper<DocumentRow> mapper=(rs,n)->new DocumentRow((UUID)rs.getObject("id"),(UUID)rs.getObject("user_id"),rs.getString("full_name"),rs.getString("email"),rs.getString("document_type"),rs.getString("status"),rs.getTimestamp("uploaded_at").toInstant(),rs.getTimestamp("reviewed_at")==null?null:rs.getTimestamp("reviewed_at").toInstant(),rs.getString("review_note"));
  return "ALL".equals(status)?jdbc.query(sql,mapper):jdbc.query(sql,mapper,status);
 }
 @GetMapping("/documents/{id}/file")
 public ResponseEntity<byte[]> file(@PathVariable UUID id){
  var rows=jdbc.queryForList("select content_type,document_bytes from booking_address_proofs where id=?",id);
  if(rows.isEmpty())throw new NotFoundException("Document not found");
  var row=rows.get(0);
  String mime=(String)row.get("content_type");
  if(!Set.of("application/pdf","image/jpeg","image/png").contains(mime))throw new BadRequestException("Unsupported document content type");
  return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL,"no-store").header("X-Content-Type-Options","nosniff").header(HttpHeaders.CONTENT_DISPOSITION,"inline; filename=verification-document").contentType(MediaType.parseMediaType(mime)).body((byte[])row.get("document_bytes"));
 }
 @PatchMapping("/documents/{id}")
 public Map<String,String> decide(@PathVariable UUID id,@RequestBody Decision request,@AuthenticationPrincipal Jwt reviewer){
  if(request==null||!Set.of("APPROVED","REJECTED").contains(request.status()))throw new BadRequestException("Choose APPROVED or REJECTED");
  if("REJECTED".equals(request.status())&&(request.note()==null||request.note().trim().length()<3))throw new BadRequestException("Rejection reason is required");
  if(reviewer==null||reviewer.getSubject()==null)throw new org.springframework.security.access.AccessDeniedException("Reviewer authentication required");
  String note=request.note()==null?null:request.note().trim();
  if(note!=null&&note.length()>1000)throw new BadRequestException("Review reason must not exceed 1000 characters");
  int changed=jdbc.update("update booking_address_proofs set status=?, reviewed_at=now(), reviewed_by=?, review_note=? where id=? and status='PENDING'",request.status(),UUID.fromString(reviewer.getSubject()),note,id);
  if(changed==0)throw new ConflictException("Document not pending or no longer available");
  return Map.of("status",request.status());
 }
}
