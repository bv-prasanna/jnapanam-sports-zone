import * as WebBrowser from 'expo-web-browser';
import {router} from 'expo-router';
import {useCallback,useEffect,useState} from 'react';
import {ActivityIndicator,Alert,Pressable,ScrollView,StyleSheet,Switch,Text,TextInput,View} from 'react-native';
import {ScreenShell} from '@/components/screen-shell';
import {BrandColors} from '@/constants/brand';
import {useAuth} from '@/features/auth/auth-context';
import {api,API_URL} from '@/lib/api';

type Offer={id:string;kind:'MEMBERSHIP'|'TRAINING';referenceId:string;title:string;amountPaise:number;period:string;interval:number;totalCycles:number};
type Agreement={id:string;title:string;status:string;paidCycles:number;totalCycles:number;amountPaise:number};
type Plan={id:string;code:string};
type Membership={id:string;planCode:string;status:string};
type Enrollment={id:string;batchId:string;status:string};
type Config={enabled:boolean;providerConfigured:boolean};
const money=(n:number)=>'₹'+(n/100).toLocaleString('en-IN',{minimumFractionDigits:2,maximumFractionDigits:2});
export default function SubscriptionsScreen(){
 const {user}=useAuth();
 const [offers,setOffers]=useState<Offer[]>([]);const [history,setHistory]=useState<Agreement[]>([]);
 const [memberships,setMemberships]=useState<Membership[]>([]);const [enrollments,setEnrollments]=useState<Enrollment[]>([]);
 const [plans,setPlans]=useState<Plan[]>([]);
 const [config,setConfig]=useState<Config>({enabled:false,providerConfigured:false});
 const [selected,setSelected]=useState('');const [consent,setConsent]=useState(false);
 const [participantName,setParticipantName]=useState('');const [participantAge,setParticipantAge]=useState('');
 const [busy,setBusy]=useState(false);const [error,setError]=useState('');const [loading,setLoading]=useState(true);
 const load=useCallback(async()=>{
  setLoading(true);setError('');
  try{const [o,c,m,e,p,h]=await Promise.all([
    api<Offer[]>('/api/v1/subscriptions/offerings'),
    api<Config>('/api/v1/subscriptions/config'),
    user?api<Membership[]>('/api/v1/memberships/me'):Promise.resolve([]),
    user?api<Enrollment[]>('/api/v1/training/enrollments/me'):Promise.resolve([]),
    api<Plan[]>('/api/v1/public/membership-plans'),
    user?api<Agreement[]>('/api/v1/subscriptions/me'):Promise.resolve([])]);
    setOffers(o);setConfig(c);setMemberships(m);setEnrollments(e);setPlans(p);setHistory(h);
  }catch(e){setError(e instanceof Error?e.message:'Unable to load subscriptions');}
  finally{setLoading(false);}
 },[user?.id]);
 useEffect(()=>{void load();},[load]);
 async function start(offering:Offer){
   if(!user||!consent||selected!==offering.id||!config.enabled||!config.providerConfigured)return;
   setBusy(true);setError('');
   try{
    let referenceId:string;
    if(offering.kind==='MEMBERSHIP'){
      const plan=plans.find(p=>p.id===offering.referenceId);
      if(!plan)throw new Error('Membership plan not available.');
      const previous=memberships.find(m=>m.planCode===plan.code&&m.status==='PENDING_PAYMENT');
      const record=previous||await api<Membership>('/api/v1/memberships',{method:'POST',body:JSON.stringify({planCode:plan.code})});
      referenceId=record.id;
    }else{
      const age=Number(participantAge);
      if(!participantName.trim()||!Number.isInteger(age)||age<3||age>100)throw new Error('Enter participant name and age (3–100).');
      const previous=enrollments.find(e=>e.batchId===offering.referenceId&&e.status==='PENDING_PAYMENT');
      const record=previous||await api<Enrollment>('/api/v1/training/batches/'+offering.referenceId+'/enrollments',
        {method:'POST',body:JSON.stringify({participantName:participantName.trim(),participantAge:age})});
      referenceId=record.id;
    }
    const result=await api<{checkoutUrl:string}>('/api/v1/subscriptions/start',
      {method:'POST',body:JSON.stringify({offeringId:offering.id,referenceId,consent:true})});
    if(!result.checkoutUrl.startsWith('/api/v1/public/subscriptions/checkout/'))throw new Error('Invalid checkout link');
    await WebBrowser.openBrowserAsync(API_URL+result.checkoutUrl,{showTitle:true});
    Alert.alert('Authorization status','Check your billing status. Benefits begin after the first captured payment is verified.');
    await load();
   }catch(e){setError(e instanceof Error?e.message:'Unable to start mandate.');}
   finally{setBusy(false);}
 }
 function cancel(id:string){
  Alert.alert('Cancel recurring billing','Stop future charges? Already paid access remains until its current end date.',[
    {text:'Keep',style:'cancel'},
    {text:'Cancel future billing',style:'destructive',onPress:()=>{void (async()=>{setBusy(true);setError('');try{await api('/api/v1/subscriptions/'+id+'/cancel',{method:'POST'});await load();}catch(e){setError(e instanceof Error?e.message:'Cancellation failed.')}finally{setBusy(false)}})();}}
  ]);
 }
 return <ScreenShell title="Recurring subscriptions">
 <ScrollView contentContainerStyle={styles.page} keyboardShouldPersistTaps="handled">
  <Text style={styles.title}>Memberships & training installments</Text>
  <Text style={styles.description}>Recurring payments require explicit consent and a Razorpay mandate. One-time purchases remain available.</Text>
  {!config.enabled&&<Text style={styles.warning}>Recurring billing is disabled until Razorpay Subscriptions UAT passes. You cannot start new automatic charges.</Text>}
  {error!==''&&<Text style={styles.error}>{error}</Text>}
  {loading&&<ActivityIndicator color={BrandColors.primary}/>}
  {!user&&<Pressable style={styles.button} onPress={()=>router.push('/login')}><Text style={styles.buttonText}>Log in to subscribe</Text></Pressable>}
  {offers.map(o=><View key={o.id} style={styles.card}>
    <Text style={styles.tag}>{o.kind==='MEMBERSHIP'?'Membership renewal':'Training installments'}</Text>
    <Text style={styles.heading}>{o.title}</Text><Text style={styles.price}>{money(o.amountPaise)} / {o.period}</Text>
    <Text style={styles.description}>{o.totalCycles} charges in total · {money(o.amountPaise*o.totalCycles)} before additional applicable fees.</Text>
    {o.kind==='TRAINING'&&selected===o.id&&<><TextInput placeholder="Participant name" style={styles.input} value={participantName} onChangeText={setParticipantName}/>
      <TextInput placeholder="Participant age" keyboardType="number-pad" style={styles.input} value={participantAge} onChangeText={setParticipantAge}/></>}
    <Pressable style={styles.consent} onPress={()=>{if(selected!==o.id){setSelected(o.id);setConsent(true);}else setConsent(!consent);}}>
     <Switch value={selected===o.id&&consent} onValueChange={v=>{setSelected(o.id);setConsent(v);}}/>
     <Text style={styles.description}>I agree to {o.totalCycles} recurring charges of {money(o.amountPaise)} each and understand I can cancel future billing.</Text>
    </Pressable>
    <Pressable disabled={busy||!user||!config.enabled||!config.providerConfigured||selected!==o.id||!consent}
      style={[styles.button,(busy||!user||!config.enabled||!config.providerConfigured||selected!==o.id||!consent)&&styles.disabled]}
      onPress={()=>void start(o)}><Text style={styles.buttonText}>{busy&&selected===o.id?'Preparing…':'Authorize with Razorpay'}</Text></Pressable>
  </View>)}
  <Text style={styles.title}>My agreements</Text>
  {history.length===0&&<Text style={styles.description}>No recurring agreements created.</Text>}
  {history.map(a=><View key={a.id} style={styles.card}><Text style={styles.heading}>{a.title}</Text>
    <Text>Status: {a.status}</Text><Text>Paid cycles: {a.paidCycles}/{a.totalCycles}</Text>
    {!['CANCELLED','EXPIRED','COMPLETED'].includes(a.status)&&<Pressable style={styles.outline} disabled={busy} onPress={()=>cancel(a.id)}><Text style={styles.outlineText}>Cancel future billing</Text></Pressable>}
   </View>)}
  <Pressable onPress={()=>void load()} style={styles.outline}><Text style={styles.outlineText}>Refresh billing status</Text></Pressable>
 </ScrollView></ScreenShell>;
}
const styles=StyleSheet.create({
 page:{padding:17,paddingBottom:38,gap:12},title:{fontSize:23,fontWeight:'900',color:BrandColors.primary,marginVertical:7},
 description:{fontSize:13,color:BrandColors.textSecondary,lineHeight:20,flexShrink:1},warning:{padding:15,borderRadius:12,backgroundColor:'#FFF0DD',color:'#84521A',lineHeight:21},error:{color:'#AC2F18',fontWeight:'700'},
 card:{padding:17,backgroundColor:'white',borderRadius:16,borderColor:BrandColors.border,borderWidth:1,gap:10},
 tag:{fontWeight:'850',color:'#BB481B',fontSize:12},heading:{fontSize:19,fontWeight:'850',color:BrandColors.textPrimary},
 price:{fontSize:21,fontWeight:'900',color:BrandColors.primary},consent:{flexDirection:'row',alignItems:'center',gap:10},
 button:{backgroundColor:BrandColors.primary,borderRadius:10,padding:13,alignItems:'center'},buttonText:{color:'white',fontWeight:'800'},disabled:{opacity:.45},
 input:{borderWidth:1,borderColor:BrandColors.border,borderRadius:10,padding:12,fontSize:14,color:BrandColors.textPrimary},
 outline:{borderWidth:1,borderColor:BrandColors.primary,padding:12,borderRadius:10,alignItems:'center'},outlineText:{color:BrandColors.primary,fontWeight:'800'}
});
