"use client";

import { useMemo, useState } from "react";
import {
  CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from "recharts";
import styles from "./my-growth-dashboard.module.css";

type Assessment={id:string;sport_name:string;skill:string;score:number;remarks?:string|null;assessed_on:string};
type Goal={id:string;sport_name:string;title:string;target_score:number|null;due_on:string|null;status:string};
type Attendance={sport_name:string;sessions:number;attended:number};
type Competition={tournament_name:string;team_name:string;round_number:number;outcome:string};
type GrowthData={assessments?:Assessment[];goals?:Goal[];attendance?:Attendance[];competitions?:Competition[]};
const score=(n:number)=>Math.min(100,Math.max(0,Number.isFinite(Number(n))?Number(n):0));
const count=(n:number)=>Math.max(0,Number.isFinite(Number(n))?Number(n):0));
const dateLabel=(value:string|null|undefined)=>{if(!value)return "—";const d=new Date(value.slice(0,10)+"T12:00:00Z");return Number.isNaN(d.getTime())?value:new Intl.DateTimeFormat("en-IN",{day:"2-digit",month:"short",year:"numeric",timeZone:"UTC"}).format(d);};
const readable=(value:string)=>value.replaceAll("_"," ").toLowerCase().replace(/^./,m=>m.toUpperCase());

function EmptyPanel({title,description}:{title:string;description:string}){
  return <div className={styles.empty}><div className={styles.emptyIcon} aria-hidden="true">↗</div><strong>{title}</strong><p>{description}</p></div>;
}

