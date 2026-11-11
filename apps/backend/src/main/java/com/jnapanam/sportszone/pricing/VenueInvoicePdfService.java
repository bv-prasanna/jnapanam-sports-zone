package com.jnapanam.sportszone.pricing;

import java.awt.Color;
import java.io.*;
import java.util.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

/**
 * A4 payment receipt, GST invoice, and credit note with independent identities.
 * This renders immutable stored accounting snapshots: it never recomputes tax or
 * converts a payment receipt into an unissued GST invoice.
 */
@Service
public class VenueInvoicePdfService {
 private static final Color OLIVE=new Color(40,51,15),SAFFRON=new Color(200,58,10);
 private static final Color MUTED=new Color(98,109,92),WARM=new Color(250,243,229),INK=new Color(32,37,28);
 private static final float LEFT=45,RIGHT=550;
 private static final PDFont REGULAR=new PDType1Font(Standard14Fonts.FontName.HELVETICA);
 private static final PDFont BOLD=new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

 public byte[] render(String number,String kind,Map<String,Object> data){
  try(PDDocument pdf=new PDDocument();ByteArrayOutputStream out=new ByteArrayOutputStream()){
   PDPage page=new PDPage(PDRectangle.A4);pdf.addPage(page);
   try(PDPageContentStream c=new PDPageContentStream(pdf,page)){
    title(pdf,c,"CREDIT_NOTE".equals(kind)?"GST CREDIT NOTE":"TAX INVOICE",number);
    float y=708;
    Map<?,?> supplier=data.get("supplier") instanceof Map<?,?> v?v:Map.of();
    y=section(c,"SUPPLIER",y);
    y=detail(c,"Name",supplier.get("legalName"),y);
    y=detail(c,"Registered address",supplier.get("registeredAddress"),y);
    y=detail(c,"GSTIN",supplier.get("gstin"),y);
    y=detail(c,"State / SAC",str(supplier.get("stateCode"))+" / "+str(supplier.get("serviceAccountingCode")),y);
    y=section(c,"BILL TO",y-3);
    y=detail(c,"Customer",data.get("recipientName"),y);
    y=detail(c,"Address",data.get("recipientAddress"),y);
    y=detail(c,"GSTIN / State",str(data.get("recipientGstin"))+" / "+str(data.get("recipientStateCode")),y);
    y=detail(c,"Place of supply",data.get("placeOfSupplyStateCode"),y);
    y=section(c,"SERVICE / DOCUMENT",y-4);
    y=detail(c,"Description",data.getOrDefault("service",data.getOrDefault("reason","Sports services")),y);
    if(data.get("bookingNumber")!=null)y=detail(c,"Booking",data.get("bookingNumber"),y);
    if(data.get("paymentReceipt")!=null)y=detail(c,"Payment receipt",data.get("paymentReceipt"),y);
    if(data.get("originalInvoice")!=null)y=detail(c,"Original invoice",data.get("originalInvoice"),y);
    y=section(c,"PRICE, DISCOUNTS AND TAXES",y-4);
    if(data.get("originalPricePaise")!=null)y=amount(c,"Original service amount",data.get("originalPricePaise"),y,false);
    if(data.get("promotionCode")!=null)y=detail(c,"Offer / coupon",data.get("promotionCode"),y);
    if(data.get("couponDiscountPaise")!=null)y=amount(c,"Promotional discount (-)",data.get("couponDiscountPaise"),y,false);
    if(data.get("loyaltyPointsUsed")!=null)y=detail(c,"Loyalty points redeemed",data.get("loyaltyPointsUsed"),y);
    if(data.get("loyaltyTenderPaise")!=null)y=amount(c,"Paid with loyalty value",data.get("loyaltyTenderPaise"),y,false);
    y=amount(c,"Taxable value",data.get("taxablePaise"),y,false);
    if(data.get("taxRatePercent")!=null)y=detail(c,"Tax rate",str(data.get("taxRatePercent"))+"%",y);
    if(paise(data.get("cgstPaise"))!=0)y=amount(c,"CGST",data.get("cgstPaise"),y,false);
    if(paise(data.get("sgstPaise"))!=0)y=amount(c,"SGST",data.get("sgstPaise"),y,false);
    if(paise(data.get("igstPaise"))!=0)y=amount(c,"IGST",data.get("igstPaise"),y,false);
    y=amount(c,"CREDIT_NOTE".equals(kind)?"TOTAL CREDIT":"INVOICE TOTAL",data.get("totalPaise"),y,true);
    if(data.get("customerCashPaidPaise")!=null)y=amount(c,"Amount collected in cash / gateway",data.get("customerCashPaidPaise"),y,false);
    footer(c,"Authorised signatory: "+str(supplier.get("signatory")),
      "Issued against the stored tax snapshot. Retain this document for your records.");
   }
   pdf.save(out);return out.toByteArray();
  }catch(IOException ex){throw new IllegalStateException("Unable to render billing document PDF",ex);}
 }

