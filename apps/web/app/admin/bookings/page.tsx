"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { api, ApiError } from "@/lib/api";
import { useApp } from "@/components/app-provider";
import styles from "./bookings.module.css";

type Row = Record<string, unknown>;
type Sport = { code:string; name:string; status?:string };
type Facility = { id:string; name:string; sportName?:string; sportCode?:string; status?:string };
type Booking = {
  id:string; bookingNumber:string; customerName?:string; customerEmail?:string; customerMobile?:string;
  sportName?:string; facilityId?:string; facilityName?:string; date?:string; startTime?:string; endTime?:string;
  quantity?:number; status:string; paymentOption?:string; paymentStatus?:string; checkedInAt?:string|null; checkedOutAt?:string|null;
  checkedInBy?:string; checkedOutBy?:string; onsitePaymentMethod?:string; onsitePaymentReference?:string; statusReason?:string;
};

type RangePreset = "TODAY"|"TOMORROW"|"WEEK"|"MONTH"|"CUSTOM";

function iso(date:Date){return date.toISOString().slice(0,10)}
function todayLocal(){const d=new Date();return new Intl.DateTimeFormat("en-CA",{timeZone:"Asia/Kolkata",year:"numeric",month:"2-digit",day:"2-digit"}).format(d)}
function addDays(value:string,days:number){const d=new Date(`${value}T00:00:00`);d.setDate(d.getDate()+days);return iso(d)}
function monthEnd(value:string){const [y,m]=value.split("-").map(Number);return iso(new Date(y,m,0))}
function pretty(value?:string){return value?value.replaceAll("_"," "):"—"}

