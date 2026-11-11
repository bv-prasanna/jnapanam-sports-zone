package com.jnapanam.sportszone.pricing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnapanam.sportszone.booking.model.Booking;
import com.jnapanam.sportszone.booking.repository.BookingRepository;
import com.jnapanam.sportszone.common.exception.*;
import com.jnapanam.sportszone.payment.model.*;
import com.jnapanam.sportszone.payment.repository.*;
import jakarta.validation.constraints.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Tax-document issuance is deliberate. A payment receipt is never automatically represented as a GST invoice. */
@Service
public class VenueBillingService {
    private final JdbcTemplate jdbc;
    private final PaymentOrderRepository orders;
    private final BookingRepository bookings;
    private final PaymentRefundRepository refunds;
    private final ObjectMapper json;
    public VenueBillingService(JdbcTemplate jdbc,PaymentOrderRepository orders,BookingRepository bookings,
                               PaymentRefundRepository refunds,ObjectMapper json){
        this.jdbc=jdbc;this.orders=orders;this.bookings=bookings;this.refunds=refunds;this.json=json;
    }

    public record BillingProfile(@Size(max=160) String legalName,
        @Pattern(regexp="^[0-9]{2}[A-Z0-9]{13}$") String gstin,
        @Size(max=400) String registeredAddress,
        @Pattern(regexp="^[0-9]{2}$") String stateCode,
        @Size(max=12) String serviceAccountingCode,
        @Size(max=120) String signatory) {}

    public record InvoiceDetails(String recipientGstin,String recipientStateCode,String recipientAddress,
        String componentMode,String placeOfSupplyStateCode) {
        public InvoiceDetails(String recipientGstin,String recipientStateCode,String recipientAddress,String componentMode){this(recipientGstin,recipientStateCode,recipientAddress,componentMode,null);}
    }

    @Transactional(readOnly=true)
    public Map<String,Object> profile(){
        List<Map<String,Object>> rows=jdbc.queryForList("""
            select legal_name as "legalName",gstin,registered_address as "registeredAddress",
            state_code as "stateCode",service_accounting_code as "serviceAccountingCode",signatory
            from venue_billing_profile where id=1
            """);
        return rows.isEmpty()?Map.of():rows.get(0);
    }

    @Transactional
    public void updateProfile(BillingProfile p){
        if(p.gstin()!=null&&!p.gstin().isBlank()&&!p.gstin().matches("[0-9]{2}[A-Z0-9]{13}"))
            throw new BadRequestException("GSTIN must be a valid 15-character format");
        if(p.stateCode()==null||!p.stateCode().matches("[0-9]{2}"))
            throw new BadRequestException("Supplier state code is required");
        jdbc.update("""
            insert into venue_billing_profile(id,legal_name,gstin,registered_address,state_code,
                    service_accounting_code,signatory)
            values(1,?,?,?,?,?,?)
            on conflict(id) do update set legal_name=excluded.legal_name,gstin=excluded.gstin,
            registered_address=excluded.registered_address,state_code=excluded.state_code,
            service_accounting_code=excluded.service_accounting_code,signatory=excluded.signatory,updated_at=now()
            """,defaultName(p.legalName()),blank(p.gstin()),draftText(p.registeredAddress()),
            p.stateCode()==null||p.stateCode().isBlank()?"29":p.stateCode(),
            blank(p.serviceAccountingCode()),blank(p.signatory()));
    }

