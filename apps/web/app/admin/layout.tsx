"use client";

import {PropsWithChildren, Suspense, useEffect, useState} from "react";
import Link from "next/link";
import Image from "next/image";
import {usePathname, useRouter, useSearchParams} from "next/navigation";
import {useApp} from "@/components/app-provider";
import styles from "./admin-layout.module.css";
import "./admin-foundation.css";
import {adminAreas,adminModules} from "./admin-navigation";


function AdminShell({children}:PropsWithChildren){
  const router=useRouter(),pathname=usePathname(),search=useSearchParams();
  const {user,ready,signOut}=useApp();
  const [menuOpen,setMenuOpen]=useState(false);
  const allowed=!!user?.roles.some(role=>role==="ADMIN"||role==="SUPER_ADMIN");
  const superAdmin=!!user?.roles.includes("SUPER_ADMIN");
  const current=pathname==="/admin"&&search.get("view")
    ?"/admin?view="+search.get("view")
    :pathname==="/admin/pricing"
      ?"/admin/pricing?tab="+(search.get("tab")==="taxes"?"taxes":"pricing-rules")
      :pathname;
  const fullName=user?.fullName||"Admin";
  const initials=fullName.split(/\s+/).filter(Boolean).slice(0,2).map(x=>x.charAt(0).toUpperCase()).join("");
  useEffect(()=>{
    if(!ready||allowed)return;
    if(!user)router.replace("/login");
    else if(user.roles.includes("OPERATIONS"))router.replace("/operations");
    else if(user.roles.includes("SHOP_KEEPER"))router.replace("/shop-counter");
    else router.replace("/dashboard");
  },[ready,allowed,user,router]);
  useEffect(()=>setMenuOpen(false),[pathname,search]);
  if(!ready||!allowed)return <div className="portal-loading" role="status">Checking administrator access…</div>;
  return <div className={styles.shell+" adminExperience"}>
    <a href="#admin-main-content" className={styles.skip}>Skip to main content</a>
    <header className={styles.topBar}>
      <div className={styles.brandGroup}>
        <button className={styles.menuToggle} type="button" aria-label={menuOpen?"Close navigation":"Open navigation"} aria-expanded={menuOpen} aria-controls="jnapanam-admin-navigation" onClick={()=>setMenuOpen(v=>!v)}>{menuOpen?"✕ Close":"☰ Menu"}</button>
        <Link href="/admin" className={styles.brand} onClick={()=>setMenuOpen(false)}><span className={styles.brandIcon} aria-hidden="true"><Image src="/images/jnapanam-logo.png" alt="" width={44} height={44} unoptimized /></span><span><strong>Jnapanam Sports Zone</strong><small>Administration</small></span></Link>
      </div>
      <div className={styles.account}>
        <div className={styles.userIdentity}><span className={styles.avatar} aria-hidden="true">{initials||"A"}</span><div><strong>{fullName}</strong><small>{superAdmin?"Super Admin":"Admin"}</small></div></div>
        <Link href="/dashboard?section=profile" className={styles.accountLink}>My profile</Link>
        <button type="button" className={styles.logout} onClick={()=>void signOut().then(()=>router.replace("/login"))}>Logout</button>
      </div>
    </header>
    <div className={styles.body}>
      {menuOpen&&<button type="button" className={styles.scrim} onClick={()=>setMenuOpen(false)} aria-label="Close navigation overlay"/>}
      <aside id="jnapanam-admin-navigation" className={styles.sidebar+(menuOpen?" "+styles.sidebarOpen:"")}>
        <nav aria-label="Admin modules">
          {adminAreas.map(category=>
            <div className={styles.navGroup} key={category}>
              <h2>{category}</h2>
              {adminModules.filter(x=>x.area===category&&(!x.superAdmin||superAdmin)).map(item=>{
                const active=current===item.href || (item.href==="/admin"&&current==="/admin");
                return <Link key={item.href} href={item.href} onClick={()=>setMenuOpen(false)}
                  className={styles.navLink+(active?" "+styles.active:"")}
                  aria-current={active?"page":undefined}>{item.label}</Link>;
              })}
            </div>
          )}
          <div className={styles.navGroup}><h2>Account</h2><Link className={styles.navLink} href="/dashboard">Customer portal</Link></div>
        </nav>
      </aside>
      <div className={styles.content} id="admin-main-content" tabIndex={-1}>{children}</div>
    </div>
  </div>;
}
export default function AdminLayout({children}:PropsWithChildren){
  return <Suspense fallback={<div className="portal-loading" role="status">Loading admin console…</div>}><AdminShell>{children}</AdminShell></Suspense>;
}
