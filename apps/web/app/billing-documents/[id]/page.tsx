"use client";
import {useEffect,useState} from "react";
import {useParams} from "next/navigation";
import {api,apiBlob} from "@/lib/api";
import styles from "./billing.module.css";
type Doc={kind:string;state:string;documentNumber:string;taxablePaise:number;taxPaise:number;totalPaise:number;details:string;issuedAt:string};
type Receipt={receiptNumber:string;status:string;paymentAmountPaise:number;bookingNumber?:string;discountPaise:number;documents:Doc[];refunds:{id:string;amountPaise:number;status:string}[]};
const rs=(paise:unknown)=>new Intl.NumberFormat("en-IN",{style:"currency",currency:"INR"}).format(Number(paise||0)/100);
export default function BillingDocumentsPage(){
 const params=useParams();const id=String(params.id||"");const [receipt,setReceipt]=useState<Receipt|null>(null),[error,setError]=useState("");
 async function download(number:string){try{const blob=await apiBlob(`/api/v1/payments/${id}/billing/${encodeURIComponent(number)}/pdf`);const url=URL.createObjectURL(blob);const anchor=document.createElement("a");anchor.href=url;anchor.download=`jnapanam-${number}.pdf`;document.body.appendChild(anchor);anchor.click();anchor.remove();setTimeout(()=>URL.revokeObjectURL(url),15000)}catch(e){setError(e instanceof Error?e.message:"Download failed")}}
 useEffect(()=>{if(!id)return;api<Receipt>(`/api/v1/payments/${id}/billing`).then(setReceipt).catch(e=>setError(e instanceof Error?e.message:"Unable to load receipt"))},[id]);
 if(error)return <main className={styles.paper}><p role="alert">{error}</p></main>;
 if(!receipt)return <main className={styles.paper}><p role="status">Loading billing documents…</p></main>;
 return <main className={styles.paper}>
 <header className={styles.toolbar}><h1>Jnapanam Billing Documents</h1><button type="button" onClick={()=>window.print()}>Print / Save as PDF</button></header>
 <section className={styles.receipt}><h2>Payment Receipt</h2><p>Receipt: <strong>{receipt.receiptNumber}</strong> · Booking: {receipt.bookingNumber||"—"}</p><p>Payment status: {receipt.status}</p><h3>Amount collected: {rs(receipt.paymentAmountPaise)}</h3><p>Promotions discount: {rs(receipt.discountPaise)}</p><p>Processed refunds: {receipt.refunds.filter(r=>r.status==="PROCESSED").map(r=>rs(r.amountPaise)).join(", ")||"None"}</p></section>
 {receipt.documents.length===0&&<section className={styles.receipt}><h2>GST document not yet issued</h2><p>An ordinary payment receipt is available above. GST invoices are issued only after supplier configuration, applicable tax review and administrator approval.</p></section>}
 {receipt.documents.map(doc=>{let d:Record<string,unknown>={};try{d=JSON.parse(doc.details||"{}")}catch{}const seller=(d.supplier||{}) as Record<string,string>;return <article key={doc.documentNumber} className={styles.receipt}>
 <header><span>{doc.kind==="INVOICE"?"TAX INVOICE":"GST CREDIT NOTE"}</span><h2>{doc.documentNumber}</h2><button type="button" onClick={()=>void download(doc.documentNumber)}>Download PDF</button><p>Issued: {doc.issuedAt?new Date(doc.issuedAt).toLocaleDateString("en-IN"):"—"}</p></header>
 <div className={styles.cols}>
 <div><h3>Supplier</h3><strong>{seller.legalName||"Jnapanam Sports Zone"}</strong><p>{seller.registeredAddress}</p><p>GSTIN: {seller.gstin}</p><p>State code: {seller.stateCode}</p><p>SAC: {seller.serviceAccountingCode}</p></div>
 <div><h3>Bill to</h3><strong>{String(d.recipientName||"Customer")}</strong><p>{String(d.recipientAddress||"")}</p><p>GSTIN: {String(d.recipientGstin||"Unregistered")}</p><p>Recipient state: {String(d.recipientStateCode||"—")}</p><p>Place of supply: {String(d.placeOfSupplyStateCode||"—")}</p></div></div>
 <p>Service: {String(d.service||d.reason||"Sports facility booking")}</p>
 {doc.kind==="CREDIT_NOTE"&&<p>Against invoice: <strong>{String(d.originalInvoice||"—")}</strong> · Refund: {String(d.refundId||"—")}</p>}
 <table className={styles.table}><thead><tr><th>Description</th><th>Amount</th></tr></thead><tbody>
 <tr><td>Taxable value</td><td>{rs(doc.taxablePaise)}</td></tr>
 <tr><td>CGST {d.componentMode==="CGST_SGST"&&d.taxRatePercent? `(${Number(d.taxRatePercent)/2}%)`:""}</td><td>{rs(d.cgstPaise)}</td></tr>
 <tr><td>SGST {d.componentMode==="CGST_SGST"&&d.taxRatePercent? `(${Number(d.taxRatePercent)/2}%)`:""}</td><td>{rs(d.sgstPaise)}</td></tr>
 <tr><td>IGST {d.componentMode==="IGST"&&d.taxRatePercent? `(${d.taxRatePercent}%)`:""}</td><td>{rs(d.igstPaise)}</td></tr>
 <tr className={styles.total}><th>Total</th><th>{rs(doc.totalPaise)}</th></tr></tbody></table>
 <footer>Authorised signatory: {seller.signatory||"—"} · {doc.state}</footer>
 </article>})}
 </main>;
}
