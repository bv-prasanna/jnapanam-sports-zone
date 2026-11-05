"use client";

import {useCallback,useEffect,useState} from "react";
import {useRouter} from "next/navigation";
import {api,ApiError} from "@/lib/api";
import {useApp} from "@/components/app-provider";
import styles from "./errors.module.css";

type Failure={id:string;occurredAt:string;method:string;path:string;status:number;
  correlationId:string|null;durationMs:number;action:string};
const statuses=[{value:"",label:"All errors"},{value:"400",label:"400 Bad request"},
  {value:"401",label:"401 Login / session"},{value:"403",label:"403 Permission"},
  {value:"404",label:"404 Not found"},{value:"409",label:"409 Conflict"},
  {value:"429",label:"429 Rate limited"},{value:"500",label:"500 Server error"},
  {value:"502",label:"502 Bad gateway"},{value:"503",label:"503 Unavailable"}];
function statusName(n:number){return n>=500?"Server failure":n===401?"Authentication":n===403?"Access denied":n===404?"Not found":n===429?"Too many requests":n===409?"Conflict":"Request rejected";}
export default function ErrorLogs(){
 const {user,ready}=useApp();
 const router=useRouter();
 const allowed=!!user?.roles?.some(r=>r==="ADMIN"||r==="SUPER_ADMIN");
 const [status,setStatus]=useState("");
 const [rows,setRows]=useState<Failure[]>([]);
 const [busy,setBusy]=useState(false);
 const [error,setError]=useState("");
 const load=useCallback(async()=>{
   if(!allowed)return;
   setBusy(true);setError("");
   try{
     const params=new URLSearchParams({limit:"100"});
     if(status)params.set("status",status);
     setRows(await api<Failure[]>("/api/v1/admin/errors?"+params.toString()));
   }catch(e){setError(e instanceof ApiError?e.message:"Unable to load error logs.");}
   finally{setBusy(false);}
 },[allowed,status]);
 useEffect(()=>{if(ready&&!allowed)router.replace(user?"/dashboard":"/login");},[ready,allowed,user,router]);
 useEffect(()=>{void load();},[load]);
 if(!ready||!allowed)return <main className={styles.page}>Checking administrator access…</main>;
 return <main className={styles.page}>
   <header className={styles.header}>
     <div><span>ADMIN · SYSTEM</span><h1>Error Logs</h1>
       <p>Review failed API requests across the Android app, website and backend. Use the reference ID to investigate an issue. New records exclude passwords, OTPs, tokens, request bodies and query strings.</p></div>
   </header>
   <div className={styles.toolbar}>
     <label>HTTP status <select value={status} onChange={e=>setStatus(e.target.value)}>
       {statuses.map(s=><option value={s.value} key={s.value}>{s.label}</option>)}
     </select></label>
     <button type="button" onClick={()=>void load()} disabled={busy}>{busy?"Loading…":"Refresh logs"}</button>
   </div>
   {error&&<p className={styles.error} role="alert">{error}</p>}
   <p className={styles.count} role="status">Showing {rows.length} recent matching errors (maximum 100 per view).</p>
   <div className={styles.tableWrap}><table><thead><tr>
     <th>When</th><th>Status</th><th>API operation</th><th>Error code</th><th>Reference ID</th><th>Elapsed</th>
   </tr></thead><tbody>{rows.map(r=><tr key={r.id}>
     <td>{new Date(r.occurredAt).toLocaleString()}</td>
     <td><strong className={r.status>=500?styles.critical:styles.warning}>{r.status}</strong><small>{statusName(r.status)}</small></td>
     <td><span className={styles.method}>{r.method}</span><code>{r.path}</code></td>
     <td><code>{r.action}</code></td>
     <td>{r.correlationId?<button className={styles.copy} title="Copy support reference" type="button" onClick={()=>{void navigator.clipboard?.writeText(r.correlationId!);}}>{r.correlationId}</button>:"Unavailable"}</td>
     <td>{r.durationMs} ms</td>
   </tr>)}</tbody></table>
   {!busy&&rows.length===0&&<p className={styles.empty}>No matching error records found.</p>}</div>
   <p className={styles.hint}>400–499: client errors or policy rejections. 500–599: investigate backend service logs using the reference ID. New error telemetry begins after backend deployment.</p>
 </main>;
}
