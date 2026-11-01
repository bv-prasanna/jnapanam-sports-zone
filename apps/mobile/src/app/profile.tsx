import {router, useFocusEffect} from 'expo-router';
import {useCallback, useEffect, useState} from 'react';
import {useTranslation} from 'react-i18next';
import {Image, Linking, Pressable, StyleSheet, Text, TextInput, View} from 'react-native';
import {enableEventLocation} from '@/lib/event-location';
import {EmptyState,ScreenShell} from '@/components/screen-shell';
import {BrandColors} from '@/constants/brand';
import {useAuth} from '@/features/auth/auth-context';
import {api,userFriendlyError,User} from '@/lib/api';

type ProfileData={displayName:string;mobile:string;addressLine:string;city:string;state:string;postalCode:string;profilePhotoUrl:string};
const userValues=(user:User):ProfileData=>({
 displayName:user.displayName||'',mobile:user.mobile||'',addressLine:user.addressLine||'',
 city:user.city||'',state:user.state||'',postalCode:user.postalCode||'',profilePhotoUrl:user.profilePhotoUrl||''
});
export default function ProfileScreen(){
 const {t}=useTranslation();
 const {user,logout,updateUser,refreshProfile}=useAuth();
 const [form,setForm]=useState<ProfileData>({displayName:'',mobile:'',addressLine:'',city:'',state:'',postalCode:'',profilePhotoUrl:''});
 const [saving,setSaving]=useState(false);
 const [feedback,setFeedback]=useState('');
 const [error,setError]=useState('');
 const [proofStatus,setProofStatus]=useState('');
 const [geoStatus,setGeoStatus]=useState(t('mobileProfile.locationDisabled'));
 const [securityOpen,setSecurityOpen]=useState(false);
 const [currentPassword,setCurrentPassword]=useState('');
 const [newPassword,setNewPassword]=useState('');
 const [confirmPassword,setConfirmPassword]=useState('');
 const [changingPassword,setChangingPassword]=useState(false);
 const [passwordError,setPasswordError]=useState('');
 const [photoFailed,setPhotoFailed]=useState(false);
 useEffect(()=>{if(user){setForm(userValues(user));setPhotoFailed(false)}},[user]);
 useFocusEffect(useCallback(()=>{
   if(!user)return;
   let active=true;
   void refreshProfile().catch(()=>{});
   void api<{status:string}>('/api/v1/profile/address-proof').then(v=>{if(active)setProofStatus(v.status)}).catch(()=>{if(active)setProofStatus('')});
   return()=>{active=false};
 },[Boolean(user)]));
 function change(key:keyof ProfileData,value:string){setForm(s=>({...s,[key]:value}));setError('');setFeedback('')}
 async function save(){
   if(!user||saving)return;
   if(form.mobile.trim()&&!/^[0-9]{10}$/.test(form.mobile.trim())){setError(t('auth.validMobile'));return;}
   try{
     setSaving(true);setFeedback('');setError('');
     const updated=await api<User>('/api/v1/profile/me',{method:'PUT',body:JSON.stringify({
       displayName:form.displayName.trim()||null,
       profilePhotoUrl:form.profilePhotoUrl.trim()||null,
       mobile:form.mobile.trim(),addressLine:form.addressLine.trim(),city:form.city.trim(),
       state:form.state.trim(),postalCode:form.postalCode.trim()
     })});
     updateUser(updated);setFeedback(t('mobileProfile.saved'));
   }catch(e){setError(userFriendlyError(e,t('errors.generic')))}
   finally{setSaving(false)}
 }
 async function changePassword(){
   if(changingPassword)return;
   setPasswordError('');
   if(newPassword.length<12){setPasswordError(t('mobileProfile.passwordTooShort'));return;}
   if(newPassword!==confirmPassword){setPasswordError(t('mobileProfile.passwordsDontMatch'));return;}
   setChangingPassword(true);
   try{
     await api<void>('/api/v1/profile/password',{method:'PUT',body:JSON.stringify({currentPassword,newPassword})});
     setCurrentPassword('');setNewPassword('');setConfirmPassword('');
     await logout();router.replace('/login');
   }catch(e){setPasswordError(userFriendlyError(e,t('errors.generic')))}
   finally{setChangingPassword(false)}
 }
 async function openWebProfile(){
   try{await Linking.openURL('https://jnapanam.in/profile')}catch{setError(t('errors.generic'))}
 }
 if(!user)return <ScreenShell title={t('navigation.profile')}>
   <EmptyState icon="account-circle-outline" title={t('auth.loginRequired')} description={t('profile.loginDescription')}/>
   <Pressable style={styles.primary} onPress={()=>router.push('/login')}><Text style={styles.primaryText}>{t('auth.login')}</Text></Pressable>
 </ScreenShell>;
 const Field=({field,label,keyboard='default',limit=150}:{field:keyof ProfileData;label:string;keyboard?:'default'|'numeric'|'phone-pad';limit?:number})=>
   <View style={styles.field}><Text style={styles.label}>{label}</Text>
     <TextInput accessibilityLabel={label} style={styles.input} value={form[field]}
       keyboardType={keyboard} maxLength={limit} autoCapitalize={field==='mobile'||field==='postalCode'?'none':'sentences'}
       onChangeText={value=>change(field,value)}/></View>;
 return <ScreenShell title={t('navigation.profile')}>
   <View style={styles.card}>
     <View style={styles.identity}>
       {form.profilePhotoUrl&&!photoFailed?<Image source={{uri:form.profilePhotoUrl}} style={{width:64,height:64,borderRadius:32,backgroundColor:BrandColors.background}} onError={()=>setPhotoFailed(true)}/>:
         <View style={styles.initials}><Text style={styles.initialsText}>{(user.displayName||user.fullName).split(/\s+/).map(w=>w[0]).join('').slice(0,2).toUpperCase()}</Text></View>}
       <View style={styles.identityCopy}><Text style={styles.name}>{user.displayName||user.fullName}</Text><Text style={styles.subText}>{user.email}</Text>
         <Text style={styles.status}>{t('profile.status')}: {user.status}</Text></View>
     </View>
     <Pressable accessibilityRole="button" style={styles.outline} onPress={()=>void openWebProfile()}>
       <Text style={styles.outlineText}>{t('mobileProfile.changePhoto')}</Text>
     </Pressable>
     <Text style={styles.hint}>{t('mobileProfile.photoHelp')}</Text>
   </View>
   <View style={styles.card}>
     <Text style={styles.sectionTitle}>{t('mobileProfile.account')}</Text>
     <View style={styles.field}><Text style={styles.label}>{t('mobileProfile.fullName')}</Text>
       <TextInput accessibilityLabel={t('mobileProfile.fullName')} editable={false} style={[styles.input,styles.readOnly]} value={user.fullName}/></View>
     <Field field="displayName" label={t('mobileProfile.displayName')} limit={60}/>
     <View style={styles.field}><Text style={styles.label}>{t('mobileProfile.email')}</Text>
       <TextInput accessibilityLabel={t('mobileProfile.email')} editable={false} style={[styles.input,styles.readOnly]} value={user.email}/></View>
     <Field field="mobile" label={t('mobileProfile.mobile')} keyboard="phone-pad" limit={10}/>
     <Field field="addressLine" label={t('mobileProfile.address')} limit={250}/>
     <Field field="city" label={t('mobileProfile.city')} limit={100}/>
     <Field field="state" label={t('mobileProfile.state')} limit={100}/>
     <Field field="postalCode" label={t('mobileProfile.pin')} keyboard="numeric" limit={12}/>
     <Field field="profilePhotoUrl" label={t('mobileProfile.photo')} limit={500}/>
     {!!error&&<Text style={styles.error} accessibilityRole="alert">{error}</Text>}
     {!!feedback&&<Text style={styles.success} accessibilityRole="alert">{feedback}</Text>}
     <Pressable accessibilityRole="button" style={[styles.primary,saving&&styles.disabled]} disabled={saving} onPress={()=>void save()}>
       <Text style={styles.primaryText}>{saving?t('mobileProfile.saving'):t('mobileProfile.update')}</Text>
     </Pressable>
   </View>
   <View style={styles.card}>
     <Text style={styles.sectionTitle}>{t('mobileProfile.verification')}</Text>
     <Text style={styles.subText}>{t('mobileProfile.proofDescription')}</Text>
     {!!proofStatus&&<Text style={styles.status}>{t('mobileProfile.proofStatus')}: {proofStatus.replace(/_/g,' ')}</Text>}
     <Pressable style={styles.outline} onPress={()=>void openWebProfile()}>
       <Text style={styles.outlineText}>{t('mobileProfile.uploadProof')}</Text>
     </Pressable>
   </View>
   <View style={styles.card}>
     <Pressable accessibilityRole="button" accessibilityState={{expanded:securityOpen}} onPress={()=>setSecurityOpen(v=>!v)} style={styles.collapsible}>
       <Text style={styles.sectionTitle}>{t('mobileProfile.security')}</Text><Text style={styles.toggle}>{securityOpen?'−':'+'}</Text>
     </Pressable>
     {securityOpen&&<View>
       <Text style={styles.subText}>{t('mobileProfile.securityHelp')}</Text>
       <Text style={styles.label}>{t('mobileProfile.currentPassword')}</Text>
       <TextInput style={styles.input} secureTextEntry autoComplete="current-password" value={currentPassword} onChangeText={setCurrentPassword} editable={!changingPassword}/>
       <Text style={styles.label}>{t('mobileProfile.newPassword')}</Text>
       <TextInput style={styles.input} secureTextEntry autoComplete="new-password" value={newPassword} onChangeText={setNewPassword} editable={!changingPassword}/>
       <Text style={styles.label}>{t('mobileProfile.confirmPassword')}</Text>
       <TextInput style={styles.input} secureTextEntry autoComplete="new-password" value={confirmPassword} onChangeText={setConfirmPassword} editable={!changingPassword}/>
       {!!passwordError&&<Text style={styles.error}>{passwordError}</Text>}
       <Pressable style={[styles.primary,changingPassword&&styles.disabled]} disabled={changingPassword} onPress={()=>void changePassword()}>
         <Text style={styles.primaryText}>{t('mobileProfile.changePassword')}</Text>
       </Pressable>
     </View>}
   </View>
   <Pressable style={styles.outline} onPress={async()=>{
     const status=await enableEventLocation();
     setGeoStatus(status==='CAPTURED'?t('mobileProfile.locationEnabled'):status==='DENIED'?t('mobileProfile.locationDenied'):t('mobileProfile.locationUnavailable'));
   }}><Text style={styles.outlineText}>{t('mobileProfile.location')}</Text></Pressable>
   <Text style={styles.hint}>{geoStatus}</Text>
   <Pressable style={styles.outline} onPress={()=>router.push('/billing')}><Text style={styles.outlineText}>{t('mobileProfile.billing')}</Text></Pressable>
   <Pressable style={styles.logout} onPress={async()=>{await logout();router.replace('/')}}><Text style={styles.logoutText}>{t('mobileProfile.logout')}</Text></Pressable>
 </ScreenShell>;
}
const styles=StyleSheet.create({
 card:{backgroundColor:BrandColors.surface,borderColor:BrandColors.border,borderWidth:1,borderRadius:17,padding:17,marginBottom:15,gap:9},
 identity:{flexDirection:'row',alignItems:'center',gap:13},
 avatar:{width:64,height:64,borderRadius:32,backgroundColor:BrandColors.background},
 initials:{width:64,height:64,borderRadius:32,backgroundColor:BrandColors.primary,alignItems:'center',justifyContent:'center'},
 initialsText:{color:'#fff',fontSize:21,fontWeight:'900'},
 identityCopy:{flex:1,minWidth:0},name:{fontSize:19,fontWeight:'900',color:BrandColors.textPrimary},
 subText:{color:BrandColors.textSecondary,fontSize:13,lineHeight:20},
 status:{fontSize:12,color:BrandColors.success,fontWeight:'800',marginTop:5},
 sectionTitle:{fontSize:18,fontWeight:'900',color:BrandColors.textPrimary},
 field:{marginTop:8,gap:6},label:{color:BrandColors.textPrimary,fontWeight:'700',fontSize:13},
 input:{borderColor:BrandColors.border,borderWidth:1,borderRadius:10,paddingHorizontal:12,minHeight:47,color:BrandColors.textPrimary,fontSize:15,backgroundColor:'#fff'},
 readOnly:{backgroundColor:'#F0F0EB',color:BrandColors.textSecondary},
 primary:{minHeight:47,backgroundColor:BrandColors.accent,borderRadius:10,alignItems:'center',justifyContent:'center',padding:12,marginTop:12},
 primaryText:{color:'#fff',fontWeight:'900',fontSize:15},
 outline:{borderColor:BrandColors.border,borderWidth:1,borderRadius:10,minHeight:46,alignItems:'center',justifyContent:'center',padding:12,backgroundColor:BrandColors.surface},
 outlineText:{color:BrandColors.primary,fontSize:14,fontWeight:'800',textAlign:'center'},
 collapsible:{flexDirection:'row',alignItems:'center',justifyContent:'space-between',minHeight:47},
 toggle:{fontSize:24,color:BrandColors.accent,fontWeight:'900'},
 logout:{minHeight:47,alignItems:'center',justifyContent:'center',borderRadius:10,borderWidth:1,borderColor:BrandColors.accent,marginTop:10,padding:13},
 logoutText:{color:BrandColors.accent,fontWeight:'800'},
 hint:{color:BrandColors.textSecondary,fontSize:12,lineHeight:18,marginVertical:6},
 error:{color:'#A32613',padding:10,backgroundColor:'#FFF0ED',borderRadius:9,marginTop:8},
 success:{color:'#166B38',fontWeight:'700',padding:10,backgroundColor:'#E8F6EC',borderRadius:9},
 disabled:{opacity:.5}
});
