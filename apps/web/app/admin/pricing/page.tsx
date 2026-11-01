"use client";

import {FormEvent,useCallback,useEffect,useState} from "react";
import {api} from "@/lib/api";
import styles from "./pricing.module.css";
import {useRouter,useSearchParams} from "next/navigation";

type Scope="SITE"|"SPORT"|"FACILITY"|"SLOT";
type Kind="taxes"|"pricing-rules";
type Item={id:string;name:string;scope:Scope;target?:string|null;targetLabel?:string|null;active:boolean;rate_percent?:number;inclusive?:boolean;adjustment?:string;value?:number;priority?:number;effective_from?:string;effective_to?:string;start_time?:string;end_time?:string;weekdays?:string};
type Option={code?:string;id?:string;name:string;bookingPrice?:number|string};
type Slot={id:string;startTime:string;endTime:string};
const scopes:[Scope,string][]=[["SITE","Entire site"],["SPORT","Sport"],["FACILITY","Facility / court"],["SLOT","Individual slot"]];
const empty=(scope:Scope="FACILITY")=>({name:"",scope,target:"",value:"",adjustment:"FIXED_PRICE",priority:"0",startTime:"",endTime:"",effectiveFrom:"",effectiveTo:"",ratePercent:"0",inclusive:false,weekdays:[] as string[]});
const days=["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY"];
const dateOnly=(v?:string)=>v?v.slice(0,10):"";
const timeOnly=(v?:string)=>v?v.slice(0,5):"";
const whenText=(r:Item)=>{
 const days=r.weekdays?.split(",").filter(Boolean)||[];
 const cadence=days.length?days.map(day=>day.slice(0,3)).join(", "):"Every day";
 const hours=r.start_time&&r.end_time?`${timeOnly(r.start_time)}–${timeOnly(r.end_time)}`:"All hours";
 const from=dateOnly(r.effective_from),to=dateOnly(r.effective_to);
 const dates=from&&to&&from===to?`Only ${from}`:from||to?`${from||"Any day"} – ${to||"Ongoing"}`:"No end date";
 return `${cadence} · ${hours} · ${dates}`;
};
export default function PricingAdminPage(){
 const router=useRouter(),searchParams=useSearchParams();
 const requestedTab=searchParams.get("tab");
 const [kind,setKind]=useState<Kind>(requestedTab==="taxes"?"taxes":"pricing-rules");
 const [records,setRecords]=useState<Item[]>([]);
 const [sports,setSports]=useState<Option[]>([]),[facilities,setFacilities]=useState<Option[]>([]),[slots,setSlots]=useState<Slot[]>([]);
 const [slotFacility,setSlotFacility]=useState(""),[slotDate,setSlotDate]=useState(new Date().toLocaleDateString("en-CA"));
 const [slotLoading,setSlotLoading]=useState(false),[slotError,setSlotError]=useState("");
 const [editing,setEditing]=useState<string|null>(null),[visible,setVisible]=useState(false);
 const [form,setForm]=useState(empty());
 const [exceptionFor,setExceptionFor]=useState<Item|null>(null);
 const [busy,setBusy]=useState(false),[error,setError]=useState(""),[success,setSuccess]=useState("");
 const load=useCallback(async()=>{try{setError("");const [data,s,f]=await Promise.all([api<Item[]>(`/api/v1/admin/${kind}`),api<Option[]>("/api/v1/admin/catalog/sports"),api<Option[]>("/api/v1/admin/catalog/facilities")]);setRecords(data);setSports(s);setFacilities(f)}catch(e){setError(e instanceof Error?e.message:"Unable to load pricing configuration")}},[kind]);
 useEffect(()=>{void load()},[load]);
 useEffect(()=>{const next:Kind=requestedTab==="taxes"?"taxes":"pricing-rules";setKind(next);setEditing(null);setVisible(false);setError("")},[requestedTab]);
 useEffect(()=>{
  if(form.scope!=="SLOT"||!slotFacility||!slotDate){setSlots([]);setSlotError("");setSlotLoading(false);return}
  let active=true;setSlotLoading(true);setSlotError("");
  api<Slot[]>(`/api/v1/public/facilities/${slotFacility}/slots?date=${slotDate}`)
    .then(values=>{if(active)setSlots(values)})
    .catch(e=>{if(active){setSlots([]);setSlotError(e instanceof Error?e.message:"Unable to load slots for that day.")}})
    .finally(()=>{if(active)setSlotLoading(false)});
  return()=>{active=false};
 },[form.scope,slotFacility,slotDate]);
 function reset(){setForm(empty(kind==="taxes"?"SITE":"FACILITY"));setEditing(null);setExceptionFor(null);setVisible(false);setError("")}
 function newPricePreset(preset:"day"|"evening"){
   setKind("pricing-rules");setEditing(null);setExceptionFor(null);setSuccess("");setError("");
   setForm({...empty(),name:preset==="day"?"Daytime standard rate":"Evening floodlight rate",adjustment:"FIXED_PRICE",
      value:preset==="day"?"300":"400",startTime:preset==="day"?"00:00":"18:00",endTime:preset==="day"?"18:00":"23:00"});
   setVisible(true);
 }
 function exceptionForDate(rule:Item){
   const priority=Math.max(0,...records.filter(r=>r.active&&r.scope===rule.scope&&r.target===rule.target).map(r=>Number(r.priority||0)))+1;
   const base=facilities.find(f=>f.code===rule.target)?.bookingPrice;
   setEditing(null);setExceptionFor(rule);setSuccess("");setError("");
   setForm({...empty(),scope:rule.scope,target:rule.target||"",name:rule.name+" — date override",
      startTime:timeOnly(rule.start_time),endTime:timeOnly(rule.end_time),
      adjustment:"FIXED_PRICE",value:base==null?"":String(base),priority:String(priority),weekdays:[]});
   setVisible(true);
 }
 function selectTab(next:Kind){setKind(next);reset();router.replace(`/admin/pricing?tab=${next}`)}
 function edit(r:Item){setForm({name:r.name,scope:r.scope,target:r.target||"",value:String(r.value??0),adjustment:r.adjustment||"ADD_PERCENT",priority:String(r.priority??0),startTime:timeOnly(r.start_time),endTime:timeOnly(r.end_time),effectiveFrom:dateOnly(r.effective_from),effectiveTo:dateOnly(r.effective_to),ratePercent:String(r.rate_percent??0),inclusive:Boolean(r.inclusive),weekdays:r.weekdays?r.weekdays.split(",").filter(Boolean):[]});setEditing(r.id);setExceptionFor(null);setVisible(true);setSuccess("")}
 const set=(key:keyof ReturnType<typeof empty>,value:unknown)=>setForm(v=>({...v,[key]:value}));
 async function save(event:FormEvent){event.preventDefault();try{setBusy(true);setError("");setSuccess("");if(form.scope!=="SITE"&&!form.target)throw new Error("Choose a sport, facility or slot.");
 if(kind==="pricing-rules"){
   if(form.value.trim()===""||!Number.isFinite(Number(form.value)))throw new Error("Enter a valid price or adjustment.");
   if(Boolean(form.startTime)!==Boolean(form.endTime))throw new Error("Set both start and end times, or leave both empty for all hours.");
   if(form.startTime&&form.endTime&&form.endTime<=form.startTime)throw new Error("End time must be after start time. Split overnight pricing into two rules.");
   if(exceptionFor&&(!form.effectiveFrom||form.effectiveTo!==form.effectiveFrom))throw new Error("Choose the one date for this exception.");
   if(form.scope==="SLOT"&&!editing&&(!slotFacility||!slotDate||!slots.some(slot=>slot.id===form.target)))throw new Error("Choose a facility, date and a generated slot; use Facility / court for recurring time-based prices.");
   if(!editing&&!exceptionFor&&form.scope==="FACILITY"&&form.startTime&&form.endTime){
     const conflict=records.find(r=>r.active&&r.scope==="FACILITY"&&r.target===form.target&&
       Number(r.priority||0)===Number(form.priority||0)&&
       (!r.effective_from&&!r.effective_to)&&
       ((r.weekdays||"")===""||form.weekdays.length===0||form.weekdays.some(d=>(r.weekdays||"").split(",").includes(d)))&&
       (!r.start_time||!r.end_time||!(form.endTime<=timeOnly(r.start_time)||form.startTime>=timeOnly(r.end_time))));
     if(conflict)throw new Error(`This time range overlaps the active rule "${conflict.name}". Edit that rule, select non-overlapping hours, or set an intentional priority.`);
   }
 }
 const shared={name:form.name.trim(),scope:form.scope,target:form.scope==="SITE"?null:form.target,effectiveFrom:form.effectiveFrom||null,effectiveTo:form.effectiveTo||null};
 const payload=kind==="taxes"?{...shared,ratePercent:Number(form.ratePercent),inclusive:form.inclusive}:{...shared,adjustment:form.adjustment,value:Number(form.value),priority:Number(form.priority),startTime:form.startTime||null,endTime:form.endTime||null,weekdays:form.weekdays};
 await api(`/api/v1/admin/${kind}${editing?"/"+editing:""}`,{method:editing?"PUT":"POST",body:JSON.stringify(payload)});
 const wasException=Boolean(exceptionFor);reset();setSuccess(wasException?"One-day override saved. All other dates keep their recurring price.":"Rule saved successfully.");await load();
 }catch(e){setError(e instanceof Error?e.message:"Unable to save rule")}finally{setBusy(false)}}
 async function disable(r:Item){if(!window.confirm(`Deactivate "${r.name}"? Existing bookings and invoices will not change.`))return;try{setBusy(true);await api(`/api/v1/admin/${kind}/${r.id}`,{method:"DELETE"});setSuccess("Rule deactivated.");await load()}catch(e){setError(e instanceof Error?e.message:"Unable to deactivate rule")}finally{setBusy(false)}}
 const targets=form.scope==="SPORT"?sports.map(x=>({value:String(x.code),label:x.name})):form.scope==="FACILITY"?facilities.map(x=>({value:String(x.code),label:x.name})):slots.map(x=>({value:x.id,label:`${x.startTime?.slice(0,5)}–${x.endTime?.slice(0,5)}`}));
 return <main className={styles.page}>
 <header className={styles.head}><div><p className={styles.eyebrow}>ARENA REVENUE MANAGEMENT</p><h1>Pricing & Taxes</h1><p>Set recurring facility rates by time of day, with optional one-day exceptions. Existing bookings keep their locked prices.</p></div><button type="button" className={styles.primary} onClick={()=>{reset();setVisible(true)}}>+ Add {kind==="taxes"?"tax":"price"} rule</button></header>
 <div className={styles.tabs} role="tablist" aria-label="Rule type"><button type="button" role="tab" aria-selected={kind==="pricing-rules"} onClick={()=>selectTab("pricing-rules")}>Dynamic Pricing</button><button type="button" role="tab" aria-selected={kind==="taxes"} onClick={()=>selectTab("taxes")}>Tax Management</button></div>
 {success&&<p role="status" className={styles.success}>{success}</p>}{error&&!visible&&<p role="alert" className={styles.error}>{error}</p>}
 {kind==="pricing-rules"&&<section className={styles.guide} aria-label="Recurring facility price examples">
  <div><strong>Regular facility prices — no date required</strong>
   <p>Choose <b>Facility / court</b>, set the hours and price, and save. The rule repeats every day unless you select weekdays. For cricket, set ₹300 before 6 PM and ₹400 from 6–11 PM, including floodlights.</p>
  </div>
  <div className={styles.quickActions}>
    <button type="button" onClick={()=>newPricePreset("day")}>Daytime ₹300</button>
    <button type="button" onClick={()=>newPricePreset("evening")}>Floodlights ₹400</button>
  </div>
 </section>}
 <div className={styles.tableWrap}><table><thead><tr><th>Rule</th><th>Applies to</th><th>{kind==="taxes"?"Tax":"Adjustment"}</th><th>Schedule / validity</th><th>Status</th><th>Actions</th></tr></thead><tbody>{records.length?records.map(r=><tr key={r.id}><td><strong>{r.name}</strong></td><td>{scopes.find(x=>x[0]===r.scope)?.[1]}{r.target?" · "+(r.targetLabel||"Unavailable target"):""}</td><td>{kind==="taxes"?`${r.rate_percent}% ${r.inclusive?"included":"extra"}`:`${r.adjustment?.replaceAll("_"," ")}: ₹${r.value}`}</td><td>{kind==="taxes"?`${dateOnly(r.effective_from)||"Any"} – ${dateOnly(r.effective_to)||"Any"}`:whenText(r)}</td><td>{r.active?"Active":"Inactive"}</td><td><button type="button" onClick={()=>edit(r)}>Edit</button> {kind==="pricing-rules"&&r.active&&r.scope==="FACILITY"&&<button type="button" onClick={()=>exceptionForDate(r)}>One-day override</button>} {r.active&&<button type="button" disabled={busy} onClick={()=>void disable(r)}>Deactivate</button>}</td></tr>):<tr><td colSpan={6}>No configured rules yet. Add a rule to begin.</td></tr>}</tbody></table></div>
 {visible&&<section className={styles.editor} aria-label={editing?"Edit rule":"Create rule"}><div className={styles.editorHead}><h2>{editing?"Edit":"Add"} {kind==="taxes"?"tax":"pricing"} rule</h2><button type="button" onClick={reset} aria-label="Close editor">✕</button></div><form onSubmit={e=>void save(e)} className={styles.form}>
 <label>Rule name <input required maxLength={120} value={form.name} onChange={e=>set("name",e.target.value)} placeholder={kind==="taxes"?"Default arena tax":"Evening peak hours"}/></label>
 <label>Apply to<select value={form.scope} disabled={Boolean(exceptionFor)} onChange={e=>{setForm(v=>({...v,scope:e.target.value as Scope,target:""}));setSlotFacility("");setSlots([]);setSlotError("");}}>{scopes.map(([v,l])=><option key={v} value={v}>{l}</option>)}</select></label>
 {kind==="pricing-rules"&&form.scope==="FACILITY"&&<p className={styles.help}>Recommended: select your court and a recurring time range below. No slot or date is required. The same rate applies to every matching future slot.</p>}
 {kind==="pricing-rules"&&form.scope==="SLOT"&&<div className={styles.slotNotice}><strong>One-off booked slot only</strong><p>A slot belongs to a particular facility and date and appears only if operating hours or a schedule have generated it. For a regular day/evening price, change <b>Apply to</b> to <b>Facility / court</b>; you do not need to select any slot.</p><button type="button" onClick={()=>{setForm(v=>({...v,scope:"FACILITY",target:""}));setSlotFacility("");setSlots([]);setSlotError("")}}>Use recurring facility price instead</button></div>}
 {exceptionFor&&<div className={styles.slotNotice}><strong>One-day price exception</strong><p>Enter a specific day below. This rule takes precedence for that facility on that date; the regular price resumes on other days. No booked slot selection is needed.</p></div>}
 {form.scope==="SLOT"&&<><div className={styles.two}><label>Select facility<select required value={slotFacility} onChange={e=>{setSlotFacility(e.target.value);set("target","");}}><option value="">Select facility</option>{facilities.map(v=><option key={v.id} value={v.id}>{v.name}</option>)}</select></label><label>Slot date<input required type="date" min={new Date().toLocaleDateString("en-CA")} value={slotDate} onChange={e=>{setSlotDate(e.target.value);set("target","");}}/></label></div>
 {slotLoading?<p role="status" className={styles.help}>Loading bookable slots…</p>:slotError?<p role="alert" className={styles.error}>{slotError}</p>:slotFacility&&slots.length===0?<div className={styles.slotNotice}><strong>No slots exist for this facility on the selected date.</strong><p>First configure the court operating hours or day schedule in Facilities. For recurring peak-hour or floodlight pricing, choose <b>Facility / court</b> above instead of an individual slot.</p><button type="button" onClick={()=>{setForm(v=>({...v,scope:"FACILITY",target:""}));setSlotFacility("");setSlotError("")}}>Switch to Facility / court</button></div>:null}</>}
 {form.scope!=="SITE"&&<label>{form.scope==="SPORT"?"Sport":form.scope==="FACILITY"?"Facility / court":"Bookable slot"}<select required disabled={Boolean(exceptionFor)||form.scope==="SLOT"&&!editing&&(slotLoading||!slotFacility||slots.length===0)} value={form.target} onChange={e=>set("target",e.target.value)}><option value="">{form.scope==="SLOT"?"Choose an available slot":"Choose "+form.scope.toLowerCase()}</option>{targets.map(x=><option key={x.value} value={x.value}>{x.label}</option>)}{form.target&&!targets.some(x=>x.value===form.target)&&<option value={form.target}>{records.find(x=>x.id===editing)?.targetLabel || "Previously selected item"}</option>}</select></label>}
 {kind==="taxes"?<><label>Tax rate (%) <input type="number" min="0" max="100" step="0.001" required value={form.ratePercent} onChange={e=>set("ratePercent",e.target.value)}/></label><label className={styles.check}><input type="checkbox" checked={form.inclusive} onChange={e=>set("inclusive",e.target.checked)}/> Published price includes tax</label><p className={styles.help}>Tax applicability and GST rates must be approved by your accountant before activation.</p></>:<><label>Adjustment type<select value={form.adjustment} onChange={e=>set("adjustment",e.target.value)}>{[["FIXED_PRICE","Fixed price"],["ADD_AMOUNT","Increase by amount (₹)"],["DISCOUNT_AMOUNT","Decrease by amount (₹)"],["ADD_PERCENT","Increase by percentage"],["DISCOUNT_PERCENT","Decrease by percentage"]].map(([v,l])=><option key={v} value={v}>{l}</option>)}</select></label><label>Value<input type="number" min="0" max={form.adjustment.endsWith("PERCENT")?100:undefined} step="0.01" required value={form.value} onChange={e=>set("value",e.target.value)}/></label><div className={styles.two}><label>From time<input type="time" value={form.startTime} onChange={e=>set("startTime",e.target.value)}/></label><label>Until time<input type="time" value={form.endTime} onChange={e=>set("endTime",e.target.value)}/></label></div><fieldset className={styles.days}><legend>Weekdays (leave blank for all)</legend>{days.map(day=><label key={day}><input type="checkbox" checked={form.weekdays.includes(day)} onChange={e=>set("weekdays",e.target.checked?[...form.weekdays,day]:form.weekdays.filter(x=>x!==day))}/>{day.slice(0,3)}</label>)}</fieldset><label>Priority<input type="number" value={form.priority} onChange={e=>set("priority",e.target.value)}/></label></>}
 {exceptionFor?<label>Exception date<input type="date" required min={new Date().toLocaleDateString("en-CA")} value={form.effectiveFrom} onChange={e=>setForm(v=>({...v,effectiveFrom:e.target.value,effectiveTo:e.target.value}))}/></label>:
 <details className={styles.advancedDates} open={Boolean(form.effectiveFrom||form.effectiveTo)}><summary>Optional validity dates {kind==="pricing-rules"?"(leave blank to repeat indefinitely)":""}</summary><div className={styles.two}><label>Effective from<input type="date" value={form.effectiveFrom} onChange={e=>set("effectiveFrom",e.target.value)}/></label><label>Effective until<input type="date" min={form.effectiveFrom||undefined} value={form.effectiveTo} onChange={e=>set("effectiveTo",e.target.value)}/></label></div></details>}
 {error&&<p role="alert" className={styles.error}>{error}</p>}
 <div className={styles.actions}><button type="button" onClick={reset}>Cancel</button><button type="submit" disabled={busy} className={styles.primary}>{busy?"Saving…":"Save rule"}</button></div></form></section>}
 </main>
}
