package com.jnapanam.sportszone.pricing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/billing")
public class CustomerBillingProfileController {
    private final JdbcTemplate jdbc;
    public CustomerBillingProfileController(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public record Details(
       @NotBlank @Size(max=400) String recipientAddress,
       @NotBlank @Pattern(regexp="^[0-9]{2}$") String stateCode,
       @Pattern(regexp="^[0-9]{2}[A-Z0-9]{13}$") String gstin){}
    @GetMapping("/me")
    public Map<String,Object> mine(Authentication auth){
        UUID user=UUID.fromString(auth.getName());
        var rows=jdbc.queryForList("""
            select recipient_address as "recipientAddress",state_code as "stateCode",gstin
            from customer_billing_profiles where user_id=?
            """,user);
        return rows.isEmpty()?Map.of():rows.get(0);
    }
    @PutMapping("/me")
    public Map<String,String> update(Authentication auth,@Valid @RequestBody Details d){
        jdbc.update("""
            insert into customer_billing_profiles(user_id,recipient_address,state_code,gstin)
            values(?,?,?,?)
            on conflict(user_id) do update set recipient_address=excluded.recipient_address,
            state_code=excluded.state_code,gstin=excluded.gstin,updated_at=now()
            """,UUID.fromString(auth.getName()),d.recipientAddress().trim(),d.stateCode(),
                d.gstin()==null||d.gstin().isBlank()?null:d.gstin().trim().toUpperCase(Locale.ROOT));
        return Map.of("status","SAVED");
    }
}
