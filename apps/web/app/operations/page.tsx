"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { api, ApiError } from "@/lib/api";
import { useApp } from "@/components/app-provider";
import styles from "./operations.module.css";
import { OperatorFacilityBoard } from "@/components/operator-facility-board";
import {PortalSideNavigation} from "@/components/portal-side-navigation";
import shell from "@/components/portal-shell.module.css";

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
  const {user,ready,signOut}=useApp();
  const allowed=!!user?.roles.some(role=>["SUPER_ADMIN","ADMIN","OPERATIONS"].includes(role));
  const today=todayLocal();
  const [selectedDate,setSelectedDate]=useState(today);
  const [bookings,setBookings]=useState<Booking[]>([]);
  const [checkingIn,setCheckingIn]=useState<Booking|null>(null); const [verified,setVerified]=useState(false); const [note,setNote]=useState("");
  const [busy,setBusy]=useState(""); const [error,setError]=useState(""); const [opsTab,setOpsTab]=useState<"bookings"|"incidents"|"equipment"|"handover">("bookings"); const [incidents,setIncidents]=useState<any[]>([]); const [equipment,setEquipment]=useState<any[]>([]); const [handovers,setHandovers]=useState<any[]>([]); const [opsText,setOpsText]=useState("");

  useEffect(()=>{if(ready&&!allowed)router.replace(user?"/dashboard":"/login")},[ready,allowed,user,router]);
  const load=useCallback(async()=>{if(!allowed)return;try{setError("");const rows=await api<Booking[]>(`/api/v1/admin/bookings?date=${selectedDate}`);setBookings(rows)}catch(e){setError(e instanceof ApiError?e.message:"Unable to load today's bookings.")}},[allowed,selectedDate]);
  useEffect(()=>{void load()},[load]); useEffect(()=>{if(!allowed)return;void Promise.all([api<any[]>("/api/v1/operations/incidents"),api<any[]>("/api/v1/operations/equipment"),api<any[]>("/api/v1/operations/handovers")]).then(([i,e,h])=>{setIncidents(i);setEquipment(e);setHandovers(h)}).catch(()=>{})},[allowed]);

  async function action(booking:Booking,kind:Action){try{setBusy(booking.id+kind);setError("");if(kind==="check-in"){setCheckingIn(booking);setBusy("");return}await api(`/api/v1/operations/bookings/${booking.id}/${kind}`,{method:"POST",body:kind==="no-show"?JSON.stringify({reason:"Customer did not arrive within the grace period"}):undefined});await load()}catch(e){setError(e instanceof ApiError?e.message:`Unable to ${kind}.`)}finally{if(kind!=="check-in")setBusy("")}}
  async function confirmCheckIn(e:FormEvent){e.preventDefault();if(!checkingIn)return;try{setBusy(checkingIn.id+"check-in");setError("");await api(`/api/v1/operations/bookings/${checkingIn.bookingNumber}/check-in`,{method:"POST",body:JSON.stringify({identityVerified:verified,verificationNote:note.trim()||null})});setCheckingIn(null);setVerified(false);setNote("");await load()}catch(err){setError(err instanceof ApiError?err.message:"Unable to check in customer.")}finally{setBusy("")}}

  if(!ready||!allowed)return <main className={styles.page}>Checking access…</main>;
  return <div className={shell.shell}><PortalSideNavigation active="operations"/><main className={styles.page}>
    <header className={styles.header}><div><span>OPERATIONS · {selectedDate}</span><h1>{selectedDate===today?"Today's Facility Operations":"Facility Operations"}</h1><p>Payment first, then verify customer, check in, and check out.</p></div><div className={styles.headerActions}><button onClick={()=>router.push("/dashboard")}>Customer portal</button><button onClick={()=>void signOut().then(()=>router.replace("/login"))}>Logout</button></div></header>
    <nav className={styles.opsTabs}><button onClick={()=>setOpsTab("bookings")}>Bookings</button><button onClick={()=>setOpsTab("incidents")}>Incidents</button><button onClick={()=>setOpsTab("equipment")}>Equipment</button><button onClick={()=>setOpsTab("handover")}>Shift Handover</button></nav>{opsTab==="bookings"&&<OperatorFacilityBoard date={selectedDate} setDate={setSelectedDate} bookings={bookings} reload={load} onAction={action}/>}{opsTab==="incidents"&&<section className={styles.opsPanel}><h2>Incident Log</h2><form onSubmit={async e=>{e.preventDefault();await api("/api/v1/operations/incidents",{method:"POST",body:JSON.stringify({incidentType:"GENERAL",severity:"NORMAL",description:opsText})});setOpsText("");setIncidents(await api<any[]>("/api/v1/operations/incidents"))}}><textarea required value={opsText} onChange={e=>setOpsText(e.target.value)} placeholder="Describe incident, injury, damage, closure or lost/found item"/><button>Record Incident</button></form>{incidents.map(x=><article key={x.id}><b>{x.incident_type}</b> · {x.severity}<p>{x.description}</p></article>)}</section>}{opsTab==="equipment"&&<section className={styles.opsPanel}><h2>Equipment Register</h2><p>Issue/return records for onsite equipment. New issues can be recorded against booking/customer through the operations API.</p>{equipment.map(x=><article key={x.id}><b>{x.equipment_name}</b> × {x.quantity}<p>{x.returned_at?"Returned":"Issued / awaiting return"}</p></article>)}</section>}{opsTab==="handover"&&<section className={styles.opsPanel}><h2>Shift Handover</h2><form onSubmit={async e=>{e.preventDefault();await api("/api/v1/operations/handovers",{method:"POST",body:JSON.stringify({shiftDate:today,notes:opsText})});setOpsText("");setHandovers(await api<any[]>("/api/v1/operations/handovers"))}}><textarea required value={opsText} onChange={e=>setOpsText(e.target.value)} placeholder="Pending payments, incidents, equipment, closures, customer follow-ups…"/><button>Save Handover</button></form>{handovers.map(x=><article key={x.id}><b>{x.shift_date} · {x.staff_name}</b><p>{x.notes}</p></article>)}</section>}
    {checkingIn&&<div className={styles.backdrop}><form className={styles.dialog} onSubmit={confirmCheckIn}><h2>Check in {checkingIn.bookingNumber}</h2><p>{checkingIn.customerName} · {checkingIn.facilityName}</p><label className={styles.check}><input type="checkbox" checked={verified} onChange={e=>setVerified(e.target.checked)}/> Customer identity verified</label><label>Verification note<input value={note} maxLength={120} onChange={e=>setNote(e.target.value)} placeholder="Example: Name matched photo ID"/></label><div className={styles.actions}><button type="button" className={styles.secondary} onClick={()=>{setCheckingIn(null);setBusy("");}}>Cancel</button><button disabled={!!busy}>{busy?"Checking in…":"Confirm Check-in"}</button></div></form></div>}
  </main></div>
}
