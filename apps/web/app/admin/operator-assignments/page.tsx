"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { api, ApiError } from "@/lib/api";
import styles from "./operator-assignments.module.css";

type Row=Record<string,unknown>;
type User={id:string;fullName:string;email:string;mobile?:string;roles:string[];status:string};
type Facility={id:string;code:string;name:string;sportName:string;status:string};

export default function OperatorAssignmentsPage(){
 const [operators,setOperators]=useState<User[]>([]);const [facilities,setFacilities]=useState<Facility[]>([]);const [operatorId,setOperatorId]=useState("");const [selected,setSelected]=useState<string[]>([]);const [loading,setLoading]=useState(true);const [saving,setSaving]=useState(false);const [error,setError]=useState("");const [notice,setNotice]=useState("");
 useEffect(()=>{Promise.all([api<Row>("/api/v1/admin/users?page=0&size=200"),api<Facility[]>("/api/v1/admin/catalog/facilities")]).then(([users,f])=>{const list=Array.isArray(users)?users:(Array.isArray(users.content)?users.content as unknown as User[]:[]);setOperators((list as User[]).filter(u=>u.status==="ACTIVE"&&u.roles.includes("OPERATIONS")));setFacilities(f.filter(x=>x.status==="ACTIVE"))}).catch(e=>setError(e instanceof ApiError?e.message:"Unable to load assignments.")).finally(()=>setLoading(false))},[]);
 useEffect(()=>{if(!operatorId){setSelected([]);return}api<Row[]>(`/api/v1/admin/operator-assignments/${operatorId}`).then(rows=>setSelected(rows.map(r=>String(r.facilityId)))).catch(e=>setError(e instanceof ApiError?e.message:"Unable to load operator assignments."))},[operatorId]);
 const grouped=useMemo(()=>{const map=new Map<string,Facility[]>();for(const f of facilities){const group=map.get(f.sportName)||[];group.push(f);map.set(f.sportName,group)}return [...map.entries()]},[facilities]);
 function toggle(id:string){setSelected(values=>values.includes(id)?values.filter(x=>x!==id):[...values,id])}
 async function save(){if(!operatorId)return;try{setSaving(true);setError("");setNotice("");await api(`/api/v1/admin/operator-assignments/${operatorId}`,{method:"PUT",body:JSON.stringify({facilityIds:selected})});setNotice("Operator assignments saved. Their operations queue is now restricted to these facilities.")}catch(e){setError(e instanceof ApiError?e.message:"Unable to save assignments.")}finally{setSaving(false)}}
 return <main className={styles.page}><header className={styles.header}><div><span>ADMIN · OPERATIONS</span><h1>Operator Assignments</h1><p>Control exactly which sports facilities each Operator can view and operate.</p></div><div><Link href="/admin/bookings">Booking Explorer</Link><Link href="/admin">Admin Console</Link></div></header>{error&&<div className={styles.error}>{error}</div>}{notice&&<div className={styles.success}>{notice}</div>}{loading?<p>Loading…</p>:<section className={styles.card}><label>Operator<select value={operatorId} onChange={e=>{setOperatorId(e.target.value);setNotice("")}}><option value="">Choose an operator</option>{operators.map(o=><option key={o.id} value={o.id}>{o.fullName} · {o.email}</option>)}</select></label>{operatorId&&<><div className={styles.summary}><strong>{selected.length}</strong><span>facilities assigned</span><button type="button" onClick={()=>setSelected(facilities.map(f=>f.id))}>Select all active</button><button type="button" onClick={()=>setSelected([])}>Clear</button></div><div className={styles.groups}>{grouped.map(([sport,items])=><fieldset key={sport}><legend>{sport}</legend>{items.map(f=><label key={f.id}><input type="checkbox" checked={selected.includes(f.id)} onChange={()=>toggle(f.id)}/><span><b>{f.name}</b><small>{f.code}</small></span></label>)}</fieldset>)}</div><button className={styles.save} onClick={()=>void save()} disabled={saving}>{saving?"Saving…":"Save Assignments"}</button></>}</section>}</main>;
}
