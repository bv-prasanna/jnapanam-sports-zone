package com.jnapanam.sportszone.apprelease;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class AppReleaseController {
    private final AppReleaseService service;
    public AppReleaseController(AppReleaseService service) { this.service=service; }

    @GetMapping("/api/v1/public/app-release/android")
    public ResponseEntity<AppReleaseService.PublicPolicy> publicStatus() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.publicPolicy());
    }

    /** The stable website QR points to /app; this endpoint resolves the admin-managed APK URL. */
    @GetMapping("/api/v1/public/app-download/android")
    public ResponseEntity<Void> download() {
        return ResponseEntity.status(HttpStatus.FOUND).cacheControl(CacheControl.noStore())
            .location(URI.create(service.publicApkUrl())).build();
    }

    @GetMapping("/api/v1/admin/app-management")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public AppReleaseService.Policy current() { return service.admin(); }

    @PutMapping("/api/v1/admin/app-management")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public AppReleaseService.Policy update(Authentication auth,@RequestBody AppReleaseService.Change request) {
        return service.update(UUID.fromString(auth.getName()),request);
    }
}
