"use client";
import {FormEvent,useCallback,useEffect,useState} from "react";
import {useRouter} from "next/navigation";


import {useApp} from "@/components/app-provider";
import {api,ApiError} from "@/lib/api";
type NumberRow={mobile:string;active:boolean;note?:string};
export default function UatOtpAllowlist(){
 const {user,ready}=useApp();const router=useRouter();
 const [rows,setRows]=useState<NumberRow[]>([]),[mobile,setMobile]=useState(""),[note,setNote]=useState(""),[message,setMessage]=useState(""),[enabled,setEnabled]=useState(false),[busy,setBusy]=useState(false);
 const permitted=!!user?.roles.includes("SUPER_ADMIN");
 const load=useCallback(async()=>{if(!permitted)return;try{
   const [numbers,settings]=await Promise.all([api<NumberRow[]>("/api/v1/admin/uat-otp/numbers"),api<{enabled:boolean}>("/api/v1/admin/uat-otp/settings")]);
   setRows(numbers);setEnabled(settings.enabled);setMessage("");
 }catch(e){setMessage(e instanceof ApiError?e.message:"Unable to load UAT allowlist");}},[permitted]);
 useEffect(()=>{if(ready&&!user)router.replace("/login");else void load()},[ready,user,router,load]);
 async function add(e:FormEvent){e.preventDefault();if(!/^\d{10}$/.test(mobile)){setMessage("Enter exactly 10 digits");return;}
 setBusy(true);try{await api("/api/v1/admin/uat-otp/numbers",{method:"POST",body:JSON.stringify({mobile,note})});setMobile("");setNote("");await load();setMessage("Approved number saved.");}catch(e){setMessage(e instanceof ApiError?e.message:"Unable to save");}finally{setBusy(false)}}
 async function disable(number:string){if(!window.confirm("Disable test OTP access for this number?"))return;
 setBusy(true);try{await api("/api/v1/admin/uat-otp/numbers/"+number,{method:"DELETE"});await load();setMessage("Number disabled.");}catch(e){setMessage(e instanceof ApiError?e.message:"Unable to disable");}finally{setBusy(false)}}
 if(!ready||!user)return <p>Loading…</p>;
 if(!permitted)return <p>Super Admin access required.</p>;
 return <><main className="portal"><section className="portal-content" style={{maxWidth:850,margin:"0 auto",width:"100%"}}>
 <div className="portal-heading"><div><span className="eyebrow">Restricted testing</span><h1>UAT OTP approved numbers</h1><p>Add or disable test phone numbers without changing environment variables.</p></div></div>
 <p role="status">Fixed OTP mode: <strong>{enabled?"Enabled in this environment":"Disabled in this environment"}</strong>. This screen never displays the OTP code.</p>
 <form onSubmit={add} className="form-grid"><label>Mobile number<input type="tel" inputMode="numeric" pattern="[0-9]{10}" maxLength={10} required value={mobile} onChange={e=>setMobile(e.target.value)} placeholder="10-digit test mobile"/></label>
 <label>Purpose or tester name<input maxLength={200} value={note} onChange={e=>setNote(e.target.value)}/></label><button disabled={busy} type="submit">Approve test number</button></form>
 {message&&<p role="status">{message}</p>}<h2>Approved numbers</h2>
 <div className="record-list">{rows.map(row=><article key={row.mobile}><strong>{row.mobile}</strong><div><small>Status</small><span>{row.active?"Active":"Disabled"}</span></div>{row.note&&<p>{row.note}</p>}
 {row.active&&<button disabled={busy} type="button" onClick={()=>void disable(row.mobile)}>Disable</button>}</article>)}</div>
 </section></main></>;
}
