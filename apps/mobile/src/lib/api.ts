import { Platform } from 'react-native';
import i18n from '@/i18n';
import { announceServerRequiredUpdate, installedAndroidBuild } from '@/lib/app-release';
import { getEventLocationHeaders } from './event-location';
import { getDeviceIdentity } from './device-identity';
import { getStoredValue, removeStoredValue, setStoredValue } from './storage';

export type TokenResponse = {
  tokenType: string;
  accessToken: string;
  accessTokenExpiresAt: string;
  refreshToken: string;
};
export type User = {
  id: string;fullName:string;displayName?:string;profilePhotoUrl?:string;
  email:string;mobile?:string;addressLine?:string;city?:string;state?:string;postalCode?:string;
  status:string;roles:string[];requiresUpfrontPayment:boolean;
};
const defaultHost=Platform.OS==='android'?'10.0.2.2':'localhost';
export const API_URL=(process.env.EXPO_PUBLIC_API_URL||'http://'+defaultHost+':8080').replace(/\/$/,'');
export const REFRESH_TOKEN_KEY = 'jnapanam.refreshToken';
let accessToken:string|null=null;
let refreshInFlight:Promise<string|null>|null=null;
let sessionExpiredHandler:(()=>void)|null=null;
export function setAccessToken(token:string|null){accessToken=token;}
export function onSessionExpired(handler:(()=>void)|null){sessionExpiredHandler=handler;}
export class ApiError extends Error {
  constructor(message:string,public status:number){super(message);this.name='ApiError';}
}
const friendly=(key:'network'|'timeout'|'server'|'session'|'generic')=>i18n.t('errors.'+key);
const technical=/socketexception|fetch failed|network request failed|software caused connection abort|stacktrace|java\.|exception:|connection refused|read timed out|unexpected token/i;
export function userFriendlyError(reason:unknown,fallback?:string){
  const message=reason instanceof Error?reason.message:'';
  if(technical.test(message))return friendly('network');
  if(reason instanceof ApiError)return reason.message;
  return fallback||friendly('generic');
}

/**
 * Refresh tokens are bound to an app-installation identifier held in SecureStore.
 * Access-token renewal is single-flight: the backend rotates refresh tokens, so
 * simultaneous refresh requests using the same old token would revoke each other.
 * Never remove saved credentials when the network is unavailable or the server is 5xx.
 */
export async function refreshAccessToken():Promise<string|null>{
  if(refreshInFlight)return refreshInFlight;
  refreshInFlight=(async()=>{
    const saved=await getStoredValue(REFRESH_TOKEN_KEY);
    if(!saved)return null;
    const device=await getDeviceIdentity();
    let response:Response;
    try{
      response=await fetch(API_URL+'/api/v1/auth/refresh',{
        method:'POST',
        headers:{Accept:'application/json','Content-Type':'application/json'},
        body:JSON.stringify({refreshToken:saved,deviceSignature:device.deviceSignature})
      });
    }catch{
      throw new ApiError(friendly('network'),0);
    }
    if(response.status===401||response.status===403){
      // A different request may have already stored a newer rotated token.
      if(await getStoredValue(REFRESH_TOKEN_KEY)===saved){
        accessToken=null;
        await removeStoredValue(REFRESH_TOKEN_KEY);
        sessionExpiredHandler?.();
      }
      return null;
    }
    if(!response.ok)throw new ApiError(friendly('server'),response.status);
    let tokens:TokenResponse;
    try{tokens=await response.json() as TokenResponse;}
    catch{throw new ApiError(friendly('server'),502);}
    if(!tokens.refreshToken||!tokens.accessToken)
      throw new ApiError(friendly('server'),502);
    await setStoredValue(REFRESH_TOKEN_KEY,tokens.refreshToken);
    accessToken=tokens.accessToken;
    return tokens.accessToken;
  })().finally(()=>{refreshInFlight=null;});
  return refreshInFlight;
}
export async function restoreAccessToken(){return !!(await refreshAccessToken());}
export async function clearMobileSession(){
  accessToken=null;
  await removeStoredValue(REFRESH_TOKEN_KEY);
}

export async function api<T>(path:string,options:RequestInit={}):Promise<T>{
  const method=(options.method||'GET').toUpperCase();
  const geoHeaders=['POST','PUT','PATCH','DELETE'].includes(method)?await getEventLocationHeaders():{};
  // Never attach an expired JWT to login, registration, password-reset or refresh endpoints.
  const publicAuth=path.startsWith('/api/v1/auth/');
  const request=async(token:string|null)=>{
    const headers:Record<string,string>={
      Accept:'application/json',
      ...(options.body && !(options.body instanceof FormData)?{'Content-Type':'application/json'}:{}),
      ...(!publicAuth&&token?{Authorization:'Bearer '+token}:{}),
      ...(Platform.OS==='android'?{'X-Jnapanam-Platform':'android','X-Jnapanam-Version-Code':String(installedAndroidBuild())}:{}),
      ...geoHeaders,...(options.headers as Record<string,string>||{})
    };
    try{return await fetch(API_URL+path,{...options,headers});}
    catch{throw new ApiError(friendly('network'),0);}
  };
  const sentToken=accessToken;
  let response=await request(sentToken);
  if(response.status===401 && !publicAuth){
    const renewed=accessToken&&accessToken!==sentToken
      ? accessToken : await refreshAccessToken();
    if(renewed)response=await request(renewed);
  }
  if(!response.ok){
    const status=response.status;
    const ref=response.headers.get('X-Correlation-ID');
    const supportRef=ref && /^[A-Za-z0-9_-]{8,80}$/.test(ref)?' Reference: '+ref:'';
    if(status===426){announceServerRequiredUpdate();throw new ApiError('An app update is required to continue. Open the update screen.',426);}
    if(status===401){
      if(publicAuth&&path==='/api/v1/auth/login'){
        const detail=await response.json().catch(()=>null);
        const message=detail?.detail||detail?.message;
        if(typeof message==='string'&&message.includes('MFA code sent'))
          throw new ApiError('MFA code sent to your registered email. Enter the 6-digit verification code.',401);
        throw new ApiError('Invalid login details or verification code. Please try again.',401);
      }
      if(publicAuth)throw new ApiError('We could not verify these details. Please try again.',401);
      throw new ApiError(friendly('session'),401);
    }
    if(status===403)throw new ApiError('You do not have permission to perform this action.',403);
    if(status===404)throw new ApiError('The requested information could not be found. Please refresh and try again.',404);
    if(status===408||status===504)throw new ApiError('The request took too long. Please try again.',status);
    if(status===429)throw new ApiError('Too many attempts. Please wait a moment and try again.',429);
    if(status>=500)throw new ApiError(friendly('server')+supportRef,status);
    let message='Please check your details and try again.';
    try{
      const body=await response.json();
      const raw=body?.detail||body?.message;
      if(typeof raw==='string'&&!technical.test(raw)&&raw.length<180
         && !/(?:<|>|stacktrace|sql|jdbc|password[=:]|token[=:])/i.test(raw))
        message=raw;
    }catch{}
    throw new ApiError(message,status);
  }
  if(response.status===204)return undefined as T;
  let body:string;
  try{body=await response.text();}catch{throw new ApiError(friendly('network'),0);}
  if(!body.trim())return undefined as T;
  try{return JSON.parse(body) as T;}catch{throw new ApiError(friendly('server'),502);}
}
