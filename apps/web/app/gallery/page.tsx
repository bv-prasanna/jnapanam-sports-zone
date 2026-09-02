import { SiteHeader } from "@/components/site-header";
import { SiteFooter } from "@/components/site-footer";
import { sports } from "@/lib/site-data";

export default function Gallery(){return <><SiteHeader/><main>
  <section className="page-hero gallery-hero"><div className="container"><span className="eyebrow gold">Gallery</span><h1>Life at Jnapanam</h1><p>A window into practice, movement, focus and community across our activities.</p></div></section>
  <section className="section"><div className="container"><div className="gallery-heading"><div><span className="eyebrow">Everyday Moments</span><h2>Play. Train. Compete. Grow.</h2></div><p>Our gallery will continue to grow with real moments from Jnapanam Sports Zone.</p></div><div className="gallery-grid">{sports.map(({name},i)=><figure className={`gallery-item gallery-item-${i} sport-photo-${i}`} key={name}><figcaption><span>Activity</span><strong>{name}</strong></figcaption></figure>)}</div></div></section>
</main><SiteFooter/></>}
