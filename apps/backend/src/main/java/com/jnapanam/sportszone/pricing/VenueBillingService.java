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

    public record BillingProfile(@NotBlank @Size(max=160) String legalName,
        @Pattern(regexp="^[0-9]{2}[A-Z0-9]{13}$") String gstin,
        @NotBlank @Size(max=400) String registeredAddress,
        @Pattern(regexp="^[0-9]{2}$") String stateCode,
        @NotBlank @Pattern(regexp="^[A-Za-z0-9]{4,12}$") String serviceAccountingCode,
        @NotBlank @Size(max=120) String signatory) {}

    public record InvoiceDetails(String recipientGstin,String recipientStateCode,String recipientAddress,
        String componentMode) {}

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
            """,p.legalName().trim(),blank(p.gstin()),p.registeredAddress().trim(),p.stateCode(),
            p.serviceAccountingCode(),p.signatory().trim());
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
        body.put("discountPaise",o.getDiscountPaise());
        body.put("loyaltyPointsUsed",o.getLoyaltyPointsUsed());
        body.put("purpose",o.getPurpose().name());
        body.put("paidAt",o.getPaidAt());
        if(o.getPurpose()==PaymentPurpose.BOOKING){
            Booking b=bookings.findById(o.getReferenceId()).orElseThrow(()->new NotFoundException("Booking not found"));
            body.put("bookingNumber",b.getBookingNumber());
            body.put("basePaise",b.getPriceBasePaise());
            body.put("taxablePaise",b.getPriceSubtotalPaise());
            body.put("quotedTaxPaise",b.getPriceTaxPaise());
            body.put("quotedTotalPaise",b.getPriceTotalPaise());
        }
        body.put("documents",documents);body.put("refunds",returns);
        return body;
    }

    @Transactional
    public Map<String,Object> issueInvoice(UUID paymentId,InvoiceDetails details){
        PaymentOrder o=orders.findById(paymentId).orElseThrow(()->new NotFoundException("Payment not found"));
        if(o.getStatus()!=PaymentStatus.PAID && o.getStatus()!=PaymentStatus.PARTIALLY_REFUNDED && o.getStatus()!=PaymentStatus.REFUNDED)
            throw new ConflictException("Only captured payments can receive an invoice");
        if(o.getPurpose()!=PaymentPurpose.BOOKING)
            throw new BadRequestException("Booking-specific GST invoice handling supports booking payments only");
        Booking b=bookings.findById(o.getReferenceId()).orElseThrow(()->new NotFoundException("Booking not found"));
        if(b.getPriceTotalPaise()==null || b.getPriceTaxPaise()==null)
            throw new ConflictException("This legacy booking has no locked tax breakdown. Manual accounting review is required.");
        if(b.getPriceTotalPaise()!=o.getAmountPaise()||o.getDiscountPaise()!=0||o.getLoyaltyPointsUsed()!=0)
            throw new ConflictException("Discount or loyalty allocation requires manual tax review before invoice issue");
        Map<String,Object> seller=profile();
        if(seller.isEmpty()||seller.get("gstin")==null)
            throw new ConflictException("Configure supplier legal name, address and GSTIN before issuing GST invoices");
        if(details==null||details.componentMode()==null||!Set.of("IGST","CGST_SGST").contains(details.componentMode()))
            throw new BadRequestException("Choose IGST or CGST + SGST after checking place-of-supply rules");
        if(details.recipientStateCode()==null||!details.recipientStateCode().matches("[0-9]{2}"))
            throw new BadRequestException("Recipient state code is required");
        if(details.recipientAddress()==null||details.recipientAddress().isBlank())
            throw new BadRequestException("Recipient address is required");
        if(details.recipientGstin()!=null&&!details.recipientGstin().isBlank()&&!details.recipientGstin().matches("[0-9]{2}[A-Z0-9]{13}"))
            throw new BadRequestException("Invalid recipient GSTIN");
        List<Map<String,Object>> existing=jdbc.queryForList("""
            select document_number as "documentNumber",state,total_paise as "totalPaise"
            from venue_billing_documents where payment_order_id=? and kind='INVOICE'
            """,paymentId);
        if(!existing.isEmpty())return existing.get(0);
        long net=b.getPriceSubtotalPaise(),tax=b.getPriceTaxPaise(),total=b.getPriceTotalPaise();
        long central=details.componentMode().equals("CGST_SGST")?tax/2:0;
        long state=details.componentMode().equals("CGST_SGST")?tax-central:0;
        long integrated=details.componentMode().equals("IGST")?tax:0;
        Map<String,Object> data=new LinkedHashMap<>();
        data.put("supplier",seller);data.put("recipientName",o.getUser().getFullName());
        data.put("recipientAddress",details.recipientAddress());data.put("recipientGstin",blank(details.recipientGstin()));
        data.put("recipientStateCode",details.recipientStateCode());data.put("componentMode",details.componentMode());
        data.put("service",b.getSlot().getFacility().getName()+" — "+b.getSlot().getDate());
        data.put("bookingNumber",b.getBookingNumber());data.put("quantity",b.getQuantity());
        data.put("taxablePaise",net);data.put("taxRatePercent",net==0?BigDecimal.ZERO:BigDecimal.valueOf(tax).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(net),3,RoundingMode.HALF_UP));data.put("cgstPaise",central);data.put("sgstPaise",state);
        data.put("igstPaise",integrated);data.put("totalPaise",total);data.put("paymentId",paymentId.toString());
        return issueDocument(paymentId,null,"INVOICE",net,tax,total,data);
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
        try{Map<?,?> original=json.readValue(String.valueOf(invoice.get("original_data")),Map.class);data.put("supplier",original.get("supplier"));data.put("recipientName",original.get("recipientName"));data.put("recipientAddress",original.get("recipientAddress"));data.put("recipientStateCode",original.get("recipientStateCode"));data.put("recipientGstin",original.get("recipientGstin"));data.put("componentMode",original.get("componentMode"));data.put("cgstPaise","CGST_SGST".equals(original.get("componentMode"))?tax/2:0);data.put("sgstPaise","CGST_SGST".equals(original.get("componentMode"))?tax-tax/2:0);data.put("igstPaise","IGST".equals(original.get("componentMode"))?tax:0);}catch(JsonProcessingException ex){throw new IllegalStateException("Invalid stored invoice snapshot",ex);}
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
        try{
            jdbc.update("""
                insert into venue_billing_documents(id,payment_order_id,refund_id,kind,state,document_number,
                fiscal_year,taxable_paise,tax_paise,total_paise,document_data,issued_at)
                values(?,?,?,?,'ISSUED',?,?,?,?,?,?::jsonb,now())
                """,UUID.randomUUID(),paymentId,refundId,kind,documentNumber,fy,net,tax,total,json.writeValueAsString(data));
        }catch(JsonProcessingException ex){throw new IllegalStateException("Unable to serialize invoice data",ex);}
        return Map.of("documentNumber",documentNumber,"state","ISSUED","totalPaise",total);
    }
    private static String blank(String s){return s==null||s.isBlank()?null:s.trim();}
}
