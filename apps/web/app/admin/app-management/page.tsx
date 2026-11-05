"use client";
import {FormEvent,useCallback,useEffect,useState} from "react";
import {api,ApiError,API_BASE_URL} from "@/lib/api";
import styles from "./app-management.module.css";

type Policy={latestVersionCode:number;latestVersionName:string;minimumSupportedVersionCode:number;
  updateMode:"OFF"|"OPTIONAL"|"MANDATORY";apkUrl:string|null;releaseNotes:string|null;downloadPageUrl:string;updatedAt:string};
const fixedLink="https://jnapanam.in/app";
export default function AppManagement(){
 const [policy,setPolicy]=useState<Policy|null>(null);
 const [busy,setBusy]=useState(false);const [loading,setLoading]=useState(true);
 const [message,setMessage]=useState("");const [error,setError]=useState("");
 const load=useCallback(async()=>{setLoading(true);
   try{setPolicy(await api<Policy>("/api/v1/admin/app-management"));setError("");}
   catch(e){setError(e instanceof ApiError?e.message:"Could not load Android app settings.");}
   finally{setLoading(false);}
 },[]);
 useEffect(()=>{void load();},[load]);
 function edit<K extends keyof Policy>(key:K,value:Policy[K]){
   setPolicy(p=>p?{...p,[key]:value}:p);setMessage("");setError("");
 }
 async function save(event:FormEvent<HTMLFormElement>){
   event.preventDefault();if(!policy||busy)return;
   if(policy.minimumSupportedVersionCode>policy.latestVersionCode){setError("Minimum supported build cannot exceed the latest build.");return;}
   if(policy.updateMode!=="OFF"&&!policy.apkUrl?.trim()){setError("Publish the APK URL before enabling updates.");return;}
   if(policy.updateMode==="MANDATORY"&&!window.confirm(
     "MANDATORY updates can block customer bookings, payment and check-in from older Android builds. Confirm the new APK is publicly downloadable, installs correctly and uses a compatible signing certificate. Enable mandatory updates now?"
   ))return;
   setBusy(true);setError("");setMessage("");
   try{const updated=await api<Policy>("/api/v1/admin/app-management",{method:"PUT",body:JSON.stringify({
     latestVersionCode:policy.latestVersionCode,
     latestVersionName:policy.latestVersionName.trim(),
     minimumSupportedVersionCode:policy.minimumSupportedVersionCode,
     updateMode:policy.updateMode,
     apkUrl:policy.apkUrl?.trim()||null,
     releaseNotes:policy.releaseNotes?.trim()||null
   })});setPolicy(updated);setMessage("App release configuration saved. New version checks use this policy.");}
   catch(e){setError(e instanceof ApiError?e.message:"Unable to save APK release policy.");}
   finally{setBusy(false);}
 }
 return <main className={styles.page}>
  <header className={styles.heading}><span>ADMIN · SYSTEM</span><h1>App Management</h1>
   <p>Manage the Android APK link and version requirements without changing the website or reprinting QR codes.</p></header>
  {loading?<p role="status">Loading current Android release settings…</p>:policy&&<div className={styles.layout}>
   <form onSubmit={event=>void save(event)} className={styles.card}>
    <h2>Android APK release</h2>
    <div className={styles.fields}>
     <label>Latest version name<input required maxLength={40} placeholder="1.1.0" value={policy.latestVersionName} onChange={e=>edit("latestVersionName",e.target.value)}/></label>
     <label>Latest build number (versionCode)<input required min={1} max={999999999} type="number" value={policy.latestVersionCode} onChange={e=>edit("latestVersionCode",Number(e.target.value))}/></label>
     <label>Minimum supported build<input required min={1} max={999999999} type="number" value={policy.minimumSupportedVersionCode} onChange={e=>edit("minimumSupportedVersionCode",Number(e.target.value))}/></label>
     <label>Update requirement<select value={policy.updateMode} onChange={e=>edit("updateMode",e.target.value as Policy["updateMode"])}>
      <option value="OFF">Off — publish/download only</option>
      <option value="OPTIONAL">Optional — users may update later</option>
      <option value="MANDATORY">Mandatory — block only builds below minimum</option>
     </select></label>
     <label className={styles.full}>Public APK link (Google Drive, GitHub release, Expo or trusted Jnapanam/R2 host)
      <input type="url" placeholder="https://drive.google.com/file/d/.../view" value={policy.apkUrl||""} onChange={e=>edit("apkUrl",e.target.value)} />
     </label>
     <label className={styles.full}>Release notes<textarea rows={4} maxLength={1500} value={policy.releaseNotes||""} onChange={e=>edit("releaseNotes",e.target.value)} placeholder="What changed in this app release?"/></label>
    </div>
    <p className={styles.hint}>Publish the APK and test it on Android before setting Mandatory. Build numbers must match the signed APK (not just the filename). Changing a Google Drive URL is enough; QR artwork stays unchanged.</p>
    <div className={styles.modeExplanation}>
      {policy.updateMode==="OFF"?"Off: link may be shown, but no app version is blocked.":
       policy.updateMode==="OPTIONAL"?"Optional: Android users on lower builds see Update / Later; access continues.":
       "Mandatory: Android builds below the minimum are blocked and directed to the permanent download page. Builds at or above the minimum continue to work."}
    </div>
    {error&&<p role="alert" className={styles.error}>{error}</p>}
    {message&&<p role="status" className={styles.success}>{message}</p>}
    <button className={styles.primary} disabled={busy}>{busy?"Saving…":"Save app settings"}</button>
   </form>
   <aside className={styles.card}>
    <h2>Permanent download link</h2>
    <a className={styles.stableLink} href={fixedLink} target="_blank" rel="noreferrer">{fixedLink}</a>
    <p className={styles.hint}>Share this link and the QR below with all customers. It always resolves through the latest admin-managed APK URL.</p>
    <img className={styles.qr} src="/app-download-qr.svg" alt="QR code for https://jnapanam.in/app"/>
    <div className={styles.links}>
      <a href="/app-download-qr.svg" download="jnapanam-android-qr.svg">Download printable QR</a>
      <a href={fixedLink} target="_blank" rel="noreferrer">Open public app page</a>
      {policy.apkUrl&&<a href={API_BASE_URL+"/api/v1/public/app-download/android"} target="_blank" rel="noreferrer">Test configured APK destination</a>}
    </div>
    <p className={styles.hint}>Updated {new Date(policy.updatedAt).toLocaleString("en-IN")}. The old QR linking directly to Drive cannot be changed; distribute this new permanent QR.</p>
   </aside>
  </div>}
 </main>;
}
