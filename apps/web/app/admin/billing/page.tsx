"use client";
import {useCallback,useEffect,useState} from "react";
import {api} from "@/lib/api";
import styles from "../pricing/pricing.module.css";
type Profile={legalName:string;gstin:string;registeredAddress:string;stateCode:string;serviceAccountingCode:string;signatory:string};
type Payment={id:string;receiptNumber:string;purpose:string;status:string;amountPaise:number};
type Refund={id:string;amountPaise:number;status:string};
type Receipt={receiptNumber:string;status:string;paymentAmountPaise:number;bookingNumber?:string;documents:{documentNumber:string;kind:string;state:string}[];refunds:Refund[]};
const states=[["29","Karnataka"],["33","Tamil Nadu"],["36","Telangana"],["32","Kerala"],["27","Maharashtra"],["07","Delhi"],["37","Andhra Pradesh"],["24","Gujarat"],["09","Uttar Pradesh"],["08","Rajasthan"],["19","West Bengal"],["06","Haryana"],["03","Punjab"],["10","Bihar"],["21","Odisha"],["23","Madhya Pradesh"],["22","Chhattisgarh"],["20","Jharkhand"],["18","Assam"],["30","Goa"],["01","Jammu & Kashmir"],["02","Himachal Pradesh"],["05","Uttarakhand"],["04","Chandigarh"],["11","Sikkim"],["12","Arunachal Pradesh"],["13","Nagaland"],["14","Manipur"],["15","Mizoram"],["16","Tripura"],["17","Meghalaya"],["26","Dadra and Nagar Haveli and Daman and Diu"],["31","Lakshadweep"],["34","Puducherry"],["35","Andaman & Nicobar Islands"],["38","Ladakh"]];
const defaultProfile:Profile={legalName:"",gstin:"",registeredAddress:"",stateCode:"29",serviceAccountingCode:"",signatory:""};
export default function BillingAdmin(){
 const [profile,setProfile]=useState<Profile>(defaultProfile);
 const [payments,setPayments]=useState<Payment[]>([]);
 const [selected,setSelected]=useState("");
 const [receipt,setReceipt]=useState<Receipt|null>(null);
 const [recipientAddress,setRecipientAddress]=useState("");
 const [recipientStateCode,setRecipientStateCode]=useState("29");
 const [recipientGstin,setRecipientGstin]=useState("");
 const [componentMode,setComponentMode]=useState("CGST_SGST");
 const [error,setError]=useState(""),[success,setSuccess]=useState(""),[busy,setBusy]=useState(false);
 const load=useCallback(async()=>{try{const[p,list]=await Promise.all([api<Partial<Profile>>("/api/v1/admin/billing/profile"),api<Payment[]>("/api/v1/admin/payments")]);setProfile({...defaultProfile,...p,gstin:p.gstin??""});setPayments(list.filter(x=>x.purpose==="BOOKING"&&["PAID","PARTIALLY_REFUNDED","REFUNDED"].includes(x.status)))}catch(e){setError(e instanceof Error?e.message:"Unable to load billing settings")}},[]);
 useEffect(()=>{void load()},[load]);
 async function loadReceipt(id:string){setSelected(id);setReceipt(null);if(!id)return;try{setError("");setReceipt(await api<Receipt>(`/api/v1/admin/billing/payments/${id}`))}catch(e){setError(e instanceof Error?e.message:"Unable to load receipt")}}
 async function saveProfile(event:React.FormEvent){event.preventDefault();try{setBusy(true);setError("");await api("/api/v1/admin/billing/profile",{method:"PUT",body:JSON.stringify({...profile,gstin:profile.gstin||null})});setSuccess("Supplier tax profile saved.");}catch(e){setError(e instanceof Error?e.message:"Could not save supplier details")}finally{setBusy(false)}}
 async function issue(){if(!selected||!window.confirm("Confirm that supplier GSTIN, tax mode, SAC and place of supply have been verified. Issue a permanent invoice?"))return;try{setBusy(true);setError("");const result=await api<{documentNumber:string}>(`/api/v1/admin/billing/invoices/${selected}/issue`,{method:"POST",body:JSON.stringify({recipientAddress,recipientStateCode,recipientGstin:recipientGstin||null,componentMode})});setSuccess("Invoice issued: "+result.documentNumber);await loadReceipt(selected)}catch(e){setError(e instanceof Error?e.message:"Unable to issue invoice")}finally{setBusy(false)}}
 async function credit(refundId:string){if(!window.confirm("Confirm this processed refund qualifies for an issued GST credit note?"))return;try{setBusy(true);setError("");const result=await api<{documentNumber:string}>(`/api/v1/admin/billing/refunds/${refundId}/credit-note`,{method:"POST"});setSuccess("Credit note issued: "+result.documentNumber);await loadReceipt(selected)}catch(e){setError(e instanceof Error?e.message:"Unable to issue credit note")}finally{setBusy(false)}}
 return <main className={styles.page}>
 <header className={styles.head}><div><p className={styles.eyebrow}>FINANCE OPERATIONS</p><h1>GST Billing & Refund Documents</h1><p>Configure legal supplier details, review paid bookings, and issue approved invoices or credit notes.</p></div></header>
 {error&&<p role="alert" className={styles.error}>{error}</p>}{success&&<p role="status" className={styles.success}>{success}</p>}
 <section className={styles.editor}><h2>Supplier tax profile</h2><form onSubmit={saveProfile} className={styles.form}>
 <label>Registered legal name<input required maxLength={160} value={profile.legalName} onChange={e=>setProfile(v=>({...v,legalName:e.target.value}))}/></label>
 <label>GSTIN<input required maxLength={15} pattern="[0-9]{2}[A-Z0-9]{13}" value={profile.gstin} onChange={e=>setProfile(v=>({...v,gstin:e.target.value.toUpperCase()}))}/></label>
 <label>Registered address<textarea required value={profile.registeredAddress} onChange={e=>setProfile(v=>({...v,registeredAddress:e.target.value}))}/></label>
 <label>Registered state<select value={profile.stateCode} onChange={e=>setProfile(v=>({...v,stateCode:e.target.value}))}>{states.map(([id,name])=><option key={id} value={id}>{name}</option>)}</select></label>
 <label>Service Accounting Code (SAC)<input required maxLength={12} placeholder="Confirm with your tax advisor" value={profile.serviceAccountingCode} onChange={e=>setProfile(v=>({...v,serviceAccountingCode:e.target.value}))}/></label>
 <label>Authorised signatory<input required value={profile.signatory} onChange={e=>setProfile(v=>({...v,signatory:e.target.value}))}/></label>
 <div className={styles.actions}><button className={styles.primary} disabled={busy} type="submit">Save tax profile</button></div></form></section>
 <section className={styles.editor}><h2>Issue booking invoice</h2><div className={styles.form}>
 <label>Select paid booking<select value={selected} onChange={e=>void loadReceipt(e.target.value)}><option value="">Select payment receipt</option>{payments.map(p=><option key={p.id} value={p.id}>{p.receiptNumber} · ₹{(p.amountPaise/100).toFixed(2)} · {p.status}</option>)}</select></label>
 {receipt&&<><p>Booking: <strong>{receipt.bookingNumber||"—"}</strong> · Collected ₹{(receipt.paymentAmountPaise/100).toFixed(2)}</p>
 <label>Recipient address<input value={recipientAddress} onChange={e=>setRecipientAddress(e.target.value)} placeholder="Full address to appear on invoice"/></label>
 <label>Recipient state / place of supply<select value={recipientStateCode} onChange={e=>setRecipientStateCode(e.target.value)}>{states.map(([id,name])=><option key={id} value={id}>{name}</option>)}</select></label>
 <label>Recipient GSTIN (registered buyers only)<input value={recipientGstin} maxLength={15} onChange={e=>setRecipientGstin(e.target.value.toUpperCase())}/></label>
 <label>Tax component allocation<select value={componentMode} onChange={e=>setComponentMode(e.target.value)}><option value="CGST_SGST">CGST + SGST (intra-state, when applicable)</option><option value="IGST">IGST (inter-state, when applicable)</option></select></label>
 <p className={styles.help}>Verify place-of-supply treatment and discount/loyalty allocation before issuing. Documents cannot be edited after issue.</p>
 <button className={styles.primary} type="button" disabled={busy||!!receipt.documents?.some(d=>d.kind==="INVOICE")} onClick={()=>void issue()}>Issue GST invoice</button>
 <h3>Issued documents</h3>{receipt.documents?.length?receipt.documents.map(d=><p key={d.documentNumber}>{d.kind.replace("_"," ")}: {d.documentNumber} · {d.state}</p>):<p>No documents issued.</p>}
 <h3>Processed refunds</h3>{receipt.refunds?.length?receipt.refunds.map(r=><p key={r.id}>₹{(r.amountPaise/100).toFixed(2)} · {r.status} {r.status==="PROCESSED"&&<button disabled={busy} type="button" onClick={()=>void credit(r.id)}>Issue credit note</button>}</p>):<p>No refunds.</p>}
 </>}</div></section></main>;
}
