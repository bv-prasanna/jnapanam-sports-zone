package com.jnapanam.sportszone.features;

import com.jnapanam.sportszone.common.exception.BadRequestException;
import com.jnapanam.sportszone.common.exception.ConflictException;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeaturePolicyService {
    public record FeatureSetting(String code, boolean enabled, List<String> allowedRoles) {}
    public record ChangeRequest(boolean enabled, List<String> allowedRoles) {}
    private static final Set<String> VALID_ROLES = Set.of(
            "CUSTOMER", "ADMIN", "SUPER_ADMIN", "OPERATIONS", "SHOP_KEEPER", "COACH", "ORGANIZER");
    private final JdbcTemplate jdbc;
    public FeaturePolicyService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional(readOnly = true)
    public List<FeatureSetting> list() {
        return jdbc.query("SELECT code,enabled,allowed_roles FROM feature_visibility ORDER BY code",
                (rs,n) -> new FeatureSetting(rs.getString("code"), rs.getBoolean("enabled"),
                        rs.getString("allowed_roles").isBlank() ? List.of() : List.of(rs.getString("allowed_roles").split(","))));
    }
    @Transactional(readOnly = true)
    public boolean visibleFor(String feature, String role) {
        return list().stream().anyMatch(f -> f.code().equals(feature)
                && f.enabled() && f.allowedRoles().contains(role));
    }
    @Transactional
    public FeatureSetting change(String code, ChangeRequest change) {
        if (change == null || change.allowedRoles() == null || (change.enabled() && change.allowedRoles().isEmpty())
                || change.allowedRoles().stream().anyMatch(role -> !VALID_ROLES.contains(role)))
            throw new BadRequestException("Select at least one valid role");
        if (!list().stream().anyMatch(f -> f.code().equals(code)))
            throw new BadRequestException("Unknown feature");
        String roles = String.join(",", new TreeSet<>(change.allowedRoles()));
        jdbc.update("UPDATE feature_visibility SET enabled=?, allowed_roles=?, updated_at=NOW() WHERE code=?",
                change.enabled(), roles, code);
        return new FeatureSetting(code, change.enabled(), roles.isBlank() ? List.of() : List.of(roles.split(",")));
    }
    @Transactional(readOnly = true)
    public void requireCustomerFeature(String feature) {
        if (!visibleFor(feature,"CUSTOMER"))
            throw new ConflictException("This feature is not currently available for customer enrollment");
    }
}
