import { ArrowRight, CheckCircle2 } from "lucide-react";
import Link from "next/link";
import { PageHero } from "./page-hero";
import { SiteFooter } from "./site-footer";
import { SiteHeader } from "./site-header";

export function InnerPage({eyebrow,title,description,items,cta="Register Now"}:{eyebrow:string;title:string;description:string;items:{title:string;text:string}[];cta?:string}){
  return <><SiteHeader/><main><PageHero eyebrow={eyebrow} title={title} description={description}/><section className="section"><div className="container inner-grid">{items.map(item=><article className="info-card" key={item.title}><CheckCircle2/><h2>{item.title}</h2><p>{item.text}</p></article>)}</div></section><section className="inner-cta"><div className="container"><h2>Ready to begin?</h2><p>Connect with our team to find the right activity, coaching program or membership.</p><Link className="btn primary large" href="/register">{cta} <ArrowRight/></Link></div></section></main><SiteFooter/></>;
}
