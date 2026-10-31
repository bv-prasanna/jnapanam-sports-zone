"use client";
import {useEffect,useState} from "react";
import Link from "next/link";
import {SiteHeader} from "@/components/site-header";
import {SiteFooter} from "@/components/site-footer";
import {useApp} from "@/components/app-provider";
import {api,ApiError,API_BASE_URL} from "@/lib/api";
import styles from "./subscriptions.module.css";

type Offering={id:string;kind:"MEMBERSHIP"|"TRAINING";referenceId:string;title:string;amountPaise:number;period:string;interval:number;totalCycles:number;enabled:boolean};
type Membership={id:string;planCode:string;status:string};
type Enrollment={id:string;batchId:string;status:string};
type Plan={id:string;code:string};
type Agreement={id:string;kind:string;title:string;status:string;paidCycles:number;totalCycles:number;amountPaise:number;period:string};
type Config={enabled:boolean;providerConfigured:boolean};
const money=(paise:number)=>new Intl.NumberFormat("en-IN",{style:"currency",currency:"INR"}).format(paise/100);
const msg=(e:unknown)=>e instanceof ApiError?e.message:e instanceof Error?e.message:"Request failed";
export default function SubscriptionsPage(){
 const {user,ready}=useApp();
 const [offerings,setOfferings]=useState<Offering[]>([]);
 const [agreements,setAgreements]=useState<Agreement[]>([]);
 const [memberships,setMemberships]=useState<Membership[]>([]);
 const [enrollments,setEnrollments]=useState<Enrollment[]>([]);
 const [plans,setPlans]=useState<Plan[]>([]);
 const [cfg,setCfg]=useState<Config>({enabled:false,providerConfigured:false});
 const [selected,setSelected]=useState("");
 const [consent,setConsent]=useState(false);
 const [participantName,setParticipantName]=useState("");
 const [participantAge,setParticipantAge]=useState("");
 const [checkoutUrl,setCheckoutUrl]=useState("");
 const [message,setMessage]=useState("");
 const [error,setError]=useState("");
 const [busy,setBusy]=useState(false);
 const [loading,setLoading]=useState(true);
 async function load(){
   setLoading(true);setError("");
   try{
     const [list,config,active,pending,pricePlans,history]=await Promise.all([
       api<Offering[]>("/api/v1/subscriptions/offerings"),
       api<Config>("/api/v1/subscriptions/config"),
       user?api<Membership[]>("/api/v1/memberships/me"):Promise.resolve([]),
       user?api<Enrollment[]>("/api/v1/training/enrollments/me"):Promise.resolve([]),
       api<Plan[]>("/api/v1/public/membership-plans"),
       user?api<Agreement[]>("/api/v1/subscriptions/me"):Promise.resolve([])
     ]);
     setOfferings(list);setCfg(config);setMemberships(active);setEnrollments(pending);setPlans(pricePlans);setAgreements(history);
   }catch(e){setError(msg(e));}finally{setLoading(false);}
 }
 useEffect(()=>{if(ready)void load();},[ready,user?.id]);
 async function start(offering:Offering){
   if(!user||!cfg.enabled||!cfg.providerConfigured||!consent||busy)return;
   setError("");setMessage("");setCheckoutUrl("");setBusy(true);
   try{
     let referenceId="";
     if(offering.kind==="MEMBERSHIP"){
       const plan=plans.find(p=>p.id===offering.referenceId);
       if(!plan)throw new Error("Membership plan no longer available");
       const found=memberships.find(m=>m.planCode===plan.code&&m.status==="PENDING_PAYMENT");
       const membership=found||await api<Membership>("/api/v1/memberships",{
         method:"POST",body:JSON.stringify({planCode:plan.code})
       });
       referenceId=membership.id;
     }else{
       if(!participantName.trim()||!(Number(participantAge)>=3&&Number(participantAge)<=100))
         throw new Error("Enter the training participant's name and age (3–100).");
       const found=enrollments.find(e=>e.batchId===offering.referenceId&&e.status==="PENDING_PAYMENT");
       const enrollment=found||await api<Enrollment>("/api/v1/training/batches/"+offering.referenceId+"/enrollments",{
         method:"POST",body:JSON.stringify({participantName:participantName.trim(),participantAge:Number(participantAge)})
       });
       referenceId=enrollment.id;
     }
     const started=await api<{id:string;checkoutUrl:string;status:string}>("/api/v1/subscriptions/start",{
       method:"POST",body:JSON.stringify({offeringId:offering.id,referenceId,consent:true})
     });
     if(!started.checkoutUrl.startsWith("/api/v1/public/subscriptions/checkout/"))
       throw new Error("Unexpected checkout link from server");
     setCheckoutUrl(API_BASE_URL+started.checkoutUrl);
     setMessage("Authorization prepared. Open the secure checkout and approve the recurring mandate.");
     await load();
   }catch(e){setError(msg(e));}finally{setBusy(false);}
 }
 async function cancel(id:string){
   if(!window.confirm("Stop future recurring charges? Already-paid access will remain until its valid end date."))return;
   setBusy(true);setError("");setMessage("");
   try{await api("/api/v1/subscriptions/"+id+"/cancel",{method:"POST"});setMessage("Recurring billing cancelled.");await load();}
   catch(e){setError(msg(e))}finally{setBusy(false)}
 }
 return <><SiteHeader/><main className={styles.page}>
  <header><span className={styles.eyebrow}>BILLING · MEMBERSHIPS & TRAINING</span><h1>Recurring Subscriptions</h1>
    <p>Choose a renewable membership or scheduled training installments. One-time purchases remain available in the regular portal.</p></header>
  {!cfg.enabled&&<div className={styles.warning} role="status">Recurring billing is not enabled for this Jnapanam deployment. No automatic charges can be started. The administrator must finish Razorpay mandate UAT first.</div>}
  {cfg.enabled&&!cfg.providerConfigured&&<div className={styles.warning}>Razorpay recurring billing credentials have not been configured.</div>}
  {error&&<div className={styles.error} role="alert">{error}</div>}
  {message&&<div className={styles.success} role="status">{message}</div>}
  {checkoutUrl&&<div className={styles.success}><strong>Mandate authorization is ready.</strong><p>Open Razorpay, complete consent, then return here and refresh your status.</p><a href={checkoutUrl} target="_blank" rel="noopener noreferrer" className={styles.button}>Open secure Razorpay checkout</a></div>}
  <section className={styles.section}><div className={styles.titleRow}><h2>Available recurring offerings</h2><button onClick={()=>void load()} type="button">Refresh</button></div>
   {loading?<p>Loading…</p>:offerings.length===0?<p>No recurring offerings have been published. <Link href="/memberships">Browse one-time membership plans</Link>.</p>:
   <div className={styles.grid}>{offerings.map(o=><article key={o.id} className={styles.card}><span className={styles.tag}>{o.kind==="MEMBERSHIP"?"Membership renewal":"Training installments"}</span><h3>{o.title}</h3>
    <strong>{money(o.amountPaise)} <small>every {o.interval>1?o.interval+" ":""}{o.period.replace("ly","")}{o.interval>1?"s":""}</small></strong>
    <p>{o.totalCycles} billing cycles · maximum {money(o.amountPaise*o.totalCycles)} before applicable taxes/fees. Automatic charges stop when the agreed cycles end.</p>
    {o.kind==="TRAINING"&&<div className={styles.participant}><label>Participant name<input value={selected===o.id?participantName:""} onFocus={()=>{setSelected(o.id);setConsent(false)}} onChange={e=>setParticipantName(e.target.value)} maxLength={120}/></label>
    <label>Participant age<input type="number" min={3} max={100} value={selected===o.id?participantAge:""} onFocus={()=>{setSelected(o.id);setConsent(false)}} onChange={e=>setParticipantAge(e.target.value)}/></label></div>}
    <label className={styles.consent}><input type="checkbox" checked={selected===o.id&&consent} onChange={e=>{setSelected(o.id);setConsent(e.target.checked)}}/>
      <span>I authorize a recurring payment mandate for {money(o.amountPaise)} per billing cycle, for {o.totalCycles} cycles, and understand how to cancel future charges.</span></label>
    <button type="button" className={styles.button} disabled={!user||!cfg.enabled||!cfg.providerConfigured||busy||!consent||selected!==o.id} onClick={()=>void start(o)}>
      {busy&&selected===o.id?"Preparing…":"Set up recurring payment"}</button>
   </article>)}</div>}
  </section>
  <section className={styles.section}><h2>My recurring agreements</h2>
  {!user?<p><Link href="/login">Log in</Link> to view your agreements.</p>:agreements.length===0?<p>No recurring agreements yet.</p>:
   <div className={styles.scroll}><table><thead><tr><th>Offering</th><th>Status</th><th>Paid cycles</th><th>Price / cycle</th><th>Action</th></tr></thead>
    <tbody>{agreements.map(a=><tr key={a.id}><td><b>{a.title}</b><small>{a.kind}</small></td><td>{a.status}</td><td>{a.paidCycles}/{a.totalCycles}</td><td>{money(a.amountPaise)}</td>
     <td>{!["CANCELLED","COMPLETED","EXPIRED"].includes(a.status)&&<button disabled={busy} onClick={()=>void cancel(a.id)}>Cancel future billing</button>}</td></tr>)}</tbody></table></div>}
  </section>
  <p className={styles.footnote}>Subscription authorization does not activate access. Access starts or extends after Razorpay confirms a captured installment. Auto-renewal support depends on your account's Razorpay Subscriptions eligibility.</p>
 </main><SiteFooter/></>;
}
