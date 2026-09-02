import Link from "next/link";
import { ArrowRight, Check } from "lucide-react";
import { SiteHeader } from "@/components/site-header";
import { SiteFooter } from "@/components/site-footer";

const plans=[{name:"Activity Access",tag:"For flexible play",items:["Choose an individual activity","Plan sessions around availability","Suitable for recreation"]},{name:"Active Membership",tag:"For a regular routine",featured:true,items:["Consistent facility access","Build a steady practice habit","Add coaching based on goals"]},{name:"Family Option",tag:"For active families",items:["Encourage healthy activity together","Mix activities by interest","Friendly support for every age"]}];
export default function Memberships(){return <><SiteHeader/><main>
  <section className="page-hero membership-hero"><div className="container"><span className="eyebrow gold">Memberships</span><h1>Flexible Plans for Every Goal</h1><p>Choose the kind of access that fits your activity, routine and family.</p></div></section>
  <section className="section"><div className="container plan-grid">{plans.map(plan=><article className={plan.featured?"plan-card featured":"plan-card"} key={plan.name}>{plan.featured&&<span className="plan-badge">Most Flexible</span>}<h2>{plan.name}</h2><p>{plan.tag}</p><div className="plan-price">Contact us <small>for current details</small></div><ul>{plan.items.map(x=><li key={x}><Check/>{x}</li>)}</ul><Link className={plan.featured?"btn primary":"btn outline"} href="/contact">Enquire About Plan <ArrowRight/></Link></article>)}</div></section>
  <section className="membership-note"><div className="container"><h2>Not sure which plan suits you?</h2><p>Tell us your preferred activity, age group and schedule. Our team will guide you.</p><Link className="text-link" href="/contact">Speak with us <ArrowRight/></Link></div></section>
</main><SiteFooter/></>}
