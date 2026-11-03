"use client";
import Link from "next/link";
import {useRouter} from "next/navigation";
import {FormEvent,useState} from "react";
import {SiteHeader} from "@/components/site-header";
import {SiteFooter} from "@/components/site-footer";
import {useApp} from "@/components/app-provider";
import {ApiError,requestEmailRegistrationOtp,registerWithEmailOtp} from "@/lib/api";

type RegistrationDetails={fullName:string;email:string;mobile:string;password:string};

export default function Register(){
 const router=useRouter(),{setUser}=useApp();
 const [busy,setBusy]=useState(false),[error,setError]=useState("");
 const [otpSent,setOtpSent]=useState(false);
 // Store the exact recipient from the successful OTP request, not an editable email field.
 const [saved,setSaved]=useState<RegistrationDetails|null>(null);
 async function submit(e:FormEvent<HTMLFormElement>){
  e.preventDefault();setBusy(true);setError("");
  const form=new FormData(e.currentTarget);
  try{
   if(!otpSent){
    const payload:RegistrationDetails={
     fullName:String(form.get("fullName")||"").trim(),
     email:String(form.get("email")||"").trim().toLowerCase(),
     mobile:String(form.get("mobile")||"").trim(),
     password:String(form.get("password")||"")
    };
    if(!/^[0-9+ -]{10,20}$/.test(payload.mobile) || payload.mobile.replace(/[^0-9]/g,"").length<10){
     setError("Enter a valid mobile number (required).");return;
    }
    if(payload.password!==String(form.get("confirm"))){
     setError("Passwords do not match");return;
    }
    await requestEmailRegistrationOtp(payload.email);
    setSaved(payload);setOtpSent(true);
   }else if(saved){
    setUser(await registerWithEmailOtp({...saved,otp:String(form.get("otp")||"").trim()}));
    router.push("/dashboard");
   }else{
    setOtpSent(false);setError("Please request a new verification code.");
   }
  }catch(e){
   setError(e instanceof ApiError?e.message:"Registration failed");
  }finally{setBusy(false)}
 }
 return <><SiteHeader/><main className="auth-page">
  <form className="auth-card" onSubmit={submit}>
   <span className="eyebrow">Join the Community</span>
   <h1>{otpSent?"Verify email":"Create Account"}</h1>
   {otpSent?<p className="registration-otp-destination" role="status">
     We sent a 6-digit verification code to: <strong>{saved?.email}</strong>
     Check the inbox or spam/junk folder for this email.
    </p>:<p>Register once to book facilities and join programs.</p>}
   {error&&<div className="form-error" role="alert">{error}</div>}
   {!otpSent&&<>
    <label>Full name<input name="fullName" required maxLength={120} defaultValue={saved?.fullName||""}/></label>
    <label>Email<input name="email" required type="email" autoComplete="email" defaultValue={saved?.email||""}/></label>
    <label>Mobile number<input name="mobile" required type="tel" autoComplete="tel" inputMode="tel" pattern="[0-9+ -]{10,20}" defaultValue={saved?.mobile||""}/></label>
    <label>Password<input name="password" required type="password" minLength={8} autoComplete="new-password"/></label>
    <label>Confirm password<input name="confirm" required type="password" minLength={8} autoComplete="new-password"/></label>
   </>}
   {otpSent&&<>
    <label>Email verification code<input name="otp" required inputMode="numeric" autoComplete="one-time-code" pattern="[0-9]{6}" maxLength={6}/></label>
    <button type="button" className="registration-change-email" disabled={busy}
     onClick={()=>{setOtpSent(false);setError("");}}>Wrong email? Change address</button>
   </>}
   <button disabled={busy} className="btn primary large" type="submit">
    {busy?"Processing…":otpSent?"Verify & Create Account":"Send Email OTP"}
   </button>
   <p>Already registered? <Link href="/login">Login</Link></p>
  </form>
 </main><SiteFooter/></>;
}
