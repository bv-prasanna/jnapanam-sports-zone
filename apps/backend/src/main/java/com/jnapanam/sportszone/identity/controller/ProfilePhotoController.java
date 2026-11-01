package com.jnapanam.sportszone.identity.controller;

import com.jnapanam.sportszone.common.exception.BadRequestException;
import com.jnapanam.sportszone.common.exception.NotFoundException;
import com.jnapanam.sportszone.identity.dto.UserResponse;
import com.jnapanam.sportszone.identity.repository.UserRepository;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Profile photos are stored in PostgreSQL, not the ephemeral Northflank/container filesystem.
 * The pre-existing profilePhotoUrl format is preserved for web/mobile compatibility.
 */
@RestController
public class ProfilePhotoController {
    static final long MAX_BYTES = 5L * 1024 * 1024;
    static final Map<String,String> EXTENSIONS = Map.of(
        MediaType.IMAGE_JPEG_VALUE, ".jpg",
        MediaType.IMAGE_PNG_VALUE, ".png",
        "image/webp", ".webp");

    private final UserRepository users;
    private final JdbcTemplate jdbc;

    public ProfilePhotoController(UserRepository users, JdbcTemplate jdbc) {
        this.users=users;
        this.jdbc=jdbc;
    }

    @PostMapping(value="/api/v1/profile/photo", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public UserResponse upload(@AuthenticationPrincipal Jwt jwt,
                               @RequestPart("file") MultipartFile file) throws IOException {
        UUID userId=UUID.fromString(jwt.getSubject());
        var user=users.findById(userId).orElseThrow(()->new NotFoundException("User not found"));
        if(file.isEmpty())throw new BadRequestException("Choose a profile photo");
        if(file.getSize()>MAX_BYTES)throw new BadRequestException("Profile photo must be 5 MB or smaller");

        String contentType=file.getContentType()==null?"":file.getContentType().toLowerCase(Locale.ROOT);
        String extension=EXTENSIONS.get(contentType);
        if(extension==null)throw new BadRequestException("Profile photo must be JPG, PNG or WebP");
        byte[] bytes=file.getBytes();
        if(bytes.length==0||bytes.length>MAX_BYTES)throw new BadRequestException("Profile photo must be 5 MB or smaller");
        if(!matchesFileSignature(bytes,contentType))throw new BadRequestException("The selected file is not a valid JPG, PNG or WebP image");

        jdbc.update("""
            insert into user_profile_photos(user_id,media_type,photo_bytes,updated_at)
            values(?,?,?,now())
            on conflict(user_id) do update set media_type=excluded.media_type,
              photo_bytes=excluded.photo_bytes,updated_at=now()
            """,userId,contentType,bytes);

        String photoUrl=ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/api/v1/public/profile-photos/")
            .path(userId.toString()+extension)
            .queryParam("v",System.currentTimeMillis()).toUriString();
        user.profile(user.getDisplayName(),photoUrl);
        return UserResponse.from(users.save(user));
    }

    static boolean matchesFileSignature(byte[] b,String type) {
        if(MediaType.IMAGE_JPEG_VALUE.equals(type))
            return b.length>=3&&(b[0]&255)==0xff&&(b[1]&255)==0xd8&&(b[2]&255)==0xff;
        if(MediaType.IMAGE_PNG_VALUE.equals(type))
            return b.length>=8&&Arrays.equals(Arrays.copyOf(b,8),
                new byte[]{(byte)137,80,78,71,13,10,26,10});
        if("image/webp".equals(type))
            return b.length>=12&&b[0]=='R'&&b[1]=='I'&&b[2]=='F'&&b[3]=='F'
                &&b[8]=='W'&&b[9]=='E'&&b[10]=='B'&&b[11]=='P';
        return false;
    }

    @GetMapping("/api/v1/public/profile-photos/{filename:.+}")
    public ResponseEntity<byte[]> photo(@PathVariable String filename) {
        if(!filename.matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)"))
            throw new NotFoundException("Photo not found");
        UUID userId=UUID.fromString(filename.substring(0,36));
        String extension=filename.substring(36).toLowerCase(Locale.ROOT);
        List<StoredPhoto> photos=jdbc.query("""
            select media_type,photo_bytes from user_profile_photos where user_id=?
            """,(rs,row)->new StoredPhoto(rs.getString("media_type"),rs.getBytes("photo_bytes")),userId);
        if(photos.isEmpty())throw new NotFoundException("Photo not found");
        StoredPhoto photo=photos.get(0);
        if(!extension.equals(EXTENSIONS.get(photo.mediaType())))throw new NotFoundException("Photo not found");
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noCache())
            .contentType(MediaType.parseMediaType(photo.mediaType()))
            .contentLength(photo.data().length)
            .body(photo.data());
    }

    private record StoredPhoto(String mediaType,byte[] data) {}
}
