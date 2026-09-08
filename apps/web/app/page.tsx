import { ArrowRight, CalendarDays, Medal, Pause, Users } from "lucide-react";
import Image from "next/image";
import Link from "next/link";
import { SiteFooter } from "@/components/site-footer";
import { SiteHeader } from "@/components/site-header";
import { BookSlotLink } from "@/components/book-slot-link";
import { HomeFeatureBand } from "@/components/home-feature-band";
import { sports, values } from "@/lib/site-data";

export default function Home() {
  return <><SiteHeader/><main>
    <section className="home-hero">
      <video className="hero-video" autoPlay muted loop playsInline poster="/images/hero-sports.png" aria-label="Jnapanam sports activities"><source src="/videos/jnapanam-hero.mp4" type="video/mp4"/></video>
      <div className="hero-shade"/><div className="container hero-content">
        <span className="eyebrow gold">Welcome to Jnapanam Sports Zone</span><h1>Where Champions <em>Begin.</em></h1>
        <h2>Play <b>&bull;</b> Train <b>&bull;</b> Compete <b>&bull;</b> Grow</h2><p>Sports, fitness and community for every age in Holalkere.</p>
        <div className="button-row"><BookSlotLink/><Link className="btn outline light large" href="/sports">Explore Facilities <ArrowRight/></Link></div>
        <div className="video-label"><Pause size={18}/><span>Watch Jnapanam in Action</span></div>
      </div>
    </section>
    <section className="trust-strip"><div className="container trust-grid">
      <div><Medal/><span><strong>7 Sports</strong><small>Diverse facilities under one roof</small></span></div><div><Users/><span><strong>Expert Coaching</strong><small>Guidance with proven experience</small></span></div><div><Users/><span><strong>All Age Groups</strong><small>Programs for kids, youth & adults</small></span></div><div><CalendarDays/><span><strong>Flexible Memberships</strong><small>Plans that fit your goals</small></span></div>
    </div></section>
    <HomeFeatureBand/>
    <section className="values-band"><div className="container"><span className="eyebrow gold">Our Purpose</span><h2>More Than a Sports Centre</h2><p>Inspired by the timeless values of Swami Vivekananda, Jnapanam Sports Zone builds character, confidence and community through sport.</p><div className="values-grid">{values.map((value, i)=><div key={value}><span>0{i+1}</span><strong>{value}</strong></div>)}</div></div></section>
    <section className="section sports-section"><div className="container"><div className="section-heading"><div><span className="eyebrow">Discover Your Game</span><h2>Explore Our Sports & Activities</h2></div><Link className="text-link" href="/sports">View all facilities <ArrowRight/></Link></div><div className="sports-grid">{sports.map(({name,icon:Icon,text},index)=><Link className="sport-card" href="/sports" key={name}><div className={`sport-photo sport-photo-${index}`}><div className="sport-icon"><Icon/></div></div><div className="sport-card-body"><h3>{name}</h3><p>{text}</p><span>Explore <ArrowRight/></span></div></Link>)}</div></div></section>
    <section className="cta-band"><div className="container"><div><span className="eyebrow gold">Your journey starts here</span><h2>Ready to get moving?</h2><p>Register today and become part of Holalkere's growing sports community.</p></div><div className="button-row"><Link className="btn primary large" href="/register">Register Now</Link><Link className="btn outline light large" href="/contact">Talk to Us</Link></div></div></section>
  </main><SiteFooter/></>;
}
