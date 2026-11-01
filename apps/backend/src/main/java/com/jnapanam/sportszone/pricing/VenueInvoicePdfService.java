package com.jnapanam.sportszone.pricing;

import java.io.*;
import java.util.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.springframework.stereotype.Service;

/** Immutable server-generated PDF; only authorized callers may supply its stored billing document. */
@Service
public class VenueInvoicePdfService {
    public byte[] render(String documentNo, String kind, Map<String,Object> data) {
        try(PDDocument pdf=new PDDocument();ByteArrayOutputStream out=new ByteArrayOutputStream()){
            PDPage page=new PDPage(org.apache.pdfbox.pdmodel.common.PDRectangle.A4);
            pdf.addPage(page);
            try(PDPageContentStream c=new PDPageContentStream(pdf,page)){
                PDType1Font heading=new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font body=new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                float y=784;
                y=line(c,heading,18,48,y,kind.equals("CREDIT_NOTE")?"JNapanam GST Credit Note":"Jnapanam Tax Invoice");
                y=line(c,body,11,48,y-8,"Document number: "+documentNo);
                y=line(c,body,10,48,y,"Payment receipt: "+str(data.get("paymentReceipt")));
                Map<?,?> supplier=data.get("supplier") instanceof Map<?,?> v?v:Map.of();
                y=line(c,heading,11,48,y-14,"SUPPLIER");
                y=line(c,body,10,48,y,str(supplier.get("legalName")));
                y=wrapped(c,body,10,48,y,str(supplier.get("registeredAddress")),86);
                y=line(c,body,10,48,y,"GSTIN: "+str(supplier.get("gstin"))+"  State: "+str(supplier.get("stateCode")));
                y=line(c,body,10,48,y,"SAC: "+str(supplier.get("serviceAccountingCode")));
                y=line(c,heading,11,48,y-12,"CUSTOMER");
                y=line(c,body,10,48,y,str(data.get("recipientName")));
                y=wrapped(c,body,10,48,y,str(data.get("recipientAddress")),86);
                y=line(c,body,10,48,y,"GSTIN: "+str(data.get("recipientGstin"))+"  State: "+str(data.get("recipientStateCode")));
                y=line(c,heading,11,48,y-13,"SERVICES");
                y=wrapped(c,body,10,48,y,str(data.getOrDefault("service",data.getOrDefault("reason","Sports services"))),86);
                if(data.containsKey("originalInvoice"))
                    y=line(c,body,10,48,y,"Original invoice: "+str(data.get("originalInvoice")));
                if(data.containsKey("couponDiscountPaise"))
                    y=line(c,body,10,48,y,"Coupon discount: "+money(data.get("couponDiscountPaise")));
                if(data.containsKey("loyaltyTenderPaise"))
                    y=line(c,body,10,48,y,"Loyalty points tender: "+money(data.get("loyaltyTenderPaise")));
                y=line(c,heading,11,48,y-10,"TAX BREAKDOWN");
                y=line(c,body,11,48,y,"Taxable value: "+money(data.get("taxablePaise")));
                y=line(c,body,10,48,y,"Rate: "+str(data.get("taxRatePercent"))+"%");
                y=line(c,body,10,48,y,"CGST: "+money(data.get("cgstPaise"))+"   SGST: "+money(data.get("sgstPaise")));
                y=line(c,body,10,48,y,"IGST: "+money(data.get("igstPaise")));
                y=line(c,heading,14,48,y-6,"TOTAL: "+money(data.get("totalPaise")));
                if(data.containsKey("customerCashPaidPaise"))
                    y=line(c,body,10,48,y,"Cash collected: "+money(data.get("customerCashPaidPaise")));
                y=line(c,body,9,48,y-20,"Authorised signatory: "+str(supplier.get("signatory")));
                line(c,body,8,48,58,"Generated from an immutable billing record. Please retain for your tax records.");
            }
            pdf.save(out);
            return out.toByteArray();
        }catch(IOException ex){throw new IllegalStateException("Unable to render billing document PDF",ex);}
    }
    /** Proof of payment, explicitly not a GST tax invoice. Available even before GST setup. */
    public byte[] renderPaymentReceipt(Map<String,Object> receipt) {
        try(PDDocument pdf=new PDDocument();ByteArrayOutputStream out=new ByteArrayOutputStream()){
            PDPage page=new PDPage(org.apache.pdfbox.pdmodel.common.PDRectangle.A4);
            pdf.addPage(page);
            try(PDPageContentStream c=new PDPageContentStream(pdf,page)){
                PDFont heading=new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDFont body=new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                float y=780;
                y=line(c,heading,19,50,y,"JNapanam Sports Zone");
                y=line(c,heading,15,50,y-15,"PAYMENT RECEIPT");
                y=line(c,body,11,50,y,"Receipt number: "+str(receipt.get("receiptNumber")));
                y=line(c,body,11,50,y,"Service: "+str(receipt.get("purpose")));
                if(receipt.get("bookingNumber")!=null)
                    y=line(c,body,11,50,y,"Booking: "+str(receipt.get("bookingNumber")));
                y=line(c,body,11,50,y,"Payment status: "+str(receipt.get("status")));
                y=line(c,body,11,50,y,"Payment date: "+str(receipt.get("paidAt")));
                y=line(c,body,11,50,y-20,"Original payment collected: "+money(receipt.get("paymentAmountPaise")));
                long refunded=0L;
                Object refunds=receipt.get("refunds");
                if(refunds instanceof List<?> items){
                    for(Object obj:items){
                        if(obj instanceof Map<?,?> row && "PROCESSED".equals(String.valueOf(row.get("status")))
                            && row.get("amountPaise") instanceof Number n)refunded+=n.longValue();
                    }
                }
                if(refunded>0){
                    y=line(c,body,11,50,y,"Completed refunds: "+money(refunded));
                    y=line(c,body,11,50,y,"Net payment retained: "+
                        money(Math.max(0L,((Number)receipt.get("paymentAmountPaise")).longValue()-refunded)));
                }
                y=line(c,body,10,50,y,"Promotion discount: "+money(receipt.get("discountPaise")));
                y=line(c,body,10,50,y,"Loyalty points used: "+str(receipt.get("loyaltyPointsUsed")));
                line(c,body,10,50,y-25,"This receipt confirms payment; it is not a GST tax invoice.");
            }
            pdf.save(out);
            return out.toByteArray();
        }catch(IOException ex){throw new IllegalStateException("Unable to render payment receipt PDF",ex);}
    }