 /** A receipt proves payment collection; it is not a substitute for a GST invoice. */
 public byte[] renderPaymentReceipt(Map<String,Object> receipt){
  try(PDDocument pdf=new PDDocument();ByteArrayOutputStream out=new ByteArrayOutputStream()){
   PDPage page=new PDPage(PDRectangle.A4);pdf.addPage(page);
   try(PDPageContentStream c=new PDPageContentStream(pdf,page)){
    title(pdf,c,"PAYMENT RECEIPT",str(receipt.get("receiptNumber")));
    float y=708;
    y=section(c,"PAYMENT",y);
    y=detail(c,"Payment status",receipt.get("status"),y);
    y=detail(c,"Payment date",receipt.get("paidAt"),y);
    y=detail(c,"Purpose",receipt.get("purpose"),y);
    if(receipt.get("bookingNumber")!=null)y=detail(c,"Booking",receipt.get("bookingNumber"),y);
    if(receipt.get("facilityName")!=null)y=detail(c,"Facility",receipt.get("facilityName"),y);
    if(receipt.get("slotDate")!=null)y=detail(c,"Slot date",receipt.get("slotDate"),y);
    if(receipt.get("startTime")!=null)y=detail(c,"Slot time",
      str(receipt.get("startTime"))+" - "+str(receipt.get("endTime")),y);
    y=section(c,"PRICE AND PAYMENT BREAKDOWN",y-5);
    if(receipt.get("originalAmountPaise")!=null)y=amount(c,"Original order value",receipt.get("originalAmountPaise"),y,false);
    if(receipt.get("promotionCode")!=null)y=detail(c,"Applied offer / coupon",receipt.get("promotionCode"),y);
    y=amount(c,"Discount applied (-)",receipt.get("discountPaise"),y,false);
    y=detail(c,"Loyalty points redeemed",receipt.getOrDefault("loyaltyPointsUsed",0),y);
    if(receipt.get("billingLoyaltyTenderPaise")!=null)y=amount(c,"Loyalty value tender",receipt.get("billingLoyaltyTenderPaise"),y,false);
    if(receipt.get("billingTaxablePaise")!=null)y=amount(c,"Taxable value (snapshot)",receipt.get("billingTaxablePaise"),y,false);
    if(receipt.get("billingTaxPaise")!=null)y=amount(c,"Tax included"+(receipt.get("billingRatePercent")==null?"":" ("+receipt.get("billingRatePercent")+"%)"),receipt.get("billingTaxPaise"),y,false);
    if(receipt.get("billingGrossPaise")!=null)y=amount(c,"Gross consideration (cash + loyalty)",receipt.get("billingGrossPaise"),y,false);
    y=amount(c,"AMOUNT PAID",receipt.get("paymentAmountPaise"),y,true);
    long returned=0L;
    if(receipt.get("refunds") instanceof List<?> refunds)
     for(Object item:refunds)if(item instanceof Map<?,?> row
       && "PROCESSED".equals(String.valueOf(row.get("status"))))returned+=paise(row.get("amountPaise"));
    if(returned>0){y=amount(c,"Completed refunds",returned,y,false);
       y=amount(c,"NET PAYMENT RETAINED",Math.max(0,paise(receipt.get("paymentAmountPaise"))-returned),y,true);}
    footer(c,"Jnapanam Sports Zone - Holalkere, Karnataka",
     "This is a payment receipt, NOT a GST tax invoice. For tax details request an issued invoice.");
   }
   pdf.save(out);return out.toByteArray();
  }catch(IOException ex){throw new IllegalStateException("Unable to render payment receipt PDF",ex);}
 }

