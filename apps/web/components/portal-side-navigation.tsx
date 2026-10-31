"use client";
import Link from "next/link";
import {useApp} from "@/components/app-provider";
import styles from "./portal-side-navigation.module.css";
type Item={key:string;label:string;href:string;admin?:boolean;operations?:boolean};
const items:Item[]=[
 {key:"dashboard",label:"My Dashboard",href:"/dashboard"},
 {key:"bookings",label:"Bookings",href:"/dashboard?section=bookings"},
 {key:"memberships",label:"Memberships",href:"/dashboard?section=memberships"},
 {key:"training",label:"Training",href:"/dashboard?section=training"},
 {key:"growth",label:"My Growth",href:"/dashboard?section=growth"},
 {key:"tournaments",label:"Tournaments",href:"/dashboard?section=tournaments"},
 {key:"payments",label:"Payments",href:"/dashboard?section=payments"},
 {key:"profile",label:"My Profile",href:"/profile"},
 {key:"account",label:"Account & Family",href:"/account-services"},
 {key:"operations",label:"Facility Operations",href:"/operations",operations:true},
 {key:"admin",label:"Admin Console",href:"/admin",admin:true},
 {key:"verification",label:"Document Verification",href:"/admin/verification",admin:true},
 {key:"assignments",label:"Operator Assignments",href:"/admin/operator-assignments",admin:true},
 {key:"exceptions",label:"Operations & Exceptions",href:"/admin/release-readiness",admin:true}
];
export function PortalSideNavigation({active}:{active:string}){
 const {user}=useApp();
 if(!user)return null;
 const privileged=user.roles.some(r=>r==="ADMIN"||r==="SUPER_ADMIN");
 const operator=privileged||user.roles.includes("OPERATIONS");
 return <aside className={styles.sidebar}>
   <div className={styles.identity}><small>Welcome</small><strong>{user.displayName||user.fullName}</strong><span>{user.roles.join(" · ").replaceAll("_"," ")}</span></div>
   <nav aria-label="Account and operations navigation">{items.filter(x=>(!x.admin||privileged)&&(!x.operations||operator)).map(x=>
     <Link className={active===x.key?styles.active:""} aria-current={active===x.key?"page":undefined} key={x.key} href={x.href}>{x.label}</Link>
   )}</nav>
 </aside>;
}
