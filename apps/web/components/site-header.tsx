"use client";

import { Menu, X } from "lucide-react";
import Image from "next/image";
import Link from "next/link";
import { useState } from "react";

const links = [
  ["Home", "/"],
  ["About Us", "/about"],
  ["Sports & Facilities", "/sports"],
  ["Services", "/services"],
  ["Memberships", "/memberships"],
  ["Events", "/events"],
  ["Gallery", "/gallery"],
  ["Contact", "/contact"],
] as const;

export function SiteHeader() {
  const [open, setOpen] = useState(false);
  return (
    <>
      <div className="utility-bar">
        <div className="container utility-inner">
          <span>⌖ Holalkere, Karnataka</span>
          <div><a href="tel:+918296022107">☎ +91 82960 22107</a><span className="utility-sep">|</span><button type="button">English</button><span>|</span><button type="button">ಕನ್ನಡ</button></div>
        </div>
      </div>
      <header className="site-header">
        <div className="container nav-wrap">
          <Link className="brand" href="/" aria-label="Jnapanam Sports Zone home">
            <Image unoptimized src="/images/jnapanam-logo.png" alt="Jnapanam Sports Zone" width={122} height={122} priority />
          </Link>
          <nav className={open ? "main-nav open" : "main-nav"} aria-label="Main navigation">
            {links.map(([label, href]) => <Link key={href} href={href} onClick={() => setOpen(false)}>{label}</Link>)}
            <div className="mobile-actions"><Link href="/login">Login</Link><Link className="btn primary" href="/register">Register Now</Link></div>
          </nav>
          <div className="nav-actions"><Link href="/login">Login</Link><Link className="btn primary" href="/register">Register Now</Link></div>
          <button className="menu-button" type="button" onClick={() => setOpen(!open)} aria-label="Toggle navigation">{open ? <X /> : <Menu />}</button>
        </div>
      </header>
    </>
  );
}
