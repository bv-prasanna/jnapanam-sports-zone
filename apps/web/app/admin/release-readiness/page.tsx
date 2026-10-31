"use client";
import {useCallback,useEffect,useMemo,useState} from "react";
import {useRouter} from "next/navigation";
import {api,ApiError} from "@/lib/api";
import {useApp} from "@/components/app-provider";
import {SiteHeader} from "@/components/site-header";
import {PortalSideNavigation} from "@/components/portal-side-navigation";
import shell from "@/components/portal-shell.module.css";
import styles from "./release-readiness.module.css";
type Ticket={id:string;ticket_number?:string;subject?:string;priority?:string;status?:string;customer_name?:string};
type Payment={id:string;receiptNumber?:string;purpose?:string;status?:string;amountPaise?:number;createdAt?:string};
const rup=(v:number)=>new Intl.NumberFormat("en-IN",{style:"currency",currency:"INR"}).format(v/100);
const readable=(v?:string)=>v?v.replaceAll("_"," "):"—";
export default function OperationsExceptions(){
 const router=useRouter();
 const {user,ready}=useApp();
 const allowed=!!user?.roles.some(role=>role==="SUPER_ADMIN"||role==="ADMIN");
 const [tickets,setTickets]=useState<Ticket[]>([]);
 const [payments,setPayments]=useState<Payment[]>([]);
 const [error,setError]=useState("");
 const [loading,setLoading]=useState(true);
 const load=useCallback(async()=>{
   if(!allowed)return;
   setError("");setLoading(true);
   const [s,p]=await Promise.allSettled([api<Ticket[]>("/api/v1/admin/support"),api<Payment[]>("/api/v1/admin/payments")]);
   if(s.status==="fulfilled")setTickets(s.value);else setError("Support tickets could not be loaded. "+(s.reason instanceof ApiError?s.reason.message:""));
   if(p.status==="fulfilled")setPayments(p.value);else setError(value=>value+" Payment records could not be loaded. "+(p.reason instanceof ApiError?p.reason.message:""));
   setLoading(false);
 },[allowed]);
 useEffect(()=>{if(ready&&!allowed)router.replace(user?"/dashboard":"/login");},[ready,allowed,router,user]);
 useEffect(()=>{void load();},[load]);
 const metrics=useMemo(()=>{
   const open=tickets.filter(t=>!["RESOLVED","CLOSED","CANCELLED"].includes(String(t.status||""))).length;
   const paid=payments.filter(p=>["PAID","PARTIALLY_REFUNDED"].includes(String(p.status)));
   return {open,pending:payments.filter(p=>["CREATED","PENDING"].includes(String(p.status))).length,paidAmount:paid.reduce((sum,p)=>sum+Number(p.amountPaise||0),0)};
 },[tickets,payments]);
 if(!ready||!allowed)return <main className={styles.page}>Checking administrator access…</main>;
 return <><SiteHeader/><div className={shell.shell}><PortalSideNavigation active="exceptions"/><main className={styles.page}>
   <header className={styles.heading}><div><span>ADMIN · OPERATIONS</span><h1>Operations & Exceptions</h1><p>Customer support and payment records for operational follow-up. This is not deployment health/readiness.</p></div><button onClick={()=>void load()} type="button" disabled={loading}>{loading?"Loading…":"Refresh"}</button></header>
   {error&&<p role="alert" className={styles.error}>{error}</p>}
   <div className={styles.metrics}>
     <article><small>Open support tickets</small><strong>{metrics.open}</strong><span>Require follow-up</span></article>
     <article><small>Pending payment records</small><strong>{metrics.pending}</strong><span>Created / pending status</span></article>
     <article><small>Recorded paid amount</small><strong>{rup(metrics.paidAmount)}</strong><span>Before refunds / settlement</span></article>
     <article><small>Total payment records</small><strong>{payments.length}</strong><span>Recorded in the application</span></article>
   </div>
   <section className={styles.card}><div className={styles.sectionHead}><h2>Customer support / complaints</h2><span>{tickets.length} records</span></div>
     <div className={styles.scroll}><table><thead><tr><th>Ticket</th><th>Subject</th><th>Customer</th><th>Priority</th><th>Status</th></tr></thead><tbody>{tickets.map(t=><tr key={t.id}><td>{t.ticket_number||"—"}</td><td>{t.subject||"—"}</td><td>{t.customer_name||"—"}</td><td>{readable(t.priority)}</td><td>{readable(t.status)}</td></tr>)}</tbody></table></div>
     {tickets.length===0&&<p className={styles.empty}>No support tickets are recorded.</p>}
   </section>
   <section className={styles.card}><div className={styles.sectionHead}><h2>Payment records</h2><span>{payments.length} records</span></div>
     <p className={styles.note}>This is an application payment-status summary, not a verified bank or Razorpay settlement reconciliation.</p>
     <div className={styles.scroll}><table><thead><tr><th>Receipt</th><th>Purpose</th><th>Amount</th><th>Status</th><th>Created</th></tr></thead><tbody>{payments.slice(0,100).map(p=><tr key={p.id}><td>{p.receiptNumber||"—"}</td><td>{readable(p.purpose)}</td><td>{rup(Number(p.amountPaise||0))}</td><td>{readable(p.status)}</td><td>{p.createdAt?new Date(p.createdAt).toLocaleDateString("en-IN"):"—"}</td></tr>)}</tbody></table></div>
     {payments.length===0&&<p className={styles.empty}>No payment records are recorded.</p>}
     {payments.length>100&&<p className={styles.note}>Showing the first 100 records. Open Payments in the admin console for full details.</p>}
   </section>
 </main></div></>;
}
