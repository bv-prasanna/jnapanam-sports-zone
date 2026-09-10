"use client";

import { ChangeEvent, FormEvent, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { api, ApiError, User } from "@/lib/api";
import { useApp } from "@/components/app-provider";
import styles from "./profile.module.css";

export default function ProfilePage(){
  const router=useRouter();
  const {user,ready,setUser}=useApp();
  const [displayName,setDisplayName]=useState("");
  const [photoUrl,setPhotoUrl]=useState("");
  const [saving,setSaving]=useState(false);
  const [uploading,setUploading]=useState(false);
  const [message,setMessage]=useState("");
  const [error,setError]=useState("");

  useEffect(()=>{if(ready&&!user)router.replace("/login")},[ready,user,router]);
  useEffect(()=>{if(user){setDisplayName(user.displayName||"");setPhotoUrl(user.profilePhotoUrl||"")}},[user]);

  async function save(event:FormEvent){
    event.preventDefault();
    try{setSaving(true);setError("");setMessage("");const updated=await api<User>("/api/v1/profile/me",{method:"PUT",body:JSON.stringify({displayName:displayName.trim()||null,profilePhotoUrl:photoUrl.trim()||null})});setUser(updated);setPhotoUrl(updated.profilePhotoUrl||"");setMessage("Profile updated successfully.")}
    catch(value){setError(value instanceof ApiError?value.message:"Unable to update profile.")}
    finally{setSaving(false)}
  }

  async function upload(event:ChangeEvent<HTMLInputElement>){
    const file=event.target.files?.[0];if(!file)return;
    if(!["image/jpeg","image/png","image/webp"].includes(file.type)){setError("Choose a JPG, PNG or WebP image.");return}
    if(file.size>5*1024*1024){setError("Profile photo must be 5 MB or smaller.");return}
    const form=new FormData();form.append("file",file);
    try{setUploading(true);setError("");setMessage("");const updated=await api<User>("/api/v1/profile/photo",{method:"POST",body:form});setUser(updated);setPhotoUrl(updated.profilePhotoUrl||"");setMessage("Profile photo uploaded successfully.")}
    catch(value){setError(value instanceof ApiError?value.message:"Unable to upload profile photo.")}
    finally{setUploading(false);event.target.value=""}
  }

  if(!ready||!user)return <main className={styles.page}>Loading profile…</main>;
  const initials=(user.displayName||user.fullName).split(/\s+/).map(x=>x[0]).join("").slice(0,2).toUpperCase();
  return <main className={styles.page}>
    <header className={styles.header}><div><span>ACCOUNT</span><h1>My Profile</h1><p>Manage how your account appears across Jnapanam Sports Zone.</p></div><Link href="/dashboard?section=profile">← Dashboard</Link></header>
    <section className={styles.card}>
      <div className={styles.photoPanel}>
        {photoUrl?<img src={photoUrl} alt="Profile" className={styles.photo}/>:<div className={styles.initials}>{initials}</div>}
        <div><h2>{user.displayName||user.fullName}</h2><p>{user.email}</p><label className={styles.upload}>{uploading?"Uploading…":"Upload photo"}<input type="file" accept="image/jpeg,image/png,image/webp" onChange={upload} disabled={uploading}/></label><small>JPG, PNG or WebP · maximum 5 MB</small></div>
      </div>
      <form className={styles.form} onSubmit={save}>
        <div className={styles.grid}><label>Full name<input value={user.fullName} disabled/></label><label>Display / calling name<input value={displayName} maxLength={60} onChange={e=>setDisplayName(e.target.value)} placeholder={user.fullName}/></label><label>Email<input value={user.email} disabled/></label><label>Mobile<input value={user.mobile||"Not provided"} disabled/></label></div>
        <label>Profile photo URL<input type="url" maxLength={500} value={photoUrl} onChange={e=>setPhotoUrl(e.target.value)} placeholder="Upload a photo above or enter an HTTPS image URL"/></label>
        <div className={styles.roles}><small>Roles</small><div>{user.roles.map(role=><span key={role}>{role.replaceAll("_"," ")}</span>)}</div></div>
        {error&&<div className={styles.error}>{error}</div>}{message&&<div className={styles.success}>{message}</div>}
        <div className={styles.actions}><button type="submit" disabled={saving||uploading}>{saving?"Saving…":"Save Profile"}</button><Link href="/dashboard">Cancel</Link></div>
      </form>
    </section>
  </main>;
}
