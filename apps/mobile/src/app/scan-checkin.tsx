import {MaterialCommunityIcons} from '@expo/vector-icons';
import {CameraView,useCameraPermissions} from 'expo-camera';
import {router} from 'expo-router';
import {useRef,useState} from 'react';
import {ActivityIndicator,Image,Pressable,ScrollView,StyleSheet,Text,View} from 'react-native';
import {BrandColors} from '@/constants/brand';
import {useAuth} from '@/features/auth/auth-context';
import {api,ApiError} from '@/lib/api';

type Scan={bookingNumber:string;customerName:string;sportName:string;facilityName:string;
  date:string;startTime:string;endTime:string;paymentStatus:string;checkedIn:boolean;profilePhotoUrl?:string|null};
type Identity={customerName:string;photoAvailable:boolean;documentAvailable:boolean;
  documentType?:string;documentStatus?:string};
export default function ScanCheckIn(){
 const {user}=useAuth();
 const allowed=!!user?.roles.some(r=>['OPERATIONS','ADMIN','SUPER_ADMIN'].includes(r));
 const [permission,requestPermission]=useCameraPermissions();
 const locked=useRef(false);
 const [scanned,setScanned]=useState('');
 const [booking,setBooking]=useState<Scan|null>(null);
 const [identity,setIdentity]=useState<Identity|null>(null);
 const [verified,setVerified]=useState(false);
 const [busy,setBusy]=useState(false);
 const [done,setDone]=useState(false);
 const [error,setError]=useState('');
 async function onScan(data:string){
   if(locked.current||!data.startsWith('JNZCI1.'))return;
   locked.current=true;setBusy(true);setError('');setVerified(false);setIdentity(null);
   try{
     const row=await api<Scan>('/api/v1/operations/check-in/qr/verify',{
       method:'POST',body:JSON.stringify({value:data})});
     const info=await api<Identity>('/api/v1/operations/bookings/'+encodeURIComponent(row.bookingNumber)+'/identity');
     setBooking(row);setIdentity(info);setScanned(data);
   }catch(e){setError(e instanceof ApiError?e.message:'Unable to verify the booking QR. Please try the manual booking list.');}
   finally{setBusy(false);}
 }
 function retry(){locked.current=false;setScanned('');setBooking(null);setIdentity(null);setVerified(false);setDone(false);setError('');}
 async function complete(){
   if(!booking||!scanned||!verified)return;
   setBusy(true);setError('');
   try{
     await api('/api/v1/operations/check-in/qr/confirm',{method:'POST',body:JSON.stringify({value:scanned,identityVerified:true})});
     setDone(true);
   }catch(e){setError(e instanceof ApiError?e.message:'Check-in could not be completed. Check live booking status.');}
   finally{setBusy(false);}
 }
 if(!allowed)return <View style={styles.center}><Text style={styles.title}>Operator access required</Text><Pressable onPress={()=>router.back()}><Text>Back</Text></Pressable></View>;
 return <ScrollView style={styles.screen} contentContainerStyle={styles.body}>
   <Text style={styles.eyebrow}>JNapanam operator check-in</Text>
   <Text style={styles.title}>Scan customer booking QR</Text>
   {!scanned&&!busy&&!permission?.granted&&<View style={styles.card}>
     <Text style={styles.note}>Camera permission is used only to scan the customer's booking QR code.</Text>
     <Pressable style={styles.primary} onPress={()=>void requestPermission()}><Text style={styles.white}>Allow camera</Text></Pressable>
   </View>}
   {!scanned&&permission?.granted&&!busy&&<CameraView style={styles.camera} facing="back"
     barcodeScannerSettings={{barcodeTypes:['qr']}} onBarcodeScanned={({data})=>void onScan(data)}/>}
   {busy&&<ActivityIndicator color={BrandColors.accent} size="large"/>}
   {!!error&&<Text style={styles.error} accessibilityRole="alert">{error}</Text>}
   {done?<View style={styles.result}>
     <MaterialCommunityIcons name="check-circle" color="#146C43" size={64}/>
     <Text style={styles.title}>Check-in successful</Text>
     <Text style={styles.note}>{booking?.customerName} · {booking?.bookingNumber}</Text>
     <Text style={styles.note}>Customer has been notified. The booking status is now Checked in.</Text>
     <Pressable style={styles.primary} onPress={retry}><Text style={styles.white}>Scan next booking</Text></Pressable>
   </View>:booking?<View style={styles.card}>
     <Text style={styles.eyebrow}>Server-verified booking</Text>
     <Text style={styles.customer}>{booking.customerName}</Text>
     <Text style={styles.note}>{booking.bookingNumber}</Text>
     <Text style={styles.note}>{booking.sportName} · {booking.facilityName}</Text>
     <Text style={styles.note}>{booking.date} · {booking.startTime.slice(0,5)}–{booking.endTime.slice(0,5)}</Text>
     <Text style={styles.paid}>✓ Payment: {booking.paymentStatus}</Text>
     {booking.profilePhotoUrl?<Image source={{uri:booking.profilePhotoUrl}} style={styles.photo}/>:<Text style={styles.error}>Photo missing. Use the manual identity verification flow.</Text>}
     <Text style={styles.note}>Uploaded document: {identity?.documentType||'Missing'} · {identity?.documentStatus||'Not reviewed'}</Text>
     <Pressable onPress={()=>setVerified(value=>!value)} style={styles.check}>
       <MaterialCommunityIcons name={verified?'checkbox-marked':'checkbox-blank-outline'} size={25} color={BrandColors.primary}/>
       <Text style={styles.checkLabel}>I matched the customer against the booking and their photo/document.</Text>
     </Pressable>
     <Pressable disabled={busy||!verified||!identity?.photoAvailable} style={[styles.primary,(!verified||!identity?.photoAvailable)&&styles.disabled]} onPress={()=>void complete()}>
       <Text style={styles.white}>Confirm QR check-in</Text>
     </Pressable>
     <Pressable style={styles.outline} onPress={retry}><Text style={styles.outlineText}>Scan another QR</Text></Pressable>
   </View>:null}
   {!done&&<Pressable style={styles.outline} onPress={()=>router.replace('/operations')}>
     <Text style={styles.outlineText}>Cannot scan? Open manual booking list</Text>
   </Pressable>}
 </ScrollView>;
}
const styles=StyleSheet.create({
 screen:{flex:1,backgroundColor:BrandColors.background},body:{padding:20,gap:13,paddingBottom:40},
 center:{flex:1,justifyContent:'center',alignItems:'center',gap:12},eyebrow:{color:BrandColors.accent,fontWeight:'900',letterSpacing:1.5,fontSize:12},
 title:{fontSize:25,fontWeight:'900',color:BrandColors.textPrimary},customer:{color:BrandColors.textPrimary,fontWeight:'900',fontSize:21},
 note:{color:BrandColors.textSecondary,fontSize:14,lineHeight:21},camera:{height:370,borderRadius:18,overflow:'hidden',backgroundColor:'#15200C'},
 card:{backgroundColor:'#fff',borderWidth:1,borderColor:BrandColors.border,borderRadius:18,padding:20,gap:12},
 primary:{padding:15,borderRadius:12,backgroundColor:BrandColors.primary,alignItems:'center'},white:{color:'#fff',fontWeight:'900'},
 outline:{padding:13,borderRadius:12,borderWidth:1,borderColor:BrandColors.primary,alignItems:'center'},
 outlineText:{color:BrandColors.primary,fontWeight:'800'},photo:{width:130,height:130,borderRadius:14,backgroundColor:'#f5f0e7'},
 paid:{color:'#146C43',fontWeight:'900'},check:{flexDirection:'row',gap:10,alignItems:'center'},checkLabel:{flex:1,fontWeight:'700',color:BrandColors.textPrimary},
 disabled:{opacity:.45},result:{backgroundColor:'#ecfdf3',borderRadius:18,padding:20,alignItems:'center',gap:12},error:{backgroundColor:'#fff0ed',color:'#a32323',padding:12,borderRadius:9}
});