export default function AdminBookingExplorer(){
  const router=useRouter();
  const {user,ready}=useApp();
  const allowed=!!user?.roles.some(role=>["SUPER_ADMIN","ADMIN"].includes(role));
  const today=todayLocal();
  const [sport,setSport]=useState(""); const [facilityId,setFacilityId]=useState("");
  const [from,setFrom]=useState(today); const [to,setTo]=useState(today); const [preset,setPreset]=useState<RangePreset>("TODAY");
  const [status,setStatus]=useState(""); const [paymentStatus,setPaymentStatus]=useState(""); const [query,setQuery]=useState("");
  const [sports,setSports]=useState<Sport[]>([]); const [facilities,setFacilities]=useState<Facility[]>([]); const [bookings,setBookings]=useState<Booking[]>([]);
  const [selected,setSelected]=useState<Booking|null>(null); const [busy,setBusy]=useState(false); const [error,setError]=useState("");

  useEffect(()=>{if(ready&&!allowed)router.replace(user?"/dashboard":"/login")},[ready,allowed,user,router]);
  useEffect(()=>{if(!allowed)return;Promise.all([api<Sport[]>("/api/v1/admin/catalog/sports"),api<Facility[]>("/api/v1/admin/catalog/facilities")]).then(([s,f])=>{setSports(s.filter(x=>x.status!=="INACTIVE"));setFacilities(f.filter(x=>x.status!=="INACTIVE"))}).catch(()=>setError("Unable to load sport/facility filters."))},[allowed]);

  const availableFacilities=useMemo(()=>facilities.filter(f=>!sport||f.sportName===sport||f.sportCode===sport),[facilities,sport]);
  useEffect(()=>{if(facilityId&&!availableFacilities.some(f=>f.id===facilityId))setFacilityId("")},[availableFacilities,facilityId]);

  const load=useCallback(async()=>{
    if(!allowed)return;
    if(from&&to&&from>to){setError("From date must be on or before To date.");return}
    const p=new URLSearchParams(); if(from)p.set("from",from); if(to)p.set("to",to); if(sport)p.set("sport",sport); if(facilityId)p.set("facilityId",facilityId); if(status)p.set("status",status); if(paymentStatus)p.set("paymentStatus",paymentStatus); if(query.trim())p.set("q",query.trim());
    try{setBusy(true);setError("");setBookings(await api<Booking[]>(`/api/v1/admin/bookings?${p.toString()}`))}catch(e){setError(e instanceof ApiError?e.message:"Unable to load bookings.")}finally{setBusy(false)}
  },[allowed,from,to,sport,facilityId,status,paymentStatus,query]);
  useEffect(()=>{void load()},[load]);

  function applyPreset(next:RangePreset){setPreset(next);if(next==="CUSTOM")return;const base=today;if(next==="TODAY"){setFrom(base);setTo(base)}else if(next==="TOMORROW"){const d=addDays(base,1);setFrom(d);setTo(d)}else if(next==="WEEK"){setFrom(base);setTo(addDays(base,6))}else{setFrom(base.slice(0,7)+"-01");setTo(monthEnd(base))}}
  function clear(){setSport("");setFacilityId("");setStatus("");setPaymentStatus("");setQuery("");setFrom(today);setTo(today);setPreset("TODAY")}

  const counts=useMemo(()=>({total:bookings.length,confirmed:bookings.filter(b=>b.status==="CONFIRMED").length,paymentDue:bookings.filter(b=>b.paymentStatus==="UNPAID").length,checkedIn:bookings.filter(b=>!!b.checkedInAt&&!b.checkedOutAt).length,completed:bookings.filter(b=>!!b.checkedOutAt).length,cancelled:bookings.filter(b=>b.status==="CANCELLED").length}),[bookings]);

  if(!ready||!allowed)return <main className={styles.page}>Checking access…</main>;
  return <main className={styles.page}>
    <header className={styles.header}><div><span>ADMIN</span><h1>Booking Explorer</h1><p>Find slot bookings by sport, facility, date range, status, payment, or customer.</p></div><Link href="/admin" className={styles.back}>← Admin console</Link></header>

    <section className={styles.presets}>{(["TODAY","TOMORROW","WEEK","MONTH","CUSTOM"] as RangePreset[]).map(x=><button key={x} className={preset===x?styles.active:""} onClick={()=>applyPreset(x)}>{x==="WEEK"?"This week":x==="MONTH"?"This month":x[0]+x.slice(1).toLowerCase()}</button>)}</section>

    <section className={styles.filters}>
      <label>Sport<select value={sport} onChange={e=>{setSport(e.target.value);setFacilityId("")}}><option value="">All sports</option>{sports.map(s=><option key={s.code} value={s.name}>{s.name}</option>)}</select></label>
      <label>Facility<select value={facilityId} onChange={e=>setFacilityId(e.target.value)}><option value="">All facilities</option>{availableFacilities.map(f=><option key={f.id} value={f.id}>{f.name}</option>)}</select></label>
      <label>From<input type="date" value={from} onChange={e=>{setFrom(e.target.value);setPreset("CUSTOM")}}/></label>
      <label>To<input type="date" value={to} onChange={e=>{setTo(e.target.value);setPreset("CUSTOM")}}/></label>
      <label>Booking status<select value={status} onChange={e=>setStatus(e.target.value)}><option value="">All statuses</option>{["PENDING_PAYMENT","CONFIRMED","PAYMENT_EXPIRED","NO_SHOW","CANCELLED"].map(x=><option key={x}>{x}</option>)}</select></label>
      <label>Payment<select value={paymentStatus} onChange={e=>setPaymentStatus(e.target.value)}><option value="">All payments</option>{["NOT_REQUIRED","UNPAID","PAID","REFUNDED"].map(x=><option key={x}>{x}</option>)}</select></label>
      <label className={styles.search}>Booking / customer<input value={query} onChange={e=>setQuery(e.target.value)} placeholder="Booking no., name, mobile, email"/></label>
      <div className={styles.filterActions}><button className={styles.primary} onClick={()=>void load()} disabled={busy}>{busy?"Loading…":"Apply filters"}</button><button onClick={clear}>Reset</button></div>
    </section>

    {error&&<div className={styles.error}>{error}</div>}
    <section className={styles.metrics}>
      <article><strong>{counts.total}</strong><span>Results</span></article><article><strong>{counts.confirmed}</strong><span>Confirmed</span></article><article><strong>{counts.paymentDue}</strong><span>Payment due</span></article><article><strong>{counts.checkedIn}</strong><span>Checked in</span></article><article><strong>{counts.completed}</strong><span>Completed</span></article><article><strong>{counts.cancelled}</strong><span>Cancelled</span></article>
    </section>

    <section className={styles.tableWrap}><table><thead><tr><th>Date / Slot</th><th>Sport / Facility</th><th>Customer</th><th>Booking</th><th>Payment</th><th>Status</th><th></th></tr></thead><tbody>{bookings.map(b=><tr key={b.id}><td><b>{b.date||"—"}</b><small>{b.startTime||"—"} – {b.endTime||"—"}</small></td><td><b>{b.sportName||"—"}</b><small>{b.facilityName||"—"}</small></td><td><b>{b.customerName||"—"}</b><small>{b.customerMobile||b.customerEmail||"—"}</small></td><td><b>{b.bookingNumber}</b><small>Qty {b.quantity??1}</small></td><td><b>{pretty(b.paymentStatus)}</b><small>{pretty(b.paymentOption)}</small></td><td><span className={styles.status}>{pretty(b.status)}</span></td><td><button onClick={()=>setSelected(b)}>View</button></td></tr>)}</tbody></table>{!busy&&bookings.length===0&&<p className={styles.empty}>No bookings match these filters.</p>}</section>

    {selected&&<div className={styles.backdrop} onMouseDown={()=>setSelected(null)}><section className={styles.dialog} onMouseDown={e=>e.stopPropagation()}><button className={styles.close} onClick={()=>setSelected(null)}>×</button><span>BOOKING DETAILS</span><h2>{selected.bookingNumber}</h2><div className={styles.details}>{Object.entries(selected).filter(([k,v])=>v!=null&&!['id','facilityId'].includes(k)).map(([k,v])=><div key={k}><small>{k.replace(/([A-Z])/g," $1")}</small><b>{String(v)}</b></div>)}</div><button onClick={()=>setSelected(null)}>Close</button></section></div>}
  </main>;
}