    @Transactional(readOnly=true)
    public Map<String,Object> receipt(UUID userId,UUID paymentId,boolean admin){
        PaymentOrder o=orders.findById(paymentId).orElseThrow(()->new NotFoundException("Payment not found"));
        if(!admin&&!o.getUser().getId().equals(userId))throw new NotFoundException("Payment not found");
        List<Map<String,Object>> documents=jdbc.queryForList("""
            select kind,state,document_number as "documentNumber",taxable_paise as "taxablePaise",
            tax_paise as "taxPaise",total_paise as "totalPaise",document_data::text as "details",issued_at as "issuedAt"
            from venue_billing_documents where payment_order_id=? order by created_at
            """,paymentId);
        List<Map<String,Object>> returns=jdbc.queryForList("""
            select id,amount_paise as "amountPaise",status,processed_at as "processedAt"
            from payment_refunds where payment_order_id=? order by requested_at
            """,paymentId);
        Map<String,Object> body=new LinkedHashMap<>();
        body.put("receiptNumber",o.getReceiptNumber());
        body.put("status",o.getStatus().name());
        body.put("paymentAmountPaise",o.getAmountPaise());
        body.put("originalAmountPaise",o.getOriginalAmountPaise());
        body.put("promotionCode",o.getPromotionCode());
        body.put("billingTaxablePaise",o.getBillingTaxablePaise());
        body.put("billingTaxPaise",o.getBillingTaxPaise());
        body.put("billingGrossPaise",o.getBillingGrossPaise());
        body.put("billingLoyaltyTenderPaise",o.getBillingLoyaltyTenderPaise());
        body.put("billingRatePercent",o.getBillingRatePercent());
        body.put("discountPaise",o.getDiscountPaise());
        body.put("loyaltyPointsUsed",o.getLoyaltyPointsUsed());
        body.put("purpose",o.getPurpose().name());
        body.put("paidAt",o.getPaidAt());
        if(o.getPurpose()==PaymentPurpose.BOOKING){
            Booking b=bookings.findById(o.getReferenceId()).orElseThrow(()->new NotFoundException("Booking not found"));
            body.put("bookingNumber",b.getBookingNumber());
            body.put("facilityName",b.getSlot().getFacility().getName());
            body.put("sportName",b.getSlot().getFacility().getSport().getName());
            body.put("slotDate",b.getSlot().getDate().toString());
            body.put("startTime",b.getSlot().getStartTime().toString());
            body.put("endTime",b.getSlot().getEndTime().toString());
            body.put("basePaise",b.getPriceBasePaise());
            body.put("taxablePaise",b.getPriceSubtotalPaise());
            body.put("quotedTaxPaise",b.getPriceTaxPaise());
            body.put("quotedTotalPaise",b.getPriceTotalPaise());
        }
        body.put("documents",documents);body.put("refunds",returns);
        return body;
    }

    /** Customer-requested invoice; no email is sent. Only approved policy and owned payment may issue. */
    @Transactional
    public Map<String,Object> requestOwnInvoice(UUID userId,UUID paymentId){
        PaymentOrder order=orders.findById(paymentId).orElseThrow(()->new NotFoundException("Payment not found"));
        if(!order.getUser().getId().equals(userId))throw new NotFoundException("Payment not found");
        if(order.getStatus()!=PaymentStatus.PAID && order.getStatus()!=PaymentStatus.PARTIALLY_REFUNDED)
            throw new ConflictException("Payment must be completed before requesting a tax invoice");
        List<Map<String,Object>> available=jdbc.queryForList("""
          select place_of_supply_state_code from venue_billing_purpose_policies
          where purpose=? and active=true and place_of_supply_state_code is not null
          """,order.getPurpose().name());
        if(available.isEmpty())
            throw new ConflictException("GST invoice configuration is not active for this service yet. Your payment receipt is available for download.");
        List<Map<String,Object>> addresses=jdbc.queryForList("""
          select recipient_address,state_code,gstin from customer_billing_profiles where user_id=?
          """,userId);
        if(addresses.isEmpty())
            throw new ConflictException("Please save your billing address under Billing Details before requesting an invoice");
        Map<String,Object> address=addresses.get(0);
        Map<String,Object> merchant=profile();
        String supplyState=String.valueOf(available.get(0).get("place_of_supply_state_code"));
        String mode=supplyState.equals(merchant.get("stateCode"))?"CGST_SGST":"IGST";
        return issueInvoice(paymentId,new InvoiceDetails(
            (String)address.get("gstin"),(String)address.get("state_code"),
            (String)address.get("recipient_address"),mode,supplyState));
    }

