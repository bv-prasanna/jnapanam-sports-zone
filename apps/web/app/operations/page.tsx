"use client";

import { FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { api, ApiError } from "@/lib/api";
import { useApp } from "@/components/app-provider";
import styles from "./operations.module.css";

type Booking = {
  id:string; bookingNumber:string; customerName?:string; customerEmail?:string; customerMobile?:string;
  sportName?:string; facilityName?:string; date?:string; startTime?:string; endTime?:string;
  status:string; paymentStatus?:string; checkedInAt?:string|null; checkedOutAt?:string|null;
};

type Action = "check-in"|"check-out"|"no-show";

function todayLocal(){return new Intl.DateTimeFormat("en-CA",{timeZone:"Asia/Kolkata",year:"numeric",month:"2-digit",day:"2-digit"}).format(new Date())}
function readable(value?:string){return value?value.replaceAll("_"," "):"—"}

export default function OperationsPage(){
  const router=useRouter();
  const {user,ready}=useApp();
  const allowed=!!user?.roles.some(role=>["SUPER_ADMIN","ADMIN","OPERATIONS"].includes(role));
  const today=todayLocal();
  const [bookings,setBookings]=useState<Booking[]>([]);
  const [sport,setSport]=useState(""); const [facility,setFacility]=useState(""); const [query,setQuery]=useState("");
  const [checkingIn,setCheckingIn]=useState<Booking|null>(null); const [verified,setVerified]=useState(false); const [note,setNote]=useState("");
  const [busy,setBusy]=useState(""); const [error,setError]=useState("");

  useEffect(()=>{if(ready&&!allowed)router.replace(user?"/dashboard":"/login")},[ready,allowed,user,router]);
  const load=useCallback(async()=>{if(!allowed)return;try{setError("");const rows=await api<Booking[]>(`/api/v1/admin/bookings?date=${today}`);setBookings(rows.filter(b=>!["CANCELLED","NO_SHOW"].includes(b.status)&&!b.checkedOutAt))}catch(e){setError(e instanceof ApiError?e.message:"Unable to load today's bookings.")}},[allowed,today]);
  useEffect(()=>{void load()},[load]);

  const sports=useMemo(()=>Array.from(new Set(bookings.map(b=>b.sportName).filter(Boolean) as string[])).sort(),[bookings]);
  const facilities=useMemo(()=>Array.from(new Set(bookings.filter(b=>!sport||b.sportName===sport).map(b=>b.facilityName).filter(Boolean) as string[])).sort(),[bookings,sport]);
  useEffect(()=>{if(facility&&!facilities.includes(facility))setFacility("")},[facility,facilities]);
  const visible=useMemo(()=>bookings.filter(b=>!sport||b.sportName===sport).filter(b=>!facility||b.facilityName===facility).filter(b=>{const q=query.trim().toLowerCase();return !q||[b.bookingNumber,b.customerName,b.customerMobile,b.customerEmail,b.sportName,b.facilityName].some(v=>v?.toLowerCase().includes(q))}).sort((a,b)=>{const rank=(x:Booking)=>x.checkedInAt?2:x.paymentStatus==="PAID"||x.paymentStatus==="NOT_REQUIRED"?1:0;return rank(a)-rank(b)||(a.startTime||"").localeCompare(b.startTime||"")}),[bookings,sport,facility,query]);

  async function action(booking:Booking,kind:Action){try{setBusy(booking.id+kind);setError("");if(kind==="check-in"){setCheckingIn(booking);return}await api(`/api/v1/operations/bookings/${booking.id}/${kind}`,{method:"POST",body:kind==="no-show"?JSON.stringify({reason:"Customer did not arrive within the grace period"}):undefined});await load()}catch(e){setError(e instanceof ApiError?e.message:`Unable to ${kind}.`)}finally{if(kind!=="check-in")setBusy("")}}
  async function confirmCheckIn(e:FormEvent){e.preventDefault();if(!checkingIn)return;try{setBusy(checkingIn.id+"check-in");setError("");await api(`/api/v1/operations/bookings/${checkingIn.bookingNumber}/check-in`,{method:"POST",body:JSON.stringify({identityVerified:verified,verificationNote:note.trim()||null})});setCheckingIn(null);setVerified(false);setNote("");await load()}catch(err){setError(err instanceof ApiError?err.message:"Unable to check in customer.")}finally{setBusy("")}}

  if(!ready||!allowed)return <main className={styles.page}>Checking access…</main>;
  return <main className={styles.page}>
    <header className={styles.header}><div><span>OPERATIONS · {today}</span><h1>Today's Facility Operations</h1><p>Payment first, then verify customer, check in, and check out.</p></div><button onClick={()=>router.push("/dashboard")}>Customer portal</button></header>
    <section className={styles.filters}><label>Sport<select value={sport} onChange={e=>{setSport(e.target.value);setFacility("")}}><option value="">All assigned sports</option>{sports.map(x=><option key={x}>{x}</option>)}</select></label><label>Facility<select value={facility} onChange={e=>setFacility(e.target.value)}><option value="">All assigned facilities</option>{facilities.map(x=><option key={x}>{x}</option>)}</select></label><label>Search<input value={query} onChange={e=>setQuery(e.target.value)} placeholder="Booking, name, mobile"/></label><button onClick={()=>void load()}>Refresh</button></section>
    {error&&<div className={styles.error}>{error}</div>}
    <section className={styles.summary}><article><strong>{visible.filter(b=>b.paymentStatus==="UNPAID").length}</strong><span>Payment due</span></article><article><strong>{visible.filter(b=>!b.checkedInAt&&(b.paymentStatus==="PAID"||b.paymentStatus==="NOT_REQUIRED")).length}</strong><span>Ready</span></article><article><strong>{visible.filter(b=>!!b.checkedInAt&&!b.checkedOutAt).length}</strong><span>Checked in</span></article></section>
    <section className={styles.list}>{visible.map(b=>{const paid=b.paymentStatus==="PAID"||b.paymentStatus==="NOT_REQUIRED";return <article key={b.id} className={styles.card}><div className={styles.cardTop}><div><strong>{b.bookingNumber}</strong><span>{b.startTime}–{b.endTime}</span></div><b className={paid?styles.paid:styles.due}>{paid?"PAID / READY":"PAYMENT DUE"}</b></div><h2>{b.customerName||"Customer"}</h2><p>{b.customerMobile||b.customerEmail||"No contact"}</p><p>{b.sportName} · {b.facilityName}</p><div className={styles.actions}>{!b.checkedInAt&&paid&&<button onClick={()=>void action(b,"check-in")}>Verify & Check In</button>}{b.checkedInAt&&!b.checkedOutAt&&<button onClick={()=>void action(b,"check-out")}>Check Out</button>}{!b.checkedInAt&&<button className={styles.secondary} onClick={()=>void action(b,"no-show")}>No-show</button>}</div></article>})}{visible.length===0&&<p>No active bookings match the current filters.</p>}</section>
    {checkingIn&&<div className={styles.backdrop}><form className={styles.dialog} onSubmit={confirmCheckIn}><h2>Check in {checkingIn.bookingNumber}</h2><p>{checkingIn.customerName} · {checkingIn.facilityName}</p><label className={styles.check}><input type="checkbox" checked={verified} onChange={e=>setVerified(e.target.checked)}/> Customer identity verified</label><label>Verification note<input value={note} maxLength={120} onChange={e=>setNote(e.target.value)} placeholder="Example: Name matched photo ID"/></label><div className={styles.actions}><button type="button" className={styles.secondary} onClick={()=>setCheckingIn(null)}>Cancel</button><button disabled={!!busy}>{busy?"Checking in…":"Confirm Check-in"}</button></div></form></div>}
  </main>
}
