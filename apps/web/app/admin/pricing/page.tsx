"use client";

import {FormEvent,useCallback,useEffect,useState} from "react";
import {api} from "@/lib/api";
import styles from "./pricing.module.css";

type Scope="SITE"|"SPORT"|"FACILITY"|"SLOT";
type Kind="taxes"|"pricing-rules";
type Item={id:string;name:string;scope:Scope;target?:string|null;active:boolean;rate_percent?:number;inclusive?:boolean;adjustment?:string;value?:number;priority?:number;effective_from?:string;effective_to?:string;start_time?:string;end_time?:string;weekdays?:string};
type Option={code?:string;id?:string;name:string};
type Slot={id:string;startTime:string;endTime:string};
const scopes:[Scope,string][]=[["SITE","Entire site"],["SPORT","Sport"],["FACILITY","Facility / court"],["SLOT","Individual slot"]];
const empty=()=>({name:"",scope:"SITE" as Scope,target:"",value:"0",adjustment:"ADD_PERCENT",priority:"0",startTime:"",endTime:"",effectiveFrom:"",effectiveTo:"",ratePercent:"0",inclusive:false,weekdays:[] as string[]});
const days=["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY"];
const dateOnly=(v?:string)=>v?v.slice(0,10):"";
const timeOnly=(v?:string)=>v?v.slice(0,5):"";
export default function PricingAdminPage(){
 const [kind,setKind]=useState<Kind>("pricing-rules");
 const [records,setRecords]=useState<Item[]>([]);
 const [sports,setSports]=useState<Option[]>([]),[facilities,setFacilities]=useState<Option[]>([]),[slots,setSlots]=useState<Slot[]>([]);
 const [slotFacility,setSlotFacility]=useState(""),[slotDate,setSlotDate]=useState(new Date().toLocaleDateString("en-CA"));
 const [editing,setEditing]=useState<string|null>(null),[visible,setVisible]=useState(false);
 const [form,setForm]=useState(empty());
 const [busy,setBusy]=useState(false),[error,setError]=useState(""),[success,setSuccess]=useState("");
 const load=useCallback(async()=>{try{setError("");const [data,s,f]=await Promise.all([api<Item[]>(`/api/v1/admin/${kind}`),api<Option[]>("/api/v1/admin/catalog/sports"),api<Option[]>("/api/v1/admin/catalog/facilities")]);setRecords(data);setSports(s);setFacilities(f)}catch(e){setError(e instanceof Error?e.message:"Unable to load pricing configuration")}},[kind]);
 useEffect(()=>{void load()},[load]);
 useEffect(()=>{if(form.scope!=="SLOT"||!slotFacility||!slotDate){setSlots([]);return}let active=true;api<Slot[]>(`/api/v1/public/facilities/${slotFacility}/slots?date=${slotDate}`).then(v=>{if(active)setSlots(v)}).catch(()=>{if(active)setSlots([])});return()=>{active=false}},[form.scope,slotFacility,slotDate]);
 function reset(){setForm(empty());setEditing(null);setVisible(false);setError("")}
 function edit(r:Item){setForm({name:r.name,scope:r.scope,target:r.target||"",value:String(r.value??0),adjustment:r.adjustment||"ADD_PERCENT",priority:String(r.priority??0),startTime:timeOnly(r.start_time),endTime:timeOnly(r.end_time),effectiveFrom:dateOnly(r.effective_from),effectiveTo:dateOnly(r.effective_to),ratePercent:String(r.rate_percent??0),inclusive:Boolean(r.inclusive),weekdays:r.weekdays?r.weekdays.split(",").filter(Boolean):[]});setEditing(r.id);setVisible(true);setSuccess("")}
 const set=(key:keyof ReturnType<typeof empty>,value:unknown)=>setForm(v=>({...v,[key]:value}));
 async function save(event:FormEvent){event.preventDefault();try{setBusy(true);setError("");setSuccess("");if(form.scope!=="SITE"&&!form.target)throw new Error("Choose a sport, facility or slot.");
 const shared={name:form.name.trim(),scope:form.scope,target:form.scope==="SITE"?null:form.target,effectiveFrom:form.effectiveFrom||null,effectiveTo:form.effectiveTo||null};
 const payload=kind==="taxes"?{...shared,ratePercent:Number(form.ratePercent),inclusive:form.inclusive}:{...shared,adjustment:form.adjustment,value:Number(form.value),priority:Number(form.priority),startTime:form.startTime||null,endTime:form.endTime||null,weekdays:form.weekdays};
 await api(`/api/v1/admin/${kind}${editing?"/"+editing:""}`,{method:editing?"PUT":"POST",body:JSON.stringify(payload)});
 reset();setSuccess("Rule saved successfully.");await load();
 }catch(e){setError(e instanceof Error?e.message:"Unable to save rule")}finally{setBusy(false)}}
 async function disable(r:Item){if(!window.confirm(`Deactivate "${r.name}"? Existing bookings and invoices will not change.`))return;try{setBusy(true);await api(`/api/v1/admin/${kind}/${r.id}`,{method:"DELETE"});setSuccess("Rule deactivated.");await load()}catch(e){setError(e instanceof Error?e.message:"Unable to deactivate rule")}finally{setBusy(false)}}
 const targets=form.scope==="SPORT"?sports.map(x=>({value:String(x.code),label:x.name})):form.scope==="FACILITY"?facilities.map(x=>({value:String(x.code),label:x.name})):slots.map(x=>({value:x.id,label:`${x.startTime?.slice(0,5)}–${x.endTime?.slice(0,5)}`}));
 return <main className={styles.page}>
 <header className={styles.head}><div><p className={styles.eyebrow}>ARENA REVENUE MANAGEMENT</p><h1>Pricing & Taxes</h1><p>Set site-wide, sport, court or individual slot rules. Existing booking prices remain locked.</p></div><button type="button" className={styles.primary} onClick={()=>{reset();setVisible(true)}}>+ Add rule</button></header>
 <div className={styles.tabs} role="tablist" aria-label="Rule type"><button type="button" role="tab" aria-selected={kind==="pricing-rules"} onClick={()=>{setKind("pricing-rules");reset()}}>Dynamic Pricing</button><button type="button" role="tab" aria-selected={kind==="taxes"} onClick={()=>{setKind("taxes");reset()}}>Tax Management</button></div>
 {success&&<p role="status" className={styles.success}>{success}</p>}{error&&!visible&&<p role="alert" className={styles.error}>{error}</p>}
 <div className={styles.tableWrap}><table><thead><tr><th>Rule</th><th>Applies to</th><th>{kind==="taxes"?"Tax":"Adjustment"}</th><th>Effective dates</th><th>Status</th><th>Actions</th></tr></thead><tbody>{records.length?records.map(r=><tr key={r.id}><td><strong>{r.name}</strong></td><td>{scopes.find(x=>x[0]===r.scope)?.[1]}{r.target?" · "+r.target:""}</td><td>{kind==="taxes"?`${r.rate_percent}% ${r.inclusive?"included":"extra"}`:`${r.adjustment?.replaceAll("_"," ")}: ${r.value}`}</td><td>{dateOnly(r.effective_from)||"Any"} – {dateOnly(r.effective_to)||"Any"}</td><td>{r.active?"Active":"Inactive"}</td><td><button type="button" onClick={()=>edit(r)}>Edit</button> {r.active&&<button type="button" disabled={busy} onClick={()=>void disable(r)}>Deactivate</button>}</td></tr>):<tr><td colSpan={6}>No configured rules yet. Add a rule to begin.</td></tr>}</tbody></table></div>
 {visible&&<section className={styles.editor} aria-label={editing?"Edit rule":"Create rule"}><div className={styles.editorHead}><h2>{editing?"Edit":"Add"} {kind==="taxes"?"tax":"pricing"} rule</h2><button type="button" onClick={reset} aria-label="Close editor">✕</button></div><form onSubmit={e=>void save(e)} className={styles.form}>
 <label>Rule name <input required maxLength={120} value={form.name} onChange={e=>set("name",e.target.value)} placeholder={kind==="taxes"?"Default arena tax":"Evening peak hours"}/></label>
 <label>Apply to<select value={form.scope} onChange={e=>setForm(v=>({...v,scope:e.target.value as Scope,target:""}))}>{scopes.map(([v,l])=><option key={v} value={v}>{l}</option>)}</select></label>
 {form.scope==="SLOT"&&<div className={styles.two}><label>Select facility<select value={slotFacility} onChange={e=>setSlotFacility(e.target.value)}><option value="">Select facility</option>{facilities.map(v=><option key={v.id} value={v.id}>{v.name}</option>)}</select></label><label>Slot date<input type="date" value={slotDate} onChange={e=>setSlotDate(e.target.value)}/></label></div>}
 {form.scope!=="SITE"&&<label>{form.scope==="SPORT"?"Sport":form.scope==="FACILITY"?"Facility":"Slot"}<select required value={form.target} onChange={e=>set("target",e.target.value)}><option value="">Choose {form.scope.toLowerCase()}</option>{targets.map(x=><option key={x.value} value={x.value}>{x.label}</option>)}{form.target&&!targets.some(x=>x.value===form.target)&&<option value={form.target}>Previously selected {form.target}</option>}</select></label>}
 {kind==="taxes"?<><label>Tax rate (%) <input type="number" min="0" max="100" step="0.001" required value={form.ratePercent} onChange={e=>set("ratePercent",e.target.value)}/></label><label className={styles.check}><input type="checkbox" checked={form.inclusive} onChange={e=>set("inclusive",e.target.checked)}/> Published price includes tax</label><p className={styles.help}>Tax applicability and GST rates must be approved by your accountant before activation.</p></>:<><label>Adjustment type<select value={form.adjustment} onChange={e=>set("adjustment",e.target.value)}>{[["FIXED_PRICE","Fixed price"],["ADD_AMOUNT","Increase by amount (₹)"],["DISCOUNT_AMOUNT","Decrease by amount (₹)"],["ADD_PERCENT","Increase by percentage"],["DISCOUNT_PERCENT","Decrease by percentage"]].map(([v,l])=><option key={v} value={v}>{l}</option>)}</select></label><label>Value<input type="number" min="0" max={form.adjustment.endsWith("PERCENT")?100:undefined} step="0.01" required value={form.value} onChange={e=>set("value",e.target.value)}/></label><div className={styles.two}><label>From time<input type="time" value={form.startTime} onChange={e=>set("startTime",e.target.value)}/></label><label>Until time<input type="time" value={form.endTime} onChange={e=>set("endTime",e.target.value)}/></label></div><fieldset className={styles.days}><legend>Weekdays (leave blank for all)</legend>{days.map(day=><label key={day}><input type="checkbox" checked={form.weekdays.includes(day)} onChange={e=>set("weekdays",e.target.checked?[...form.weekdays,day]:form.weekdays.filter(x=>x!==day))}/>{day.slice(0,3)}</label>)}</fieldset><label>Priority<input type="number" value={form.priority} onChange={e=>set("priority",e.target.value)}/></label></>}
 <div className={styles.two}><label>Effective from<input type="date" value={form.effectiveFrom} onChange={e=>set("effectiveFrom",e.target.value)}/></label><label>Effective until<input type="date" min={form.effectiveFrom||undefined} value={form.effectiveTo} onChange={e=>set("effectiveTo",e.target.value)}/></label></div>
 {error&&<p role="alert" className={styles.error}>{error}</p>}
 <div className={styles.actions}><button type="button" onClick={reset}>Cancel</button><button type="submit" disabled={busy} className={styles.primary}>{busy?"Saving…":"Save rule"}</button></div></form></section>}
 </main>
}
