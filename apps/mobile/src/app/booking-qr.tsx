import {MaterialCommunityIcons} from '@expo/vector-icons';
import {router,useLocalSearchParams} from 'expo-router';
import {useCallback,useEffect,useState} from 'react';
import {ActivityIndicator,Pressable,ScrollView,StyleSheet,Text,View} from 'react-native';
import QRCode from 'react-native-qrcode-svg';
import {BrandColors} from '@/constants/brand';
import {useAuth} from '@/features/auth/auth-context';
import {api,ApiError} from '@/lib/api';
import {getStoredValue,setStoredValue} from '@/lib/storage';

type Pass={bookingNumber:string;value:string;date:string;startTime:string;facilityName:string};
export default function CustomerBookingQr(){
 const {id}=useLocalSearchParams<{id:string}>();
 const {user,initializing}=useAuth();
 const [pass,setPass]=useState<string|null>(null);
 const [loading,setLoading]=useState(true);
 const [error,setError]=useState('');
 const [booking,setBooking]=useState('');
 const key='jnapanam.checkin-pass.'+(user?.id||'')+'.'+id;
 const load=useCallback(async(force=false)=>{
   if(!user||!id||Array.isArray(id))return;
   setLoading(true);setError('');
   try{
     if(!force){
       const cached=await getStoredValue(key);
       if(cached?.startsWith('JNZCI1.')){setPass(cached);return;}
     }
     // The same signed QR is returned each time: no per-open token creation.
     const response=await api<Pass>('/api/v1/bookings/'+encodeURIComponent(id)+'/checkin-pass');
     await setStoredValue(key,response.value);
     setBooking(response.bookingNumber);
     setPass(response.value);
   }catch(e){setError(e instanceof ApiError?e.message:'Unable to load your check-in pass. Verify payment and try again.');}
   finally{setLoading(false);}
 },[user?.id,id,key]);
 useEffect(()=>{void load();},[load]);
 if(initializing)return <View style={styles.center}><ActivityIndicator color={BrandColors.accent}/></View>;
 if(!user)return <View style={styles.center}><Text style={styles.title}>Sign in to show your booking QR</Text><Pressable style={styles.button} onPress={()=>router.push('/login')}><Text style={styles.white}>Sign in</Text></Pressable></View>;
 return <ScrollView style={styles.screen} contentContainerStyle={styles.content}>
   <MaterialCommunityIcons name="qrcode" size={43} color={BrandColors.primary}/>
   <Text style={styles.eyebrow}>JNapanam check-in pass</Text>
   <Text style={styles.title}>Show this QR to the operator</Text>
   <Text style={styles.subtitle}>Your pass is stored securely on this phone and reused for the same booking. The operator checks the live booking, payment and identity before allowing entry.</Text>
   {loading&&<ActivityIndicator color={BrandColors.accent}/>}
   {pass&&!loading&&<View style={styles.qrCard}>
     <QRCode value={pass} size={244} color="#17240F" backgroundColor="#FFFFFF" ecl="M"/>
     <Text style={styles.booking}>{booking||'Booking check-in QR'}</Text>
     <Text style={styles.help}>Valid only for this booked slot, from 30 minutes before start until its end. One successful check-in invalidates reuse.</Text>
   </View>}
   {!!error&&<Text style={styles.error} accessibilityRole="alert">{error}</Text>}
   <Pressable accessibilityRole="button" style={styles.outline} disabled={loading} onPress={()=>void load(true)}><Text style={styles.outlineText}>Refresh pass (after rescheduling)</Text></Pressable>
   <Pressable accessibilityRole="button" style={styles.button} onPress={()=>router.replace('/bookings')}><Text style={styles.white}>Back to My Bookings</Text></Pressable>
   <Text style={styles.subtitle}>Never share this QR screenshot with another person. If you cannot show it, the operator can verify your photo and document against the booking list.</Text>
 </ScrollView>;
}
const styles=StyleSheet.create({
 screen:{flex:1,backgroundColor:BrandColors.background},content:{padding:24,alignItems:'center',gap:14,paddingBottom:45},center:{flex:1,alignItems:'center',justifyContent:'center',padding:24,backgroundColor:BrandColors.background},
 eyebrow:{color:BrandColors.accent,fontSize:12,fontWeight:'900',letterSpacing:1.6},title:{fontSize:25,color:BrandColors.textPrimary,fontWeight:'900',textAlign:'center'},subtitle:{color:BrandColors.textSecondary,fontSize:14,textAlign:'center',lineHeight:21},qrCard:{padding:20,borderRadius:18,borderWidth:1,borderColor:BrandColors.border,backgroundColor:'#fff',alignItems:'center',gap:16},booking:{fontSize:14,fontWeight:'900',color:BrandColors.primary},help:{color:BrandColors.textSecondary,textAlign:'center',fontSize:12,maxWidth:250,lineHeight:19},button:{backgroundColor:BrandColors.primary,padding:14,borderRadius:12,alignItems:'center',minWidth:190},white:{color:'#fff',fontWeight:'900'},outline:{padding:13,borderRadius:12,borderWidth:1,borderColor:BrandColors.primary},outlineText:{color:BrandColors.primary,fontWeight:'800'},error:{color:'#B42318',backgroundColor:'#FEECEB',padding:12,borderRadius:10}
});
