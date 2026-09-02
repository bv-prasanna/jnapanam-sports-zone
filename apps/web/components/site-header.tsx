"use client";

import { Menu, X } from "lucide-react";
import Image from "next/image";
import Link from "next/link";
import { useState } from "react";
import { useApp } from "@/components/app-provider";
import type { TranslationKey } from "@/lib/i18n";

const links: ReadonlyArray<readonly [TranslationKey, string]> = [
  ["home", "/"],
  ["about", "/about"],
  ["sports", "/sports"],
  ["services", "/services"],
  ["memberships", "/memberships"],
  ["events", "/events"],
  ["gallery", "/gallery"],
  ["contact", "/contact"],
];

export function SiteHeader() {
  const [open, setOpen] = useState(false);
  const { language, setLanguage, user, signOut, t } = useApp();
  return (
    <>
      <div className="utility-bar">
        <div className="container utility-inner">
          <span>⌖ {t("location")}</span>
          <div><a href="tel:+918296022107">☎ +91 82960 22107</a><span className="utility-sep">|</span><button className={language==="en"?"active-language":""} onClick={()=>setLanguage("en")} type="button">English</button><span>|</span><button className={language==="kn"?"active-language":""} onClick={()=>setLanguage("kn")} type="button">ಕನ್ನಡ</button></div>
        </div>
      </div>
      <header className="site-header">
        <div className="container nav-wrap">
          <Link className="brand" href="/" aria-label="Jnapanam Sports Zone home">
            <Image unoptimized src="/images/jnapanam-logo.png" alt="Jnapanam Sports Zone" width={150} height={76} priority />
          </Link>
          <nav className={open ? "main-nav open" : "main-nav"} aria-label="Main navigation">
            {links.map(([key, href]) => <Link key={href} href={href} onClick={() => setOpen(false)}>{t(key)}</Link>)}
            <div className="mobile-actions">{user?<><Link href="/dashboard">{t("dashboard")}</Link><button onClick={()=>void signOut()}>{t("logout")}</button></>:<><Link href="/login">{t("login")}</Link><Link className="btn primary" href="/register">{t("register")}</Link></>}</div>
          </nav>
          <div className="nav-actions">{user?<><Link href="/dashboard">{t("dashboard")}</Link><button className="header-link" onClick={()=>void signOut()}>{t("logout")}</button></>:<><Link href="/login">{t("login")}</Link><Link className="btn primary" href="/register">{t("register")}</Link></>}</div>
          <button className="menu-button" type="button" onClick={() => setOpen(!open)} aria-label="Toggle navigation">{open ? <X /> : <Menu />}</button>
        </div>
      </header>
    </>
  );
}
