"use client";

import { ChevronLeft, ChevronRight, PlayCircle } from "lucide-react";
import Image from "next/image";
import Link from "next/link";
import { useEffect, useState } from "react";
import {api} from "@/lib/api";
import responsive from "./founder-responsive.module.css";

type LiveEvent={id:string;title:string;description:string;eventType:string;startsAt:string;endsAt:string;status:string;imageUrl?:string};

export function HomeFeatureBand(){
 const [events,setEvents]=useState<LiveEvent[]>([]);const[index,setIndex]=useState(0);const event=events[index];
 useEffect(()=>{let active=true;api<LiveEvent[]>("/api/v1/public/content/events").then(rows=>{if(active){setEvents((rows||[]).filter(e=>e.status==="PUBLISHED"&&new Date(e.endsAt).getTime()>=Date.now()).sort((a,b)=>a.startsAt.localeCompare(b.startsAt)).slice(0,5));setIndex(0);}}).catch(()=>{if(active)setEvents([])});return()=>{active=false}},[]);
 useEffect(()=>{if(events.length<2)return;const timer=window.setInterval(()=>setIndex(i=>(i+1)%events.length),5500);return()=>window.clearInterval(timer)},[events.length]);
 const move=(step:number)=>setIndex(i=>events.length?(i+step+events.length)%events.length:0);
 return <section className="feature-band section"><div className="container feature-grid">
  <article className="feature-card quote-card"><div className="swami-crop"><Image unoptimized src="/images/swami-vivekananda.png" alt="Swami Vivekananda" fill sizes="280px"/></div><div><span className="eyebrow">Words That Inspire</span><blockquote>&ldquo;Arise, awake, and stop not till the goal is reached.&rdquo;</blockquote><cite>&mdash; Swami Vivekananda</cite></div></article>
  <article className="feature-card event-card"><div className="section-kicker"><span/>Upcoming Events<span/></div>
  {event?<><div className="event-body"><div className="event-visual" style={{backgroundImage:"linear-gradient(#0003,#0008),url('"+(event.imageUrl||"/images/hero-sports.png")+"')"}}><PlayCircle/><span>{event.eventType.replaceAll("_"," ")}</span></div><div><h3>{event.title}</h3><p>{event.description}</p><p className="event-meta">{new Intl.DateTimeFormat("en-IN",{day:"2-digit",month:"short",year:"numeric",timeZone:"Asia/Kolkata"}).format(new Date(event.startsAt))}</p><Link className="text-link" href="/events">Event details</Link></div></div>
  {events.length>1&&<div className="slider-controls"><button type="button" aria-label="Previous event" onClick={()=>move(-1)}><ChevronLeft/></button>{events.map((x,i)=><button type="button" className={i===index?"event-dot active":"event-dot"} aria-label={"Show "+x.title} onClick={()=>setIndex(i)} key={x.id}/>)}<button type="button" aria-label="Next event" onClick={()=>move(1)}><ChevronRight/></button></div>}</>
  :<div className="event-body"><div className="event-visual" style={{backgroundImage:"linear-gradient(#0003,#0008),url('/images/hero-sports.png')"}}><PlayCircle/><span>Jnapanam Sports Zone</span></div><div><h3>No upcoming events published</h3><p>Confirmed camps and competitions will appear here when announced.</p><Link className="text-link" href="/events">See all events</Link></div></div>}
  </article>
  <article className={"feature-card founder-card "+responsive.card}><div><span className="eyebrow">Founder's Message</span><h3>Shri L Jayasimha Khatroth</h3><p>Jnapanam Sports Zone makes quality sports and fitness accessible to families in Holalkere. We build healthier lives through disciplined training and an inclusive community.</p></div><div className={"founder-portrait "+responsive.portrait}><Image unoptimized src="/images/founder-jayasimha.png" alt="Founder Shri L Jayasimha Khatroth" fill sizes="(max-width: 520px) 240px, (max-width: 1050px) 32vw, 220px"/></div></article>
 </div></section>
}
