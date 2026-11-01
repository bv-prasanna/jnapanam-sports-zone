package com.jnapanam.sportszone.pricing;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** Issues only when the merchant approved tax treatment, service timing and place-of-supply. */
@Service
public class VenueBillingAutoIssueJob {
    private final JdbcTemplate jdbc;
    private final VenueBillingService service;
    public VenueBillingAutoIssueJob(JdbcTemplate jdbc,VenueBillingService service){
        this.jdbc=jdbc;this.service=service;
    }
    @Scheduled(fixedDelayString="${app.billing.auto-issue-period-ms:300000}",initialDelay=90000)
    public void run(){
        // E-invoice-applicable suppliers cannot use this flow until an IRP integration is configured.
        List<Map<String,Object>> candidates=jdbc.queryForList("""
            select p.id,c.recipient_address,c.state_code,c.gstin,policy.place_of_supply_state_code,
                   seller.state_code as seller_state
            from payment_orders p
            join venue_billing_purpose_policies policy on policy.purpose=p.purpose
            join venue_billing_profile seller on seller.id=1
            join customer_billing_profiles c on c.user_id=p.user_id
            where p.status in ('PAID','PARTIALLY_REFUNDED') and p.billing_gross_paise is not null
            and policy.active=true and policy.auto_issue_enabled=true
            and policy.place_of_supply_state_code is not null
            and seller.gstin is not null and coalesce(seller.e_invoice_applicable,false)=false
            and not exists(select 1 from venue_billing_documents d where d.payment_order_id=p.id and d.kind='INVOICE')
            order by p.paid_at asc limit 20
            """);
        for(Map<String,Object> row:candidates){
            try{
                String place=String.valueOf(row.get("place_of_supply_state_code"));
                String mode=place.equals(row.get("seller_state"))?"CGST_SGST":"IGST";
                service.issueInvoice((UUID)row.get("id"),new VenueBillingService.InvoiceDetails(
                    (String)row.get("gstin"),(String)row.get("state_code"),
                    (String)row.get("recipient_address"),mode,place));
            }catch(RuntimeException ex){
                // Leave for admin review; never forge invoice fields or retry blindly with altered values.
                // No personally identifiable information or GST numbers are logged.
            }
        }
    }
}
