"use client";
import {useCallback,useEffect,useMemo,useState} from "react";
import {api,ApiError} from "@/lib/api";
import styles from "./notification-providers.module.css";
const groups=[
 {name:"Email (SMTP)",help:"Mail server details for registration and booking notifications.",fields:[["SMTP_HOST","SMTP host","text"],["SMTP_PORT","SMTP port","number"],["SMTP_USERNAME","SMTP username","text"],["SMTP_PASSWORD","SMTP password / app password","password"],["SMTP_FROM","Sender email address","email"]]},
 {name:"SMS · Fast2SMS",help:"Configure only if you plan to send SMS; email-only OTP can continue without SMS credentials.",fields:[["FAST2SMS_API_KEY","API key","password"],["FAST2SMS_SENDER_ID","Sender ID","text"],["FAST2SMS_OTP_ID","OTP template ID","text"]]},
 {name:"WhatsApp",help:"Connect a configured WhatsApp provider. Messages will not send until valid provider credentials are saved.",fields:[["WHATSAPP_PROVIDER_URL","Provider URL","url"],["WHATSAPP_PROVIDER_TOKEN","Provider access token","password"]]}
] as const;
type Setting={configured:boolean;value?:string};
export default function NotificationProviders(){
 const [data,setData]=useState<Record<string,Setting>>({}),[form,setForm]=useState<Record<string,string>>({});
 const [message,setMessage]=useState(""),[error,setError]=useState(""),[busy,setBusy]=useState(false),[loading,setLoading]=useState(true),[testing,setTesting]=useState("");
 const total=groups.reduce((n,g)=>n+g.fields.length,0);
 const configured=useMemo(()=>groups.reduce((n,g)=>n+g.fields.filter(([k])=>data[k]?.configured).length,0),[data]);
 const load=useCallback(async()=>{setLoading(true);try{setData(await api<Record<string,Setting>>("/api/v1/admin/notification-providers"));setError("")}catch(e){setError(e instanceof ApiError?e.message:"Unable to load providers.")}finally{setLoading(false)}},[]);
 useEffect(()=>{void load()},[load]);
 async function save(){const changes=Object.fromEntries(Object.entries(form).filter(([_,v])=>v.trim().length>0));
   if(!Object.keys(changes).length){setError("Enter at least one setting to update.");return;}
   setBusy(true);setError("");setMessage("");
   try{await api("/api/v1/admin/notification-providers",{method:"PUT",body:JSON.stringify(changes)});setForm({});await load();setMessage("Provider settings saved. Secret values remain hidden.")}
   catch(e){setError(e instanceof ApiError?e.message:"Unable to save settings.");}finally{setBusy(false)}
 }
 async function test(channel:string){setTesting(channel);setError("");setMessage("");
   try{const result=await api<{provider:string}>("/api/v1/admin/notification-providers/test/"+channel,{method:"POST"});setMessage("Test "+channel+" dispatched via "+result.provider+". Check delivery.");}
   catch(e){setError(e instanceof ApiError?e.message:"Unable to test "+channel+".");}finally{setTesting("")}
 }
 return <main className={styles.page}>
   <header className={styles.header}><span className={styles.eyebrow}>ADMIN · INTEGRATIONS</span><h1>Notification Providers</h1><p>Configure secure delivery settings for email, SMS and WhatsApp. Saved secrets are encrypted; configured secret values are never displayed.</p></header>
   <div className={styles.summary}><article><strong>{loading?"—":configured+"/"+total}</strong><span>Settings configured</span></article><article><strong>3</strong><span>Notification channels</span></article><article><strong>Protected</strong><span>Super Admin access</span></article></div>
   {error&&<p role="alert" className={styles.error}>{error}</p>}{message&&<p role="status" className={styles.success}>{message}</p>}
   {loading?<p className={styles.info}>Loading current configuration…</p>:groups.map(group=><section className={styles.card} key={group.name}>
     <div className={styles.cardHeading}><div><h2>{group.name}</h2><p>{group.help}</p></div><span className={styles.count}>{group.fields.filter(([key])=>data[key]?.configured).length}/{group.fields.length} configured</span></div>
     <div className={styles.grid}>{group.fields.map(([key,label,type])=><label key={key} className={styles.field}><span>{label}</span>
       <input type={type} autoComplete="off" value={form[key]||""} onChange={e=>setForm(x=>({...x,[key]:e.target.value}))}
         placeholder={data[key]?.configured?(type==="password"?"Leave blank to keep existing secret":data[key]?.value||"Configured"):"Not configured"}/>
       <small className={data[key]?.configured?styles.configured:styles.missing}>{data[key]?.configured?"Configured · leave blank to keep":"Not configured"}</small>
     </label>)}</div></section>)}
   <div className={styles.actions}><button onClick={()=>void save()} disabled={loading||busy||Object.values(form).every(x=>!x.trim())}>{busy?"Saving…":"Save configuration"}</button>
     <div>{["email","sms","whatsapp"].map(channel=><button key={channel} className={styles.secondary} disabled={busy||!!testing} onClick={()=>void test(channel)}>{testing===channel?"Sending…":"Test "+channel.toUpperCase()}</button>)}</div>
   </div>
   <p className={styles.info}>Environment variables are used only where no saved setting overrides them. Test messages use the signed-in Super Admin's contact details. Sending a test does not guarantee delivery.</p>
 </main>;
}
