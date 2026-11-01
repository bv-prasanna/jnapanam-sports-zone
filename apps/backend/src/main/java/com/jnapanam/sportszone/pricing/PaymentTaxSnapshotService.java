package com.jnapanam.sportszone.pricing;

import com.jnapanam.sportszone.booking.model.Booking;
import com.jnapanam.sportszone.booking.repository.BookingRepository;
import com.jnapanam.sportszone.common.exception.*;
import com.jnapanam.sportszone.payment.model.*;
import java.math.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Snapshot the billing treatment when a checkout order is created, never at invoice render time.
 * The initial advertised fee is tax-inclusive for configured non-booking purposes.
 * Loyalty is either a tender or an invoice discount, as expressly configured by the merchant.
 */
@Service
public class PaymentTaxSnapshotService {
    private final JdbcTemplate jdbc;
    private final BookingRepository bookings;
    public PaymentTaxSnapshotService(JdbcTemplate jdbc,BookingRepository bookings){
        this.jdbc=jdbc;this.bookings=bookings;
    }
    public record Policy(String purpose,BigDecimal taxRatePercent,boolean active){}
    public record Treatments(String couponTaxTreatment,String loyaltyTaxTreatment,boolean eInvoiceApplicable,boolean invoiceEmailEnabled){}

    @Transactional(readOnly=true)
    public List<Map<String,Object>> policies(){
        return jdbc.queryForList("select purpose,tax_rate_percent as \"taxRatePercent\",active,place_of_supply_state_code as \"placeOfSupplyStateCode\",auto_issue_enabled as \"autoIssueEnabled\" from venue_billing_purpose_policies order by purpose");
    }
    @Transactional
    public void configure(String purpose,BigDecimal rate,boolean active,String placeOfSupplyStateCode,boolean autoIssueEnabled){
        if(!Set.of("BOOKING","MEMBERSHIP","TRAINING","TOURNAMENT").contains(purpose))
            throw new BadRequestException("Invalid billing purpose");
        if(rate==null||rate.signum()<0||rate.compareTo(new BigDecimal("100"))>0)
            throw new BadRequestException("Invalid tax rate");
        if(placeOfSupplyStateCode!=null&&!placeOfSupplyStateCode.matches("[0-9]{2}"))
            throw new BadRequestException("Place-of-supply state code must have two digits");
        if(autoIssueEnabled&&(!active||placeOfSupplyStateCode==null))
            throw new BadRequestException("Activate the policy and configure place of supply before enabling auto-issue");
        jdbc.update("""
          insert into venue_billing_purpose_policies(purpose,tax_rate_percent,active,place_of_supply_state_code,auto_issue_enabled)
          values(?,?,?,?,?)
          on conflict(purpose) do update set tax_rate_percent=excluded.tax_rate_percent,
          active=excluded.active,place_of_supply_state_code=excluded.place_of_supply_state_code,
          auto_issue_enabled=excluded.auto_issue_enabled,updated_at=now()
          """,purpose,rate,active,placeOfSupplyStateCode,autoIssueEnabled);
    }
    @Transactional
    public void configureTreatments(Treatments t){
        if(t==null||!Set.of("REVIEW_REQUIRED","INVOICE_DISCOUNT").contains(t.couponTaxTreatment())
           ||!Set.of("REVIEW_REQUIRED","PAYMENT_TENDER","INVOICE_DISCOUNT").contains(t.loyaltyTaxTreatment()))
           throw new BadRequestException("Select approved coupon and loyalty tax treatments");
        int count=jdbc.update("""
          update venue_billing_profile set coupon_tax_treatment=?,loyalty_tax_treatment=?,
          e_invoice_applicable=?,invoice_email_enabled=?,updated_at=now() where id=1
          """,t.couponTaxTreatment(),t.loyaltyTaxTreatment(),t.eInvoiceApplicable(),t.invoiceEmailEnabled());
        if(count==0)throw new ConflictException("Configure the supplier GST profile first");
    }
    @Transactional(readOnly=true)
    public Map<String,Object> treatmentSettings(){
        List<Map<String,Object>> rows=jdbc.queryForList("""
         select coupon_tax_treatment as "couponTaxTreatment",loyalty_tax_treatment as "loyaltyTaxTreatment",
         e_invoice_applicable as "eInvoiceApplicable",invoice_email_enabled as "invoiceEmailEnabled"
         from venue_billing_profile where id=1
         """);
        return rows.isEmpty()?Map.of():rows.get(0);
    }
    @Transactional
    public void snapshot(PaymentOrder order){
        BigDecimal rate=rate(order);
        if(rate==null)return; // Policy not activated; never invent a GST rate.
        var settings=treatmentSettings();
        String coupon=String.valueOf(settings.getOrDefault("couponTaxTreatment","REVIEW_REQUIRED"));
        String loyalty=String.valueOf(settings.getOrDefault("loyaltyTaxTreatment","REVIEW_REQUIRED"));
        long couponPaise=order.getDiscountPaise();
        long inferredLoyalty=order.getOriginalAmountPaise()-couponPaise-order.getAmountPaise();
        if(inferredLoyalty<0)throw new ConflictException("Payment amount exceeds quoted amount");
        if((couponPaise>0&&!coupon.equals("INVOICE_DISCOUNT"))||
           (order.getLoyaltyPointsUsed()>0&&loyalty.equals("REVIEW_REQUIRED")))return; // Payment may proceed, invoice requires review.
        // ₹0.01 payment floor in legacy checkout must not silently change the invoice.
        if(order.getLoyaltyPointsUsed()>0&&inferredLoyalty==0)return;
        long loyaltyDiscount=loyalty.equals("INVOICE_DISCOUNT")?inferredLoyalty:0;
        long tender=loyalty.equals("PAYMENT_TENDER")?inferredLoyalty:0;
        long gross=order.getOriginalAmountPaise()-couponPaise-loyaltyDiscount;
        if(gross!=order.getAmountPaise()+tender)throw new ConflictException("Payment and invoice amounts do not reconcile");
        if(gross<=0)throw new ConflictException("A zero-value invoice requires a separate billing policy");
        long net=BigDecimal.valueOf(gross).multiply(BigDecimal.valueOf(100))
                .divide(new BigDecimal("100").add(rate),0,RoundingMode.HALF_UP).longValueExact();
        order.billingSnapshot(gross,net,gross-net,rate,tender,
                couponPaise>0?coupon:"NONE",order.getLoyaltyPointsUsed()>0?loyalty:"NONE");
    }
    private BigDecimal rate(PaymentOrder order){
        if(order.getPurpose()==PaymentPurpose.BOOKING){
            Booking booking=bookings.findById(order.getReferenceId())
                .orElseThrow(()->new NotFoundException("Booking not found"));
            return booking.getTaxRatePercent();
        }
        List<BigDecimal> rates=jdbc.query("""
          select tax_rate_percent from venue_billing_purpose_policies where purpose=? and active=true
          """,(rs,i)->rs.getBigDecimal(1),order.getPurpose().name());
        return rates.isEmpty()?null:rates.get(0);
    }
}