    @Transactional
    public Map<String,Object> issueInvoice(UUID paymentId,InvoiceDetails details){
        PaymentOrder o=orders.findById(paymentId).orElseThrow(()->new NotFoundException("Payment not found"));
        if(o.getStatus()!=PaymentStatus.PAID && o.getStatus()!=PaymentStatus.PARTIALLY_REFUNDED)
            throw new ConflictException("Only captured payments can receive an invoice");
        List<Map<String,Object>> existing=jdbc.queryForList("""
            select document_number as "documentNumber",state,total_paise as "totalPaise"
            from venue_billing_documents where payment_order_id=? and kind='INVOICE'
            """,paymentId);
        if(!existing.isEmpty())return existing.get(0);
        if(o.getBillingGrossPaise()==null||o.getBillingTaxablePaise()==null
           ||o.getBillingTaxPaise()==null||o.getBillingRatePercent()==null)
            throw new ConflictException("Payment has no approved tax snapshot. Review its discount/loyalty treatment or legacy tax data.");
        long net=o.getBillingTaxablePaise(),tax=o.getBillingTaxPaise(),total=o.getBillingGrossPaise();
        if(net+tax!=total||total!=o.getAmountPaise()+o.getBillingLoyaltyTenderPaise())
            throw new ConflictException("The invoice breakdown does not match captured cash and loyalty consideration");
        Map<String,Object> seller=profile();
        if(seller.isEmpty() || seller.get("gstin")==null
            || isBlank(seller.get("legalName")) || isBlank(seller.get("registeredAddress"))
            || isBlank(seller.get("stateCode")) || isBlank(seller.get("serviceAccountingCode"))
            || isBlank(seller.get("signatory")))
            throw new ConflictException("Complete and verify legal name, registered address, GSTIN, state, SAC and signatory in Admin billing settings before issuing GST invoices");
        if(details==null||details.componentMode()==null||!Set.of("IGST","CGST_SGST").contains(details.componentMode()))
            throw new BadRequestException("Choose applicable IGST or CGST + SGST");
        if(details.recipientStateCode()==null||!details.recipientStateCode().matches("[0-9]{2}"))
            throw new BadRequestException("Recipient state code is required");
        if(details.recipientAddress()==null||details.recipientAddress().isBlank())
            throw new BadRequestException("Recipient address is required");
        if(details.recipientGstin()!=null&&!details.recipientGstin().isBlank()
           &&!details.recipientGstin().matches("[0-9]{2}[A-Z0-9]{13}"))
            throw new BadRequestException("Invalid recipient GSTIN");
        if(details.placeOfSupplyStateCode()==null||!details.placeOfSupplyStateCode().matches("[0-9]{2}"))
            throw new BadRequestException("Choose the GST place of supply state separately from the customer state");
        if(!details.placeOfSupplyStateCode().equals(seller.get("stateCode"))&&!"IGST".equals(details.componentMode()))
            throw new BadRequestException("Place of supply is outside supplier state. Select IGST, subject to tax review.");
        if(details.placeOfSupplyStateCode().equals(seller.get("stateCode"))&&!"CGST_SGST".equals(details.componentMode()))
            throw new BadRequestException("Place of supply is within supplier state. Select CGST + SGST, subject to tax review.");
        Boolean einvoice=jdbc.queryForObject("select coalesce(e_invoice_applicable,false) from venue_billing_profile where id=1",Boolean.class);
        if(Boolean.TRUE.equals(einvoice))
            throw new ConflictException("E-invoicing is enabled: an IRP-approved invoice/IRN workflow is required before issue. Do not issue a non-IRP GST invoice.");
        long central=details.componentMode().equals("CGST_SGST")?tax/2:0;
        long state=details.componentMode().equals("CGST_SGST")?tax-central:0;
        long integrated=details.componentMode().equals("IGST")?tax:0;
        Map<String,Object> data=new LinkedHashMap<>();
        data.put("supplier",seller);data.put("recipientName",o.getUser().getFullName());
        data.put("recipientAddress",details.recipientAddress());data.put("recipientGstin",blank(details.recipientGstin()));
        data.put("recipientStateCode",details.recipientStateCode());data.put("placeOfSupplyStateCode",details.placeOfSupplyStateCode());data.put("componentMode",details.componentMode());
        data.put("service",serviceDescription(o));data.put("quantity",1);
        data.put("purpose",o.getPurpose().name());data.put("paymentReceipt",o.getReceiptNumber());
        data.put("originalPricePaise",o.getOriginalAmountPaise());
        data.put("couponDiscountPaise",o.getDiscountPaise());
        data.put("promotionCode",o.getPromotionCode());
        data.put("loyaltyPointsUsed",o.getLoyaltyPointsUsed());
        data.put("loyaltyTenderPaise",o.getBillingLoyaltyTenderPaise());
        data.put("customerCashPaidPaise",o.getAmountPaise());
        data.put("taxablePaise",net);data.put("taxRatePercent",o.getBillingRatePercent());
        data.put("cgstPaise",central);data.put("sgstPaise",state);
        data.put("igstPaise",integrated);data.put("totalPaise",total);data.put("paymentId",paymentId.toString());
        if(o.getPurpose()==PaymentPurpose.BOOKING){
            Booking b=bookings.findById(o.getReferenceId()).orElseThrow(()->new NotFoundException("Booking not found"));
            data.put("bookingNumber",b.getBookingNumber());data.put("quantity",b.getQuantity());
        }
        return issueDocument(paymentId,null,"INVOICE",net,tax,total,data);
    }

