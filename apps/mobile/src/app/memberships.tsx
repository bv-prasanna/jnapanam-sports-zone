import { MaterialCommunityIcons } from '@expo/vector-icons';
import { useEffect, useState } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, Text, View } from 'react-native';

import { EmptyState, ScreenShell } from '@/components/screen-shell';
import { BrandColors } from '@/constants/brand';
import { useAuth } from '@/features/auth/auth-context';
import { api, ApiError } from '@/lib/api';

type Plan = { id:string; code:string; name:string; description?:string; durationDays:number; price:number; dailyBookingLimit?:number; monthlyBookingLimit?:number; sportCodes:string[] };
type Membership = { id:string; planName?:string; status:string; startDate?:string; endDate?:string };

export default function MembershipsScreen() {
  const { user } = useAuth();
  const [plans,setPlans]=useState<Plan[]>([]);
  const [mine,setMine]=useState<Membership[]>([]);
  const [loading,setLoading]=useState(true);
  const [message,setMessage]=useState('');
  const load=async()=>{setLoading(true);try{const p=await api<Plan[]>('/api/v1/public/membership-plans');setPlans(p);setMine(user?await api<Membership[]>('/api/v1/memberships/me'):[]);setMessage('')}catch(e){setMessage(e instanceof ApiError?e.message:'Unable to load memberships.')}finally{setLoading(false)}};
  useEffect(()=>{void load()},[user]);
  async function subscribe(planId:string){if(!user){setMessage('Please login before choosing a membership.');return}try{await api('/api/v1/memberships',{method:'POST',body:JSON.stringify({planId})});setMessage('Membership selected successfully.');await load()}catch(e){setMessage(e instanceof ApiError?e.message:'Unable to select membership.')}}
  async function cancel(id:string){try{await api('/api/v1/memberships/'+id+'/cancel',{method:'POST'});await load()}catch(e){setMessage(e instanceof ApiError?e.message:'Unable to cancel membership.')}}
  return <ScreenShell title="Memberships">{loading?<ActivityIndicator color={BrandColors.accent}/>:<>
    {!!message&&<Text style={styles.message}>{message}</Text>}
    {plans.length?<View style={styles.list}>{plans.map(plan=><View style={styles.card} key={plan.id}>
      <View style={styles.icon}><MaterialCommunityIcons name="card-account-details-outline" size={25} color={BrandColors.accent}/></View>
      <Text style={styles.name}>{plan.name}</Text><Text style={styles.description}>{plan.description}</Text>
      <Text style={styles.meta}>{plan.durationDays} days · ₹{Number(plan.price).toFixed(2)}</Text>
      {!!plan.sportCodes?.length&&<Text style={styles.sports}>{plan.sportCodes.join(' · ')}</Text>}
      <Pressable style={styles.button} onPress={()=>void subscribe(plan.id)}><Text style={styles.buttonText}>Choose plan</Text></Pressable>
    </View>)}</View>:<EmptyState icon="card-account-details-outline" title="No membership plans" description="Active membership plans will appear here."/>}
    {!!mine.length&&<><Text style={styles.heading}>My memberships</Text><View style={styles.list}>{mine.map(m=><View style={styles.card} key={m.id}><Text style={styles.name}>{m.planName||'Membership'}</Text><Text style={styles.meta}>{m.status}{m.endDate?' · Until '+m.endDate:''}</Text>{!['CANCELLED','EXPIRED'].includes(m.status)&&<Pressable style={styles.outline} onPress={()=>void cancel(m.id)}><Text style={styles.outlineText}>Cancel</Text></Pressable>}</View>)}</View></>}
  </>}</ScreenShell>
}
const styles=StyleSheet.create({list:{gap:12},card:{backgroundColor:BrandColors.surface,borderWidth:1,borderColor:BrandColors.border,borderRadius:18,padding:18},icon:{width:48,height:48,borderRadius:14,backgroundColor:'#FFF0E7',alignItems:'center',justifyContent:'center',marginBottom:12},name:{fontSize:18,fontWeight:'900',color:BrandColors.textPrimary},description:{fontSize:14,lineHeight:20,color:BrandColors.textSecondary,marginTop:6},meta:{fontSize:14,fontWeight:'800',color:BrandColors.primary,marginTop:12},sports:{fontSize:12,color:BrandColors.textSecondary,marginTop:5},button:{backgroundColor:BrandColors.accent,padding:13,borderRadius:12,alignItems:'center',marginTop:15},buttonText:{color:BrandColors.white,fontWeight:'900'},heading:{fontSize:22,fontWeight:'900',color:BrandColors.textPrimary,marginTop:28,marginBottom:12},outline:{borderWidth:1,borderColor:BrandColors.accent,padding:10,borderRadius:10,alignSelf:'flex-start',marginTop:12},outlineText:{color:BrandColors.accent,fontWeight:'800'},message:{backgroundColor:'#FFF4DF',padding:12,borderRadius:10,color:BrandColors.textPrimary,marginBottom:14}});
