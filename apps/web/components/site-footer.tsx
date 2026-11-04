import Image from "next/image";
import Link from "next/link";
import {CONTACT_PHONES} from "@/lib/contact";

export function SiteFooter() {
  return <footer className="site-footer"><div className="container footer-grid">
    <div><Image unoptimized src="/images/jnapanam-logo.png" alt="Jnapanam Sports Zone" width={155} height={155}/><p>Play. Train. Compete. Grow.</p><p>Building healthier lives and stronger character through sport.</p></div>
    <div><h3>Explore</h3><Link href="/about">About Us</Link><Link href="/sports">Sports & Facilities</Link><Link href="/memberships">Memberships</Link><Link href="/events">Events</Link></div>
    <div><h3>Support</h3><Link href="/contact">Contact Us</Link><Link href="/login">Login</Link><Link href="/register">Register</Link><a href="mailto:info@jnapanamsportszone.in">Email Us</a></div>
    <div><h3>Visit Us</h3><p>Jnapanam Sports Zone<br/>Holalkere, Chitradurga<br/>Karnataka, India</p>{CONTACT_PHONES.map(phone=><a key={phone.e164} href={`tel:${phone.e164}`}>{phone.display}</a>)}</div>
  </div><div className="footer-bottom">© 2026 Jnapanam Sports Zone. All rights reserved.</div></footer>;
}
