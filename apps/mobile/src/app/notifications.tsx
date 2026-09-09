import { MaterialCommunityIcons } from '@expo/vector-icons';
import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, Pressable, RefreshControl, ScrollView, StyleSheet, Switch, Text, View } from 'react-native';
import { EmptyState } from '@/components/screen-shell';
import { BrandColors } from '@/constants/brand';
import { useAuth } from '@/features/auth/auth-context';
import { api, ApiError } from '@/lib/api';

type Notice={id:string;category:string;subject:string;message:string;readAt?:string;createdAt:string};
type Preferences={emailEnabled:boolean;smsEnabled:boolean;pushEnabled:boolean};

export default function NotificationsScreen(){
 const {user}=useAuth();const[items,setItems]=useState<Notice[]>([]);const[prefs,setPrefs]=useState<Preferences>({emailEnabled:true,smsEnabled:true,pushEnabled:true});
 const[loading,setLoading]=useState(true);const[refreshing,setRefreshing]=useState(false);const[error,setError]=useState('');
 const load=useCallback(async()=>{if(!user){setLoading(false);return}try{const[n,p]=await Promise.all([api<Notice[]>('/api/v1/notifications/me'),api<Preferences>('/api/v1/notifications/preferences')]);setItems(n);setPrefs(p);setError('')}catch(cause){setError(cause instanceof ApiError?cause.message:'Unable to load notifications.')}finally{setLoading(false);setRefreshing(false)}},[user]);
 useEffect(()=>{void load()},[load]);
 async function read(id:string){try{await api('/api/v1/notifications/'+id+'/read',{method:'PATCH'});setItems(current=>current.map(item=>item.id===id?{...item,readAt:new Date().toISOString()}:item))}catch(cause){setError(cause instanceof ApiError?cause.message:'Unable to mark notification as read.')}}
 async function preference(key:keyof Preferences,value:boolean){const next={...prefs,[key]:value};setPrefs(next);try{setPrefs(await api<Preferences>('/api/v1/notifications/preferences',{method:'PUT',body:JSON.stringify(next)}))}catch(cause){setPrefs(prefs);setError(cause instanceof ApiError?cause.message:'Unable to update notification preferences.')}}
 if(!user)return <View style={styles.center}><EmptyState icon="bell-lock-outline" title="Login required" description="Login to see booking, payment and account messages."/></View>;
 return <ScrollView style={styles.screen} contentContainerStyle={styles.content} refreshControl={<RefreshControl refreshing={refreshing} onRefresh={()=>{setRefreshing(true);void load()}}/>}>
  <Text style={styles.eyebrow}>MESSAGE CENTRE</Text><Text style={styles.title}>Notifications</Text>
  <Text style={styles.subtitle}>Account, booking, payment, membership, training, tournament and shop updates.</Text>
  {!!error&&<Text style={styles.error}>{error}</Text>}
  <View style={styles.preferences}><Text style={styles.section}>Delivery preferences</Text><Preference label="Email" value={prefs.emailEnabled} onChange={v=>void preference('emailEnabled',v)}/><Preference label="SMS" value={prefs.smsEnabled} onChange={v=>void preference('smsEnabled',v)}/><Preference label="Push" value={prefs.pushEnabled} onChange={v=>void preference('pushEnabled',v)}/></View>
  <Text style={styles.section}>Recent messages</Text>
  {loading?<ActivityIndicator color={BrandColors.accent}/>:items.length?items.map(item=><Pressable key={item.id} onPress={()=>!item.readAt&&void read(item.id)} style={[styles.card,!item.readAt&&styles.unread]}><View style={styles.row}><View style={styles.icon}><MaterialCommunityIcons name={icon(item.category)} size={22} color={BrandColors.primary}/></View><View style={styles.flex}><Text style={styles.subject}>{item.subject}</Text><Text style={styles.message}>{item.message}</Text><Text style={styles.date}>{new Date(item.createdAt).toLocaleString()}</Text></View>{!item.readAt&&<View style={styles.dot}/>}</View></Pressable>):<EmptyState icon="bell-outline" title="No notifications" description="Your transactional messages will appear here."/>}
 </ScrollView>
}
function Preference({label,value,onChange}:{label:string;value:boolean;onChange(value:boolean):void}){return <View style={styles.preference}><Text style={styles.preferenceLabel}>{label}</Text><Switch value={value} onValueChange={onChange} trackColor={{true:BrandColors.accent}}/></View>}
function icon(category:string):keyof typeof MaterialCommunityIcons.glyphMap{const icons:Record<string,keyof typeof MaterialCommunityIcons.glyphMap>={ACCOUNT:'account-check-outline',SECURITY:'shield-key-outline',BOOKING:'calendar-check-outline',PAYMENT:'cash-check',REFUND:'cash-refund',MEMBERSHIP:'card-account-details-outline',TRAINING:'whistle-outline',TOURNAMENT:'trophy-outline',COMMERCE:'shopping-outline'};return icons[category]||'bell-outline'}
const styles=StyleSheet.create({screen:{flex:1,backgroundColor:BrandColors.background},content:{padding:20,paddingTop:52,paddingBottom:110,gap:12},center:{flex:1,backgroundColor:BrandColors.background,padding:24,justifyContent:'center'},eyebrow:{color:BrandColors.accent,fontSize:12,fontWeight:'900',letterSpacing:1.5},title:{fontSize:31,fontWeight:'900',color:BrandColors.textPrimary},subtitle:{color:BrandColors.textSecondary,lineHeight:21,marginBottom:6},section:{fontSize:18,fontWeight:'900',color:BrandColors.textPrimary,marginTop:8},preferences:{backgroundColor:BrandColors.surface,borderWidth:1,borderColor:BrandColors.border,borderRadius:18,padding:15},preference:{flexDirection:'row',justifyContent:'space-between',alignItems:'center',minHeight:45},preferenceLabel:{fontWeight:'800',color:BrandColors.textPrimary},card:{backgroundColor:BrandColors.surface,borderWidth:1,borderColor:BrandColors.border,borderRadius:16,padding:14},unread:{borderColor:BrandColors.accent,backgroundColor:'#FFF8F1'},row:{flexDirection:'row',gap:11},icon:{width:42,height:42,borderRadius:13,backgroundColor:'#E9F3E6',alignItems:'center',justifyContent:'center'},flex:{flex:1},subject:{fontWeight:'900',color:BrandColors.textPrimary},message:{color:BrandColors.textSecondary,lineHeight:19,marginTop:4},date:{fontSize:11,color:BrandColors.textSecondary,marginTop:7},dot:{width:9,height:9,borderRadius:5,backgroundColor:BrandColors.accent,marginTop:5},error:{backgroundColor:'#FFF0EC',color:'#A32613',padding:13,borderRadius:12}});
