import Image from "next/image";
import Link from "next/link";
import { ArrowRight, HeartHandshake, ShieldCheck, Sparkles, Users } from "lucide-react";
import { SiteHeader } from "@/components/site-header";
import { SiteFooter } from "@/components/site-footer";
import { values } from "@/lib/site-data";

export default function About(){return <><SiteHeader/><main>
  <section className="page-hero about-hero"><div className="container"><span className="eyebrow gold">Our Story</span><h1>More Than a Sports Centre</h1><p>A welcoming community in Holalkere where sport builds strength, discipline and character.</p></div></section>
  <section className="section"><div className="container story-grid"><div><span className="eyebrow">Why Jnapanam</span><h2>A place to play, train, compete and grow</h2><p>Jnapanam Sports Zone brings sports, fitness and community together in one inclusive environment. Every program is shaped to help children, youth, adults and families build healthy habits with confidence.</p><p>Inspired by Swami Vivekananda’s ideals, we believe sporting progress should strengthen character as much as performance.</p><Link className="text-link" href="/contact">Visit our centre <ArrowRight/></Link></div><div className="story-portrait"><Image unoptimized priority src="/images/swami-vivekananda.png" alt="Swami Vivekananda" width={520} height={650}/></div></div></section>
  <section className="values-cards section"><div className="container"><div className="section-heading"><div><span className="eyebrow">What Guides Us</span><h2>Values in action</h2></div></div><div className="value-card-grid">{[ShieldCheck,Users,HeartHandshake,Sparkles].map((Icon,i)=><article key={values[i]}><span>0{i+1}</span><Icon/><h3>{values[i]}</h3><p>Part of every session, every interaction and every sporting journey at Jnapanam.</p></article>)}</div></div></section>
  <section className="section founder-story"><div className="container"><div><span className="eyebrow gold">Founder’s Vision</span><h2>Shri L Jayasimha Khatroth</h2><p>Jnapanam Sports Zone was founded to make quality sports and fitness accessible to every family in Holalkere. Our mission is to nurture discipline, confidence and healthy living through expert guidance and an inclusive sporting community.</p></div><Image unoptimized src="/images/founder-jayasimha.png" alt="Founder Shri L Jayasimha Khatroth" width={360} height={430}/></div></section>
</main><SiteFooter/></>}
