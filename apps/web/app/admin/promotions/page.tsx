"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ApiError, api } from "@/lib/api";
import { useApp } from "@/components/app-provider";
import styles from "./promotions.module.css";

type DiscountType = "PERCENTAGE" | "FIXED";
type Purpose = "BOOKING" | "MEMBERSHIP" | "TRAINING" | "TOURNAMENT";
type Promotion = {
  id:string; code:string; name:string; description?:string|null; discountType:DiscountType;
  discountValue:number; maxDiscountPaise?:number|null; minOrderPaise:number; purposes:Purpose[];
  automatic:boolean; onlineOnly:boolean; startsAt:string; endsAt:string;
  globalUsageLimit?:number|null; perUserLimit:number; usageCount:number; active:boolean;
};
type Form = {
  code:string; name:string; description:string; discountType:DiscountType; discountValue:string;
  maxDiscount:string; minOrder:string; purposes:Purpose[]; automatic:boolean; onlineOnly:boolean;
  startsAt:string; endsAt:string; globalUsageLimit:string; perUserLimit:string; active:boolean;
};
const purposes:Purpose[]=["BOOKING","MEMBERSHIP","TRAINING","TOURNAMENT"];
const blank=():Form=>({code:"",name:"",description:"",discountType:"PERCENTAGE",discountValue:"10",maxDiscount:"",minOrder:"0",purposes:["BOOKING"],automatic:false,onlineOnly:true,startsAt:"",endsAt:"",globalUsageLimit:"",perUserLimit:"1",active:true});
const localDateTime=(value:string)=>{const d=new Date(value);if(Number.isNaN(d.getTime()))return "";const shifted=new Date(d.getTime()-d.getTimezoneOffset()*60000);return shifted.toISOString().slice(0,16)};
const errorText=(e:unknown)=>e instanceof ApiError?e.message:e instanceof Error?e.message:"Something went wrong. Please try again.";

