"use client";
import {useEffect,useState} from "react";
import {useParams} from "next/navigation";
import {api,apiBlob} from "@/lib/api";
import styles from "./billing.module.css";
type Doc={kind:string;state:string;documentNumber:string;taxablePaise:number;taxPaise:number;totalPaise:number;details:string;issuedAt:string};
type Receipt={receiptNumber:string;status:string;paymentAmountPaise:number;bookingNumber?:string;discountPaise:number;
originalAmountPaise?:number;promotionCode?:string;loyaltyPointsUsed?:number;billingLoyaltyTenderPaise?:number;billingGrossPaise?:number;billingTaxablePaise?:number;billingTaxPaise?:number;billingRatePercent?:number;
facilityName?:string;sportName?:string;slotDate?:string;startTime?:string;endTime?:string;
documents:Doc[];refunds:{id:string;amountPaise:number;status:string}[]};
const rs=(paise:unknown)=>new Intl.NumberFormat("en-IN",{style:"currency",currency:"INR"}).format(Number(paise||0)/100);
export default function BillingDocumentsPage(){
 const params=useParams();const id=String(params.id||"");const [receipt,setReceipt]=useState<Receipt|null>(null),[error,setError]=useState(""),[busy,setBusy]=useState(false);
 async function download(number:string){try{const blob=await apiBlob(`/api/v1/payments/${id}/billing/${encodeURIComponent(number)}/pdf`);const url=URL.createObjectURL(blob);const anchor=document.createElement("a");anchor.href=url;anchor.download=`jnapanam-${number}.pdf`;document.body.appendChild(anchor);anchor.click();anchor.remove();setTimeout(()=>URL.revokeObjectURL(url),15000)}catch(e){setError(e instanceof Error?e.message:"Download failed")}}
 async function downloadReceipt(){try{setError("");const blob=await apiBlob(`/api/v1/payments/${id}/billing/receipt/pdf`);const url=URL.createObjectURL(blob);const link=document.createElement("a");link.href=url;link.download="jnapanam-payment-receipt.pdf";document.body.appendChild(link);link.click();link.remove();setTimeout(()=>URL.revokeObjectURL(url),15000)}catch(e){setError(e instanceof Error?e.message:"Unable to download payment receipt")}}
 async function requestInvoice(){try{setBusy(true);setError("");await api(`/api/v1/payments/${id}/billing/invoice`,{method:"POST"});setReceipt(await api<Receipt>(`/api/v1/payments/${id}/billing`))}catch(e){setError(e instanceof Error?e.message:"GST invoice cannot be issued until the merchant's tax configuration and your billing details are complete.")}finally{setBusy(false)}}
 useEffect(()=>{if(!id)return;api<Receipt>(`/api/v1/payments/${id}/billing`).then(setReceipt).catch(e=>setError(e instanceof Error?e.message:"Unable to load receipt"))},[id]);

 if(!receipt)return <main className={styles.paper}>{error?<p role="alert">{error}</p>:<p role="status">Loading billing documents…</p>}</main>;
 return <main className={styles.paper}>
 <header className={styles.toolbar}><h1>Jnapanam Billing Documents</h1><button type="button" onClick={()=>window.print()}>Print / Save as PDF</button></header>
 {error&&<p role="alert" style={{color:"#a32323",padding:12}}>{error}</p>}
 <section className={styles.receipt}><h2>Payment Receipt</h2><p>Receipt: <strong>{receipt.receiptNumber}</strong> · Booking: {receipt.bookingNumber||"—"}</p><p>Payment status: {receipt.status}</p><button type="button" disabled={busy} onClick={()=>void downloadReceipt()}>Download payment receipt (PDF)</button><p>{[receipt.sportName,receipt.facilityName,receipt.slotDate,receipt.startTime&&receipt.endTime?`${receipt.startTime.slice(0,5)}–${receipt.endTime.slice(0,5)}`:null].filter(Boolean).join(" · ")}</p>
 <table className={styles.table}><thead><tr><th>Description</th><th>Amount</th></tr></thead><tbody>
 {receipt.originalAmountPaise!=null&&<tr><td>Original order value</td><td>{rs(receipt.originalAmountPaise)}</td></tr>}
 {receipt.promotionCode&&<tr><td>Applied offer / coupon</td><td>{receipt.promotionCode}</td></tr>}
 <tr><td>Promotion / coupon discount</td><td>- {rs(receipt.discountPaise)}</td></tr>
 <tr><td>Loyalty points redeemed</td><td>{receipt.loyaltyPointsUsed??0} points</td></tr>
 {receipt.billingLoyaltyTenderPaise!=null&&<tr><td>Loyalty tender</td><td>- {rs(receipt.billingLoyaltyTenderPaise)}</td></tr>}
 {receipt.billingTaxablePaise!=null&&<tr><td>Taxable value (billing snapshot)</td><td>{rs(receipt.billingTaxablePaise)}</td></tr>}
 {receipt.billingTaxPaise!=null&&<tr><td>Included tax {receipt.billingRatePercent!=null?`(${receipt.billingRatePercent}%)`:""}</td><td>{rs(receipt.billingTaxPaise)}</td></tr>}
 {receipt.billingGrossPaise!=null&&<tr><td>Total consideration (cash + loyalty)</td><td>{rs(receipt.billingGrossPaise)}</td></tr>}
 <tr className={styles.total}><th>Actual payment collected</th><th>{rs(receipt.paymentAmountPaise)}</th></tr>
 <tr><td>Processed refunds</td><td>{rs(receipt.refunds.filter(r=>r.status==="PROCESSED").reduce((sum,r)=>sum+(r.amountPaise||0),0))}</td></tr>
 <tr className={styles.total}><th>Net payment retained</th><th>{rs(receipt.paymentAmountPaise-receipt.refunds.filter(r=>r.status==="PROCESSED").reduce((sum,r)=>sum+(r.amountPaise||0),0))}</th></tr>
 </tbody></table><p>This payment receipt is not a GST tax invoice. GST components are displayed only in an issued tax document below.</p></section>
 {receipt.documents.length===0&&<section className={styles.receipt}><h2>GST document not yet issued</h2><p>Your payment receipt is available above. A GST invoice can be requested after the venue configures the appropriate tax rules and you save your billing address.</p><p><a href="/billing-profile">Update my billing address / GSTIN</a></p><button type="button" disabled={busy} onClick={()=>void requestInvoice()}>{busy?"Checking…":"Request GST invoice (no email)"}</button></section>}
 {receipt.documents.map(doc=>{let d:Record<string,unknown>={};try{d=JSON.parse(doc.details||"{}")}catch{}const seller=(d.supplier||{}) as Record<string,string>;return <article key={doc.documentNumber} className={styles.receipt}>
 <header><span>{doc.kind==="INVOICE"?"TAX INVOICE":"GST CREDIT NOTE"}</span><h2>{doc.documentNumber}</h2><button type="button" onClick={()=>void download(doc.documentNumber)}>Download PDF</button><p>Issued: {doc.issuedAt?new Date(doc.issuedAt).toLocaleDateString("en-IN"):"—"}</p></header>
 <div className={styles.cols}>
 <div><h3>Supplier</h3><strong>{seller.legalName||"Jnapanam Sports Zone"}</strong><p>{seller.registeredAddress}</p><p>GSTIN: {seller.gstin}</p><p>State code: {seller.stateCode}</p><p>SAC: {seller.serviceAccountingCode}</p></div>
 <div><h3>Bill to</h3><strong>{String(d.recipientName||"Customer")}</strong><p>{String(d.recipientAddress||"")}</p><p>GSTIN: {String(d.recipientGstin||"Unregistered")}</p><p>Recipient state: {String(d.recipientStateCode||"—")}</p><p>Place of supply: {String(d.placeOfSupplyStateCode||"—")}</p></div></div>
 <p>Service: {String(d.service||d.reason||"Sports facility booking")}</p>
 {doc.kind==="CREDIT_NOTE"&&<p>Against invoice: <strong>{String(d.originalInvoice||"—")}</strong> · Refund: {String(d.refundId||"—")}</p>}
 <table className={styles.table}><thead><tr><th>Description</th><th>Amount</th></tr></thead><tbody>
 {d.originalPricePaise!=null&&<tr><td>Original service value</td><td>{rs(d.originalPricePaise)}</td></tr>}
 {d.promotionCode!=null&&String(d.promotionCode).trim()!==""&&<tr><td>Offer / coupon code</td><td>{String(d.promotionCode)}</td></tr>}
 {d.couponDiscountPaise!=null&&<tr><td>Coupon / promotional discount</td><td>- {rs(d.couponDiscountPaise)}</td></tr>}
 {d.loyaltyPointsUsed!=null&&<tr><td>Loyalty redeemed</td><td>{String(d.loyaltyPointsUsed)} points</td></tr>}
 {d.loyaltyTenderPaise!=null&&<tr><td>Paid using loyalty value</td><td>{rs(d.loyaltyTenderPaise)}</td></tr>}
 <tr><td>Taxable value</td><td>{rs(doc.taxablePaise)}</td></tr>
 <tr><td>CGST {d.componentMode==="CGST_SGST"&&d.taxRatePercent? `(${Number(d.taxRatePercent)/2}%)`:""}</td><td>{rs(d.cgstPaise)}</td></tr>
 <tr><td>SGST {d.componentMode==="CGST_SGST"&&d.taxRatePercent? `(${Number(d.taxRatePercent)/2}%)`:""}</td><td>{rs(d.sgstPaise)}</td></tr>
 <tr><td>IGST {d.componentMode==="IGST"&&d.taxRatePercent? `(${d.taxRatePercent}%)`:""}</td><td>{rs(d.igstPaise)}</td></tr>
 <tr className={styles.total}><th>{doc.kind==="CREDIT_NOTE"?"Total credit":"Invoice total"}</th><th>{rs(doc.totalPaise)}</th></tr>
 {doc.kind==="INVOICE"&&d.customerCashPaidPaise!=null&&<tr className={styles.total}><th>Customer cash paid</th><th>{rs(d.customerCashPaidPaise)}</th></tr>}</tbody></table>
 <footer>Authorised signatory: {seller.signatory||"—"} · {doc.state}</footer>
 </article>})}
 </main>;
}
