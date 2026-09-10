package com.jnapanam.sportszone.identity.controller;

import com.jnapanam.sportszone.common.exception.BadRequestException;
import com.jnapanam.sportszone.common.exception.NotFoundException;
import com.jnapanam.sportszone.identity.dto.UserResponse;
import com.jnapanam.sportszone.identity.repository.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
public class ProfilePhotoController {
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS = Map.of(
            MediaType.IMAGE_JPEG_VALUE, ".jpg",
            MediaType.IMAGE_PNG_VALUE, ".png",
            "image/webp", ".webp");

    private final UserRepository users;
    private final Path directory;

    public ProfilePhotoController(UserRepository users,
            @Value("${app.upload.profile-dir:uploads/profile}") String directory) {
        this.users = users;
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    @PostMapping(value = "/api/v1/profile/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserResponse upload(@org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt,
            @RequestPart("file") MultipartFile file) throws IOException {
        UUID userId = UUID.fromString(jwt.getSubject());
        var user = users.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        if (file.isEmpty()) throw new BadRequestException("Choose a profile photo");
        if (file.getSize() > MAX_BYTES) throw new BadRequestException("Profile photo must be 5 MB or smaller");
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        String extension = EXTENSIONS.get(contentType);
        if (extension == null) throw new BadRequestException("Profile photo must be JPG, PNG or WebP");

        Files.createDirectories(directory);
        for (String ext : Set.of(".jpg", ".png", ".webp")) Files.deleteIfExists(directory.resolve(userId + ext));
        Path destination = directory.resolve(userId + extension).normalize();
        if (!destination.startsWith(directory)) throw new BadRequestException("Invalid profile photo path");
        try (var input = file.getInputStream()) {
            Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
        }
        String publicUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/public/profile-photos/").path(destination.getFileName().toString())
                .queryParam("v", System.currentTimeMillis()).toUriString();
        user.profile(user.getDisplayName(), publicUrl);
        return UserResponse.from(users.save(user));
    }

    @GetMapping("/api/v1/public/profile-photos/{filename:.+}")
    public ResponseEntity<Resource> photo(@PathVariable String filename) {
        if (!filename.matches("[0-9a-fA-F-]{36}\\.(jpg|png|webp)")) throw new NotFoundException("Photo not found");
        Path file = directory.resolve(filename).normalize();
        if (!file.startsWith(directory) || !Files.isRegularFile(file)) throw new NotFoundException("Photo not found");
        MediaType type = filename.endsWith(".png") ? MediaType.IMAGE_PNG
                : filename.endsWith(".webp") ? MediaType.parseMediaType("image/webp") : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().cacheControl(CacheControl.noCache()).contentType(type).body(new FileSystemResource(file));
    }
}