export default function PromotionsAdminPage(){
 const router=useRouter();const {user,ready}=useApp();const allowed=!!user?.roles.some(r=>r==="ADMIN"||r==="SUPER_ADMIN");
 const [rows,setRows]=useState<Promotion[]>([]);const [loading,setLoading]=useState(true);const [busy,setBusy]=useState(false);
 const [editing,setEditing]=useState<string|null>(null);const [visible,setVisible]=useState(false);const [form,setForm]=useState<Form>(blank());
 const [error,setError]=useState("");const [notice,setNotice]=useState("");
 const load=useCallback(async()=>{if(!allowed)return;try{setError("");const value=await api<Promotion[]>("/api/v1/admin/promotions");setRows(Array.isArray(value)?value:[])}catch(e){setError(errorText(e))}finally{setLoading(false)}},[allowed]);
 useEffect(()=>{if(ready&&!allowed)router.replace(user?"/dashboard":"/login")},[ready,allowed,user,router]);
 useEffect(()=>{void load()},[load]);
 function startCreate(){setEditing(null);setForm(blank());setError("");setNotice("");setVisible(true)}
 function startEdit(row:Promotion){setEditing(row.id);setForm({code:row.code,name:row.name,description:row.description||"",discountType:row.discountType,discountValue:String(row.discountValue),maxDiscount:row.maxDiscountPaise==null?"":String(row.maxDiscountPaise/100),minOrder:String(row.minOrderPaise/100),purposes:row.purposes||[],automatic:row.automatic,onlineOnly:row.onlineOnly,startsAt:localDateTime(row.startsAt),endsAt:localDateTime(row.endsAt),globalUsageLimit:row.globalUsageLimit==null?"":String(row.globalUsageLimit),perUserLimit:String(row.perUserLimit),active:row.active});setError("");setNotice("");setVisible(true)}
 function set<K extends keyof Form>(key:K,value:Form[K]){setForm(current=>({...current,[key]:value}))}
 function togglePurpose(purpose:Purpose){setForm(current=>({...current,purposes:current.purposes.includes(purpose)?current.purposes.filter(x=>x!==purpose):[...current.purposes,purpose]}))}
 async function save(event:FormEvent<HTMLFormElement>){event.preventDefault();try{
   if(!form.purposes.length)throw new Error("Select at least one eligible payment purpose.");
   if(!form.startsAt||!form.endsAt)throw new Error("Set both start and end date/time.");
   const start=new Date(form.startsAt),end=new Date(form.endsAt);if(!(end>start))throw new Error("End date/time must be after the start date/time.");
   const discount=Number(form.discountValue),minimum=Number(form.minOrder),maximum=form.maxDiscount===""?null:Number(form.maxDiscount),global=form.globalUsageLimit===""?null:Number(form.globalUsageLimit),perUser=Number(form.perUserLimit);
   if(!Number.isFinite(discount)||discount<=0)throw new Error("Enter a valid discount value.");
   if(form.discountType==="PERCENTAGE"&&discount>100)throw new Error("Percentage discount cannot exceed 100%.");
   if(!Number.isFinite(minimum)||minimum<0)throw new Error("Minimum order must be zero or greater.");
   if(maximum!==null&&(!Number.isFinite(maximum)||maximum<=0))throw new Error("Maximum discount must be greater than zero.");
   if(global!==null&&(!Number.isInteger(global)||global<=0))throw new Error("Global usage limit must be a positive whole number.");
   if(!Number.isInteger(perUser)||perUser<1)throw new Error("Per-customer usage limit must be at least one.");
   const payload={code:form.code.trim().toUpperCase(),name:form.name.trim(),description:form.description.trim()||null,discountType:form.discountType,discountValue:discount,maxDiscountPaise:maximum===null?null:Math.round(maximum*100),minOrderPaise:Math.round(minimum*100),purposes:form.purposes,automatic:form.automatic,onlineOnly:form.onlineOnly,startsAt:start.toISOString(),endsAt:end.toISOString(),globalUsageLimit:global,perUserLimit:perUser,active:form.active};
   setBusy(true);setError("");setNotice("");
   await api(editing?"/api/v1/admin/promotions/"+editing:"/api/v1/admin/promotions",{method:editing?"PUT":"POST",body:JSON.stringify(payload)});
   setVisible(false);setNotice(editing?"Promotion updated successfully.":"Promotion created successfully.");await load();
 }catch(e){setError(errorText(e))}finally{setBusy(false)}}
 if(!ready||!allowed)return <main className={styles.page}>Checking access…</main>;
 return <main className={styles.page}>
  <header className={styles.header}><div><span>ADMIN · CUSTOMER ENGAGEMENT</span><h1>Promotions & Offers</h1><p>Create coupon codes and automatic discounts with eligibility, validity and redemption limits.</p></div><div className={styles.headerActions}><Link href="/admin/pricing">Pricing & taxes</Link><button onClick={startCreate}>+ Create promotion</button></div></header>
  {notice&&<div className={styles.notice} role="status">{notice}</div>}{error&&<div className={styles.error} role="alert">{error}</div>}
  <section className={styles.metrics}><article><small>Total offers</small><strong>{rows.length}</strong></article><article><small>Active offers</small><strong>{rows.filter(x=>x.active).length}</strong></article><article><small>Automatic offers</small><strong>{rows.filter(x=>x.active&&x.automatic).length}</strong></article><article><small>Total redemptions</small><strong>{rows.reduce((sum,x)=>sum+x.usageCount,0)}</strong></article></section>
  <section className={styles.panel}><div className={styles.panelHeading}><div><h2>Offer catalogue</h2><p>Review each offer's status, eligibility and usage before changing its configuration.</p></div></div>
   {loading?<div className={styles.empty}>Loading promotions…</div>:rows.length===0?<div className={styles.empty}><strong>No promotions created yet</strong><span>Create a coupon or automatic offer to get started.</span><button onClick={startCreate}>Create your first offer</button></div>:<div className={styles.tableWrap}><table><thead><tr><th>Offer</th><th>Discount</th><th>Eligibility</th><th>Validity</th><th>Usage</th><th>Status</th><th></th></tr></thead><tbody>{rows.map(row=><tr key={row.id}><td><strong>{row.name}</strong><span className={styles.code}>{row.code}</span>{row.automatic&&<em>Automatic</em>}</td><td><strong>{row.discountType==="PERCENTAGE"?row.discountValue+"%":"₹"+Number(row.discountValue).toFixed(2)}</strong>{row.maxDiscountPaise!=null&&<small>Max ₹{(row.maxDiscountPaise/100).toFixed(2)}</small>}<small>Min order ₹{(row.minOrderPaise/100).toFixed(2)}</small></td><td><span>{row.purposes?.join(", ")||"All purposes"}</span><small>{row.onlineOnly?"Online only":"All eligible channels"}</small><small>{row.perUserLimit} per customer</small></td><td><small>{new Date(row.startsAt).toLocaleString()}</small><small>to {new Date(row.endsAt).toLocaleString()}</small></td><td><strong>{row.usageCount}</strong><small>{row.globalUsageLimit==null?"No global cap":"of "+row.globalUsageLimit}</small></td><td><span className={row.active?styles.active:styles.inactive}>{row.active?"Active":"Inactive"}</span></td><td><button className={styles.edit} onClick={()=>startEdit(row)}>Edit</button></td></tr>)}</tbody></table></div>}
  </section>
  {visible&&<div className={styles.backdrop} role="presentation"><section className={styles.dialog} role="dialog" aria-modal="true" aria-labelledby="promotion-title"><div className={styles.dialogHeader}><div><span>{editing?"EDIT OFFER":"NEW OFFER"}</span><h2 id="promotion-title">{editing?"Update promotion":"Create promotion"}</h2></div><button type="button" className={styles.close} aria-label="Close" onClick={()=>setVisible(false)}>×</button></div>
   <form onSubmit={save}><div className={styles.formGrid}>
    <label>Coupon code<input value={form.code} onChange={e=>set("code",e.target.value.toUpperCase().replace(/[^A-Z0-9_-]/g,""))} maxLength={40} required placeholder="WELCOME10"/></label>
    <label>Offer name<input value={form.name} onChange={e=>set("name",e.target.value)} maxLength={120} required placeholder="Welcome offer"/></label>
    <label className={styles.wide}>Description<textarea value={form.description} onChange={e=>set("description",e.target.value)} maxLength={500} rows={2}/></label>
    <label>Discount type<select value={form.discountType} onChange={e=>set("discountType",e.target.value as DiscountType)}><option value="PERCENTAGE">Percentage</option><option value="FIXED">Fixed amount (₹)</option></select></label>
    <label>Discount value<input type="number" min="0.01" step="0.01" value={form.discountValue} onChange={e=>set("discountValue",e.target.value)} required/><small>{form.discountType==="PERCENTAGE"?"Percentage of eligible order amount":"Fixed rupee discount"}</small></label>
    <label>Maximum discount (₹)<input type="number" min="0.01" step="0.01" value={form.maxDiscount} onChange={e=>set("maxDiscount",e.target.value)} placeholder="Optional cap"/></label>
    <label>Minimum order (₹)<input type="number" min="0" step="0.01" value={form.minOrder} onChange={e=>set("minOrder",e.target.value)} required/></label>
    <fieldset className={styles.wide}><legend>Eligible payment purposes</legend><div className={styles.checks}>{purposes.map(p=><label key={p}><input type="checkbox" checked={form.purposes.includes(p)} onChange={()=>togglePurpose(p)}/>{p}</label>)}</div></fieldset>
    <label>Starts at<input type="datetime-local" value={form.startsAt} onChange={e=>set("startsAt",e.target.value)} required/></label>
    <label>Ends at<input type="datetime-local" value={form.endsAt} onChange={e=>set("endsAt",e.target.value)} required/></label>
    <label>Global redemption limit<input type="number" min="1" step="1" value={form.globalUsageLimit} onChange={e=>set("globalUsageLimit",e.target.value)} placeholder="Unlimited"/></label>
    <label>Per-customer limit<input type="number" min="1" step="1" value={form.perUserLimit} onChange={e=>set("perUserLimit",e.target.value)} required/></label>
    <fieldset className={styles.wide}><legend>Offer behaviour</legend><div className={styles.checks}><label><input type="checkbox" checked={form.automatic} onChange={e=>set("automatic",e.target.checked)}/>Apply automatically when eligible</label><label><input type="checkbox" checked={form.onlineOnly} onChange={e=>set("onlineOnly",e.target.checked)}/>Online payment only</label><label><input type="checkbox" checked={form.active} onChange={e=>set("active",e.target.checked)}/>Active</label></div></fieldset>
   </div><div className={styles.formActions}><button type="button" className={styles.cancel} onClick={()=>setVisible(false)}>Cancel</button><button type="submit" disabled={busy}>{busy?"Saving…":editing?"Save changes":"Create offer"}</button></div></form>
  </section></div>}
 </main>;
}
