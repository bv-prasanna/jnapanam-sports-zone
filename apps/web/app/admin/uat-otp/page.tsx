"use client";
import {FormEvent,useCallback,useEffect,useState} from "react";
import {useRouter} from "next/navigation";
import {useApp} from "@/components/app-provider";
import {api,ApiError} from "@/lib/api";
import styles from "./uat-otp.module.css";
type NumberRow={mobile:string;active:boolean;note?:string};
export default function UatOtpAllowlist(){
 const {user,ready}=useApp();const router=useRouter();
 const [rows,setRows]=useState<NumberRow[]>([]),[mobile,setMobile]=useState(""),[note,setNote]=useState(""),[message,setMessage]=useState(""),[error,setError]=useState(""),[enabled,setEnabled]=useState(false),[busy,setBusy]=useState(false),[loading,setLoading]=useState(true);
 const permitted=!!user?.roles.includes("SUPER_ADMIN");
 const load=useCallback(async()=>{if(!permitted)return;setLoading(true);
 try{const [numbers,settings]=await Promise.all([api<NumberRow[]>("/api/v1/admin/uat-otp/numbers"),api<{enabled:boolean}>("/api/v1/admin/uat-otp/settings")]);setRows(numbers);setEnabled(settings.enabled);setError("")}
 catch(e){setError(e instanceof ApiError?e.message:"Unable to load UAT allowlist.")}
 finally{setLoading(false)}},[permitted]);
 useEffect(()=>{if(ready&&!user)router.replace("/login");else void load()},[ready,user,router,load]);
 async function add(e:FormEvent){e.preventDefault();setError("");setMessage("");
   if(!/^\d{10}$/.test(mobile)){setError("Enter a valid 10-digit Indian mobile number.");return;}
   setBusy(true);try{await api("/api/v1/admin/uat-otp/numbers",{method:"POST",body:JSON.stringify({mobile,note})});setMobile("");setNote("");await load();setMessage("Test number approved. The UAT OTP environment switch is unchanged.");}
   catch(e){setError(e instanceof ApiError?e.message:"Unable to save.");}finally{setBusy(false)}
 }
 async function disable(number:string){if(!window.confirm("Disable test OTP access for this number?"))return;setBusy(true);setError("");setMessage("");
   try{await api("/api/v1/admin/uat-otp/numbers/"+number,{method:"DELETE"});await load();setMessage("Number disabled.")}
   catch(e){setError(e instanceof ApiError?e.message:"Unable to disable.");}finally{setBusy(false)}
 }
 if(!ready||!user)return <div className={styles.page}>Checking access…</div>;
 if(!permitted)return <div className={styles.page}>Super Admin access required.</div>;
 return <main className={styles.page}>
   <header><span className={styles.eyebrow}>ADMIN · CONTROLLED UAT</span><h1>UAT OTP Numbers</h1><p>Manage approved mobile numbers for optional fixed-OTP testing. The OTP itself is never shown here.</p></header>
   <section className={enabled?styles.statusOn:styles.statusOff}><strong>{enabled?"Test OTP enabled":"Test OTP disabled"}</strong>
     <p>{enabled?"Only permitted test numbers can use the configured test OTP.":"The backend has TEST_OTP_ENABLED=false. Adding a number does not enable fixed OTP. Keep this disabled for production."}</p></section>
   {error&&<p role="alert" className={styles.error}>{error}</p>}{message&&<p role="status" className={styles.success}>{message}</p>}
   <section className={styles.card}><h2>Approve a test number</h2><form className={styles.form} onSubmit={add}>
     <label>Mobile number <input type="tel" inputMode="numeric" pattern="[0-9]{10}" maxLength={10} required placeholder="10-digit mobile number" value={mobile} onChange={e=>setMobile(e.target.value.replace(/\D/g,""))}/></label>
     <label>Purpose / tester name <input maxLength={200} placeholder="Example: Operator UAT" value={note} onChange={e=>setNote(e.target.value)}/></label>
     <button disabled={busy}>{busy?"Saving…":"Approve number"}</button>
   </form></section>
   <section className={styles.card}><div className={styles.tableHeading}><h2>Approved numbers</h2><span>{rows.filter(x=>x.active).length} active</span></div>
     {loading?<p>Loading…</p>:rows.length===0?<div className={styles.empty}>No test numbers have been approved.</div>:
     <div className={styles.scroll}><table><thead><tr><th>Mobile</th><th>Purpose</th><th>Status</th><th>Action</th></tr></thead><tbody>{rows.map(row=><tr key={row.mobile}><td><strong>{row.mobile}</strong></td><td>{row.note||"—"}</td><td><span className={row.active?styles.active:styles.disabled}>{row.active?"Active":"Disabled"}</span></td><td>{row.active?<button type="button" disabled={busy} onClick={()=>void disable(row.mobile)}>Disable</button>:"—"}</td></tr>)}</tbody></table></div>}</section>
 </main>;
}
