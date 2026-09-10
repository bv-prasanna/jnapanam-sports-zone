"use client";

import { FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { api, ApiError } from "@/lib/api";
import { useApp } from "@/components/app-provider";
import styles from "./bookings.module.css";

type Sport = { code:string; name:string; status?:string };
type Facility = { id:string; name:string; sportName?:string; sportCode?:string; status?:string };
type Booking = {
  id:string; bookingNumber:string; customerName?:string; customerEmail?:string; customerMobile?:string;
  sportName?:string; facilityId?:string; facilityName?:string; date?:string; startTime?:string; endTime?:string;
  quantity?:number; status:string; paymentOption?:string; paymentStatus?:string; checkedInAt?:string|null; checkedOutAt?:string|null;
  checkedInBy?:string; checkedOutBy?:string; onsitePaymentMethod?:string; onsitePaymentReference?:string; statusReason?:string;
};
type Payment = { id:string; referenceId?:string; purpose?:string; receiptNumber?:string; status?:string; amountPaise?:number; originalAmountPaise?:number; discountPaise?:number; provider?:string; providerPaymentId?:string; paidAt?:string; createdAt?:string };
type RangePreset = "TODAY"|"TOMORROW"|"WEEK"|"MONTH"|"CUSTOM";
type ActionKind = "check-out"|"cancel"|"no-show";

function iso(date:Date){return date.toISOString().slice(0,10)}
function todayLocal(){return new Intl.DateTimeFormat("en-CA",{timeZone:"Asia/Kolkata",year:"numeric",month:"2-digit",day:"2-digit"}).format(new Date())}
function addDays(value:string,days:number){const d=new Date(`${value}T00:00:00`);d.setDate(d.getDate()+days);return iso(d)}
function monthEnd(value:string){const [y,m]=value.split("-").map(Number);return iso(new Date(y,m,0))}
function pretty(value?:string){return value?value.replaceAll("_"," "):"—"}
function moneyPaise(value?:number){return value==null?"—":new Intl.NumberFormat("en-IN",{style:"currency",currency:"INR"}).format(value/100)}
function message(error:unknown,fallback:string){return error instanceof ApiError&&error.message?error.message:fallback}

export default function AdminBookingExplorer(){
  const router=useRouter();
  const {user,ready}=useApp();
  const allowed=!!user?.roles.some(role=>["SUPER_ADMIN","ADMIN"].includes(role));
  const today=todayLocal();
  const [sport,setSport]=useState(""); const [facilityId,setFacilityId]=useState("");
  const [from,setFrom]=useState(today); const [to,setTo]=useState(today); const [preset,setPreset]=useState<RangePreset>("TODAY");
  const [status,setStatus]=useState(""); const [paymentStatus,setPaymentStatus]=useState(""); const [query,setQuery]=useState("");
  const [sports,setSports]=useState<Sport[]>([]); const [facilities,setFacilities]=useState<Facility[]>([]); const [bookings,setBookings]=useState<Booking[]>([]);
  const [selected,setSelected]=useState<Booking|null>(null); const [busy,setBusy]=useState(false); const [error,setError]=useState(""); const [notice,setNotice]=useState("");
  const [checkingIn,setCheckingIn]=useState<Booking|null>(null); const [identityVerified,setIdentityVerified]=useState(false); const [verificationNote,setVerificationNote]=useState("");
  const [actionBusy,setActionBusy]=useState(""); const [payments,setPayments]=useState<Payment[]>([]); const [paymentsLoaded,setPaymentsLoaded]=useState(false);

  useEffect(()=>{if(ready&&!allowed)router.replace(user?"/dashboard":"/login")},[ready,allowed,user,router]);
  useEffect(()=>{if(!allowed)return;Promise.all([api<Sport[]>("/api/v1/admin/catalog/sports"),api<Facility[]>("/api/v1/admin/catalog/facilities")]).then(([s,f])=>{setSports(s.filter(x=>x.status!=="INACTIVE"));setFacilities(f.filter(x=>x.status!=="INACTIVE"))}).catch(()=>setError("Unable to load sport/facility filters."))},[allowed]);

  const availableFacilities=useMemo(()=>facilities.filter(f=>!sport||f.sportName===sport||f.sportCode===sport),[facilities,sport]);
  useEffect(()=>{if(facilityId&&!availableFacilities.some(f=>f.id===facilityId))setFacilityId("")},[availableFacilities,facilityId]);

  const load=useCallback(async()=>{
    if(!allowed)return;
    if(from&&to&&from>to){setError("From date must be on or before To date.");return}
    const p=new URLSearchParams(); if(from)p.set("from",from); if(to)p.set("to",to); if(sport)p.set("sport",sport); if(facilityId)p.set("facilityId",facilityId); if(status)p.set("status",status); if(paymentStatus)p.set("paymentStatus",paymentStatus); if(query.trim())p.set("q",query.trim());
    try{setBusy(true);setError("");setBookings(await api<Booking[]>(`/api/v1/admin/bookings?${p.toString()}`))}catch(e){setError(message(e,"Unable to load bookings."))}finally{setBusy(false)}
  },[allowed,from,to,sport,facilityId,status,paymentStatus,query]);
  useEffect(()=>{void load()},[load]);

  function applyPreset(next:RangePreset){setPreset(next);if(next==="CUSTOM")return;const base=today;if(next==="TODAY"){setFrom(base);setTo(base)}else if(next==="TOMORROW"){const d=addDays(base,1);setFrom(d);setTo(d)}else if(next==="WEEK"){setFrom(base);setTo(addDays(base,6))}else{setFrom(base.slice(0,7)+"-01");setTo(monthEnd(base))}}
  function clear(){setSport("");setFacilityId("");setStatus("");setPaymentStatus("");setQuery("");setFrom(today);setTo(today);setPreset("TODAY")}

  async function runAction(row:Booking,kind:ActionKind){
    const label=kind.replace("-"," ");
    if((kind==="cancel"||kind==="no-show")&&!window.confirm(`${kind==="cancel"?"Cancel":"Mark no-show for"} booking ${row.bookingNumber}?`))return;
    try{setActionBusy(row.id+kind);setError("");setNotice("");const url=kind==="check-out"?`/api/v1/operations/bookings/${row.id}/check-out`:`/api/v1/operations/bookings/${row.id}/${kind}`;await api(url,{method:"POST",body:kind==="check-out"?undefined:JSON.stringify({reason:kind==="cancel"?"Cancelled by administrator":"Customer did not arrive within the grace period"})});setSelected(null);setNotice(`Booking ${label} completed.`);await load()}catch(e){setError(message(e,`Unable to ${label} this booking.`))}finally{setActionBusy("")}
  }

  async function checkIn(event:FormEvent<HTMLFormElement>){
    event.preventDefault();if(!checkingIn)return;
    if(!identityVerified){setError("Confirm customer identity before check-in.");return}
    try{setActionBusy(checkingIn.id+"check-in");setError("");await api(`/api/v1/operations/bookings/${checkingIn.bookingNumber}/check-in`,{method:"POST",body:JSON.stringify({identityVerified:true,verificationNote:verificationNote.trim()||null})});setCheckingIn(null);setSelected(null);setIdentityVerified(false);setVerificationNote("");setNotice("Customer checked in successfully.");await load()}catch(e){setError(message(e,"Unable to check in this customer."))}finally{setActionBusy("")}
  }

  async function loadPayments(){
    if(paymentsLoaded)return;
    try{setActionBusy("payments");setError("");setPayments(await api<Payment[]>("/api/v1/admin/payments"));setPaymentsLoaded(true)}catch(e){setError(message(e,"Unable to load payment details."))}finally{setActionBusy("")}
  }

  const linkedPayments=useMemo(()=>selected?payments.filter(p=>p.purpose==="BOOKING"&&String(p.referenceId)===String(selected.id)):[],[payments,selected]);
  const counts=useMemo(()=>({total:bookings.length,confirmed:bookings.filter(b=>b.status==="CONFIRMED").length,paymentDue:bookings.filter(b=>["UNPAID","PENDING"].includes(String(b.paymentStatus))).length,checkedIn:bookings.filter(b=>!!b.checkedInAt&&!b.checkedOutAt).length,completed:bookings.filter(b=>!!b.checkedOutAt).length,cancelled:bookings.filter(b=>b.status==="CANCELLED").length}),[bookings]);

  function actionButtons(row:Booking){const confirmed=row.status==="CONFIRMED";const pending=["CONFIRMED","PENDING_PAYMENT"].includes(row.status);return <div className={styles.detailActions}>{confirmed&&!row.checkedInAt&&<button onClick={()=>{setCheckingIn(row);setIdentityVerified(false);setVerificationNote("");setError("")}}>Check in</button>}{row.checkedInAt&&!row.checkedOutAt&&<button disabled={!!actionBusy} onClick={()=>void runAction(row,"check-out")}>Check out</button>}{confirmed&&!row.checkedInAt&&<button disabled={!!actionBusy} onClick={()=>void runAction(row,"no-show")}>No-show</button>}{pending&&<button className={styles.danger} disabled={!!actionBusy} onClick={()=>void runAction(row,"cancel")}>Cancel booking</button>}</div>}

  if(!ready||!allowed)return <main className={styles.page}>Checking access…</main>;
  return <main className={styles.page}>
    <header className={styles.header}><div><span>ADMIN</span><h1>Booking Explorer</h1><p>Find bookings, inspect payment details and take permitted booking actions.</p></div><Link href="/admin" className={styles.back}>← Admin console</Link></header>
    <section className={styles.presets}>{(["TODAY","TOMORROW","WEEK","MONTH","CUSTOM"] as RangePreset[]).map(x=><button key={x} className={preset===x?styles.active:""} onClick={()=>applyPreset(x)}>{x==="WEEK"?"This week":x==="MONTH"?"This month":x[0]+x.slice(1).toLowerCase()}</button>)}</section>
    <section className={styles.filters}>
      <label>Sport<select value={sport} onChange={e=>{setSport(e.target.value);setFacilityId("")}}><option value="">All sports</option>{sports.map(s=><option key={s.code} value={s.name}>{s.name}</option>)}</select></label>
      <label>Facility<select value={facilityId} onChange={e=>setFacilityId(e.target.value)}><option value="">All facilities</option>{availableFacilities.map(f=><option key={f.id} value={f.id}>{f.name}</option>)}</select></label>
      <label>From<input type="date" value={from} onChange={e=>{setFrom(e.target.value);setPreset("CUSTOM")}}/></label><label>To<input type="date" value={to} onChange={e=>{setTo(e.target.value);setPreset("CUSTOM")}}/></label>
      <label>Booking status<select value={status} onChange={e=>setStatus(e.target.value)}><option value="">All statuses</option>{["PENDING_PAYMENT","CONFIRMED","PAYMENT_EXPIRED","NO_SHOW","CANCELLED"].map(x=><option key={x}>{x}</option>)}</select></label>
      <label>Payment<select value={paymentStatus} onChange={e=>setPaymentStatus(e.target.value)}><option value="">All payments</option>{["NOT_REQUIRED","UNPAID","PAID","REFUNDED"].map(x=><option key={x}>{x}</option>)}</select></label>
      <label className={styles.search}>Booking / customer<input value={query} onChange={e=>setQuery(e.target.value)} placeholder="Booking no., name, mobile, email"/></label><div className={styles.filterActions}><button className={styles.primary} onClick={()=>void load()} disabled={busy}>{busy?"Loading…":"Apply filters"}</button><button onClick={clear}>Reset</button></div>
    </section>
    {notice&&<div className={styles.notice}>{notice}</div>}{error&&<div className={styles.error}>{error}</div>}
    <section className={styles.metrics}><article><strong>{counts.total}</strong><span>Results</span></article><article><strong>{counts.confirmed}</strong><span>Confirmed</span></article><article><strong>{counts.paymentDue}</strong><span>Payment due</span></article><article><strong>{counts.checkedIn}</strong><span>Checked in</span></article><article><strong>{counts.completed}</strong><span>Completed</span></article><article><strong>{counts.cancelled}</strong><span>Cancelled</span></article></section>
    <section className={styles.tableWrap}><table><thead><tr><th>Date / Slot</th><th>Sport / Facility</th><th>Customer</th><th>Booking</th><th>Payment</th><th>Status</th><th>Actions</th></tr></thead><tbody>{bookings.map(b=><tr key={b.id}><td><b>{b.date||"—"}</b><small>{b.startTime||"—"} – {b.endTime||"—"}</small></td><td><b>{b.sportName||"—"}</b><small>{b.facilityName||"—"}</small></td><td><b>{b.customerName||"—"}</b><small>{b.customerMobile||b.customerEmail||"—"}</small></td><td><b>{b.bookingNumber}</b><small>Qty {b.quantity??1}</small></td><td><b>{pretty(b.paymentStatus)}</b><small>{pretty(b.paymentOption)}</small></td><td><span className={styles.status}>{pretty(b.status)}</span></td><td><div className={styles.rowActions}><button onClick={()=>{setSelected(b);setPaymentsLoaded(false);setPayments([])}}>View</button>{actionButtons(b)}</div></td></tr>)}</tbody></table>{!busy&&bookings.length===0&&<p className={styles.empty}>No bookings match these filters.</p>}</section>

    {selected&&<div className={styles.backdrop} onMouseDown={()=>setSelected(null)}><section className={styles.dialog} onMouseDown={e=>e.stopPropagation()}><button className={styles.close} onClick={()=>setSelected(null)}>×</button><span>BOOKING DETAILS</span><h2>{selected.bookingNumber}</h2><div className={styles.details}>{Object.entries(selected).filter(([k,v])=>v!=null&&!['id','facilityId'].includes(k)).map(([k,v])=><div key={k}><small>{k.replace(/([A-Z])/g," $1")}</small><b>{String(v)}</b></div>)}</div><section className={styles.paymentPanel}><div><h3>Payment details</h3><button onClick={()=>void loadPayments()} disabled={actionBusy==="payments"}>{paymentsLoaded?"Refresh":"Load payment details"}</button></div>{paymentsLoaded&&(linkedPayments.length?linkedPayments.map(p=><article key={p.id}><b>{p.receiptNumber||p.id}</b><span>{pretty(p.status)} · {moneyPaise(p.amountPaise)}</span><small>{p.paidAt||p.createdAt||"—"}{p.provider?` · ${p.provider}`:""}</small></article>):<p>No linked booking payment record found.</p>)}</section><div className={styles.modalActions}>{actionButtons(selected)}<button onClick={()=>setSelected(null)}>Close</button></div></section></div>}
    {checkingIn&&<div className={styles.backdrop}><section className={styles.dialog}><button className={styles.close} onClick={()=>setCheckingIn(null)}>×</button><span>CUSTOMER ARRIVAL</span><h2>Check in {checkingIn.bookingNumber}</h2><p>Verify the arriving customer before entry. Payment must already be cleared.</p><form onSubmit={checkIn} className={styles.checkInForm}><label><input type="checkbox" checked={identityVerified} onChange={e=>setIdentityVerified(e.target.checked)}/> Customer identity visually verified</label><label>Verification note<input value={verificationNote} maxLength={120} onChange={e=>setVerificationNote(e.target.value)} placeholder="Example: Name matched photo ID"/></label>{error&&<div className={styles.error}>{error}</div>}<div className={styles.modalActions}><button type="button" onClick={()=>setCheckingIn(null)}>Cancel</button><button className={styles.primary} disabled={!!actionBusy||!identityVerified}>{actionBusy?"Checking in…":"Confirm check-in"}</button></div></form></section></div>}
  </main>;
}