 private static void title(PDDocument doc,PDPageContentStream c,String heading,String number)throws IOException{
  c.setNonStrokingColor(OLIVE);c.addRect(0,756,PDRectangle.A4.getWidth(),86);c.fill();
  c.setNonStrokingColor(SAFFRON);c.addRect(0,752,PDRectangle.A4.getWidth(),4);c.fill();
  try(InputStream logo=VenueInvoicePdfService.class.getResourceAsStream("/branding/jnapanam-logo.png")){
    if(logo!=null){
      PDImageXObject image=PDImageXObject.createFromByteArray(doc,logo.readAllBytes(),"jnapanam-brand");
      c.drawImage(image,LEFT,770,62,62);
    }
  }
  text(c,BOLD,20,LEFT+78,801,Color.WHITE,"JNAPANAM SPORTS ZONE");
  text(c,REGULAR,11,LEFT+78,779,Color.WHITE,heading);
  text(c,BOLD,11,LEFT,733,INK,"DOCUMENT NO.  "+number);
 }
 private static float section(PDPageContentStream c,String label,float y)throws IOException{
  if(y<115)throw new IOException("Invoice exceeds one page; add pagination");
  c.setNonStrokingColor(WARM);c.addRect(LEFT,y-7,RIGHT-LEFT,24);c.fill();
  text(c,BOLD,10,LEFT+10,y+1,OLIVE,label);return y-24;
 }
 private static float detail(PDPageContentStream c,String label,Object val,float y)throws IOException{
  String value=safe(str(val));if(y<107)throw new IOException("Invoice exceeds one page; add pagination");
  text(c,REGULAR,9,LEFT+5,y,MUTED,label);
  int max=76;String[] split=wrap(value,max);
  for(String row:split){text(c,REGULAR,9,LEFT+139,y,INK,row);y-=13;}
  return y-4;
 }
 private static float amount(PDPageContentStream c,String label,Object value,float y,boolean prominent)throws IOException{
  if(y<95)throw new IOException("Invoice exceeds one page; add pagination");
  if(prominent){c.setNonStrokingColor(WARM);c.addRect(LEFT,y-9,RIGHT-LEFT,29);c.fill();}
  PDFont f=prominent?BOLD:REGULAR;int size=prominent?12:10;
  text(c,f,size,LEFT+8,y,prominent?OLIVE:INK,label);
  right(c,f,size,RIGHT-10,y,prominent?OLIVE:INK,money(value));
  return y-(prominent?35:21);
 }
 private static void footer(PDPageContentStream c,String top,String bottom)throws IOException{
  c.setStrokingColor(new Color(224,215,190));c.moveTo(LEFT,77);c.lineTo(RIGHT,77);c.stroke();
  text(c,REGULAR,9,LEFT,64,MUTED,top);
  text(c,REGULAR,8,LEFT,47,MUTED,bottom);
 }
 private static void right(PDPageContentStream c,PDFont font,int size,float x,float y,Color color,String label)throws IOException{
  String content=safe(label);
  float width=font.getStringWidth(content)/1000*size;
  text(c,font,size,x-width,y,color,content);
 }
 private static void text(PDPageContentStream c,PDFont font,int size,float x,float y,Color color,String label)throws IOException{
  c.setNonStrokingColor(color);c.beginText();c.setFont(font,size);c.newLineAtOffset(x,y);
  c.showText(safe(label));c.endText();
 }
 private static String[] wrap(String s,int max){
  if(s.length()<=max)return new String[]{s};
  List<String> words=new ArrayList<>();StringBuilder current=new StringBuilder();
  for(String piece:s.split(" ")){if(current.length()+piece.length()+1>max&&current.length()>0){words.add(current.toString());current.setLength(0);}
    if(current.length()>0)current.append(' ');current.append(piece);}
  if(current.length()>0)words.add(current.toString());
  return words.toArray(String[]::new);
 }
 private static String safe(String s){
  if(s==null)return "-";
  String cleaned=s.replace("₹","Rs.").replaceAll("[^\\x20-\\x7E]"," ").replaceAll("\\s+"," ").trim();
  return cleaned.length()>180?cleaned.substring(0,177)+"...":cleaned;
 }
 private static String str(Object value){return value==null?"-":String.valueOf(value);}
 private static long paise(Object x){return x instanceof Number n?n.longValue():0L;}
 private static String money(Object x){long n=paise(x);return (n<0?"-":"")+"INR "+String.format(Locale.ROOT,"%d.%02d",Math.abs(n)/100,Math.abs(n)%100);}
}