export function MyGrowthDashboard({data}:{data:unknown}){
  const result=(data&&typeof data==="object"?data:{} ) as GrowthData;
  const assessments=Array.isArray(result.assessments)?result.assessments:[];
  const goals=Array.isArray(result.goals)?result.goals:[];
  const attendance=Array.isArray(result.attendance)?result.attendance:[];
  const competitions=Array.isArray(result.competitions)?result.competitions:[];
  const [selectedSkill,setSelectedSkill]=useState("");
  const [search,setSearch]=useState("");
  const [goalStatus,setGoalStatus]=useState("ALL");

  const skillOptions=useMemo(()=>Array.from(new Map(assessments.map(a=>[a.sport_name+" · "+a.skill,a.sport_name+" · "+a.skill])).values()).sort(),[assessments]);
  const activeSkill=skillOptions.includes(selectedSkill)?selectedSkill:(skillOptions[0]||"");
  const timeline=useMemo(()=>assessments.filter(a=>a.sport_name+" · "+a.skill===activeSkill)
    .slice().sort((a,b)=>a.assessed_on.localeCompare(b.assessed_on)).slice(-12)
    .map((a,i)=>({id:a.id+"-"+i,date:dateLabel(a.assessed_on),score:score(a.score)})),[assessments,activeSkill]);
  const visibleAssessments=useMemo(()=>assessments.filter(a=>(a.sport_name+" "+a.skill+" "+(a.remarks||"")).toLowerCase().includes(search.toLowerCase()))
    .slice().sort((a,b)=>b.assessed_on.localeCompare(a.assessed_on)),[assessments,search]);
  const visibleGoals=goals.filter(g=>goalStatus==="ALL"||g.status===goalStatus);
  const attendanceTotal=attendance.reduce((n,row)=>n+count(row.sessions),0);
  const attendedTotal=attendance.reduce((n,row)=>n+Math.min(count(row.attended),count(row.sessions)),0);
  const attendancePercentage=attendanceTotal?Math.round(attendedTotal/attendanceTotal*100):null;
  const activeGoals=goals.filter(g=>g.status==="ACTIVE").length;
  const achievedGoals=goals.filter(g=>g.status==="ACHIEVED").length;
  const latestAssessment=assessments.slice().sort((a,b)=>b.assessed_on.localeCompare(a.assessed_on))[0];
  const latestScore=latestAssessment?Math.round(score(latestAssessment.score)):null;

  return <div className={styles.root}>
    <header className={styles.header}>
      <div><span className={styles.eyebrow}>PLAY · TRAIN · COMPETE · GROW</span>
        <h1>My Growth</h1><p>Your personal sports development, coaching feedback and milestones in one place.</p>
      </div>
      <span className={styles.privateTag}>Private to you and authorized staff</span>
    </header>

    <div className={styles.kpis}>
      <article className={styles.kpi}><span className={styles.kpiIcon} aria-hidden="true">◎</span><span>Skill assessments</span><strong>{assessments.length}</strong><small>Coach-reviewed evaluations</small></article>
      <article className={styles.kpi}><span className={styles.kpiIcon} aria-hidden="true">↗</span><span>Latest skill score</span><strong>{latestScore===null?"—":latestScore+"/100"}</strong><small>{latestAssessment?latestAssessment.skill:"No assessment yet"}</small></article>
      <article className={styles.kpi}><span className={styles.kpiIcon} aria-hidden="true">◉</span><span>Active training goals</span><strong>{activeGoals}</strong><small>Goals in progress</small></article>
      <article className={styles.kpi}><span className={styles.kpiIcon} aria-hidden="true">★</span><span>Milestones achieved</span><strong>{achievedGoals}</strong><small>Goals completed</small></article>
    </div>

    <div className={styles.twoColumn}>
      <section className={styles.card} aria-labelledby="growth-trend-heading">
        <div className={styles.cardHead}><div><span className={styles.overline}>PERFORMANCE</span><h2 id="growth-trend-heading">Skill development</h2><p>Coach scores over time · out of 100</p></div>
          {skillOptions.length>0&&<label className={styles.selectLabel}>Sport & skill<select aria-label="Choose a skill to chart" value={activeSkill} onChange={e=>setSelectedSkill(e.target.value)}>{skillOptions.map(value=><option key={value} value={value}>{value}</option>)}</select></label>}
        </div>
        {timeline.length===0?<EmptyPanel title="Skill chart awaiting data" description="Once a coach records an assessment, the score trend will appear here. We do not display sample scores."/>:
        <div className={styles.chartArea} role="img" aria-label={"Chart of "+activeSkill+" coach scores from zero to 100"}>
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={timeline} margin={{top:12,right:18,bottom:8,left:-20}}>
              <CartesianGrid stroke="#e7edf0" strokeDasharray="4 5" vertical={false}/>
              <XAxis dataKey="date" tick={{fill:"#647480",fontSize:11}} tickLine={false} axisLine={false} minTickGap={12}/>
              <YAxis domain={[0,100]} ticks={[0,25,50,75,100]} tick={{fill:"#647480",fontSize:11}} tickLine={false} axisLine={false}/>
              <Tooltip contentStyle={{border:"1px solid #dce6eb",borderRadius:12,boxShadow:"0 8px 24px #102a3a12"}} formatter={(value)=>[String(value)+"/100","Coach score"]}/>
              <Line type="monotone" dataKey="score" stroke="#0877b9" strokeWidth={3} dot={{r:5,fill:"#f47a20",stroke:"#fff",strokeWidth:2}} activeDot={{r:7}} connectNulls={false} isAnimationActive={false}/>
            </LineChart>
          </ResponsiveContainer>
        </div>}
      </section>

      <section className={styles.card} aria-labelledby="attendance-heading">
        <div className={styles.cardHead}><div><span className={styles.overline}>CONSISTENCY</span><h2 id="attendance-heading">Training attendance</h2><p>Sessions attended by sport</p></div></div>
        <div className={styles.attendanceTop}>
          <strong>{attendancePercentage===null?"—":attendancePercentage+"%"}</strong><div><b>Overall attendance</b><span>{attendedTotal} of {attendanceTotal} recorded sessions</span></div>
        </div>
        {attendance.length===0?<EmptyPanel title="No attendance recorded" description="Attendance bars will update after training sessions are marked. No artificial percentages are shown."/>:
        <div className={styles.barList}>{attendance.map((row,i)=>{const sessions=count(row.sessions);const attended=Math.min(count(row.attended),sessions);const pct=sessions?Math.round(attended/sessions*100):0;
          return <div className={styles.barItem} key={row.sport_name+"-"+i}><div><b>{row.sport_name}</b><span>{attended}/{sessions} · {pct}%</span></div><div className={styles.track} role="progressbar" aria-label={row.sport_name+" attendance"} aria-valuenow={pct} aria-valuemin={0} aria-valuemax={100}><span style={{width:pct+"%"}}/></div></div>;
        })}</div>}
      </section>
    </div>

    <section className={styles.card} aria-labelledby="assessment-table-heading">
      <div className={styles.cardHead}><div><span className={styles.overline}>COACH FEEDBACK</span><h2 id="assessment-table-heading">Skill assessment history</h2><p>Search and review individual assessments.</p></div>
        {assessments.length>0&&<input className={styles.search} type="search" aria-label="Search skill assessments" placeholder="Search sport, skill or feedback" value={search} onChange={e=>setSearch(e.target.value)}/>}
      </div>
      <div className={styles.tableWrap}><table className={styles.table}><thead><tr><th>Date</th><th>Sport / Skill</th><th>Score</th><th>Coach feedback</th></tr></thead><tbody>
        {visibleAssessments.map(a=><tr key={a.id}><td>{dateLabel(a.assessed_on)}</td><td><b>{a.sport_name}</b><span>{a.skill}</span></td><td><div className={styles.scoreCell}><b>{Math.round(score(a.score))}/100</b><div className={styles.miniTrack}><span style={{width:score(a.score)+"%"}}/></div></div></td><td className={styles.notes}>{a.remarks||"No notes recorded"}</td></tr>)}
      </tbody></table></div>
      {visibleAssessments.length===0&&<EmptyPanel title={search?"No matching assessments":"No skill assessments yet"} description={search?"Try another search term.":"Your coach can add the first assessment."}/>}
    </section>

    <section className={styles.card} aria-labelledby="goals-heading">
      <div className={styles.cardHead}><div><span className={styles.overline}>MILESTONES</span><h2 id="goals-heading">Training goals</h2><p>Follow your targets and completion status.</p></div>
        {goals.length>0&&<label className={styles.selectLabel}>Status<select aria-label="Filter training goals by status" value={goalStatus} onChange={e=>setGoalStatus(e.target.value)}><option value="ALL">All goals</option><option value="ACTIVE">Active</option><option value="ACHIEVED">Achieved</option><option value="CANCELLED">Cancelled</option></select></label>}
      </div>
      <div className={styles.tableWrap}><table className={styles.table}><thead><tr><th>Goal</th><th>Sport</th><th>Target score</th><th>Due date</th><th>Status</th></tr></thead><tbody>
        {visibleGoals.map(g=><tr key={g.id}><td><b>{g.title}</b></td><td>{g.sport_name}</td><td>{g.target_score==null?"—":Math.round(score(g.target_score))+"/100"}</td><td>{dateLabel(g.due_on)}</td><td><span className={[styles.pill,g.status==="ACHIEVED"?styles.achieved:g.status==="ACTIVE"?styles.active:styles.neutral].join(" ")}>{readable(g.status)}</span></td></tr>)}
      </tbody></table></div>
      {visibleGoals.length===0&&<EmptyPanel title={goals.length?"No goals with that status":"No training goals yet"} description="Goals assigned by authorized staff will appear here."/>}
    </section>

    <section className={styles.card} aria-labelledby="matches-heading">
      <div className={styles.cardHead}><div><span className={styles.overline}>COMPETITIONS</span><h2 id="matches-heading">Tournament results</h2><p>Matches from teams where you're registered as captain.</p></div></div>
      <div className={styles.tableWrap}><table className={styles.table}><thead><tr><th>Tournament</th><th>Team</th><th>Round</th><th>Outcome</th></tr></thead><tbody>
        {competitions.map((c,i)=><tr key={c.tournament_name+"-"+c.team_name+"-"+i}><td><b>{c.tournament_name}</b></td><td>{c.team_name}</td><td>{c.round_number}</td><td><span className={[styles.pill,c.outcome==="WIN"?styles.achieved:c.outcome==="LOSS"?styles.loss:styles.neutral].join(" ")}>{readable(c.outcome)}</span></td></tr>)}
      </tbody></table></div>
      {competitions.length===0&&<EmptyPanel title="No tournament results yet" description="Captain match results will appear when fixtures are recorded."/>}
    </section>
    <p className={styles.privacy}>Growth records are private to you and authorized coaching/administrative staff. Data is fetched live from your account; no demo records are substituted.</p>
  </div>;
}
