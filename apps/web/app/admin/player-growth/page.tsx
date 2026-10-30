"use client";
import { FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { SiteHeader } from "@/components/site-header";
import { SiteFooter } from "@/components/site-footer";
import { useApp } from "@/components/app-provider";
import { api, ApiError } from "@/lib/api";

type Sport={id:string;name:string};
const privileged=new Set(["COACH","ADMIN","SUPER_ADMIN"]);
export default function GrowthCoachPage(){
 const {user,ready}=useApp();const router=useRouter();
 const [playerId,setPlayerId]=useState("");const [sportId,setSportId]=useState("");
 const [skill,setSkill]=useState("");const [score,setScore]=useState("50");
 const [remarks,setRemarks]=useState("");const [title,setTitle]=useState("");
 const [target,setTarget]=useState("75");const [due,setDue]=useState("");
 const [result,setResult]=useState("");const [busy,setBusy]=useState(false);
 useEffect(()=>{if(ready&&!user)router.replace("/login")},[ready,user,router]);
 if(!ready||!user)return <div className="portal-loading">Loading…</div>;
 if(!user.roles.some(r=>privileged.has(r)))return <div className="portal-loading">Not authorized</div>;
 async function saveAssessment(e:FormEvent){e.preventDefault();setBusy(true);setResult("");try{
  await api("/api/v1/growth/assessments",{method:"POST",body:JSON.stringify({playerId,sportId,skill,score:Number(score),remarks,assessedOn:new Date().toISOString().slice(0,10)})});
  setResult("Assessment saved.");setSkill("");setRemarks("");
 }catch(err){setResult(err instanceof ApiError?err.message:"Unable to save assessment");}finally{setBusy(false)}}
 async function saveGoal(e:FormEvent){e.preventDefault();setBusy(true);setResult("");try{
  await api("/api/v1/growth/goals",{method:"POST",body:JSON.stringify({playerId,sportId,title,targetScore:Number(target),dueOn:due||null})});
  setResult("Goal saved.");setTitle("");
 }catch(err){setResult(err instanceof ApiError?err.message:"Unable to save goal");}finally{setBusy(false)}}
 return <><SiteHeader/><main className="portal"><section className="portal-content" style={{width:"100%",maxWidth:850,margin:"0 auto"}}>
 <div className="portal-heading"><div><span className="eyebrow">Play • Train • Compete • Grow</span><h1>Player Growth — Coach</h1><p>Review a player's progress and record a sport-specific skill score or training goal. Coaches can manage only their assigned training players.</p></div></div>
 <p>Enter the player's user ID and sport ID from the administration records. Scores are private to the player and authorized coaches.</p>
 <div className="form-grid"><label>Player ID<input required value={playerId} onChange={e=>setPlayerId(e.target.value)} placeholder="Player UUID"/></label>
 <label>Sport ID<input required value={sportId} onChange={e=>setSportId(e.target.value)} placeholder="Sport UUID"/></label></div>
 {result&&<p role="status">{result}</p>}
 <h2>Skill assessment</h2><form className="form-grid" onSubmit={saveAssessment}>
 <label>Skill<input required maxLength={100} value={skill} onChange={e=>setSkill(e.target.value)} placeholder="e.g. Serve accuracy"/></label>
 <label>Coach score (0–100)<input required type="number" min="0" max="100" value={score} onChange={e=>setScore(e.target.value)}/></label>
 <label>Feedback<textarea maxLength={500} value={remarks} onChange={e=>setRemarks(e.target.value)}/></label>
 <button disabled={busy||!playerId||!sportId} type="submit">Save assessment</button></form>
 <h2>Personal training goal</h2><form className="form-grid" onSubmit={saveGoal}>
 <label>Goal<input required maxLength={140} value={title} onChange={e=>setTitle(e.target.value)} placeholder="e.g. Improve footwork"/></label>
 <label>Target score<input required type="number" min="0" max="100" value={target} onChange={e=>setTarget(e.target.value)}/></label>
 <label>Target date<input type="date" value={due} onChange={e=>setDue(e.target.value)}/></label>
 <button disabled={busy||!playerId||!sportId} type="submit">Save goal</button></form>
 </section></main><SiteFooter/></>;
}
