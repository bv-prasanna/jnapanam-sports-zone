import * as Application from 'expo-application';
import Constants from 'expo-constants';
import {Platform} from 'react-native';
import {getStoredValue,setStoredValue} from '@/lib/storage';

export const PERMANENT_APP_LINK='https://jnapanam.in/app';
const CACHE_KEY='jnapanam.android.release-policy.v1';
export type AppReleasePolicy={
 available:boolean;latestVersionCode:number;latestVersionName:string;
 minimumSupportedVersionCode:number;updateMode:'OFF'|'OPTIONAL'|'MANDATORY';
 releaseNotes?:string|null;downloadPageUrl:string;
};
export type UpdateDecision='none'|'optional'|'mandatory';
const listeners=new Set<()=>void>();
let blockedByServer=false;
export function onServerRequiredUpdate(listener:()=>void){listeners.add(listener);return ()=>{listeners.delete(listener);};}
export function announceServerRequiredUpdate(){blockedByServer=true;for(const listener of listeners)listener();}
export function wasBlockedByServer(){return blockedByServer;}
export function clearServerUpdateBlock(){blockedByServer=false;}

export function installedAndroidBuild():number{
 if(Platform.OS!=='android')return 0;
 const native=Number(Application.nativeBuildVersion);
 const fallback=Number(Constants.expoConfig?.android?.versionCode||1);
 return Number.isSafeInteger(native)&&native>0?native:(Number.isSafeInteger(fallback)&&fallback>0?fallback:1);
}
export function installedVersionName():string{
 return Application.nativeApplicationVersion||Constants.expoConfig?.version||'Unknown';
}
export function decideUpdate(policy:AppReleasePolicy|undefined|null,installed:number):UpdateDecision{
 if(!policy?.available||!installed||policy.updateMode==='OFF')return 'none';
 if(policy.updateMode==='MANDATORY'&&installed<policy.minimumSupportedVersionCode)return 'mandatory';
 if(installed<policy.latestVersionCode)return 'optional';
 return 'none';
}
export async function loadReleasePolicy(baseUrl:string,signal?:AbortSignal):Promise<AppReleasePolicy>{
 const response=await fetch(baseUrl+'/api/v1/public/app-release/android',{headers:{Accept:'application/json'},cache:'no-store',signal});
 if(!response.ok)throw new Error('App release service unavailable');
 const data=await response.json() as AppReleasePolicy;
 if(!['OFF','OPTIONAL','MANDATORY'].includes(data.updateMode)||!Number.isSafeInteger(data.latestVersionCode)
    ||!Number.isSafeInteger(data.minimumSupportedVersionCode)||data.minimumSupportedVersionCode>data.latestVersionCode
    ||data.minimumSupportedVersionCode<1)throw new Error('Invalid app release response');
 try{await setStoredValue(CACHE_KEY,JSON.stringify(data));}catch{}
 return data;
}
export async function savedReleasePolicy():Promise<AppReleasePolicy|null>{
 try{const raw=await getStoredValue(CACHE_KEY);return raw?JSON.parse(raw) as AppReleasePolicy:null;}catch{return null;}
}
