import {router} from 'expo-router';
import {useEffect,useState} from 'react';
import {ActivityIndicator,Pressable,ScrollView,StyleSheet,Text,TextInput,View} from 'react-native';
import {ScreenShell,EmptyState} from '@/components/screen-shell';
import {BrandColors} from '@/constants/brand';
import {useAuth} from '@/features/auth/auth-context';
import {api} from '@/lib/api';

type Profile={recipientAddress:string;stateCode:string;gstin:string};
type Payment={id:string;receiptNumber:string;amountPaise:number;status:string;purpose:string};
type Billing={receiptNumber:string;documents:{documentNumber:string;kind:string;totalPaise:number;taxPaise:number;state:string}[];refunds:{amountPaise:number;status:string}[]};
const money=(n:number)=>`₹${(n/100).toFixed(2)}`;
const states=[['29','Karnataka'],['37','Andhra Pradesh'],['33','Tamil Nadu'],['32','Kerala'],['36','Telangana'],['27','Maharashtra'],['07','Delhi'],['24','Gujarat'],['09','Uttar Pradesh'],['19','West Bengal'],['30','Goa'],['38','Ladakh']];

export default function BillingScreen(){
 const {user}=useAuth();
 const [profile,setProfile]=useState<Profile>({recipientAddress:'',stateCode:'29',gstin:''});
 const [payments,setPayments]=useState<Payment[]>([]);
 const [details,setDetails]=useState<Billing|null>(null);
 const [busy,setBusy]=useState(false),[error,setError]=useState(''),[success,setSuccess]=useState('');
 const [selected,setSelected]=useState('');
 useEffect(()=>{if(!user)return;setBusy(true);Promise.all([api<Partial<Profile>>('/api/v1/billing/me'),api<Payment[]>('/api/v1/payments/me')]).then(([p,rows])=>{setProfile(v=>({...v,...p,gstin:p.gstin||''}));setPayments(rows.filter(x=>['PAID','PARTIALLY_REFUNDED','REFUNDED'].includes(x.status)))}).catch(e=>setError(e instanceof Error?e.message:'Unable to load billing')).finally(()=>setBusy(false))},[user]);
 async function save(){try{setBusy(true);setError('');await api('/api/v1/billing/me',{method:'PUT',body:JSON.stringify({...profile,gstin:profile.gstin||null})});setSuccess('Billing address saved for future invoices.')}catch(e){setError(e instanceof Error?e.message:'Unable to save')}finally{setBusy(false)}}
 async function review(id:string){try{setBusy(true);setSelected(id);setError('');setDetails(await api<Billing>(`/api/v1/payments/${id}/billing`))}catch(e){setError(e instanceof Error?e.message:'Unable to load documents')}finally{setBusy(false)}}
 if(!user)return <ScreenShell title="Billing & invoices"><EmptyState icon="file-document-outline" title="Login required" description="Sign in to manage billing and view invoices."/><Pressable onPress={()=>router.push('/login')}><Text>Sign in</Text></Pressable></ScreenShell>;
 return <ScreenShell title="Billing & invoices">
 <ScrollView contentContainerStyle={{paddingBottom:40}}>
 {busy&&<ActivityIndicator color={BrandColors.primary}/>}
 {error?<Text accessibilityRole="alert" style={styles.error}>{error}</Text>:null}
 {success?<Text style={styles.success}>{success}</Text>:null}
 <View style={styles.card}><Text style={styles.heading}>Billing details</Text><Text style={styles.note}>For future invoices. Existing issued documents remain unchanged.</Text>
 <Text style={styles.label}>Full billing address</Text><TextInput multiline numberOfLines={3} style={styles.input} placeholder="Building, street, city, PIN" value={profile.recipientAddress} onChangeText={v=>setProfile(p=>({...p,recipientAddress:v}))}/>
 <Text style={styles.label}>State code</Text><View style={styles.row}>{states.map(([code,name])=><Pressable key={code} accessibilityRole="radio" accessibilityState={{checked:profile.stateCode===code}} style={[styles.choice,profile.stateCode===code&&styles.selected]} onPress={()=>setProfile(p=>({...p,stateCode:code}))}><Text style={styles.choiceText}>{name}</Text></Pressable>)}</View>
 <Text style={styles.label}>GSTIN (optional)</Text><TextInput style={styles.input} placeholder="Only for registered buyers" autoCapitalize="characters" maxLength={15} value={profile.gstin} onChangeText={v=>setProfile(p=>({...p,gstin:v.toUpperCase()}))}/>
 <Pressable disabled={busy||!profile.recipientAddress.trim()} accessibilityRole="button" style={[styles.primary,busy&&styles.disabled]} onPress={()=>void save()}><Text style={styles.primaryText}>Save billing details</Text></Pressable></View>
 <View style={styles.card}><Text style={styles.heading}>Payment receipts and GST documents</Text>
 {payments.length===0?<Text style={styles.note}>No paid transactions found.</Text>:payments.map(p=><Pressable key={p.id} accessibilityRole="button" style={styles.payment} onPress={()=>void review(p.id)}><Text style={styles.headingSmall}>{p.purpose.replaceAll('_',' ')} · {money(p.amountPaise)}</Text><Text style={styles.note}>{p.receiptNumber} · {p.status}</Text><Text style={styles.link}>View billing documents</Text></Pressable>)}
 {selected&&details?<View style={styles.payment}><Text style={styles.headingSmall}>Documents for {details.receiptNumber}</Text>
 {details.documents?.length?details.documents.map(d=><View key={d.documentNumber} style={styles.document}><Text style={styles.label}>{d.kind.replaceAll('_',' ')} {d.documentNumber}</Text><Text>{money(d.totalPaise)} · Tax {money(d.taxPaise)}</Text><Text style={styles.note}>A PDF is emailed automatically when enabled by the venue. You can also download it from your web account.</Text></View>):<Text style={styles.note}>An invoice has not been issued yet. Your payment receipt is available in your account.</Text>}
 </View>:null}</View></ScrollView></ScreenShell>;
}
const styles=StyleSheet.create({
 card:{marginBottom:18,borderRadius:15,padding:18,backgroundColor:'#fff',borderWidth:1,borderColor:BrandColors.border},
 heading:{fontSize:20,fontWeight:'900',color:BrandColors.primary},headingSmall:{fontSize:15,fontWeight:'800',color:BrandColors.primary},
 note:{color:BrandColors.textSecondary,fontSize:13,marginTop:6},label:{fontWeight:'700',marginTop:14,marginBottom:6,color:BrandColors.textPrimary},
 input:{borderWidth:1,borderColor:BrandColors.border,borderRadius:8,padding:10,minHeight:48,color:BrandColors.textPrimary},
 primary:{backgroundColor:BrandColors.primary,padding:14,borderRadius:10,alignItems:'center',marginTop:18},
 primaryText:{color:'#fff',fontWeight:'800'},row:{flexDirection:'row',flexWrap:'wrap',gap:7},
 choice:{borderWidth:1,borderColor:BrandColors.border,borderRadius:8,padding:8,marginBottom:4},
 selected:{borderColor:BrandColors.primary,backgroundColor:'#e5f2fb'},choiceText:{fontSize:12,color:BrandColors.textPrimary},
 payment:{borderTopWidth:1,borderTopColor:BrandColors.border,paddingVertical:13},document:{backgroundColor:'#f5f9fc',borderRadius:10,padding:10,marginTop:8},
 link:{color:BrandColors.primary,fontWeight:'800',marginTop:5},error:{color:'#A32613'},success:{color:'#218456'},disabled:{opacity:0.55},
});
