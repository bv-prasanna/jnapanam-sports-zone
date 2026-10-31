"use client";
import {useCallback,useEffect,useMemo,useState} from "react";
import Link from "next/link";
import {ArrowRight,CalendarDays,Clock3,MapPin,CheckCircle2} from "lucide-react";
import {api,ApiError} from "@/lib/api";
import styles from "./public-catalog.module.css";

type Event={id:string;title:string;titleKn?:string;description:string;eventType:string;sportCode?:string;facilityName?:string;startsAt:string;endsAt:string;registrationUrl?:string;imageUrl?:string;status:string;featured:boolean};
type Gallery={id:string;title:string;mediaType:string;mediaUrl:string;thumbnailUrl?:string;altText?:string;sportCode?:string;published:boolean};
type Sport={id:string;code:string;name:string;description?:string;status:string;displayOrder:number};
type Facility={id:string;name:string;sportCode:string;status:string;locationLabel?:string;capacity:number;bookingPrice:number};
type Plan={id:string;name:string;description?:string;durationDays:number;price:number;status:string;sportCodes:string[]};
function useLive<T>(path:string){
 const [rows,setRows]=useState<T[]>([]),[loading,setLoading]=useState(true),[error,setError]=useState("");
 const refresh=useCallback(async()=>{
   setLoading(true);setError("");
   try{const value=await api<T[]>(path);setRows(Array.isArray(value)?value:[])}
   catch(e){setError(e instanceof ApiError?e.message:"Unable to load information right now.");}
   finally{setLoading(false)}
 },[path]);
 useEffect(()=>{void refresh()},[refresh]);
 return {rows,loading,error,refresh};
}
function ErrorNotice({error,refresh}:{error:string;refresh:()=>Promise<void>}){return <div className={styles.error} role="alert">{error}<button type="button" onClick={()=>void refresh()}>Retry</button></div>;}
function Empty({message}:{message:string}){return <div className={styles.empty}><strong>Updates will appear here</strong><p>{message}</p><Link href="/contact">Contact Jnapanam <ArrowRight size={15}/></Link></div>;}
const dateFmt=(value:string)=>{const d=new Date(value);return Number.isNaN(d.getTime())?"Date to be confirmed":new Intl.DateTimeFormat("en-IN",{day:"2-digit",month:"short",year:"numeric",hour:"2-digit",minute:"2-digit",timeZone:"Asia/Kolkata"}).format(d)+" IST"};
const safeLink=(url?:string)=>{if(!url)return "";try{const u=new URL(url);return u.protocol==="https:"||u.protocol==="http:"?u.href:""}catch{return ""}};
const sportPhoto=(text:string)=>{const x=text.toLowerCase();for(const [key,file] of [["swim","swimming"],["cricket","cricket"],["badminton","badminton"],["carrom","carrom"],["chess","chess"],["gym","gym"],["aerobic","aerobics"]])if(x.includes(key))return "/images/sport-"+file+".png";return "/images/hero-sports.png";};
export function PublishedEvents(){
 const {rows,loading,error,refresh}=useLive<Event>("/api/v1/public/content/events");
 const events=useMemo(()=>rows.filter(e=>e.status==="PUBLISHED").sort((a,b)=>a.startsAt.localeCompare(b.startsAt)),[rows]);
 const upcoming=events.filter(e=>new Date(e.endsAt).getTime()>=Date.now()),past=events.filter(e=>new Date(e.endsAt).getTime()<Date.now());
 return <section className="section"><div className="container">
   <div className={styles.sectionHead}><div><span className="eyebrow">Published by Jnapanam</span><h2>Events & Camps</h2><p>Live event information, schedules and registration links.</p></div><button type="button" onClick={()=>void refresh()}>Refresh</button></div>
   {loading?<p role="status">Loading published events…</p>:error?<ErrorNotice error={error} refresh={refresh}/>:
   events.length===0?<Empty message="No events are published yet. Please check back for confirmed camps, workshops and competitions."/>:<>
     <div className={styles.eventGrid}>{upcoming.map(e=><article key={e.id} className={styles.eventCard}>
       {e.imageUrl?<img src={e.imageUrl} alt={e.title} loading="lazy" className={styles.eventImage}/>:<div className={styles.photoFallback} style={{backgroundImage:"url('/images/hero-sports.png')"}} aria-hidden="true"/>}
       <div className={styles.eventInfo}><span className={styles.category}>{e.eventType.replaceAll("_"," ")}</span><h3>{e.title}</h3><p>{e.description}</p>
       <div className={styles.meta}><CalendarDays size={16}/><span>{dateFmt(e.startsAt)}</span></div>
       <div className={styles.meta}><Clock3 size={16}/><span>Ends {dateFmt(e.endsAt)}</span></div>
       {e.facilityName&&<div className={styles.meta}><MapPin size={16}/><span>{e.facilityName}</span></div>}
       {safeLink(e.registrationUrl)?<a className={styles.action} href={safeLink(e.registrationUrl)} target="_blank" rel="noopener noreferrer">Register for event <ArrowRight size={16}/></a>:
       <Link className={styles.action} href="/contact">Enquire about this event <ArrowRight size={16}/></Link>}</div>
     </article>)}</div>
     {upcoming.length===0&&<Empty message="There are no upcoming published events at the moment."/>}
     {past.length>0&&<details className={styles.archive}><summary>Past events ({past.length})</summary><ul>{past.map(e=><li key={e.id}><strong>{e.title}</strong> · {dateFmt(e.startsAt)}</li>)}</ul></details>}
   </>}
 </div></section>;
}
export function PublishedGallery(){
 const {rows,loading,error,refresh}=useLive<Gallery>("/api/v1/public/content/gallery");
 const gallery=rows.filter(r=>r.published&&!!r.mediaUrl);
 return <section className="section"><div className="container">
  <div className={styles.sectionHead}><div><span className="eyebrow">Real moments</span><h2>Photo & Video Gallery</h2><p>Only published media appears here.</p></div><button onClick={()=>void refresh()}>Refresh</button></div>
  {loading?<p>Loading gallery…</p>:error?<ErrorNotice error={error} refresh={refresh}/>:
   gallery.length===0?<Empty message="No gallery photos have been published yet."/>:
   <div className={styles.galleryGrid}>{gallery.map(item=><figure key={item.id}>
    {item.mediaType==="VIDEO"?<video controls preload="metadata" playsInline poster={item.thumbnailUrl||undefined}><source src={item.mediaUrl}/></video>:<img src={item.mediaUrl} loading="lazy" alt={item.altText||item.title}/>}
    <figcaption><span>{item.sportCode||"Jnapanam"}</span><strong>{item.title}</strong></figcaption></figure>)}</div>}
 </div></section>;
}
export function LiveSports(){
 const {rows:rowsSports,loading,error,refresh}=useLive<Sport>("/api/v1/public/sports");
 const {rows:rowsFacilities,loading:facilitiesLoading,error:facilitiesError,refresh:reloadFacilities}=useLive<Facility>("/api/v1/public/facilities");
 const sports=rowsSports.filter(s=>s.status==="ACTIVE").sort((a,b)=>a.displayOrder-b.displayOrder);
 return <section className="section"><div className="container"><div className={styles.sectionHead}><div><span className="eyebrow">Current activities</span><h2>Available Sports & Facilities</h2><p>Only active sports and facilities configured by the sports zone.</p></div></div>
 {(loading||facilitiesLoading)?<p>Loading active activities…</p>:error||facilitiesError?<ErrorNotice error={error||facilitiesError} refresh={async()=>{await Promise.all([refresh(),reloadFacilities()])}}/>:
 sports.length===0?<Empty message="Sports and facilities are being configured. Please contact our team for availability."/>:
 <div className={styles.sportsGrid}>{sports.map(s=>{const facilities=rowsFacilities.filter(f=>f.sportCode===s.code&&f.status==="ACTIVE");
 return <article className={styles.sportCard} key={s.id}><img src={sportPhoto(s.name)} alt="" loading="lazy"/><div><h3>{s.name}</h3><p>{s.description||"Explore available sports facilities and session options."}</p><div className={styles.sportFacilities}><b>{facilities.length} active facilities</b>{facilities.slice(0,4).map(f=><span key={f.id}><CheckCircle2 size={14}/>{f.name}</span>)}</div>
 <Link href="/dashboard?section=bookings" className={styles.action}>Check booking availability <ArrowRight size={16}/></Link></div></article>})}</div>}
 </div></section>;
}
export function LiveMemberships(){
 const {rows,loading,error,refresh}=useLive<Plan>("/api/v1/public/membership-plans");
 const plans=rows.filter(p=>p.status==="ACTIVE");
 return <section className="section"><div className="container"><div className={styles.sectionHead}><div><span className="eyebrow">Available membership plans</span><h2>Choose a Membership</h2><p>Published pricing and duration from the Jnapanam membership catalogue.</p></div><button onClick={()=>void refresh()}>Refresh</button></div>
 {loading?<p>Loading membership plans…</p>:error?<ErrorNotice error={error} refresh={refresh}/>:
 plans.length===0?<Empty message="No membership plans are active yet. Please contact our team for current options."/>:
 <div className={styles.planGrid}>{plans.map(p=><article key={p.id} className={styles.planCard}><span className={styles.category}>Membership plan</span><h3>{p.name}</h3><p>{p.description||"Membership access according to the active plan terms."}</p>
 <strong>{new Intl.NumberFormat("en-IN",{style:"currency",currency:"INR",maximumFractionDigits:2}).format(Number(p.price))}</strong><small>{p.durationDays} day{p.durationDays===1?"":"s"} · {p.sportCodes?.length||0} sports</small>
 <Link href="/dashboard?section=memberships" className={styles.action}>View and enrol <ArrowRight size={16}/></Link></article>)}</div>}
 </div></section>;
}
