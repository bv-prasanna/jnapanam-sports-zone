package com.jnapanam.sportszone.features;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class FeatureVisibilityController {
    private final FeaturePolicyService policy;
    public FeatureVisibilityController(FeaturePolicyService policy) { this.policy = policy; }
    @GetMapping("/api/v1/public/features")
    public List<FeaturePolicyService.FeatureSetting> publicFeatures() { return policy.list(); }
    @GetMapping("/api/v1/admin/features")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public List<FeaturePolicyService.FeatureSetting> adminFeatures() { return policy.list(); }
    @PutMapping("/api/v1/admin/features/{code}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public FeaturePolicyService.FeatureSetting change(@PathVariable String code,
            @Valid @RequestBody FeaturePolicyService.ChangeRequest request) {
        return policy.change(code,request);
    }
}