    private static float line(PDPageContentStream c,PDFont f,int size,float x,float y,String value)throws IOException{
        if(y<45)throw new IOException("Document overflows one-page invoice template");
        c.beginText();c.setFont(f,size);c.newLineAtOffset(x,y);
        c.showText(safe(value));c.endText();return y-size-9;
    }
    private static float wrapped(PDPageContentStream c,PDFont font,int size,float x,float y,String text,int length)throws IOException{
        String input=safe(text);while(input.length()>length){int split=input.lastIndexOf(' ',length);if(split<12)split=length;y=line(c,font,size,x,y,input.substring(0,split));input=input.substring(split).trim();}return line(c,font,size,x,y,input);
    }
    private static String safe(String value){if(value==null)return "-";String cleaned=value.replace("₹","Rs.").replaceAll("[^\\x20-\\x7E]"," ").replaceAll("\\s+"," ").trim();return cleaned.substring(0,Math.min(cleaned.length(),110));}
    private static String str(Object v){return v==null?"-":String.valueOf(v);}
    private static String money(Object o){
        long paise=o instanceof Number?nLong((Number)o):0L;
        return String.format(Locale.ROOT,"INR %d.%02d",paise/100,Math.abs(paise%100));
    }
    private static long nLong(Number n){return n.longValue();}
}
