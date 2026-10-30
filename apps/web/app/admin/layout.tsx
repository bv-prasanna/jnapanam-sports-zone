"use client";

import { MouseEvent, PropsWithChildren, useEffect } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useApp } from "@/components/app-provider";
import styles from "./admin-layout.module.css";

export default function AdminLayout({children}:PropsWithChildren){
  const router=useRouter();
  const pathname=usePathname();
  const {user,ready,signOut}=useApp();
  const admin=!!user?.roles.some(role=>role==="ADMIN"||role==="SUPER_ADMIN");
  const profile=user as unknown as {profilePhotoUrl?:string;photoUrl?:string;avatarUrl?:string}|null;
  const photo=profile?.profilePhotoUrl||profile?.photoUrl||profile?.avatarUrl;
  const initials=(user?.fullName||"Admin").split(/\s+/).filter(Boolean).slice(0,2).map(x=>x[0]?.toUpperCase()).join("");

  useEffect(()=>{
    if(!ready||admin)return;
    if(!user){router.replace("/login");return;}
    if(user.roles.includes("OPERATIONS")){router.replace("/operations");return;}
    if(user.roles.includes("SHOP_KEEPER")){router.replace("/shop-counter");return;}
    router.replace("/dashboard");
  },[ready,admin,user,router]);

  function routeLegacyNavigation(event:MouseEvent<HTMLDivElement>){
    if(pathname!=="/admin")return;
    const target=event.target as HTMLElement;
    const button=target.closest("button");
    if(!button)return;
    const text=(button.textContent||"").trim().toLowerCase();
    if(text==="bookings"||text.startsWith("bookings ")||text.includes("open details")&&text.includes("booking")){
      event.preventDefault();event.stopPropagation();router.push("/admin/bookings");return;
    }
    if(text==="users"||text.startsWith("users ")){
      event.preventDefault();event.stopPropagation();router.push("/admin/users");
    }
  }

  if(!ready||!admin)return <div className="portal-loading">Checking administrator access…</div>;
  return <div className={styles.shell}>
    <section className={styles.staffBar} aria-label="Administrator quick navigation">
      <div className={styles.identity}>{photo?<img src={photo} alt="Profile"/>:<span className={styles.avatar}>{initials||"A"}</span>}<div><strong>{user?.fullName}</strong><small>{user?.roles.join(" · ")}</small></div></div>
      <nav className={styles.links} aria-label="Account actions">
        <Link href="/dashboard?section=profile">My Profile</Link>
        <button type="button" onClick={()=>void signOut().then(()=>router.replace("/login"))}>Logout</button>
      </nav>
    </section>
    {pathname!=="/admin"&&<div className={styles.secondaryLayout}><aside className={styles.sideNav} aria-label="Admin modules"><strong>Admin modules</strong>{[
      ["Dashboard","/admin"],["Verification","/admin/verification"],["Users","/admin/users"],["Bookings","/admin/bookings"],["Operator Assignments","/admin/operator-assignments"],
      ["Release Readiness","/admin/release-readiness"],["Operations","/operations"],["Shop Counter","/shop-counter"],
      ["Commerce","/admin/commerce"],["Notification Providers","/admin/notification-providers"],
      ...(user?.roles.includes("SUPER_ADMIN")?[["UAT OTP Numbers","/admin/uat-otp"]]:[])
    ].map(([label,href])=><Link key={href} href={href} aria-current={pathname===href?"page":undefined}>{label}</Link>)}</aside><div className={styles.subPage}>{children}</div></div>}
    {pathname==="/admin"&&<div onClickCapture={routeLegacyNavigation}>{children}</div>}
  </div>;
}
