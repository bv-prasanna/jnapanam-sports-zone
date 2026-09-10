"use client";

import { MouseEvent, PropsWithChildren } from "react";
import { useRouter } from "next/navigation";
import { useApp } from "@/components/app-provider";

export default function DashboardLayout({children}:PropsWithChildren){
  const router=useRouter();
  const {user}=useApp();

  function routeActions(event:MouseEvent<HTMLDivElement>){
    const target=event.target as HTMLElement;
    const button=target.closest("button");
    if(!button||!user)return;
    const text=(button.textContent||"").trim().toLowerCase();
    if(text==="profile"||text.includes("my profile")){
      event.preventDefault();event.stopPropagation();router.push("/profile");return;
    }
    if(text.includes("admin")&&user.roles.includes("OPERATIONS")&&!user.roles.some(r=>r==="ADMIN"||r==="SUPER_ADMIN")){
      event.preventDefault();event.stopPropagation();router.push("/operations");return;
    }
    if(text.includes("admin")&&user.roles.includes("COACH")&&!user.roles.some(r=>r==="ADMIN"||r==="SUPER_ADMIN")){
      event.preventDefault();event.stopPropagation();router.push("/dashboard");
    }
  }

  return <div onClickCapture={routeActions}>{user?.roles.includes("SHOP_KEEPER")&&!user.roles.some(r=>r==="ADMIN"||r==="SUPER_ADMIN")&&<div style={{position:"fixed",right:18,bottom:18,zIndex:1000}}><button onClick={()=>router.push("/shop-counter")} style={{border:0,borderRadius:12,padding:"12px 16px",fontWeight:800,background:"#063B63",color:"white",boxShadow:"0 8px 24px rgba(16,42,58,.22)"}}>Open Shop Counter</button></div>}{children}</div>;
}
