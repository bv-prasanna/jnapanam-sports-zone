"use client";
import {useEffect,useState} from "react";
import {api,apiDocument} from "@/lib/api";
type DocumentRow={id:string;userId:string;customer:string;email:string;type:string;status:string;uploadedAt:string};
export default function Verification(){
 const [rows,setRows]=useState<DocumentRow[]>([]);
 const [filter,setFilter]=useState("PENDING");
 const [pending,setPending]=useState(0);
 const [error,setError]=useState("");
 const [busy,setBusy]=useState("");
 const [section,setSection]=useState("documents");
 async function load(){
  try{
   const [docs,summary]=await Promise.all([
    api<DocumentRow[]>(`/api/v1/admin/verification/documents?status=${filter}`),
    api<{pendingDocuments:number}>("/api/v1/admin/verification/summary")
   ]);
   setRows(docs);setPending(summary.pendingDocuments);setError("");
  }catch(e){setError(e instanceof Error?e.message:"Unable to load documents");}
 }
 useEffect(()=>{void load()},[filter]);
 async function review(id:string,status:"APPROVED"|"REJECTED"){
  let note="";
  if(status==="REJECTED"){const value=window.prompt("Enter the rejection reason");if(!value||value.trim().length<3)return;note=value.trim();}
  try{setBusy(id);await api(`/api/v1/admin/verification/documents/${id}`,{method:"PATCH",body:JSON.stringify({status,note})});await load();}
  catch(e){setError(e instanceof Error?e.message:"Unable to update document");}finally{setBusy("");}
 }
 async function view(id:string){
  try{const blob=await apiDocument(`/api/v1/admin/verification/documents/${id}/file`);
   const url=URL.createObjectURL(blob);const w=window.open(url,"_blank","noopener,noreferrer");
   if(!w)setError("Allow popups to preview documents.");window.setTimeout(()=>URL.revokeObjectURL(url),60000);
  }catch(e){setError(e instanceof Error?e.message:"Unable to view document");}
 }
 return <main style={{padding:28,maxWidth:1300,margin:"auto"}}><span className="eyebrow">ADMIN · VERIFICATION</span><h1>Verification</h1><p>Review customer-submitted documents and verification requests.</p>
 <div style={{display:"flex",gap:12,flexWrap:"wrap",margin:"18px 0"}}>
 <button className="btn outline" onClick={()=>setSection("documents")}>Documents ({pending} pending)</button>
 <button className="btn outline" onClick={()=>setSection("users")}>User verification</button></div>
 {section==="users"?<p>User verification workflows will be added separately. Customer identity documents can be reviewed under Documents.</p>:<>
 <div style={{display:"flex",gap:12,alignItems:"center",marginBottom:20}}><strong>Pending reviews: {pending}</strong><label>Show <select value={filter} onChange={e=>setFilter(e.target.value)}>{["PENDING","APPROVED","REJECTED","ALL"].map(s=><option key={s} value={s}>{s}</option>)}</select></label><button className="btn outline" onClick={()=>void load()}>Refresh</button></div>
 {error&&<div className="form-error" role="alert">{error}</div>}
 <div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Customer</th><th>Email</th><th>Document</th><th>Uploaded</th><th>Status</th><th>Actions</th></tr></thead><tbody>{rows.map(d=><tr key={d.id}><td>{d.customer}</td><td>{d.email}</td><td>{d.type}</td><td>{new Date(d.uploadedAt).toLocaleString()}</td><td>{d.status}</td><td><button className="btn outline" onClick={()=>void view(d.id)}>View</button> {d.status==="PENDING"&&<><button className="btn outline" disabled={busy===d.id} onClick={()=>void review(d.id,"APPROVED")}>Approve</button> <button className="btn outline" disabled={busy===d.id} onClick={()=>void review(d.id,"REJECTED")}>Reject</button></>}</td></tr>)}</tbody></table>{rows.length===0&&<p className="empty-state">No documents match this filter.</p>}</div>
 </>}</main>;
}
