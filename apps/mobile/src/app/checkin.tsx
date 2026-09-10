import { MaterialCommunityIcons } from '@expo/vector-icons';
import { router, useLocalSearchParams } from 'expo-router';
import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

import { BrandColors } from '@/constants/brand';
import { useAuth } from '@/features/auth/auth-context';
import { api, ApiError } from '@/lib/api';

type Booking={id:string;bookingNumber:string;sportName?:string;facilityName?:string;date:string;startTime:string;endTime:string;paymentStatus:string;paymentOption?:string;checkedInAt?:string|null};

export default function VenueCheckInScreen(){
  const params=useLocalSearchParams<{venue?:string}>();
  const {user,initializing}=useAuth();
  const venue=String(params.venue||'').toUpperCase();
  const [bookings,setBookings]=useState<Booking[]>([]);const [loading,setLoading]=useState(true);const [busy,setBusy]=useState('');const [message,setMessage]=useState('');

  const load=useCallback(async()=>{
    if(!user||!venue)return;
    try{setLoading(true);setMessage('');setBookings(await api<Booking[]>(`/api/v1/checkin/venue/${encodeURIComponent(venue)}`));}
    catch(e){setMessage(e instanceof ApiError?e.message:'Unable to verify this venue QR.');}
    finally{setLoading(false)}
  },[user,venue]);
  useEffect(()=>{void load()},[load]);

  async function checkIn(booking:Booking){
    if(booking.paymentStatus==='UNPAID'){setMessage('Payment due — please contact the operator before check-in.');return;}
    try{setBusy(booking.id);setMessage('');await api(`/api/v1/checkin/venue/${encodeURIComponent(venue)}/${encodeURIComponent(booking.bookingNumber)}`,{method:'POST'});setMessage(`Check-in successful for ${booking.bookingNumber}.`);await load();}
    catch(e){setMessage(e instanceof ApiError?e.message:'Unable to check in.');}
    finally{setBusy('')}
  }

  if(initializing)return <View style={styles.center}><ActivityIndicator color={BrandColors.accent}/></View>;
  if(!user)return <View style={styles.center}><MaterialCommunityIcons name="qrcode-scan" size={52} color={BrandColors.primary}/><Text style={styles.title}>Sign in to check in</Text><Text style={styles.muted}>Your venue QR will continue to work after you sign in.</Text><Pressable style={styles.primary} onPress={()=>router.push('/login')}><Text style={styles.primaryText}>Sign in</Text></Pressable></View>;
  if(venue!=='JNAPANAM')return <View style={styles.center}><Text style={styles.title}>Invalid venue QR</Text><Text style={styles.muted}>Please scan the QR displayed at Jnapanam Sports Zone.</Text></View>;

  return <ScrollView style={styles.screen} contentContainerStyle={styles.content}>
    <MaterialCommunityIcons name="qrcode-scan" size={46} color={BrandColors.primary}/><Text style={styles.eyebrow}>JNapanam Sports Zone</Text><Text style={styles.title}>Venue Check-in</Text><Text style={styles.muted}>Select today’s booking. Payment must be completed before entry.</Text>
    {!!message&&<Text style={styles.message}>{message}</Text>}
    {loading?<ActivityIndicator color={BrandColors.accent}/>:bookings.map(b=>{const paid=b.paymentStatus==='PAID'||b.paymentStatus==='NOT_REQUIRED';return <View key={b.id} style={styles.card}><View style={styles.row}><Text style={styles.number}>{b.bookingNumber}</Text><Text style={[styles.badge,paid?styles.paid:styles.due]}>{paid?'READY':'PAYMENT DUE'}</Text></View><Text style={styles.sport}>{b.sportName} · {b.facilityName}</Text><Text style={styles.muted}>{b.startTime?.slice(0,5)}–{b.endTime?.slice(0,5)}</Text><Pressable disabled={!!busy||!paid} style={[styles.primary,!paid&&styles.disabled]} onPress={()=>void checkIn(b)}><Text style={styles.primaryText}>{busy===b.id?'Checking in…':paid?'Check In':'Contact Operator'}</Text></Pressable></View>})}
    {!loading&&bookings.length===0&&<View style={styles.card}><Text style={styles.sport}>No eligible booking</Text><Text style={styles.muted}>Only today’s confirmed bookings appear here.</Text></View>}
    <Pressable style={styles.secondary} onPress={()=>router.replace('/')}><Text style={styles.secondaryText}>Back to Home</Text></Pressable>
  </ScrollView>;
}

const styles=StyleSheet.create({screen:{flex:1,backgroundColor:BrandColors.background},content:{padding:24,paddingTop:70,paddingBottom:100,gap:13},center:{flex:1,alignItems:'center',justifyContent:'center',padding:28,gap:12,backgroundColor:BrandColors.background},eyebrow:{fontSize:12,fontWeight:'900',letterSpacing:1,color:BrandColors.accent},title:{fontSize:29,fontWeight:'900',color:BrandColors.textPrimary},muted:{fontSize:14,lineHeight:21,color:BrandColors.textSecondary},message:{padding:12,borderRadius:12,backgroundColor:BrandColors.surface,color:BrandColors.textPrimary,fontWeight:'700'},card:{backgroundColor:BrandColors.surface,borderRadius:16,borderWidth:1,borderColor:BrandColors.border,padding:16,gap:8},row:{flexDirection:'row',justifyContent:'space-between',alignItems:'center',gap:8},number:{fontWeight:'900',color:BrandColors.accent},sport:{fontSize:17,fontWeight:'900',color:BrandColors.textPrimary},badge:{fontSize:11,fontWeight:'900',paddingHorizontal:9,paddingVertical:5,borderRadius:10},paid:{backgroundColor:'#E9F8EE',color:'#146C43'},due:{backgroundColor:'#FFF1E8',color:'#9A3412'},primary:{backgroundColor:BrandColors.primary,borderRadius:12,paddingVertical:12,alignItems:'center',marginTop:4},primaryText:{color:BrandColors.white,fontWeight:'900'},disabled:{opacity:.45},secondary:{borderWidth:1,borderColor:BrandColors.border,borderRadius:12,paddingVertical:12,alignItems:'center'},secondaryText:{color:BrandColors.primary,fontWeight:'900'}});
