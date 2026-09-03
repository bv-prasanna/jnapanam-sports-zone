import { router } from 'expo-router';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ActivityIndicator, KeyboardAvoidingView, Platform, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';

import { LanguageSwitch } from '@/components/language-switch';
import { BrandColors } from '@/constants/brand';
import { useAuth } from '@/features/auth/auth-context';

export default function RegisterScreen() {
  const { t } = useTranslation(); const { register } = useAuth();
  const [form,setForm]=useState({fullName:'',email:'',mobile:'',password:'',confirmPassword:''});
  const [error,setError]=useState(''); const [busy,setBusy]=useState(false);
  const set=(key:keyof typeof form)=>(value:string)=>setForm(v=>({...v,[key]:value}));

  async function submit(){
    const nameOk=/^[\p{L}][\p{L} .'-]{1,119}$/u.test(form.fullName.trim());
    if(!nameOk){setError(t('auth.validName'));return;}
    if(!/^\S+@\S+\.\S+$/.test(form.email.trim())){setError(t('auth.validEmail'));return;}
    if(form.mobile && !/^[0-9+ -]{10,20}$/.test(form.mobile)){setError(t('auth.validMobile'));return;}
    if(form.password.length<8){setError(t('auth.passwordLength'));return;}
    if(form.password!==form.confirmPassword){setError(t('auth.passwordMismatch'));return;}
    setBusy(true);setError('');
    try{await register(form);router.replace('/');}
    catch(e){setError(e instanceof Error?e.message:t('common.tryAgain'));}
    finally{setBusy(false);}
  }

  return <KeyboardAvoidingView style={styles.screen} behavior={Platform.OS==='ios'?'padding':undefined}>
    <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
      <View style={styles.card}><View style={styles.top}><Text style={styles.eyebrow}>{t('auth.joinCommunity')}</Text><LanguageSwitch /></View>
      <Text style={styles.title}>{t('auth.createAccount')}</Text><Text style={styles.subtitle}>{t('auth.registerDescription')}</Text>
      {(['fullName','email','mobile','password','confirmPassword'] as const).map(key=><View key={key}>
        <Text style={styles.label}>{t(`auth.${key}`)}</Text>
        <TextInput value={form[key]} onChangeText={set(key)} autoCapitalize={key==='fullName'?'words':'none'}
          keyboardType={key==='email'?'email-address':key==='mobile'?'phone-pad':'default'}
          secureTextEntry={key==='password'||key==='confirmPassword'} style={styles.input} />
      </View>)}
      {!!error&&<Text style={styles.error}>{error}</Text>}
      <Pressable onPress={submit} disabled={busy} style={[styles.button,busy&&styles.disabled]}>{busy?<ActivityIndicator color="#fff"/>:<Text style={styles.buttonText}>{t('auth.createAccount')}</Text>}</Pressable>
      <Pressable onPress={()=>router.replace('/login')}><Text style={styles.link}>{t('auth.haveAccount')}</Text></Pressable></View>
    </ScrollView>
  </KeyboardAvoidingView>;
}
const styles=StyleSheet.create({
  screen:{flex:1,backgroundColor:'#263712'},scroll:{padding:20,paddingVertical:40,flexGrow:1,justifyContent:'center'},
  card:{backgroundColor:'#FFFCF6',borderRadius:26,padding:24,width:'100%',maxWidth:560,alignSelf:'center'},
  top:{flexDirection:'row',justifyContent:'space-between',alignItems:'center'},eyebrow:{color:BrandColors.accent,fontSize:12,fontWeight:'900',letterSpacing:1.6},
  title:{fontSize:32,fontWeight:'900',color:BrandColors.textPrimary,marginTop:12},subtitle:{color:BrandColors.textSecondary,fontSize:14,lineHeight:20,marginTop:6,marginBottom:10},
  label:{fontSize:14,fontWeight:'800',color:BrandColors.textPrimary,marginTop:12,marginBottom:6},
  input:{borderWidth:1,borderColor:'#DCCCAD',backgroundColor:'#fff',borderRadius:13,paddingHorizontal:14,paddingVertical:12,fontSize:16},
  error:{color:'#B42318',backgroundColor:'#FEECEB',borderRadius:10,padding:11,marginTop:14},
  button:{backgroundColor:'#D23808',borderRadius:13,padding:15,alignItems:'center',marginTop:20},disabled:{opacity:.65},
  buttonText:{color:'#fff',fontWeight:'900',fontSize:16},link:{textAlign:'center',color:BrandColors.primary,fontWeight:'800',marginTop:18}
});
