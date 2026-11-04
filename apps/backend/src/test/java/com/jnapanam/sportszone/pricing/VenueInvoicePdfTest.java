package com.jnapanam.sportszone.pricing;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VenueInvoicePdfTest {
 @Test void everyCompletedPaymentHasAnOrdinaryDownloadablePdfReceipt(){
    var receipt=new LinkedHashMap<String,Object>();
    receipt.put("receiptNumber","JNZ-PAY-DEMO");
    receipt.put("purpose","BOOKING");
    receipt.put("status","PAID");
    receipt.put("paymentAmountPaise",10000L);
    receipt.put("discountPaise",0L);
    receipt.put("loyaltyPointsUsed",0L);
    byte[] bytes=new VenueInvoicePdfService().renderPaymentReceipt(receipt);
    assertTrue(bytes.length>500);
    assertEquals("%PDF-",new String(bytes,0,5,StandardCharsets.US_ASCII));
 }
 @Test void downloadableInvoiceIsActualPdf(){
  var seller=new LinkedHashMap<String,Object>();
  seller.put("legalName","Jnapanam Sports Zone");
  seller.put("registeredAddress","Holalkere, Karnataka");
  seller.put("gstin","29ABCDE1234F1Z5");
  seller.put("stateCode","29");
  seller.put("serviceAccountingCode","999699");
  seller.put("signatory","Accounts");
  var document=new LinkedHashMap<String,Object>();
  document.put("supplier",seller);
  document.put("recipientName","Test Customer");
  document.put("recipientAddress","Bengaluru");
  document.put("recipientStateCode","29");
  document.put("service","Badminton court booking");
  document.put("taxablePaise",10000L);
  document.put("taxRatePercent",18);
  document.put("cgstPaise",900L);
  document.put("sgstPaise",900L);
  document.put("totalPaise",11800L);
  byte[] bytes=new VenueInvoicePdfService().render("JNZ26-000001","INVOICE",document);
  assertTrue(bytes.length>500);
  assertEquals("%PDF-",new String(bytes,0,5,StandardCharsets.US_ASCII));
 }
 @Test void receiptHasItemizedDiscountsLoyaltyAndTaxWithoutClaimingToBeGstInvoice() throws IOException {
   var receipt=new LinkedHashMap<String,Object>();
   receipt.put("receiptNumber","JNZ-PAY-123");receipt.put("purpose","BOOKING");
   receipt.put("status","PAID");receipt.put("paymentAmountPaise",13200);
   receipt.put("originalAmountPaise",15000);receipt.put("discountPaise",1800);
   receipt.put("promotionCode","WELCOME");receipt.put("loyaltyPointsUsed",20);
   receipt.put("billingLoyaltyTenderPaise",200);receipt.put("billingTaxablePaise",11000);
   receipt.put("billingTaxPaise",2400);receipt.put("billingGrossPaise",13400);
   receipt.put("billingRatePercent",18);
   byte[] pdf=new VenueInvoicePdfService().renderPaymentReceipt(receipt);
   try(PDDocument document=Loader.loadPDF(pdf)){
     var text=new PDFTextStripper().getText(document);
     assertTrue(text.contains("Discount applied"),text);
     assertTrue(text.contains("Applied offer"),text);
     assertTrue(text.contains("Loyalty value tender"),text);
     assertTrue(text.contains("Tax included"),text);
     assertTrue(text.contains("AMOUNT PAID"),text);
     assertTrue(text.contains("NOT a GST tax invoice"),text);
   }
 }
 @Test void invoiceListsTaxComponentsOffersLoyaltyAndTotal() throws IOException {
  var seller=Map.<String,Object>of("legalName","Jnapanam Sports Zone","registeredAddress","Holalkere",
      "gstin","29ABCDE1234F1Z5","stateCode","29","serviceAccountingCode","999699","signatory","Accounts");
  var info=new LinkedHashMap<String,Object>();
  info.put("supplier",seller);info.put("recipientName","Customer");info.put("recipientAddress","Bengaluru");
  info.put("service","Swimming slot");info.put("taxablePaise",10000);
  info.put("cgstPaise",900);info.put("sgstPaise",900);info.put("totalPaise",11800);
  info.put("promotionCode","FESTIVE");info.put("couponDiscountPaise",1000);
  info.put("loyaltyPointsUsed",5);info.put("loyaltyTenderPaise",500);
  byte[] pdf=new VenueInvoicePdfService().render("JNZ26-000001","INVOICE",info);
  try(PDDocument document=Loader.loadPDF(pdf)){
   String text=new PDFTextStripper().getText(document);
   for(String label:List.of("TAX INVOICE","Promotional discount","CGST","SGST","Loyalty points redeemed","INVOICE TOTAL"))
     assertTrue(text.contains(label),label+" not on invoice:\n"+text);
  }
 }

}
