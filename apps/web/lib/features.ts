"use client";
import {useEffect,useState} from "react";
import {api} from "@/lib/api";
export type FeatureSetting={code:string;enabled:boolean;allowedRoles:string[]};
const featureOf:Record<string,string>={bookings:"BOOKINGS",memberships:"MEMBERSHIPS",training:"TRAINING",growth:"GROWTH",tournaments:"TOURNAMENTS",organizer:"TOURNAMENTS",shop:"SHOP",subscriptions:"SUBSCRIPTIONS"};
export function useFeatureVisibility(roles:string[]=[]){
 const [settings,setSettings]=useState<FeatureSetting[]|null>(null);
 useEffect(()=>{let mounted=true;void api<FeatureSetting[]>("/api/v1/public/features").then(values=>{if(mounted)setSettings(values)}).catch(()=>{});return()=>{mounted=false}},[]);
 return (key:string)=>{
   const code=featureOf[key]||key.toUpperCase();
   if(!settings)return code!=="TRAINING"; // fail closed for training until availability is confirmed
   const rule=settings.find(f=>f.code===code);
   if(!rule)return true;
   return rule.enabled&&(roles.length===0?rule.allowedRoles.includes("CUSTOMER"):roles.some(role=>rule.allowedRoles.includes(role)));
 };
}
