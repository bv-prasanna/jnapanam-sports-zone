"use client";
import {useCallback,useEffect,useMemo,useState} from "react";
import {api,ApiError} from "@/lib/api";
import styles from "./operator-facility-board.module.css";

type Facility={facilityId:string;facilityCode:string;facilityName:string;sportCode:string;sportName:string};
type Slot={id:string;facilityId:string;startTime:string;endTime:string;capacity:number;available:number};
export type OperatorBooking={
 id:string;slotId?:string;facilityId?:string;bookingNumber:string;customerName?:string;customerEmail?:string;customerMobile?:string;
 sportName?:string;facilityName?:string;date?:string;startTime?:string;endTime?:string;quantity?:number;
 status:string;paymentStatus?:string;checkedInAt?:string|null;checkedOutAt?:string|null;
};
type Action="check-in"|"check-out"|"no-show";
type Props={date:string;setDate:(value:string)=>void;bookings:OperatorBooking[];reload:()=>Promise<void>;onAction:(booking:OperatorBooking,kind:Action)=>Promise<void>};
type SlotGroup={key:string;startTime:string;endTime:string;capacity:number|null;available:number|null;bookings:OperatorBooking[]};
const readable=(s?:string)=>s?s.replaceAll("_"," ").toLowerCase().replace(/^./,letter=>letter.toUpperCase()):"—";
const clock=(s?:string)=>s?s.slice(0,5):"—";
const today=()=>new Intl.DateTimeFormat("en-CA",{timeZone:"Asia/Kolkata",year:"numeric",month:"2-digit",day:"2-digit"}).format(new Date());

