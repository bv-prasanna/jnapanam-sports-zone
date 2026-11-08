"use client";
import Link from "next/link";
import {useRouter} from "next/navigation";
import {FormEvent,useEffect,useState} from "react";
import {SiteHeader} from "@/components/site-header";
import {SiteFooter} from "@/components/site-footer";
import {useApp} from "@/components/app-provider";
import {ApiError,login} from "@/lib/api";

type LoginFailure="MFA_REQUIRED"|"MFA_INVALID"|"MFA_DELIVERY_FAILED"|"INVALID_CREDENTIALS";
function loginFailure(error:unknown):LoginFailure|null{
 if(!(error instanceof ApiError)||!error.details||typeof error.details!=="object")return null;
 const code=(error.details as {code?:unknown}).code;
 return code==="MFA_REQUIRED"||code==="MFA_INVALID"||code==="MFA_DELIVERY_FAILED"||code==="INVALID_CREDENTIALS"?code:null;
}

export default function Login(){
 const router=useRouter();
 const {setUser}=useApp();
 const [identifier,setIdentifier]=useState("");
 const [password,setPassword]=useState("");
 const [mfaCode,setMfaCode]=useState("");
 const [mfaRequired,setMfaRequired]=useState(false);
 const [busy,setBusy]=useState(false);
 const [error,setError]=useState("");
 const [notice,setNotice]=useState("");
 const [sessionExpired,setSessionExpired]=useState(false);
 useEffect(()=>setSessionExpired(new URLSearchParams(window.location.search).get("reason")==="session-expired"),[]);

 function resetChallenge(){
  setMfaRequired(false);
  setMfaCode("");
  setError("");
  setNotice("");
 }
 async function attempt(code:string,resending=false){
  if(busy)return;
  setBusy(true);setError("");setNotice("");setSessionExpired(false);
  try{
   setUser(await login(identifier.trim(),password,code));
   router.push("/dashboard");
  }catch(e){
   const failure=loginFailure(e);
   if(failure==="MFA_REQUIRED"){
    setMfaRequired(true);setMfaCode("");
    setNotice(resending?"A new 6-digit code was requested. Check your email and spam folder.":"A 6-digit code was requested for your registered email. Check your inbox and spam folder.");
   }else if(failure==="MFA_INVALID"){
    setMfaRequired(true);setMfaCode("");
    setError("The verification code is invalid or expired. Enter a valid code or choose Resend code.");
   }else if(failure==="MFA_DELIVERY_FAILED"){
    // Do not claim success: the SMTP provider reported a failure.
    setError(e instanceof ApiError?e.message:"Verification email delivery failed.");
    if(!resending){setMfaRequired(false);setMfaCode("");}
   }else{
    resetChallenge();
    setError(e instanceof ApiError?e.message:"Unable to sign in. Please try again.");
   }
  }finally{setBusy(false)}
 }
 async function submit(event:FormEvent<HTMLFormElement>){
  event.preventDefault();
  await attempt(mfaRequired?mfaCode.trim():"");
 }
 return <><SiteHeader/><main className="auth-page"><form className="auth-card" onSubmit={event=>void submit(event)}>
  <span className="eyebrow">Member Access</span><h1>Welcome Back</h1>
  <p>Access bookings, memberships, training and rewards. For your security, password sign-in also requires a 6-digit code sent to your registered email address.</p>
  {sessionExpired&&<div className="form-error" role="alert">Your session has expired. Please sign in again.</div>}
  {error&&<div className="form-error" role="alert">{error}</div>}
  {notice&&<div className="form-note" role="status">{notice}</div>}
  <label>Email or mobile<input name="email" value={identifier} onChange={e=>{setIdentifier(e.target.value);resetChallenge()}} required autoComplete="username" placeholder="Email or 10-digit mobile"/></label>
  <label>Password<input name="password" value={password} onChange={e=>{setPassword(e.target.value);resetChallenge()}} required type="password" autoComplete="current-password"/></label>
  {mfaRequired&&<label>Email verification code<input name="mfaCode" value={mfaCode} onChange={e=>{setMfaCode(e.target.value.replace(/\D/g,"").slice(0,6));setError("")}} required inputMode="numeric" autoComplete="one-time-code" minLength={6} maxLength={6} pattern="[0-9]{6}" placeholder="6-digit code"/></label>}
  {mfaRequired&&<button className="btn outline" type="button" disabled={busy} onClick={()=>void attempt("",true)}>Resend code</button>}
  <button disabled={busy} className="btn primary large" type="submit">{busy?"Signing in…":mfaRequired?"Verify & Login":"Login"}</button>
  <div className="auth-links"><Link href="/forgot-password">Forgot password?</Link><span>New to Jnapanam? <Link href="/register">Register now</Link></span></div>
 </form></main><SiteFooter/></>;
}
