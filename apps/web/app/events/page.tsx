import Link from "next/link";
import { ArrowRight, CalendarDays, Clock3, MapPin } from "lucide-react";
import { SiteHeader } from "@/components/site-header";
import { SiteFooter } from "@/components/site-footer";

const events=["Badminton Championship","Fitness Workshops","Indoor Games Meet"];
export default function Events(){return <><SiteHeader/><main>
  <section className="page-hero events-hero"><div className="container"><span className="eyebrow gold">Events</span><h1>Compete, Celebrate and Grow</h1><p>Camps, tournaments and community activities that bring Holalkere together.</p></div></section>
  <section className="section"><div className="container events-layout"><article className="featured-event"><div className="featured-event-photo"/><div><span className="eyebrow gold">Featured Event</span><h2>Summer Cricket Camp</h2><p>Registration dates, age groups and timings will be announced soon.</p><ul><li><CalendarDays/>Dates announced shortly</li><li><Clock3/>Timings announced shortly</li><li><MapPin/>Jnapanam Sports Zone, Holalkere</li></ul><Link className="btn primary" href="/contact">Get Event Updates <ArrowRight/></Link></div></article><aside className="event-list"><span className="eyebrow">Coming Up</span><h2>More community events</h2>{events.map((event,i)=><article key={event}><b>0{i+1}</b><div><h3>{event}</h3><p>Details and registrations will be announced.</p></div></article>)}</aside></div></section>
</main><SiteFooter/></>}