    private String serviceDescription(PaymentOrder order){
        UUID reference=order.getReferenceId();
        return switch(order.getPurpose()){
            case BOOKING -> {
                Booking b=bookings.findById(reference).orElseThrow(()->new NotFoundException("Booking not found"));
                yield "Court booking: "+b.getSlot().getFacility().getName()+" / "+b.getSlot().getDate();
            }
            case MEMBERSHIP -> jdbc.queryForObject("""
                select p.name from customer_memberships m join membership_plans p on p.id=m.plan_id where m.id=?
                """,String.class,reference);
            case TRAINING -> jdbc.queryForObject("""
                select b.name from training_enrollments e join training_batches b on b.id=e.batch_id where e.id=?
                """,String.class,reference);
            case TOURNAMENT -> jdbc.queryForObject("""
                select t.name from tournament_teams e join tournaments t on t.id=e.tournament_id where e.id=?
                """,String.class,reference);
        };
    }

    @Transactional
    public Map<String,Object> issueCreditNote(UUID refundId){
        PaymentRefund refund=refunds.findById(refundId).orElseThrow(()->new NotFoundException("Refund not found"));
        if(refund.getStatus()!=RefundStatus.PROCESSED)throw new ConflictException("Only completed refunds can have a credit note");
        PaymentOrder o=refund.getPaymentOrder();
        List<Map<String,Object>> previous=jdbc.queryForList("""
            select id,taxable_paise,tax_paise,total_paise,document_number,document_data::text as original_data
            from venue_billing_documents where payment_order_id=? and kind='INVOICE' and state='ISSUED'
            """,o.getId());
        if(previous.isEmpty())throw new ConflictException("An issued GST invoice is required before a credit note");
        List<Map<String,Object>> existing=jdbc.queryForList("""
            select document_number as "documentNumber",state,total_paise as "totalPaise"
            from venue_billing_documents where refund_id=? and kind='CREDIT_NOTE'
            """,refundId);
        if(!existing.isEmpty())return existing.get(0);
        Map<String,Object> invoice=previous.get(0);
        long invoiceTotal=((Number)invoice.get("total_paise")).longValue();
        long invoiceTax=((Number)invoice.get("tax_paise")).longValue();
        long refunded=refund.getAmountPaise();
        long already=jdbc.queryForObject("""
            select coalesce(sum(total_paise),0) from venue_billing_documents
            where payment_order_id=? and kind='CREDIT_NOTE' and state='ISSUED'
            """,Long.class,o.getId());
        if(refunded<=0 || refunded+already>invoiceTotal)
            throw new ConflictException("Refund exceeds original invoice balance");
        long previouslyCreditedTax=jdbc.queryForObject("""
            select coalesce(sum(tax_paise),0) from venue_billing_documents
            where payment_order_id=? and kind='CREDIT_NOTE' and state='ISSUED'
            """,Long.class,o.getId());
        long availableTax=Math.max(0,invoiceTax-previouslyCreditedTax);
        long proratedTax=BigDecimal.valueOf(invoiceTax).multiply(BigDecimal.valueOf(refunded))
            .divide(BigDecimal.valueOf(invoiceTotal),0,RoundingMode.HALF_UP).longValueExact();
        long tax=refunded+already==invoiceTotal?availableTax:Math.min(availableTax,proratedTax);
        long net=refunded-tax;
        Map<String,Object> data=new LinkedHashMap<>();
        try{Map<?,?> original=json.readValue(String.valueOf(invoice.get("original_data")),Map.class);data.put("supplier",original.get("supplier"));data.put("recipientName",original.get("recipientName"));data.put("recipientAddress",original.get("recipientAddress"));data.put("recipientStateCode",original.get("recipientStateCode"));data.put("placeOfSupplyStateCode",original.get("placeOfSupplyStateCode"));data.put("recipientGstin",original.get("recipientGstin"));data.put("componentMode",original.get("componentMode"));data.put("cgstPaise","CGST_SGST".equals(original.get("componentMode"))?tax/2:0);data.put("sgstPaise","CGST_SGST".equals(original.get("componentMode"))?tax-tax/2:0);data.put("igstPaise","IGST".equals(original.get("componentMode"))?tax:0);}catch(JsonProcessingException ex){throw new IllegalStateException("Invalid stored invoice snapshot",ex);}
        data.put("originalInvoice",invoice.get("document_number"));
        data.put("refundId",refundId.toString());
        data.put("reason",refund.getReason());
        data.put("taxablePaise",net);data.put("taxPaise",tax);data.put("totalPaise",refunded);
        return issueDocument(o.getId(),refundId,"CREDIT_NOTE",net,tax,refunded,data);
    }

