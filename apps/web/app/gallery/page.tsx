import { SiteHeader } from "@/components/site-header";
import { SiteFooter } from "@/components/site-footer";
import {PublishedGallery} from "@/components/public-catalog";

export default function Gallery(){return <><SiteHeader/><main>
  <section className="page-hero gallery-hero"><div className="container"><span className="eyebrow gold">Gallery</span><h1>Life at Jnapanam</h1><p>A window into practice, movement, focus and community across our activities.</p></div></section>
  <PublishedGallery/>
</main><SiteFooter/></>}
