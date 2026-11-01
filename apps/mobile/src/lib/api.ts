import { Platform } from 'react-native';
import i18n from '@/i18n';
import { getEventLocationHeaders } from './event-location';

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
let accessToken:string|null=null;
export function setAccessToken(token:string|null){accessToken=token;}
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
export async function api<T>(path:string,options:RequestInit={}):Promise<T>{
  const method=(options.method||'GET').toUpperCase();
  const geoHeaders=['POST','PUT','PATCH','DELETE'].includes(method)?await getEventLocationHeaders():{};
  const publicAuth=path.startsWith('/api/v1/auth/');
  let response:Response;
  try{
    response=await fetch(API_URL+path,{
      ...options,
      headers:{
        Accept:'application/json',
        ...(options.body && !(options.body instanceof FormData)?{'Content-Type':'application/json'}:{}),
        ...(!publicAuth&&accessToken?{Authorization:'Bearer '+accessToken}:{}),
        ...geoHeaders,...options.headers
      }
    });
  }catch(e){
    // Never expose low-level Java/Android SocketException messages in the UI.
    throw new ApiError(friendly('network'),0);
  }
  if(!response.ok){
    const status=response.status;
    if(status===401)throw new ApiError(friendly('session'),status);
    if(status>=500)throw new ApiError(friendly('server'),status);
    let message=friendly('generic');
    try{
      const body=await response.json();
      const raw=body?.message||body?.detail;
      if(typeof raw==='string'&&!technical.test(raw)&&raw.length<220)message=raw;
    }catch{}
    throw new ApiError(message,status);
  }
  if(response.status===204)return undefined as T;
  let body:string;
  try{body=await response.text();}catch{throw new ApiError(friendly('network'),0);}
  if(!body.trim())return undefined as T;
  try{return JSON.parse(body) as T;}catch{throw new ApiError(friendly('server'),502);}
}
