"use client";

import { MouseEvent, PropsWithChildren, useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";
import { useApp } from "@/components/app-provider";

export default function AdminLayout({children}:PropsWithChildren){
  const router=useRouter();
  const pathname=usePathname();
  const {user,ready}=useApp();
  const admin=!!user?.roles.some(role=>role==="ADMIN"||role==="SUPER_ADMIN");

  useEffect(()=>{
    if(!ready||admin)return;
    if(!user){router.replace("/login");return;}
    if(user.roles.includes("OPERATIONS")){router.replace("/operations");return;}
    if(user.roles.includes("SHOP_KEEPER")){router.replace("/dashboard");return;}
    router.replace("/dashboard");
  },[ready,admin,user,router]);

  function routeLegacyBooking(event:MouseEvent<HTMLDivElement>){
    if(pathname!=="/admin")return;
    const target=event.target as HTMLElement;
    const button=target.closest("button");
    if(!button)return;
    const text=(button.textContent||"").trim().toLowerCase();
    if(text==="bookings"||text.startsWith("bookings ")||text.includes("open details")&&text.includes("booking")){
      event.preventDefault();
      event.stopPropagation();
      router.push("/admin/bookings");
    }
  }

  if(!ready||!admin)return <div className="portal-loading">Checking administrator access…</div>;
  return <div onClickCapture={routeLegacyBooking}>{children}</div>;
}
