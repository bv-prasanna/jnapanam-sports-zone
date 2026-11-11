import Image from "next/image";
import Link from "next/link";
import {CONTACT_PHONES} from "@/lib/contact";

export function SiteFooter() {
  return <footer className="site-footer"><div className="container footer-grid">
    <div className="footer-brand-block">
      <div className="footer-brand-qr-row">
        <Image unoptimized src="/images/jnapanam-logo.png" alt="Jnapanam Sports Zone" width={155} height={155} className="footer-logo"/>
        <Link href="/app" className="footer-app-qr" aria-label="Open the Jnapanam Android app download page">
          <Image unoptimized src="/app-download-qr.svg" alt="QR code to download Jnapanam Android app" width={112} height={112}/>
          <span><strong>Download App</strong><small>ಆ್ಯಪ್ ಡೌನ್‌ಲೋಡ್</small></span>
        </Link>
      </div>
      <p>Play. Train. Compete. Grow.</p>
      <p>Building healthier lives and stronger character through sport.</p>
    </div>
    <div><h3>Explore</h3><Link href="/about">About Us</Link><Link href="/sports">Sports & Facilities</Link><Link href="/memberships">Memberships</Link><Link href="/events">Events</Link></div>
    <div><h3>Support</h3><Link href="/contact">Contact Us</Link><Link href="/login">Login</Link><Link href="/register">Register</Link><a href="mailto:info@jnapanamsportszone.in">Email Us</a></div>
    <div><h3>Visit Us</h3><p>Jnapanam Sports Zone<br/>Holalkere, Chitradurga<br/>Karnataka, India</p>{CONTACT_PHONES.map(phone=><a key={phone.e164} href={`tel:${phone.e164}`}>{phone.display}</a>)}</div>
  </div><div className="footer-bottom">© 2026 Jnapanam Sports Zone. All rights reserved.</div></footer>;
}
