import Link from "next/link";
import { ArrowRight, CheckCircle2 } from "lucide-react";
import { SiteHeader } from "@/components/site-header";
import { SiteFooter } from "@/components/site-footer";
import {LiveSports} from "@/components/public-catalog";

export default function Sports(){return <><SiteHeader/><main>
  <section className="page-hero sports-hero"><div className="container"><span className="eyebrow gold">Sports & Facilities</span><h1>Find Your Game</h1><p>Explore sports and facilities currently available at Jnapanam Sports Zone.</p></div></section>
  <LiveSports/>
  <section className="inner-cta"><div className="container"><span className="eyebrow">Choose your activity</span><h2>Ready to begin?</h2><p>Tell us which sport interests you and we’ll help with the next step.</p><Link className="btn primary" href="/register">Register Your Interest</Link></div></section>
</main><SiteFooter/></>}
