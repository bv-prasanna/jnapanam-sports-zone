import { useEffect, useState } from 'react';
import { ActivityIndicator, StyleSheet, Text, View } from 'react-native';
import { EmptyState, ScreenShell } from '@/components/screen-shell';
import { BrandColors } from '@/constants/brand';
import { api, ApiError } from '@/lib/api';

type Program={id:string;code:string;sportCode:string;name:string;description?:string;level:string};
type Batch={id:string;programCode:string;facilityCode:string;coachName?:string;name:string;startDate:string;endDate:string;startTime:string;endTime:string;daysOfWeek:string[];capacity:number;available:number;fee:number};

export default function TrainingScreen(){
 const [programs,setPrograms]=useState<Program[]>([]);const[batches,setBatches]=useState<Batch[]>([]);const[loading,setLoading]=useState(true);const[error,setError]=useState('');
 useEffect(()=>{Promise.all([api<Program[]>('/api/v1/public/training/programs'),api<Batch[]>('/api/v1/public/training/batches')]).then(([p,b])=>{setPrograms(p);setBatches(b)}).catch(e=>setError(e instanceof ApiError?e.message:'Unable to load training programs.')).finally(()=>setLoading(false))},[]);
 return <ScreenShell title="Training">{loading?<ActivityIndicator color={BrandColors.accent}/>:error?<Text style={styles.error}>{error}</Text>:programs.length?<View style={styles.list}>{programs.map(p=><View style={styles.card} key={p.id}><Text style={styles.sport}>{p.sportCode} · {p.level}</Text><Text style={styles.title}>{p.name}</Text>{!!p.description&&<Text style={styles.description}>{p.description}</Text>}{batches.filter(b=>b.programCode===p.code).map(b=><View style={styles.batch} key={b.id}><Text style={styles.batchTitle}>{b.name}</Text><Text style={styles.meta}>{b.startTime.slice(0,5)}–{b.endTime.slice(0,5)} · {b.daysOfWeek.join(', ')}</Text><Text style={styles.meta}>{b.facilityCode} · {b.available}/{b.capacity} places · ₹{Number(b.fee).toFixed(2)}</Text>{!!b.coachName&&<Text style={styles.meta}>Coach: {b.coachName}</Text>}</View>)}</View>)}</View>:<EmptyState icon="whistle-outline" title="No training programs" description="Published programs and batches will appear here."/>}</ScreenShell>
}
const styles=StyleSheet.create({list:{gap:14},card:{backgroundColor:BrandColors.surface,borderColor:BrandColors.border,borderWidth:1,borderRadius:18,padding:18},sport:{color:BrandColors.accent,fontWeight:'900',fontSize:12},title:{color:BrandColors.textPrimary,fontWeight:'900',fontSize:19,marginTop:5},description:{color:BrandColors.textSecondary,lineHeight:20,marginTop:7},batch:{backgroundColor:'#F7F4EC',borderRadius:13,padding:13,marginTop:13},batchTitle:{fontWeight:'900',color:BrandColors.textPrimary},meta:{color:BrandColors.textSecondary,fontSize:12,marginTop:4},error:{backgroundColor:'#FFF0EC',color:'#A32613',padding:14,borderRadius:12}});
