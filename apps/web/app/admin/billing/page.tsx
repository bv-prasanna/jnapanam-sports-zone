"use client";
import {useCallback,useEffect,useState} from "react";
import {api} from "@/lib/api";
import styles from "../pricing/pricing.module.css";
type Profile={legalName:string;gstin:string;registeredAddress:string;stateCode:string;serviceAccountingCode:string;signatory:string};
type Payment={id:string;receiptNumber:string;purpose:string;status:string;amountPaise:number};
type Refund={id:string;amountPaise:number;status:string};
type TaxSettings={couponTaxTreatment:"REVIEW_REQUIRED"|"INVOICE_DISCOUNT";loyaltyTaxTreatment:"REVIEW_REQUIRED"|"PAYMENT_TENDER"|"INVOICE_DISCOUNT";eInvoiceApplicable:boolean;invoiceEmailEnabled:boolean};
type PurposeTax={purpose:string;taxRatePercent:number;active:boolean;placeOfSupplyStateCode?:string;autoIssueEnabled?:boolean};
type Receipt={receiptNumber:string;status:string;paymentAmountPaise:number;bookingNumber?:string;documents:{documentNumber:string;kind:string;state:string}[];refunds:Refund[]};
const states=[["29","Karnataka"],["33","Tamil Nadu"],["36","Telangana"],["32","Kerala"],["27","Maharashtra"],["07","Delhi"],["37","Andhra Pradesh"],["24","Gujarat"],["09","Uttar Pradesh"],["08","Rajasthan"],["19","West Bengal"],["06","Haryana"],["03","Punjab"],["10","Bihar"],["21","Odisha"],["23","Madhya Pradesh"],["22","Chhattisgarh"],["20","Jharkhand"],["18","Assam"],["30","Goa"],["01","Jammu & Kashmir"],["02","Himachal Pradesh"],["05","Uttarakhand"],["04","Chandigarh"],["11","Sikkim"],["12","Arunachal Pradesh"],["13","Nagaland"],["14","Manipur"],["15","Mizoram"],["16","Tripura"],["17","Meghalaya"],["26","Dadra and Nagar Haveli and Daman and Diu"],["31","Lakshadweep"],["34","Puducherry"],["35","Andaman & Nicobar Islands"],["38","Ladakh"]];
const defaultProfile:Profile={legalName:"Jnapanam Sports Zone",gstin:"",registeredAddress:"",stateCode:"29",serviceAccountingCode:"",signatory:""};
export default function BillingAdmin(){
 const [profile,setProfile]=useState<Profile>(defaultProfile);
 const [taxSettings,setTaxSettings]=useState<TaxSettings>({couponTaxTreatment:"REVIEW_REQUIRED",loyaltyTaxTreatment:"REVIEW_REQUIRED",eInvoiceApplicable:false,invoiceEmailEnabled:false});
 const [purposeRates,setPurposeRates]=useState<Record<string,{taxRatePercent:string;active:boolean;placeOfSupplyStateCode:string;autoIssueEnabled:boolean}>>({});
 const [payments,setPayments]=useState<Payment[]>([]);
 const [selected,setSelected]=useState("");
 const [receipt,setReceipt]=useState<Receipt|null>(null);
 const [recipientAddress,setRecipientAddress]=useState("");
 const [recipientStateCode,setRecipientStateCode]=useState("29");
 const [placeOfSupplyStateCode,setPlaceOfSupplyStateCode]=useState("29");
 const [recipientGstin,setRecipientGstin]=useState("");
 const [componentMode,setComponentMode]=useState("CGST_SGST");
 const [error,setError]=useState(""),[success,setSuccess]=useState(""),[busy,setBusy]=useState(false);
 const load=useCallback(async()=>{try{const[p,list,settings,policies]=await Promise.all([api<Partial<Profile>>("/api/v1/admin/billing/profile"),api<Payment[]>("/api/v1/admin/payments"),api<Partial<TaxSettings>>("/api/v1/admin/billing/tax-treatments"),api<PurposeTax[]>("/api/v1/admin/billing/purpose-taxes")]);setProfile({...defaultProfile,...p,gstin:p.gstin??""});setPayments(list.filter(x=>["PAID","PARTIALLY_REFUNDED","REFUNDED"].includes(x.status)));setTaxSettings(v=>({...v,...settings}));const rates:Record<string,{taxRatePercent:string;active:boolean;placeOfSupplyStateCode:string;autoIssueEnabled:boolean}>={};for(const x of policies)rates[x.purpose]={taxRatePercent:String(x.taxRatePercent),active:x.active,placeOfSupplyStateCode:x.placeOfSupplyStateCode||"",autoIssueEnabled:!!x.autoIssueEnabled};setPurposeRates(rates)}catch(e){setError(e instanceof Error?e.message:"Unable to load billing settings")}},[]);
 useEffect(()=>{void load()},[load]);
 async function loadReceipt(id:string){setSelected(id);setReceipt(null);if(!id)return;try{setError("");setReceipt(await api<Receipt>(`/api/v1/admin/billing/payments/${id}`))}catch(e){setError(e instanceof Error?e.message:"Unable to load receipt")}}
 async function saveProfile(event:React.FormEvent){event.preventDefault();try{setBusy(true);setError("");await api("/api/v1/admin/billing/profile",{method:"PUT",body:JSON.stringify({...profile,gstin:profile.gstin||null})});setSuccess("Merchant settings saved. Missing GST details can be completed later; tax invoices remain blocked until verified fields are present.");}catch(e){setError(e instanceof Error?e.message:"Could not save supplier details")}finally{setBusy(false)}}

 async function saveTreatments(event:React.FormEvent){event.preventDefault();try{setBusy(true);setError("");await api("/api/v1/admin/billing/tax-treatments",{method:"PUT",body:JSON.stringify(taxSettings)});setSuccess("Tax treatments and email delivery preferences saved. Existing payments remain unchanged.")}catch(e){setError(e instanceof Error?e.message:"Unable to save tax settings")}finally{setBusy(false)}}
 async function savePurpose(purpose:string){const row=purposeRates[purpose]||{taxRatePercent:"0",active:false,placeOfSupplyStateCode:"",autoIssueEnabled:false};try{setBusy(true);setError("");await api(`/api/v1/admin/billing/purpose-taxes/${purpose}`,{method:"PUT",body:JSON.stringify({taxRatePercent:Number(row.taxRatePercent),active:row.active,placeOfSupplyStateCode:row.placeOfSupplyStateCode||null,autoIssueEnabled:row.autoIssueEnabled})});setSuccess(purpose+" tax settings saved for future payments.")}catch(e){setError(e instanceof Error?e.message:"Unable to save rate")}finally{setBusy(false)}}
 async function issue(){if(!selected||!window.confirm("Confirm that supplier GSTIN, tax mode, SAC and place of supply have been verified. Issue a permanent invoice?"))return;try{setBusy(true);setError("");const result=await api<{documentNumber:string}>(`/api/v1/admin/billing/invoices/${selected}/issue`,{method:"POST",body:JSON.stringify({recipientAddress,recipientStateCode,placeOfSupplyStateCode,recipientGstin:recipientGstin||null,componentMode})});setSuccess("Invoice issued: "+result.documentNumber);await loadReceipt(selected)}catch(e){setError(e instanceof Error?e.message:"Unable to issue invoice")}finally{setBusy(false)}}
 async function credit(refundId:string){if(!window.confirm("Confirm this processed refund qualifies for an issued GST credit note?"))return;try{setBusy(true);setError("");const result=await api<{documentNumber:string}>(`/api/v1/admin/billing/refunds/${refundId}/credit-note`,{method:"POST"});setSuccess("Credit note issued: "+result.documentNumber);await loadReceipt(selected)}catch(e){setError(e instanceof Error?e.message:"Unable to issue credit note")}finally{setBusy(false)}}
 return <main className={styles.page}>
 <header className={styles.head}><div><p className={styles.eyebrow}>FINANCE OPERATIONS</p><h1>GST Billing & Refund Documents</h1><p>Configure legal supplier details, review paid bookings, and issue approved invoices or credit notes.</p></div></header>
 {error&&<p role="alert" className={styles.error}>{error}</p>}{success&&<p role="status" className={styles.success}>{success}</p>}
 <section className={styles.editor}><h2>Merchant and GST configuration</h2><p className={styles.help}>Default draft: Jnapanam Sports Zone, Karnataka. GSTIN, registered legal name/address, SAC and signatory must be verified when available. Save this draft now; no GST rate or registration is assumed.</p><form onSubmit={saveProfile} className={styles.form}>
 <label>Legal / trading name (verify registered name before invoicing)<input maxLength={160} value={profile.legalName} onChange={e=>setProfile(v=>({...v,legalName:e.target.value}))}/></label>
 <label>GSTIN (add when available)<input maxLength={15} pattern="[0-9]{2}[A-Z0-9]{13}" value={profile.gstin} onChange={e=>setProfile(v=>({...v,gstin:e.target.value.toUpperCase()}))}/></label>
 <label>Registered address (add when available)<textarea placeholder="Enter registered business address when verified" value={profile.registeredAddress} onChange={e=>setProfile(v=>({...v,registeredAddress:e.target.value}))}/></label>
 <label>Registered state<select value={profile.stateCode} onChange={e=>setProfile(v=>({...v,stateCode:e.target.value}))}>{states.map(([id,name])=><option key={id} value={id}>{name}</option>)}</select></label>
 <label>Service Accounting Code (SAC, after accountant review)<input maxLength={12} placeholder="Confirm with your tax advisor" value={profile.serviceAccountingCode} onChange={e=>setProfile(v=>({...v,serviceAccountingCode:e.target.value}))}/></label>
 <label>Authorised signatory<input value={profile.signatory} onChange={e=>setProfile(v=>({...v,signatory:e.target.value}))}/></label>
 <div className={styles.actions}><button className={styles.primary} disabled={busy} type="submit">Save merchant settings</button></div></form></section>
 <section className={styles.editor}><h2>GST calculation and automatic email</h2>
 <form className={styles.form} onSubmit={saveTreatments}>
 <label>Coupon tax treatment<select value={taxSettings.couponTaxTreatment} onChange={e=>setTaxSettings(v=>({...v,couponTaxTreatment:e.target.value as TaxSettings["couponTaxTreatment"]}))}><option value="REVIEW_REQUIRED">Manual review required</option><option value="INVOICE_DISCOUNT">Eligible pre-supply invoice discount</option></select></label>
 <label>Loyalty-point treatment<select value={taxSettings.loyaltyTaxTreatment} onChange={e=>setTaxSettings(v=>({...v,loyaltyTaxTreatment:e.target.value as TaxSettings["loyaltyTaxTreatment"]}))}><option value="REVIEW_REQUIRED">Manual review required</option><option value="PAYMENT_TENDER">Wallet settlement (does not reduce taxable value)</option><option value="INVOICE_DISCOUNT">Eligible invoice discount (reduces taxable value)</option></select></label>
 <label className={styles.check}><input type="checkbox" checked={taxSettings.invoiceEmailEnabled} onChange={e=>setTaxSettings(v=>({...v,invoiceEmailEnabled:e.target.checked}))}/> Email issued invoice PDFs automatically (currently OFF by default)</label><p className={styles.help}>No automatic invoice email is required for normal bookings. Customers can log in, review their own payments, and download receipts or issued GST invoices on demand. Enable email only when you decide to use it.</p>
 <label className={styles.check}><input type="checkbox" checked={taxSettings.eInvoiceApplicable} onChange={e=>setTaxSettings(v=>({...v,eInvoiceApplicable:e.target.checked}))}/> Jnapanam is subject to GST e-invoicing/IRN requirements</label>
 <p className={styles.help}>Select tax treatments only after accountant approval. Changes apply to new payments, not existing snapshots. E-invoice applicability must be verified before issuing documents.</p>
 <div className={styles.actions}><button type="submit" className={styles.primary} disabled={busy}>Save billing policy</button></div></form>
 <h3>Tax and auto-invoice policies for each purchase type</h3>
 <p className={styles.help}>Membership, training and tournament rates are extracted from advertised tax-inclusive prices. Booking tax is quoted from the slot rule. Auto-issuance must be approved for the relevant service and place-of-supply.</p>
 {["BOOKING","MEMBERSHIP","TRAINING","TOURNAMENT"].map(purpose=>{const value=purposeRates[purpose]||{taxRatePercent:"0",active:false,placeOfSupplyStateCode:"",autoIssueEnabled:false};return <div key={purpose} className={`${styles.form} admin-settings-section`}>
 <strong>{purpose.charAt(0)+purpose.slice(1).toLowerCase()}</strong>
 <label>GST rate (%)<input type="number" min="0" max="100" step="0.001" value={value.taxRatePercent} onChange={e=>setPurposeRates(prev=>({...prev,[purpose]:{...value,taxRatePercent:e.target.value}}))}/></label>
 <label className={styles.check}><input type="checkbox" checked={value.active} onChange={e=>setPurposeRates(prev=>({...prev,[purpose]:{...value,active:e.target.checked}}))}/> Activate for new payments</label>
 <label>Approved GST place of supply<select value={value.placeOfSupplyStateCode} onChange={e=>setPurposeRates(prev=>({...prev,[purpose]:{...value,placeOfSupplyStateCode:e.target.value}}))}><option value="">Manual invoice only / not confirmed</option>{states.map(([code,name])=><option key={code} value={code}>{name}</option>)}</select></label>
 <label className={styles.check}><input type="checkbox" checked={value.autoIssueEnabled} onChange={e=>setPurposeRates(prev=>({...prev,[purpose]:{...value,autoIssueEnabled:e.target.checked}}))}/> Automatically issue invoices after captured payment, only when customer billing details are complete</label>
 <button type="button" disabled={busy} onClick={()=>void savePurpose(purpose)}>Save rate</button></div>})}
 </section>
<section className={styles.editor}><h2>Issue tax invoices and credit notes</h2><div className={styles.form}>
 <label>Select paid booking<select value={selected} onChange={e=>void loadReceipt(e.target.value)}><option value="">Select payment receipt</option>{payments.map(p=><option key={p.id} value={p.id}>{p.receiptNumber} · ₹{(p.amountPaise/100).toFixed(2)} · {p.status}</option>)}</select></label>
 {receipt&&<><p>Booking: <strong>{receipt.bookingNumber||"—"}</strong> · Collected ₹{(receipt.paymentAmountPaise/100).toFixed(2)}</p>
 <label>Recipient address<input value={recipientAddress} onChange={e=>setRecipientAddress(e.target.value)} placeholder="Full address to appear on invoice"/></label>
 <label>Recipient address state<select value={recipientStateCode} onChange={e=>setRecipientStateCode(e.target.value)}>{states.map(([id,name])=><option key={id} value={id}>{name}</option>)}</select></label>
 <label>Recipient GSTIN (registered buyers only)<input value={recipientGstin} maxLength={15} onChange={e=>setRecipientGstin(e.target.value.toUpperCase())}/></label>
 <label>GST place of supply<select value={placeOfSupplyStateCode} onChange={e=>setPlaceOfSupplyStateCode(e.target.value)}>{states.map(([id,name])=><option key={id} value={id}>{name}</option>)}</select></label>
 <label>Tax component allocation<select value={componentMode} onChange={e=>setComponentMode(e.target.value)}><option value="CGST_SGST">CGST + SGST (intra-state, when applicable)</option><option value="IGST">IGST (inter-state, when applicable)</option></select></label>
 <p className={styles.help}>Verify place-of-supply treatment and discount/loyalty allocation before issuing. Documents cannot be edited after issue.</p>
 <button className={styles.primary} type="button" disabled={busy||!!receipt.documents?.some(d=>d.kind==="INVOICE")} onClick={()=>void issue()}>Issue GST invoice</button>
 <a href={`/billing-documents/${selected}`} target="_blank" rel="noopener noreferrer">View / print receipt and issued documents</a><h3>Issued documents</h3>{receipt.documents?.length?receipt.documents.map(d=><p key={d.documentNumber}>{d.kind.replace("_"," ")}: {d.documentNumber} · {d.state}</p>):<p>No documents issued.</p>}
 <h3>Processed refunds</h3>{receipt.refunds?.length?receipt.refunds.map(r=><p key={r.id}>₹{(r.amountPaise/100).toFixed(2)} · {r.status} {r.status==="PROCESSED"&&<button disabled={busy} type="button" onClick={()=>void credit(r.id)}>Issue credit note</button>}</p>):<p>No refunds.</p>}
 </>}</div></section></main>;
}
