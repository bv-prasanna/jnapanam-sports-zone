"use client";
import {useEffect,useState} from "react";
import {api,ApiError} from "@/lib/api";
import {useApp} from "@/components/app-provider";
import styles from "./admin-subscriptions.module.css";
type Option={id:string;code?:string;name:string;price?:number;fee?:number;durationDays?:number;startDate?:string;endDate?:string;status:string};
type Offering={id:string;kind:string;referenceId:string;title:string;amountPaise:number;period:string;interval:number;totalCycles:number;razorpayPlanId:string;enabled:boolean};
type Agreement={id:string;ownerName:string;email:string;kind:string;title:string;status:string;paidCycles:number;amountPaise:number;period:string;subscriptionId:string};
const money=(n:number)=>new Intl.NumberFormat("en-IN",{style:"currency",currency:"INR"}).format(n/100);
const errorMessage=(e:unknown)=>e instanceof ApiError?e.message:e instanceof Error?e.message:"Request failed";
export default function AdminSubscriptions(){
 const {user}=useApp();const superAdmin=!!user?.roles.includes("SUPER_ADMIN");
 const [kind,setKind]=useState<"MEMBERSHIP"|"TRAINING">("MEMBERSHIP");
 const [referenceId,setReferenceId]=useState("");
 const [title,setTitle]=useState("");
 const [amount,setAmount]=useState("");
 const [period,setPeriod]=useState("monthly");
 const [interval,setInterval]=useState("1");
 const [totalCycles,setTotalCycles]=useState("3");
 const [memberships,setMemberships]=useState<Option[]>([]);
 const [training,setTraining]=useState<Option[]>([]);
 const [offerings,setOfferings]=useState<Offering[]>([]);
 const [agreements,setAgreements]=useState<Agreement[]>([]);
 const [error,setError]=useState("");const [notice,setNotice]=useState("");
 const [busy,setBusy]=useState(false);const [loading,setLoading]=useState(true);
 async function load(){setLoading(true);try{
 const [m,t,o,a]=await Promise.all([
 api<Option[]>("/api/v1/admin/memberships/plans"),api<Option[]>("/api/v1/admin/training/batches"),
 api<Offering[]>("/api/v1/admin/subscriptions/offerings"),api<Agreement[]>("/api/v1/admin/subscriptions")]);
 setMemberships(m);setTraining(t);setOfferings(o);setAgreements(a);setError("");
 }catch(e){setError(errorMessage(e));}finally{setLoading(false)}}
 useEffect(()=>{void load()},[]);
 const options=(kind==="MEMBERSHIP"?memberships:training).filter(o=>o.status==="ACTIVE");
 function selectReference(id:string){setReferenceId(id);const record=options.find(o=>o.id===id);if(record){setTitle(record.name+" recurring");setAmount(String(record.price??record.fee??""));}}
 async function create(){setError("");setNotice("");if(!superAdmin)return;
 const amountPaise=Math.round(Number(amount)*100);
 if(!referenceId||!title.trim()||!Number.isSafeInteger(amountPaise)||amountPaise<100){
   setError("Select a product and enter valid charge amount in rupees.");return;
 }
 if(!window.confirm("Create a new Razorpay recurring plan with the stated charge and cycles? New plans cannot be retroactively changed."))return;
 setBusy(true);try{
 await api("/api/v1/admin/subscriptions/offerings",{method:"POST",body:JSON.stringify({
  kind,referenceId,title:title.trim(),amountPaise,period,interval:Number(interval),totalCycles:Number(totalCycles)
 })});
 setNotice("Razorpay plan created in the configured test/live account. It is unpublished until explicitly enabled.");
 setReferenceId("");await load();
 }catch(e){setError(errorMessage(e));}finally{setBusy(false)}}
 async function toggle(id:string,enabled:boolean){
  if(!superAdmin||!window.confirm(enabled?"Make this plan visible to customers? Verify billing credentials and product approval first.":"Hide this recurring offering from new customers?"))return;
  setBusy(true);try{await api("/api/v1/admin/subscriptions/offerings/"+id+"/availability",{method:"PUT",body:JSON.stringify({enabled})});setNotice("Offering updated.");await load();}
  catch(e){setError(errorMessage(e))}finally{setBusy(false)}
 }
 async function cancel(id:string){
  if(!superAdmin||!window.confirm("Cancel the customer's future Razorpay recurring charges immediately? Already paid benefits remain until expiry."))return;
  setBusy(true);try{await api("/api/v1/admin/subscriptions/"+id+"/cancel",{method:"POST"});setNotice("Provider cancellation requested and stored.");await load();}
  catch(e){setError(errorMessage(e))}finally{setBusy(false)}
 }
 return <main className={styles.page}><header><span className={styles.eyebrow}>ADMIN · RECURRING PAYMENTS</span><h1>Membership & Training Subscriptions</h1><p>Configure explicit recurring prices, mandate durations and paid-cycle records. One-time plans remain unchanged.</p></header>
 <div className={styles.notice}>Safety: recurring billing is disabled at backend startup unless <code>RECURRING_SUBSCRIPTIONS_ENABLED=true</code>. Do not enable until Razorpay Subscriptions appears in your dashboard, test mandates are successful and the webhook events are subscribed.</div>
 {error&&<div className={styles.error} role="alert">{error}</div>}{notice&&<div className={styles.success} role="status">{notice}</div>}
 <section className={styles.card}><h2>Create Razorpay plan</h2><p>Available to Super Admin only. The total number of cycles must fit the membership duration or remaining training batch dates.</p>
 <div className={styles.form}><label>Product type<select disabled={!superAdmin||busy} value={kind} onChange={e=>{setKind(e.target.value as "MEMBERSHIP"|"TRAINING");setReferenceId("");setTitle("");setAmount("");}}><option value="MEMBERSHIP">Membership renewal</option><option value="TRAINING">Training installment</option></select></label>
 <label>Existing active {kind==="MEMBERSHIP"?"membership plan":"training batch"}<select value={referenceId} disabled={!superAdmin||busy} onChange={e=>selectReference(e.target.value)}><option value="">Choose product</option>{options.map(o=><option key={o.id} value={o.id}>{o.name} {o.code?"("+o.code+")":""}</option>)}</select></label>
 <label>Public offer name<input disabled={!superAdmin||busy} value={title} onChange={e=>setTitle(e.target.value)} maxLength={150}/></label>
 <label>Charge each cycle (₹)<input disabled={!superAdmin||busy} type="number" min="1" step=".01" value={amount} onChange={e=>setAmount(e.target.value)}/></label>
 <label>Billing frequency<select value={period} disabled={!superAdmin||busy} onChange={e=>setPeriod(e.target.value)}><option value="weekly">Weekly</option><option value="monthly">Monthly</option><option value="yearly">Yearly</option></select></label>
 <label>Interval<select value={interval} disabled={!superAdmin||busy} onChange={e=>setInterval(e.target.value)}>{[1,2,3,6,12].map(i=><option key={i} value={String(i)}>Every {i} period{i>1?"s":""}</option>)}</select></label>
 <label>Billing cycles<input type="number" min={2} max={60} disabled={!superAdmin||busy} value={totalCycles} onChange={e=>setTotalCycles(e.target.value)}/></label></div>
 <p className={styles.hint}>Total agreed charge: {Number(amount)>0&&Number(totalCycles)>0?money(Math.round(Number(amount)*100)*Number(totalCycles)):"—"} across {totalCycles||"—"} cycles. Currency: INR. Changes to an existing plan require a new product version.</p>
 <button className={styles.primary} type="button" disabled={!superAdmin||busy||!referenceId} onClick={()=>void create()}>{busy?"Working…":"Create test Razorpay recurring plan"}</button>
 </section>
 <section className={styles.card}><div className={styles.heading}><h2>Configured recurring offers</h2><button type="button" onClick={()=>void load()}>Refresh</button></div>
 {loading?<p>Loading…</p>:offerings.length===0?<p>No recurring offerings configured.</p>:
 <div className={styles.scroll}><table><thead><tr><th>Offering</th><th>Type</th><th>Charge</th><th>Period</th><th>Cycles</th><th>Visible</th><th>Action</th></tr></thead>
 <tbody>{offerings.map(o=><tr key={o.id}><td><strong>{o.title}</strong><small>{o.razorpayPlanId}</small></td><td>{o.kind}</td><td>{money(o.amountPaise)}</td><td>Every {o.interval} {o.period}</td><td>{o.totalCycles}</td><td>{o.enabled?"Published":"Hidden"}</td><td><button disabled={!superAdmin||busy} onClick={()=>void toggle(o.id,!o.enabled)}>{o.enabled?"Hide":"Publish"}</button></td></tr>)}</tbody></table></div>}
 </section>
 <section className={styles.card}><h2>Customer recurring agreements</h2>{agreements.length===0?<p>No agreements yet.</p>:
 <div className={styles.scroll}><table><thead><tr><th>Customer</th><th>Offering</th><th>Status</th><th>Paid cycles</th><th>Subscription ID</th><th>Action</th></tr></thead><tbody>
 {agreements.map(a=><tr key={a.id}><td><b>{a.ownerName}</b><small>{a.email}</small></td><td>{a.title}</td><td>{a.status}</td><td>{a.paidCycles}</td><td>{a.subscriptionId}</td><td>{!["CANCELLED","COMPLETED","EXPIRED"].includes(a.status)&&<button disabled={!superAdmin||busy} onClick={()=>void cancel(a.id)}>Stop future billing</button>}</td></tr>)}</tbody></table></div>}</section>
 <p className={styles.hint}>Settlement reconciliation, refunds and mandate changes must be checked against the Razorpay dashboard before moving to live billing.</p>
 </main>;
}
