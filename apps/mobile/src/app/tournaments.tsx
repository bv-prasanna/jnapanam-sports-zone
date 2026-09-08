import { useEffect, useState } from 'react';
import { ActivityIndicator, StyleSheet, Text, View } from 'react-native';
import { EmptyState, ScreenShell } from '@/components/screen-shell';
import { BrandColors } from '@/constants/brand';
import { api, ApiError } from '@/lib/api';

type Tournament={id:string;code:string;name:string;description?:string;sportCode:string;organizerName:string;format:string;status:string;registrationOpens:string;registrationCloses:string;startDate:string;endDate:string;teamSize:number;maxTeams:number;registeredTeams:number;entryFee:number};
export default function TournamentsScreen(){
 const[data,setData]=useState<Tournament[]>([]);const[loading,setLoading]=useState(true);const[error,setError]=useState('');
 useEffect(()=>{api<Tournament[]>('/api/v1/public/tournaments').then(setData).catch(e=>setError(e instanceof ApiError?e.message:'Unable to load tournaments.')).finally(()=>setLoading(false))},[]);
 return <ScreenShell title="Tournaments">{loading?<ActivityIndicator color={BrandColors.accent}/>:error?<Text style={styles.error}>{error}</Text>:data.length?<View style={styles.list}>{data.map(x=><View style={styles.card} key={x.id}><Text style={styles.sport}>{x.sportCode} · {x.status}</Text><Text style={styles.title}>{x.name}</Text>{!!x.description&&<Text style={styles.description}>{x.description}</Text>}<Text style={styles.meta}>{x.startDate} to {x.endDate} · {x.format.replaceAll('_',' ')}</Text><Text style={styles.meta}>{x.registeredTeams}/{x.maxTeams} teams · Team size {x.teamSize}</Text><Text style={styles.price}>₹{Number(x.entryFee).toFixed(2)}</Text><Text style={styles.organizer}>Organized by {x.organizerName}</Text></View>)}</View>:<EmptyState icon="trophy-outline" title="No upcoming tournaments" description="Published tournaments will appear here."/>}</ScreenShell>
}
const styles=StyleSheet.create({list:{gap:14},card:{backgroundColor:BrandColors.surface,borderColor:BrandColors.border,borderWidth:1,borderRadius:18,padding:18},sport:{color:BrandColors.accent,fontWeight:'900',fontSize:12},title:{color:BrandColors.textPrimary,fontWeight:'900',fontSize:19,marginTop:5},description:{color:BrandColors.textSecondary,lineHeight:20,marginTop:7},meta:{color:BrandColors.textSecondary,fontSize:13,marginTop:8},price:{color:BrandColors.primary,fontSize:18,fontWeight:'900',marginTop:10},organizer:{color:BrandColors.textSecondary,fontSize:12,marginTop:6},error:{backgroundColor:'#FFF0EC',color:'#A32613',padding:14,borderRadius:12}});
