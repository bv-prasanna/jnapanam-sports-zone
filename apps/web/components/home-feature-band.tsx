"use client";

import { ChevronLeft, ChevronRight, PlayCircle } from "lucide-react";
import Image from "next/image";
import Link from "next/link";
import { useEffect, useState } from "react";

const events=[
 {title:"Summer Cricket Camp",summary:"Dates and registrations will be announced shortly.",label:"Cricket Camp",image:"/images/sport-cricket.png"},
 {title:"Swimming Coaching Camp",summary:"Structured swimming sessions for children and adults.",label:"Swimming Camp",image:"/images/sport-swimming.png"},
 {title:"Badminton Championship",summary:"Competitive matches and community participation at Jnapanam.",label:"Badminton Event",image:"/images/sport-badminton.png"}
];

export function HomeFeatureBand(){
 const[index,setIndex]=useState(0);const event=events[index];
 useEffect(()=>{const timer=window.setInterval(()=>setIndex(i=>(i+1)%events.length),5500);return()=>window.clearInterval(timer)},[]);
 const move=(step:number)=>setIndex(i=>(i+step+events.length)%events.length);
 return <section className="feature-band section"><div className="container feature-grid">
  <article className="feature-card quote-card"><div className="swami-crop"><Image unoptimized src="/images/swami-vivekananda.png" alt="Swami Vivekananda" fill sizes="280px"/></div><div><span className="eyebrow">Words That Inspire</span><blockquote>&ldquo;Arise, awake, and stop not till the goal is reached.&rdquo;</blockquote><cite>&mdash; Swami Vivekananda</cite></div></article>
  <article className="feature-card event-card"><div className="section-kicker"><span/>Upcoming Events<span/></div><div className="event-body"><div className="event-visual" style={{backgroundImage:`linear-gradient(#0003,#0008),url('${event.image}')`}}><PlayCircle/><span>{event.label}</span></div><div><h3>{event.title}</h3><p>{event.summary}</p><p className="event-meta">Jnapanam Sports Zone, Holalkere</p><Link className="text-link" href="/events">View all events</Link></div></div><div className="slider-controls"><button type="button" aria-label="Previous event" onClick={()=>move(-1)}><ChevronLeft/></button>{events.map((x,i)=><button type="button" className={i===index?"event-dot active":"event-dot"} aria-label={`Show ${x.title}`} onClick={()=>setIndex(i)} key={x.title}/>)}<button type="button" aria-label="Next event" onClick={()=>move(1)}><ChevronRight/></button></div></article>
  <article className="feature-card founder-card"><div><span className="eyebrow">Founder's Message</span><h3>Shri L Jayasimha Khatroth</h3><p>Jnapanam Sports Zone makes quality sports and fitness accessible to families in Holalkere. We build healthier lives through disciplined training and an inclusive community.</p></div><div className="founder-portrait"><Image unoptimized src="/images/founder-jayasimha.png" alt="Founder Shri L Jayasimha Khatroth" fill sizes="(max-width: 520px) 240px, (max-width: 1050px) 32vw, 220px"/></div></article>
 </div></section>
}
