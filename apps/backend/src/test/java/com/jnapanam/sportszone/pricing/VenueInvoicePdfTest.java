package com.jnapanam.sportszone.pricing;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VenueInvoicePdfTest {
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
}
