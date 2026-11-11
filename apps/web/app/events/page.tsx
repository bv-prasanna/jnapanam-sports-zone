import Link from "next/link";
import { ArrowRight, CalendarDays, Clock3, MapPin } from "lucide-react";
import { SiteHeader } from "@/components/site-header";
import { SiteFooter } from "@/components/site-footer";
import {PublishedEvents} from "@/components/public-catalog";


export default function Events(){return <><SiteHeader/><main>
  <section className="page-hero events-hero"><div className="container"><span className="eyebrow gold">Events</span><h1>Compete, Celebrate and Grow</h1><p>Camps, tournaments and community activities that bring Holalkere together.</p></div></section>
  <PublishedEvents/>
</main><SiteFooter/></>}
