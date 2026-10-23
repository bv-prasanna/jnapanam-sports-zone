import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, Text, View } from 'react-native';
import { ScreenShell, EmptyState } from '@/components/screen-shell';
import { BrandColors } from '@/constants/brand';
import { useAuth } from '@/features/auth/auth-context';
import { api, ApiError } from '@/lib/api';
type Assessment={id:string;sport_name:string;skill:string;score:number;assessed_on:string;remarks?:string};
type Goal={id:string;sport_name:string;title:string;target_score?:number;status:string;due_on?:string};
type Attendance={sport_name:string;sessions:number;attended:number};
type Competition={tournament_name:string;team_name:string;round_number:number;outcome:string};
type Data={assessments:Assessment[];goals:Goal[];attendance:Attendance[];competitions:Competition[]};
export default function GrowthScreen(){
 const {user}=useAuth();const [data,setData]=useState<Data|null>(null);
 const [loading,setLoading]=useState(false);const [error,setError]=useState('');
 const load=useCallback(async()=>{if(!user)return;setLoading(true);try{setData(await api<Data>('/api/v1/growth/me'));setError('')}catch(e){setError(e instanceof ApiError?e.message:'Unable to load progress')}finally{setLoading(false)}},[user]);
 useEffect(()=>{void load()},[load]);
 return <ScreenShell title="My Growth">
 {!user?<EmptyState icon="account-circle-outline" title="Sign in to track growth" description="Track your coach assessments and training goals."/>:loading?<ActivityIndicator color={BrandColors.accent}/>:<>
 <Text style={styles.tag}>Play • Train • Compete • Grow</Text>
 <Text style={styles.h}>My sports journey</Text>
 {!!error&&<Text style={styles.error}>{error}</Text>}
 <Pressable onPress={()=>void load()} style={styles.refresh}><Text style={styles.refreshText}>Refresh progress</Text></Pressable>
 <View style={styles.card}><Text style={styles.h}>Skill assessments ({data?.assessments?.length||0})</Text>
 {(data?.assessments||[]).length===0?<Text>No coach assessments yet.</Text>:(data?.assessments||[]).map(a=><View key={a.id} style={styles.item}>
 <Text style={styles.label}>{a.sport_name} • {a.skill}</Text><Text>{a.score}/100 · {a.assessed_on}</Text>
 <View style={styles.track}><View style={[styles.progress,{width:`${Math.min(100,Math.max(0,a.score))}%`}]}/></View>
 {!!a.remarks&&<Text style={styles.comment}>{a.remarks}</Text>}</View>)}</View>
 <View style={styles.card}><Text style={styles.h}>Training goals ({data?.goals?.length||0})</Text>
 {(data?.goals||[]).length===0?<Text>No goals assigned yet.</Text>:(data?.goals||[]).map(g=><View key={g.id} style={styles.item}>
 <Text style={styles.label}>{g.title}</Text><Text>{g.sport_name} · {g.status}</Text>
 {g.target_score!=null&&<Text>Target {g.target_score}/100</Text>}
 {!!g.due_on&&<Text>Due {g.due_on}</Text>}</View>)}</View>
 <View style={styles.card}><Text style={styles.h}>Training attendance</Text>{(data?.attendance||[]).length===0?<Text>No recorded attendance yet.</Text>:(data?.attendance||[]).map((a,i)=><View key={i} style={styles.item}><Text style={styles.label}>{a.sport_name}</Text><Text>{a.attended} of {a.sessions} sessions attended</Text><View style={styles.track}><View style={[styles.progress,{width:`${Math.round(100*a.attended/Math.max(1,a.sessions))}%`}]}/></View></View>)}</View>
 <View style={styles.card}><Text style={styles.h}>Competition results</Text>{(data?.competitions||[]).length===0?<Text>No team captain results yet.</Text>:(data?.competitions||[]).map((c,i)=><View key={i} style={styles.item}><Text style={styles.label}>{c.tournament_name}</Text><Text>{c.team_name} · Round {c.round_number} · {c.outcome}</Text></View>)}</View>
 </>}</ScreenShell>;
}
const styles=StyleSheet.create({tag:{color:BrandColors.accent,fontSize:14,fontWeight:'800',marginVertical:10},h:{fontWeight:'900',fontSize:20,color:BrandColors.textPrimary,marginVertical:8},error:{color:'#C7350A'},refresh:{padding:12,backgroundColor:BrandColors.accent,borderRadius:9,alignItems:'center',marginBottom:12},refreshText:{color:'#fff',fontWeight:'800'},card:{backgroundColor:'#fff',padding:16,borderRadius:15,borderColor:BrandColors.border,borderWidth:1,marginBottom:15},item:{paddingVertical:12,borderBottomColor:BrandColors.border,borderBottomWidth:1},label:{color:BrandColors.textPrimary,fontWeight:'800'},track:{height:8,backgroundColor:'#E8E8E8',borderRadius:9,marginTop:8,overflow:'hidden'},progress:{height:8,backgroundColor:BrandColors.success},comment:{color:BrandColors.textSecondary,marginTop:6}});
