"use client";
import {useEffect,useState} from "react";
import {api} from "@/lib/api";
import {SiteHeader} from "@/components/site-header";
import {PortalSideNavigation} from "@/components/portal-side-navigation";
import shell from "@/components/portal-shell.module.css";
import styles from "./billing-profile.module.css";
type Details={recipientAddress:string;stateCode:string;gstin:string};
const states=[["29","Karnataka"],["37","Andhra Pradesh"],["33","Tamil Nadu"],["32","Kerala"],["36","Telangana"],["27","Maharashtra"],["07","Delhi"],["24","Gujarat"],["09","Uttar Pradesh"],["08","Rajasthan"],["19","West Bengal"],["06","Haryana"],["03","Punjab"],["10","Bihar"],["21","Odisha"],["23","Madhya Pradesh"],["22","Chhattisgarh"],["20","Jharkhand"],["18","Assam"],["30","Goa"],["01","Jammu & Kashmir"],["02","Himachal Pradesh"],["05","Uttarakhand"],["04","Chandigarh"],["11","Sikkim"],["12","Arunachal Pradesh"],["13","Nagaland"],["14","Manipur"],["15","Mizoram"],["16","Tripura"],["17","Meghalaya"],["26","Dadra and Nagar Haveli and Daman and Diu"],["31","Lakshadweep"],["34","Puducherry"],["35","Andaman & Nicobar Islands"],["38","Ladakh"]];
export default function BillingProfile(){
 const [details,setDetails]=useState<Details>({recipientAddress:"",stateCode:"29",gstin:""});
 const [busy,setBusy]=useState(false),[error,setError]=useState(""),[saved,setSaved]=useState("");
 useEffect(()=>{api<Partial<Details>>("/api/v1/billing/me").then(v=>setDetails(d=>({...d,...v,gstin:v.gstin||""}))).catch(e=>setError(e instanceof Error?e.message:"Unable to load billing details"))},[]);
 async function submit(e:React.FormEvent){e.preventDefault();try{setBusy(true);setError("");await api("/api/v1/billing/me",{method:"PUT",body:JSON.stringify({...details,gstin:details.gstin||null})});setSaved("Billing details saved. Future eligible invoices can use this information.")}catch(e){setError(e instanceof Error?e.message:"Unable to save billing details")}finally{setBusy(false)}}
 return <><SiteHeader/><div className={shell.shell}><PortalSideNavigation active="payments"/><main className={styles.page}>
  <header className={styles.heading}><div><span className={styles.eyebrow}>ACCOUNT & BILLING</span><h1>Billing details</h1><p>These details appear on invoices for new purchases. Existing invoices will not change.</p></div><a href="/dashboard?section=payments">← My payments</a></header>
  {error&&<p role="alert" className={styles.error}>{error}</p>}{saved&&<p role="status" className={styles.success}>{saved}</p>}
  <form onSubmit={submit} className={styles.form}>
   <label >Full billing address<textarea required maxLength={400} rows={4} value={details.recipientAddress} onChange={e=>setDetails(d=>({...d,recipientAddress:e.target.value}))}/></label>
   <label >Your billing address state<select value={details.stateCode} onChange={e=>setDetails(d=>({...d,stateCode:e.target.value}))}>{states.map(([code,name])=><option key={code} value={code}>{name}</option>)}</select></label>
   <label >GSTIN (optional for unregistered customers)<input maxLength={15} pattern="[0-9]{2}[A-Z0-9]{13}" placeholder="Enter GSTIN only if registered" value={details.gstin} onChange={e=>setDetails(d=>({...d,gstin:e.target.value.toUpperCase()}))}/></label>
   <button type="submit" disabled={busy} className={styles.save}>{busy?"Saving…":"Save billing details"}</button>
  </form><p className={styles.backLink}><a href="/dashboard?section=payments">View payments and invoices</a></p>
 </main></div></>;
}
