import Link from "next/link";
import { ArrowRight, CheckCircle2 } from "lucide-react";
import { SiteHeader } from "@/components/site-header";
import { SiteFooter } from "@/components/site-footer";
import { sports } from "@/lib/site-data";

export default function Sports(){return <><SiteHeader/><main>
  <section className="page-hero sports-hero"><div className="container"><span className="eyebrow gold">Sports & Facilities</span><h1>Find Your Game</h1><p>Seven distinct activities, welcoming facilities and guidance for every stage of your sporting journey.</p></div></section>
  <section className="section"><div className="container facility-list">{sports.map(({name,icon:Icon,text},i)=><article className="facility-card" key={name}><div className={`facility-photo sport-photo-${i}`}/><div><Icon/><h2>{name}</h2><p>{text}</p><ul><li><CheckCircle2/>Recreation and regular practice</li><li><CheckCircle2/>Supportive learning environment</li></ul><Link className="text-link" href="/contact">Enquire now <ArrowRight/></Link></div></article>)}</div></section>
  <section className="inner-cta"><div className="container"><span className="eyebrow">Choose your activity</span><h2>Ready to begin?</h2><p>Tell us which sport interests you and we’ll help with the next step.</p><Link className="btn primary" href="/register">Register Your Interest</Link></div></section>
</main><SiteFooter/></>}