    private Map<String,Object> issueDocument(UUID paymentId,UUID refundId,String kind,long net,long tax,long total,Map<String,Object> data){
        LocalDate today=LocalDate.now(ZoneId.of("Asia/Kolkata"));
        int fy=today.getMonthValue()>=4?today.getYear():today.getYear()-1;
        long serial=jdbc.queryForObject("select nextval('venue_billing_document_seq')",Long.class);
        String documentNumber="JNZ"+(fy%100)+"-"+String.format(Locale.ROOT,"%08d",serial);
        if(documentNumber.length()>16)throw new ConflictException("Invoice series exceeded its configured size");
        UUID documentId=UUID.randomUUID();
        try{
            jdbc.update("""
                insert into venue_billing_documents(id,payment_order_id,refund_id,kind,state,document_number,
                fiscal_year,taxable_paise,tax_paise,total_paise,document_data,issued_at)
                values(?,?,?,?,'ISSUED',?,?,?,?,?,?::jsonb,now())
                """,documentId,paymentId,refundId,kind,documentNumber,fy,net,tax,total,json.writeValueAsString(data));
        }catch(JsonProcessingException ex){throw new IllegalStateException("Unable to serialize invoice data",ex);}
        // Queued atomically with issuance. Delivery failures never retract a legal document.
        jdbc.update("""
          insert into venue_billing_email_deliveries(id,document_id,payment_order_id,recipient)
          select ?,?,?,u.email from payment_orders p join users u on u.id=p.user_id
          where p.id=? and u.email is not null and u.email<>'' and
          exists(select 1 from venue_billing_profile where id=1 and invoice_email_enabled=true)
          on conflict(document_id) do nothing
          """,UUID.randomUUID(),documentId,paymentId,paymentId);
        return Map.of("documentNumber",documentNumber,"state","ISSUED","totalPaise",total);
    }
    private static boolean isBlank(Object value){return value==null||String.valueOf(value).isBlank();}
    private static String defaultName(String value){return value==null||value.isBlank()?"Jnapanam Sports Zone":value.trim();}
    private static String draftText(String value){return value==null?"":value.trim();}
    private static String blank(String s){return s==null||s.isBlank()?null:s.trim();}
}
