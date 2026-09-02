import Link from "next/link";
import { ArrowRight, CalendarCheck, Dumbbell, Trophy, Users } from "lucide-react";
import { SiteHeader } from "@/components/site-header";
import { SiteFooter } from "@/components/site-footer";

const services=[{icon:CalendarCheck,title:"Facility Booking",text:"Plan recreational play and focused practice across available sports spaces."},{icon:Users,title:"Coaching Programs",text:"Structured guidance for skills, confidence and steady sporting progress."},{icon:Dumbbell,title:"Fitness Guidance",text:"Gym and aerobics support built around safe, sustainable movement."},{icon:Trophy,title:"Camps & Tournaments",text:"Seasonal camps and community events that encourage participation."}];
export default function Services(){return <><SiteHeader/><main>
  <section className="page-hero services-hero"><div className="container"><span className="eyebrow gold">Our Services</span><h1>Support for Every Sporting Journey</h1><p>Flexible ways to practise, learn, improve and participate at Jnapanam.</p></div></section>
  <section className="section"><div className="container services-layout"><div className="service-intro"><span className="eyebrow">What We Offer</span><h2>Everything you need to keep moving</h2><p>Start with one activity or build a complete routine around sport and fitness.</p><Link className="btn primary" href="/contact">Talk to our team <ArrowRight/></Link></div><div className="service-stack">{services.map(({icon:Icon,title,text},i)=><article key={title}><span>0{i+1}</span><Icon/><div><h3>{title}</h3><p>{text}</p></div></article>)}</div></div></section>
  <section className="process-band"><div className="container"><span className="eyebrow gold">Simple to Start</span><h2>Your first visit in four steps</h2><div className="process-grid">{["Choose an activity","Contact our team","Find a suitable slot","Play and progress"].map((x,i)=><div key={x}><b>{i+1}</b><strong>{x}</strong></div>)}</div></div></section>
</main><SiteFooter/></>}
