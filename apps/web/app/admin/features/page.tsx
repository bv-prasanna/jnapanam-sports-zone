"use client";
import {useEffect,useState} from "react";
import {api,ApiError} from "@/lib/api";
import {useApp} from "@/components/app-provider";
import type {FeatureSetting} from "@/lib/features";

const roles=["CUSTOMER","OPERATIONS","SHOP_KEEPER","COACH","ORGANIZER","ADMIN","SUPER_ADMIN"];
export default function FeatureVisibilityAdmin(){
 const {user,ready}=useApp();
 const [features,setFeatures]=useState<FeatureSetting[]>([]);
 const [saving,setSaving]=useState("");
 const [message,setMessage]=useState("");
 useEffect(()=>{if(user?.roles.some(r=>r==="ADMIN"||r==="SUPER_ADMIN"))void api<FeatureSetting[]>("/api/v1/admin/features").then(setFeatures).catch(()=>setMessage("Unable to load feature settings."))},[user]);
 function change(code:string,updates:Partial<FeatureSetting>){setFeatures(items=>items.map(f=>f.code===code?{...f,...updates}:f))}
 async function setAvailability(f:FeatureSetting,enabled:boolean){
   setMessage("");setSaving(f.code);
   try {
     const result=await api<FeatureSetting>(`/api/v1/admin/features/${f.code}`,{method:"PUT",body:JSON.stringify({enabled,allowedRoles:f.allowedRoles})});
     change(f.code,result);
     setMessage(`${f.code} ${enabled?"enabled":"disabled"} successfully.`);
   }catch(e){setMessage(e instanceof ApiError?e.message:"Unable to change feature availability.")}
   finally{setSaving("")}
 }
 async function save(f:FeatureSetting){setMessage("");setSaving(f.code);try{
   const result=await api<FeatureSetting>(`/api/v1/admin/features/${f.code}`,{method:"PUT",body:JSON.stringify({enabled:f.enabled,allowedRoles:f.allowedRoles})});
   change(f.code,result);setMessage(`${f.code} visibility updated.`);
 }catch(e){setMessage(e instanceof ApiError?e.message:"Unable to save this feature.")}finally{setSaving("")}}
 if(!ready)return <main className="container">Loading…</main>;
 if(!user?.roles.some(r=>r==="ADMIN"||r==="SUPER_ADMIN"))return <main className="container"><h1>Access restricted</h1></main>;
 return <main className="admin-feature-page">
 <a className="admin-feature-back" href="/admin">← Back to Admin</a><h1>Feature visibility & access</h1>
 <p className="admin-feature-intro">Select Disable to hide an unavailable feature. Select Enable to restore it. Choose permitted roles and save when needed. Turning a feature off does not delete existing bookings, products or payments.</p>
 {message&&<p role="status">{message}</p>}
 <div className="admin-feature-list">{features.map(f=><section key={f.code} className="admin-feature-card">
 <div className="admin-feature-heading">
 <h2>{f.code.replaceAll("_"," ")}</h2><strong aria-live="polite">{f.enabled?"Enabled":"Disabled"}</strong>
 <label><input type="checkbox" checked={f.enabled} onChange={e=>change(f.code,{enabled:e.target.checked})}/> Enabled for selected roles</label></div>
 <fieldset className="admin-feature-roles">
 <legend>Visible to user types</legend>
 {roles.map(role=><label key={role}><input type="checkbox" checked={f.allowedRoles.includes(role)} onChange={e=>change(f.code,{allowedRoles:e.target.checked?[...f.allowedRoles,role]:f.allowedRoles.filter(r=>r!==role)})}/> {role.replaceAll("_"," ")}</label>)}
 </fieldset><div className="admin-feature-actions"><button type="button" className="btn secondary" disabled={saving===f.code} onClick={()=>void setAvailability(f,!f.enabled)}>{f.enabled?"Disable feature":"Enable feature"}</button> <button type="button" className="btn primary" disabled={saving===f.code||(f.enabled&&!f.allowedRoles.length)} onClick={()=>void save(f)}>{saving===f.code?"Saving…":"Save role visibility"}</button></div>
 </section>)}</div></main>;
}
