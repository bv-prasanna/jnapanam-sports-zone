"use client";
import { createContext, useContext, useEffect, useMemo, useState } from "react";
import {useRouter} from "next/navigation";
import { Language, translate, TranslationKey } from "@/lib/i18n";
import { logout as apiLogout, restoreSession, User } from "@/lib/api";
import { DocumentLanguage } from "@/components/document-language";

type AppState = { language: Language; setLanguage(v:Language):void; t(k:TranslationKey):string; user:User|null; setUser(v:User|null):void; ready:boolean; signOut():Promise<void> };
const Context = createContext<AppState|null>(null);
export function AppProvider({children}:{children:React.ReactNode}) {
  const router=useRouter();
  const [language,setLanguageState]=useState<Language>("en"); const [user,setUser]=useState<User|null>(null); const [ready,setReady]=useState(false);
  useEffect(()=>{
    let expired=false;
    // This is shared by all customer/admin routes; an expired login immediately
    // removes privileged navigation without waiting for each page's API retry.
    const onExpired=()=>{
      expired=true;setUser(null);setReady(true);
      router.replace("/login?reason=session-expired");
    };
    window.addEventListener("jnapanam:session-expired",onExpired);
    const saved=localStorage.getItem("jnapanam.language") as Language|null;
    if(saved==="kn"||saved==="en")setLanguageState(saved);
    restoreSession().then(profile=>{if(!expired)setUser(profile);})
      .catch(()=>{/* Network/server failures preserve stored credentials. */})
      .finally(()=>setReady(true));
    return ()=>window.removeEventListener("jnapanam:session-expired",onExpired);
  },[router]);
  const setLanguage=(value:Language)=>{setLanguageState(value);localStorage.setItem("jnapanam.language",value)};
  const value=useMemo(()=>({language,setLanguage,t:(key:TranslationKey)=>translate(language,key),user,setUser,ready,signOut:async()=>{await apiLogout();setUser(null)}}),[language,user,ready]);
  return <Context.Provider value={value}><DocumentLanguage language={language}/>{children}</Context.Provider>;
}
export function useApp(){const value=useContext(Context);if(!value)throw new Error("AppProvider missing");return value}
