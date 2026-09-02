import { ArrowRight, CalendarDays, ChevronLeft, ChevronRight, Medal, Pause, PlayCircle, Users } from "lucide-react";
import Image from "next/image";
import Link from "next/link";
import { SiteFooter } from "@/components/site-footer";
import { SiteHeader } from "@/components/site-header";
import { sports, values } from "@/lib/site-data";

export default function Home() {
  return <><SiteHeader/><main>
    <section className="home-hero">
      <video className="hero-video" autoPlay muted loop playsInline poster="/images/hero-sports.png" aria-label="Jnapanam sports activities"><source src="/videos/jnapanam-hero.mp4" type="video/mp4"/></video>
      <div className="hero-shade"/><div className="container hero-content">
        <span className="eyebrow gold">Welcome to Jnapanam Sports Zone</span><h1>Where Champions <em>Begin.</em></h1>
        <h2>Play <b>&bull;</b> Train <b>&bull;</b> Compete <b>&bull;</b> Grow</h2><p>Sports, fitness and community for every age in Holalkere.</p>
        <div className="button-row"><Link className="btn primary large" href="/register">Book a Slot <ArrowRight/></Link><Link className="btn outline light large" href="/sports">Explore Facilities <ArrowRight/></Link></div>
        <div className="video-label"><Pause size={18}/><span>Watch Jnapanam in Action</span></div>
      </div>
    </section>
    <section className="trust-strip"><div className="container trust-grid">
      <div><Medal/><span><strong>7 Sports</strong><small>Diverse facilities under one roof</small></span></div><div><Users/><span><strong>Expert Coaching</strong><small>Guidance with proven experience</small></span></div><div><Users/><span><strong>All Age Groups</strong><small>Programs for kids, youth & adults</small></span></div><div><CalendarDays/><span><strong>Flexible Memberships</strong><small>Plans that fit your goals</small></span></div>
    </div></section>
    <section className="feature-band section"><div className="container feature-grid">
      <article className="feature-card quote-card"><div className="swami-crop"><Image unoptimized src="/images/swami-vivekananda.png" alt="Swami Vivekananda" fill sizes="280px"/></div><div><span className="eyebrow">Words That Inspire</span><blockquote>&ldquo;Arise, awake, and stop not till the goal is reached.&rdquo;</blockquote><cite>&mdash; Swami Vivekananda</cite></div></article>
      <article className="feature-card event-card"><div className="section-kicker"><span/>Upcoming Events<span/></div><div className="event-body"><div className="event-visual"><PlayCircle/><span>Cricket Camp</span></div><div><h3>Summer Cricket Camp</h3><p>Dates and registrations will be announced shortly.</p><p className="event-meta">Jnapanam Sports Zone, Holalkere</p><Link className="text-link" href="/events">View all events <ArrowRight/></Link></div></div><div className="slider-controls"><button aria-label="Previous event"><ChevronLeft/></button><i/><i className="active"/><i/><button aria-label="Next event"><ChevronRight/></button></div></article>
      <article className="feature-card founder-card"><div><span className="eyebrow">Founder's Message</span><h3>Shri Jayasimha</h3><p>Jnapanam Sports Zone makes quality sports and fitness accessible to families in Holalkere. We build healthier lives through disciplined training and an inclusive community.</p></div><Image unoptimized src="/images/founder-jayasimha.png" alt="Founder Shri Jayasimha" width={220} height={220}/></article>
    </div></section>
    <section className="values-band"><div className="container"><span className="eyebrow gold">Our Purpose</span><h2>More Than a Sports Centre</h2><p>Inspired by the timeless values of Swami Vivekananda, Jnapanam Sports Zone builds character, confidence and community through sport.</p><div className="values-grid">{values.map((value, i)=><div key={value}><span>0{i+1}</span><strong>{value}</strong></div>)}</div></div></section>
    <section className="section sports-section"><div className="container"><div className="section-heading"><div><span className="eyebrow">Discover Your Game</span><h2>Explore Our Sports & Activities</h2></div><Link className="text-link" href="/sports">View all facilities <ArrowRight/></Link></div><div className="sports-grid">{sports.map(({name,icon:Icon,text},index)=><Link className="sport-card" href="/sports" key={name}><div className={`sport-photo sport-photo-${index}`}><div className="sport-icon"><Icon/></div></div><div className="sport-card-body"><h3>{name}</h3><p>{text}</p><span>Explore <ArrowRight/></span></div></Link>)}</div></div></section>
    <section className="cta-band"><div className="container"><div><span className="eyebrow gold">Your journey starts here</span><h2>Ready to get moving?</h2><p>Register today and become part of Holalkere's growing sports community.</p></div><div className="button-row"><Link className="btn primary large" href="/register">Register Now</Link><Link className="btn outline light large" href="/contact">Talk to Us</Link></div></div></section>
  </main><SiteFooter/></>;
}