export function OperatorFacilityBoard({date,setDate,bookings,reload,onAction}:Props){
 const [facilities,setFacilities]=useState<Facility[]>([]);
 const [assignedBusy,setAssignedBusy]=useState(true);
 const [facilityError,setFacilityError]=useState("");
 const [selectedFacility,setSelectedFacility]=useState("");
 const [slots,setSlots]=useState<Slot[]>([]);
 const [slotError,setSlotError]=useState("");
 const [slotBusy,setSlotBusy]=useState(false);
 const [search,setSearch]=useState("");
 const loadFacilities=useCallback(async()=>{
   setAssignedBusy(true);setFacilityError("");
   try{setFacilities(await api<Facility[]>("/api/v1/operations/facilities"));}
   catch(e){setFacilityError(e instanceof ApiError?e.message:"Unable to load assigned facilities.");setFacilities([]);}
   finally{setAssignedBusy(false);}
 },[]);
 useEffect(()=>{void loadFacilities();},[loadFacilities]);
 useEffect(()=>{if(!facilities.some(f=>f.facilityId===selectedFacility))setSelectedFacility(facilities[0]?.facilityId||"");},[facilities,selectedFacility]);
 const reloadSlots=useCallback(async()=>{
   setSlots([]);setSlotError("");
   if(!selectedFacility||date<today())return;
   setSlotBusy(true);
   try{setSlots(await api<Slot[]>("/api/v1/public/facilities/"+selectedFacility+"/slots?date="+encodeURIComponent(date)));}
   catch(e){setSlotError(e instanceof ApiError?e.message:"Unable to load available slots. Existing bookings remain visible.");}
   finally{setSlotBusy(false);}
 },[date,selectedFacility]);
 useEffect(()=>{void reloadSlots();},[reloadSlots]);
 const chosen=facilities.find(f=>f.facilityId===selectedFacility);
 const selectedBookings=useMemo(()=>bookings.filter(b=>b.facilityId===selectedFacility),[bookings,selectedFacility]);
 const groups=useMemo(()=>{
   const grouped=new Map<string,SlotGroup>();
   for(const slot of slots)grouped.set(slot.id,{key:slot.id,startTime:slot.startTime,endTime:slot.endTime,capacity:slot.capacity,available:slot.available,bookings:[]});
   for(const b of selectedBookings){
     const key=b.slotId||"booking-"+b.id;
     const existing=grouped.get(key);
     if(existing)existing.bookings.push(b);
     else grouped.set(key,{key,startTime:b.startTime||"",endTime:b.endTime||"",capacity:null,available:null,bookings:[b]});
   }
   const q=search.trim().toLowerCase();
   return [...grouped.values()].map(g=>({...g,bookings:g.bookings.slice().sort((a,b)=>a.bookingNumber.localeCompare(b.bookingNumber))}))
     .filter(g=>!q||[g.startTime,g.endTime,...g.bookings.flatMap(b=>[b.bookingNumber,b.customerName||"",b.customerMobile||"",b.status,b.paymentStatus||""])].some(v=>v.toLowerCase().includes(q)))
     .sort((a,b)=>a.startTime.localeCompare(b.startTime)||a.endTime.localeCompare(b.endTime));
 },[slots,selectedBookings,search]);
 const activeBookings=selectedBookings.filter(b=>!["CANCELLED","NO_SHOW"].includes(b.status));
 const pending=activeBookings.filter(b=>b.paymentStatus==="UNPAID"||b.paymentStatus==="PENDING").length;
 const checkedIn=selectedBookings.filter(b=>b.checkedInAt&&!b.checkedOutAt).length;
 const refreshAll=async()=>{await Promise.all([reload(),reloadSlots(),loadFacilities()]);};
 const canNoShow=(b:OperatorBooking)=>{
   if(b.checkedInAt||["CANCELLED","NO_SHOW"].includes(b.status))return false;
   const start=new Date(date+"T"+clock(b.startTime)+":00+05:30");
   return !Number.isNaN(start.getTime())&&Date.now()>start.getTime()+15*60*1000;
 };
 return <section className={styles.board}>
   <div className={styles.toolbar}><div><span className={styles.eyebrow}>ASSIGNED FACILITIES</span><h2>Facility schedule & bookings</h2><p>Select a facility to see its slots, booked customers and payment/check-in status.</p></div>
     <div className={styles.toolActions}><label>Date<input aria-label="Booking date" type="date" value={date} onChange={e=>setDate(e.target.value)}/></label><button type="button" onClick={()=>void refreshAll()}>Refresh</button></div></div>
   {facilityError&&<p className={styles.error} role="alert">{facilityError}</p>}
   {assignedBusy?<div className={styles.empty}>Loading assigned facilities…</div>:
   facilities.length===0?<div className={styles.empty}><strong>No facilities assigned</strong><p>An administrator can assign facilities under Operator Assignments.</p></div>:
   <>
     <div className={styles.tabs} role="tablist" aria-label="Assigned facilities">{facilities.map(f=>{
       const num=bookings.filter(b=>b.facilityId===f.facilityId).length;
       return <button key={f.facilityId} type="button" role="tab" aria-selected={selectedFacility===f.facilityId}
         className={selectedFacility===f.facilityId?styles.selected:""} onClick={()=>{setSelectedFacility(f.facilityId);setSearch("");}}>
         <span>{f.facilityName}</span><small>{f.sportName} · {num} bookings</small></button>;
     })}</div>
     <div className={styles.panel} role="tabpanel">
       <div className={styles.panelHeading}><div><h3>{chosen?.facilityName||"Facility"}</h3><p>{chosen?.sportName} · {date}</p></div>
         <input aria-label="Search bookings" type="search" value={search} onChange={e=>setSearch(e.target.value)} placeholder="Search time, customer, booking or status"/></div>
       <div className={styles.metrics}>
         <article><span>Total bookings</span><strong>{selectedBookings.length}</strong></article>
         <article><span>Checked in</span><strong>{checkedIn}</strong></article>
         <article><span>Payment pending</span><strong>{pending}</strong></article>
         <article><span>Slots displayed</span><strong>{groups.length}</strong></article>
       </div>
       {slotError&&<div className={styles.warning} role="status">{slotError}</div>}
       {slotBusy&&<p className={styles.muted}>Loading available slots…</p>}
       <div className={styles.tableScroll}><table className={styles.table}><thead><tr><th>Slot / Capacity</th><th>Customer & Booking</th><th>Booking</th><th>Payment</th><th>Visit</th><th>Actions</th></tr></thead><tbody>
       {groups.flatMap(g=>g.bookings.length?g.bookings.map((b,index)=>{
         const paid=b.paymentStatus==="PAID"||b.paymentStatus==="NOT_REQUIRED";
         const checkout=!!b.checkedInAt&&!b.checkedOutAt;
         const checkin=!b.checkedInAt&&!["CANCELLED","NO_SHOW"].includes(b.status)&&paid;
         const noShow=canNoShow(b);
         return <tr key={b.id}>
           <td>{index===0?<><strong className={styles.time}>{clock(g.startTime)} – {clock(g.endTime)}</strong><small>{g.capacity===null?"Booked slot":String(g.available)+" of "+String(g.capacity)+" places available"}</small></>:<span className={styles.muted}>↳ Same slot</span>}</td>
           <td><b>{b.customerName||"Name unavailable"}</b><small>{b.bookingNumber}{b.customerMobile?" · "+b.customerMobile:""}</small></td>
           <td><span className={styles.status}>{readable(b.status)}</span></td>
           <td><span className={paid?styles.paid:styles.due}>{readable(b.paymentStatus)}</span></td>
           <td><span className={styles.status}>{b.checkedOutAt?"Checked out":b.checkedInAt?"Checked in":"Not checked in"}</span></td>
           <td><div className={styles.rowActions}>{checkin&&<button type="button" onClick={()=>void onAction(b,"check-in")}>Check in</button>}{checkout&&<button type="button" onClick={()=>void onAction(b,"check-out")}>Check out</button>}{noShow&&<button type="button" className={styles.secondary} onClick={()=>void onAction(b,"no-show")}>No-show</button>}{!checkin&&!checkout&&!noShow&&<small>—</small>}</div></td>
         </tr>;
       }):[<tr key={g.key}><td><strong className={styles.time}>{clock(g.startTime)} – {clock(g.endTime)}</strong><small>{g.available} of {g.capacity} places available</small></td><td colSpan={5} className={styles.unbooked}>No booking for this slot</td></tr>])}</tbody></table></div>
       {groups.length===0&&<div className={styles.empty}><strong>No slots or bookings found</strong><p>{search?"Try another search.":"No slots or bookings are recorded for this facility and date."}</p></div>}
       <p className={styles.note}>Includes cancelled, no-show and checked-out bookings for shift history. Availability is shown for upcoming slots; past booked slots remain visible.</p>
     </div>
   </>}
 </section>;
}
