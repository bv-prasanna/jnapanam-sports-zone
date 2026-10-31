package com.jnapanam.sportszone.content.controller;

import com.jnapanam.sportszone.common.exception.BadRequestException;
import com.jnapanam.sportszone.common.exception.NotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Upload small event images into durable PostgreSQL storage, not ephemeral container files. */
@RestController
public class EventMediaController {
    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private final JdbcTemplate jdbc;

    public EventMediaController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @PostMapping(value="/api/v1/admin/content/media/images", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public Map<String, String> upload(@RequestPart("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new BadRequestException("Choose an image to upload");
        if (file.getSize() > MAX_BYTES) throw new BadRequestException("Maximum image size is 2 MB");
        byte[] bytes = file.getBytes();
        // Validate actual file signatures rather than trusting multipart content-type or filename.
        String type = detectedType(bytes);
        if (type == null) throw new BadRequestException("Only JPG, PNG and WebP images are supported");
        UUID id = UUID.randomUUID();
        jdbc.update("insert into event_media_images(id,data,content_type,byte_size) values (?, ?, ?, ?)",
                id, bytes, type, bytes.length);
        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/public/content/media/images/").path(id.toString()).toUriString();
        return Map.of("id", id.toString(), "url", url);
    }

    @GetMapping("/api/v1/public/content/media/images/{id}")
    public ResponseEntity<byte[]> image(@PathVariable UUID id) {
        var entries = jdbc.query(
                "select data,content_type from event_media_images where id=?",
                (rs,index) -> Map.of("content",rs.getBytes("data"), "type",rs.getString("content_type")), id);
        if (entries.isEmpty()) throw new NotFoundException("Image not found");
        var value = entries.get(0);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(7)).cachePublic())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType((String)value.get("type")))
                .body((byte[])value.get("content"));
    }

    private static String detectedType(byte[] b) {
        if (b.length >= 8 && (b[0]&255)==137 && b[1]==80 && b[2]==78 && b[3]==71
                && b[4]==13 && b[5]==10 && b[6]==26 && b[7]==10) return "image/png";
        if (b.length >= 3 && (b[0]&255)==255 && (b[1]&255)==216 && (b[2]&255)==255)
            return "image/jpeg";
        if (b.length >= 12 && b[0]==82 && b[1]==73 && b[2]==70 && b[3]==70
                && b[8]==87 && b[9]==69 && b[10]==66 && b[11]==80) return "image/webp";
        return null;
    }
}
