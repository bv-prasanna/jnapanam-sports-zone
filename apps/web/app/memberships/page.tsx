import Link from "next/link";
import { ArrowRight, Check } from "lucide-react";
import { SiteHeader } from "@/components/site-header";
import { SiteFooter } from "@/components/site-footer";
import {LiveMemberships} from "@/components/public-catalog";


export default function Memberships(){return <><SiteHeader/><main>
  <section className="page-hero membership-hero"><div className="container"><span className="eyebrow gold">Memberships</span><h1>Flexible Plans for Every Goal</h1><p>Choose the kind of access that fits your activity, routine and family.</p></div></section>
  <LiveMemberships/>
  <section className="membership-note"><div className="container"><h2>Not sure which plan suits you?</h2><p>Tell us your preferred activity, age group and schedule. Our team will guide you.</p><Link className="text-link" href="/contact">Speak with us <ArrowRight/></Link></div></section>
</main><SiteFooter/></>}
