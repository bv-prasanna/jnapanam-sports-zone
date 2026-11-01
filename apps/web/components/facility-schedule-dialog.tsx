"use client";
import {FormEvent,useEffect,useState} from "react";
import {api,ApiError} from "@/lib/api";

type Row=Record<string,unknown>;
type Hour={dayOfWeek:string;openTime:string;closeTime:string;closed:boolean};
const days=["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY"];
const dayName:Record<string,string>={MONDAY:"Monday",TUESDAY:"Tuesday",WEDNESDAY:"Wednesday",THURSDAY:"Thursday",FRIDAY:"Friday",SATURDAY:"Saturday",SUNDAY:"Sunday"};
const defaults=()=>days.map(dayOfWeek=>({dayOfWeek,openTime:"06:00",closeTime:"21:00",closed:false}));
function message(error:unknown,fallback:string){if(error instanceof ApiError&&error.message&&error.message.length<180&&!/https?:\/\/|\/api\/|exception/i.test(error.message))return error.message;return fallback}

export function FacilityScheduleDialog({facility,onClose}:{facility:Row;onClose:()=>void}){
 const [hours,setHours]=useState<Hour[]>(defaults());const [savedDays,setSavedDays]=useState<string[]>([]);const [closures,setClosures]=useState<Row[]>([]);const [loading,setLoading]=useState(true);const [saving,setSaving]=useState(false);const [editedHours,setEditedHours]=useState(false);const [confirmDiscard,setConfirmDiscard]=useState(false);const [error,setError]=useState("");const [notice,setNotice]=useState("");
 async function load(preserveEdits=false){setLoading(true);setError("");try{const value=await api<Row>(`/api/v1/admin/catalog/facilities/${facility.id}/schedule`);const existing=Array.isArray(value.weeklyOperatingHours)?value.weeklyOperatingHours as Row[]:[];setSavedDays([...new Set(existing.map(item=>String(item.dayOfWeek)))]);if(!preserveEdits){setEditedHours(false);setHours(defaults().flatMap(base=>{const found=existing.filter(item=>item.dayOfWeek===base.dayOfWeek);return found.length?found.map(item=>({dayOfWeek:base.dayOfWeek,openTime:String(item.openTime||base.openTime).slice(0,5),closeTime:String(item.closeTime||base.closeTime).slice(0,5),closed:Boolean(item.closed)})):[base]}));}setClosures(Array.isArray(value.closures)?value.closures as Row[]:[])}catch(e){setError(message(e,"Unable to load the facility schedule."))}finally{setLoading(false)}}
 useEffect(()=>{void load()},[facility.id]);
 function update(index:number,changes:Partial<Hour>){
  setHours(values=>{
   const day=values[index].dayOfWeek;
   if(changes.closed===true)return values.filter((v,i)=>i===index||v.dayOfWeek!==day).map((v,i)=>({...v,...(v.dayOfWeek===day?{closed:true}:{} )}));
   return values.map((v,i)=>i===index?{...v,...changes}:v);
  });
  setEditedHours(true);setConfirmDiscard(false);setError("");setNotice("");
 }
 function addPeriod(index:number){
  const current=hours[index];
  if(current.closed)return;
  const start=current.closeTime||"17:00";
  const [h,m]=start.split(":").map(Number);
  const candidate=(h*60+m)+60;
  const end=candidate<1440?String(Math.floor(candidate/60)).padStart(2,"0")+":"+String(candidate%60).padStart(2,"0"):"23:59";
  setHours(values=>[...values.slice(0,index+1),{dayOfWeek:current.dayOfWeek,openTime:start,closeTime:end,closed:false},...values.slice(index+1)]);
  setEditedHours(true);setNotice("");
 }
 function removePeriod(index:number){
  const day=hours[index].dayOfWeek;
  if(hours.filter(p=>p.dayOfWeek===day).length<2)return;
  setHours(values=>values.filter((_,i)=>i!==index));setEditedHours(true);setNotice("");
 }
 function requestClose(){if(saving)return;if(editedHours||savedDays.length<7){setConfirmDiscard(true);return}onClose()}
 async function saveHours(closeAfter=false){
  for(const day of days){
    const periods=hours.filter(hour=>hour.dayOfWeek===day).sort((a,b)=>a.openTime.localeCompare(b.openTime));
    if(!periods.length){setError(`Configure at least one period for ${dayName[day]}.`);return}
    if(periods.some(hour=>hour.closed)&&periods.length>1){setError(`Closed ${dayName[day]} cannot have other periods.`);return}
    for(const hour of periods){
      if(!hour.closed&&(!hour.openTime||!hour.closeTime||hour.openTime>=hour.closeTime)){
        setError(`${dayName[day]} closing time must be after opening time.`);return;
      }
    }
    for(let i=1;i<periods.length;i++)
      if(periods[i-1].closeTime>periods[i].openTime){
        setError(`${dayName[day]} has overlapping periods. Adjust the opening times.`);return;
      }
  }
  try{setSaving(true);setError("");setNotice("");
    for(const day of days){
      const periods=hours.filter(hour=>hour.dayOfWeek===day);
      await api(`/api/v1/admin/catalog/facilities/${facility.id}/operating-periods/${day}`,{
        method:"PUT",body:JSON.stringify(periods.map(hour=>({
          dayOfWeek:day,openTime:hour.closed?null:hour.openTime,
          closeTime:hour.closed?null:hour.closeTime,closed:hour.closed
        })))
      });
    }
    setNotice("Operating hours saved successfully.");await load();if(closeAfter)onClose()
  }catch(e){setError(message(e,"Unable to save operating hours."))}finally{setSaving(false)}
 }
 async function addClosure(event:FormEvent<HTMLFormElement>){event.preventDefault();const formElement=event.currentTarget;const form=new FormData(formElement);const fromDate=String(form.get("fromDate")||"");const toDate=String(form.get("toDate")||"");const reason=String(form.get("reason")||"").trim();if(!fromDate||!toDate||reason.length<3){setError("Choose the closure date range and enter a clear reason.");return}const from=new Date(`${fromDate}T00:00:00`);const to=new Date(`${toDate}T00:00:00`);const days=Math.round((to.getTime()-from.getTime())/86400000)+1;if(days<1){setError("To date must be on or after from date.");return}if(days>366){setError("A temporary closure can cover at most 366 days.");return}try{setSaving(true);setError("");for(let offset=0;offset<days;offset++){const date=new Date(from);date.setDate(from.getDate()+offset);const closureDate=date.toISOString().slice(0,10);await api(`/api/v1/admin/catalog/facilities/${facility.id}/closures`,{method:"POST",body:JSON.stringify({closureDate,reason})})}formElement.reset();setNotice(days===1?"Closure added successfully.":`${days} closure days added successfully.`);await load(editedHours)}catch(e){setError(message(e,"Unable to add the closure."))}finally{setSaving(false)}}
 async function removeClosure(id:unknown){try{setSaving(true);setError("");await api(`/api/v1/admin/catalog/facilities/${facility.id}/closures/${id}`,{method:"DELETE"});setNotice("Closure removed.");await load(editedHours)}catch(e){setError(message(e,"Unable to remove the closure."))}finally{setSaving(false)}}
 return <div className="portal-dialog-backdrop"><section className="portal-dialog facility-schedule-dialog" role="dialog" aria-modal="true" aria-labelledby="facility-schedule-title"><button className="dialog-close" type="button" aria-label="Close facility hours editor" onClick={requestClose} disabled={saving}>×</button><span className="eyebrow">Facility availability</span><h2 id="facility-schedule-title">{String(facility.name)}</h2><p>Set multiple opening periods for each weekday. Slot duration, cleaning buffer, and maximum members per slot are configured under Edit facility.</p>
 {!loading&&savedDays.length===0&&<div className="schedule-setup-alert" role="status"><strong>Opening hours have not been saved for this facility.</strong><p>The times shown below are an <b>unsaved example</b>. Customers will not see bookable slots until you confirm the real operating hours and click <b>Save hours</b>.</p></div>}
 {!loading&&savedDays.length>0&&savedDays.length<7&&<div className="schedule-setup-alert" role="status"><strong>Some weekdays are not configured.</strong><p>Unsaved days are marked below. Set the appropriate opening/closing times or mark them Closed, then select <b>Save hours</b>.</p></div>}{error&&<div className="dialog-error" role="alert">{error}</div>}{notice&&<div className="form-note" role="status">{notice}</div>}{loading?<div className="loading-line">Loading…</div>:<><section className="schedule-section"><div className="schedule-title"><h3>Weekly operating hours</h3><span className="schedule-save-location">Add more periods for split shifts, then save all weekdays below.</span></div><div className="weekly-hours">{hours.map((hour,index)=><div className={hour.closed?"hour-row is-closed":"hour-row"} key={`${hour.dayOfWeek}-${index}`}><strong>{dayName[hour.dayOfWeek]}{!savedDays.includes(hour.dayOfWeek)&&<small className="schedule-unsaved">Not saved</small>}</strong><label className="closed-toggle"><input type="checkbox" checked={hour.closed} onChange={e=>update(index,{closed:e.target.checked})}/><span>Closed</span></label><label>Opens<input type="time" value={hour.openTime} disabled={hour.closed} onChange={e=>update(index,{openTime:e.target.value})}/></label><label>Closes<input type="time" value={hour.closeTime} disabled={hour.closed} onChange={e=>update(index,{closeTime:e.target.value})}/></label><button type="button" className="btn outline" disabled={hour.closed} onClick={()=>addPeriod(index)}>+ Add another period</button>{hours.filter(value=>value.dayOfWeek===hour.dayOfWeek).length>1&&<button type="button" className="btn outline" onClick={()=>removePeriod(index)} aria-label={`Remove ${dayName[hour.dayOfWeek]} period`}>Remove</button>}</div>)}</div></section><section className="schedule-section"><h3>Temporary closures</h3><form className="closure-form" onSubmit={addClosure}><label>From date<input name="fromDate" type="date" required/></label><label>To date<input name="toDate" type="date" required/></label><label>Reason<input name="reason" maxLength={250} placeholder="Example: Maintenance" required/></label><button className="btn primary" disabled={saving}>{saving?"Adding…":"Add closure"}</button></form><div className="closure-list">{closures.length?closures.map(closure=><article key={String(closure.id)}><div><strong>{String(closure.closureDate)}</strong><span>{String(closure.reason)}</span></div><button type="button" onClick={()=>void removeClosure(closure.id)} disabled={saving}>Remove</button></article>):<p>No temporary closures configured.</p>}</div></section></>}{confirmDiscard&&<div className="schedule-discard-warning" role="alert"><strong>Operating hours have not been saved.</strong><p>Closing without saving can leave this facility without bookable slots. Do you want to discard these hours?</p><div><button type="button" className="btn secondary" onClick={()=>setConfirmDiscard(false)}>Keep editing</button><button type="button" className="btn outline" onClick={onClose}>Discard and close</button></div></div>}
 <div className="dialog-actions schedule-dialog-actions"><button className="btn secondary" type="button" onClick={requestClose} disabled={saving}>{editedHours||savedDays.length<7?"Close without saving":"Close"}</button><button className="btn primary" type="button" onClick={()=>void saveHours(true)} disabled={saving||loading}>{saving?"Saving hours…":"Save hours & close"}</button></div></section></div>
}
