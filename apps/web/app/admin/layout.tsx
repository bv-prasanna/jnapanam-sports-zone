"use client";

import {PropsWithChildren, Suspense, useEffect, useState} from "react";
import Link from "next/link";
import {usePathname, useRouter, useSearchParams} from "next/navigation";
import {useApp} from "@/components/app-provider";
import styles from "./admin-layout.module.css";
import "./admin-foundation.css";

type Entry={label:string;href:string;category:"Overview"|"Arena"|"Finance & commerce"|"Operations & people"|"System";superAdmin?:boolean};
const entries:Entry[]=[
  {label:"Dashboard",href:"/admin",category:"Overview"},
  {label:"Bookings",href:"/admin/bookings",category:"Arena"},
  {label:"Sports",href:"/admin?view=sports",category:"Arena"},
  {label:"Facilities",href:"/admin?view=facilities",category:"Arena"},
  {label:"Memberships",href:"/admin?view=memberships",category:"Arena"},
  {label:"Training",href:"/admin?view=training",category:"Arena"},
  {label:"Tournaments",href:"/admin?view=tournaments",category:"Arena"},
  {label:"Users",href:"/admin/users",category:"Operations & people"},
  {label:"Verification",href:"/admin/verification",category:"Operations & people"},
  {label:"Operator Assignments",href:"/admin/operator-assignments",category:"Operations & people"},
  {label:"Release Readiness",href:"/admin/release-readiness",category:"Operations & people"},
  {label:"Operations",href:"/operations",category:"Operations & people"},
  {label:"Shop Counter",href:"/shop-counter",category:"Operations & people"},
  {label:"Commerce",href:"/admin/commerce",category:"Finance & commerce"},
  {label:"Pricing & Taxes",href:"/admin/pricing",category:"Finance & commerce"},
  {label:"GST Billing & Refunds",href:"/admin/billing",category:"Finance & commerce"},
  {label:"Payments",href:"/admin?view=payments",category:"Finance & commerce"},
  {label:"Promotions",href:"/admin?view=promotions",category:"Finance & commerce"},
  {label:"Recurring Subscriptions",href:"/admin/subscriptions",category:"Finance & commerce"},
  {label:"Notifications",href:"/admin?view=notifications",category:"System"},
  {label:"Notification Providers",href:"/admin/notification-providers",category:"System"},
  {label:"Content & Events",href:"/admin?view=content",category:"System"},
  {label:"Audit Log",href:"/admin?view=audit",category:"System"},
  {label:"UAT OTP Numbers",href:"/admin/uat-otp",category:"System",superAdmin:true},
];
export const adminNavEntries=entries;

function AdminShell({children}:PropsWithChildren){
  const router=useRouter(),pathname=usePathname(),search=useSearchParams();
  const {user,ready,signOut}=useApp();
  const [menuOpen,setMenuOpen]=useState(false);
  const allowed=!!user?.roles.some(role=>role==="ADMIN"||role==="SUPER_ADMIN");
  const superAdmin=!!user?.roles.includes("SUPER_ADMIN");
  const current=pathname==="/admin"&&search.get("view")
    ?"/admin?view="+search.get("view"):pathname;
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
        <button className={styles.menuToggle} type="button" aria-label={menuOpen?"Close navigation":"Open navigation"} aria-expanded={menuOpen} aria-controls="jnapanam-admin-navigation" onClick={()=>setMenuOpen(v=>!v)}>{menuOpen?"✕":"☰"}</button>
        <Link href="/admin" className={styles.brand} onClick={()=>setMenuOpen(false)}><span className={styles.brandIcon} aria-hidden="true">J</span><span><strong>Jnapanam Sports Zone</strong><small>Administration</small></span></Link>
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
          {(["Overview","Arena","Finance & commerce","Operations & people","System"] as const).map(category=>
            <div className={styles.navGroup} key={category}>
              <h2>{category}</h2>
              {entries.filter(x=>x.category===category&&(!x.superAdmin||superAdmin)).map(item=>{
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
