import { router } from 'expo-router';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ActivityIndicator, Image, KeyboardAvoidingView, Platform, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';

import { LanguageSwitch } from '@/components/language-switch';
import { BrandColors } from '@/constants/brand';
import { useAuth } from '@/features/auth/auth-context';

export default function LoginScreen() {
  const { t } = useTranslation();
  const { login } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit() {
    if (!/^\S+@\S+\.\S+$/.test(email.trim()) || !password) {
      setError(t('auth.validCredentials'));
      return;
    }
    setBusy(true); setError('');
    try { await login(email, password); router.replace('/'); }
    catch (e) { setError(e instanceof Error ? e.message : t('common.tryAgain')); }
    finally { setBusy(false); }
  }

  return <KeyboardAvoidingView style={styles.screen} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
    <View style={styles.card}>
      <View style={styles.top}><Image source={require('@/assets/images/jnapanam-logo.png')} style={styles.logo} resizeMode="contain" /><LanguageSwitch /></View>
      <Text style={styles.eyebrow}>{t('auth.welcomeBack')}</Text>
      <Text style={styles.title}>{t('auth.login')}</Text>
      <Text style={styles.subtitle}>{t('auth.loginDescription')}</Text>
      <Text style={styles.label}>{t('auth.email')}</Text>
      <TextInput value={email} onChangeText={setEmail} autoCapitalize="none" keyboardType="email-address" autoComplete="email" style={styles.input} />
      <Text style={styles.label}>{t('auth.password')}</Text>
      <TextInput value={password} onChangeText={setPassword} secureTextEntry autoComplete="current-password" style={styles.input} />
      {!!error && <Text style={styles.error}>{error}</Text>}
      <Pressable onPress={submit} disabled={busy} style={[styles.button, busy && styles.disabled]}>
        {busy ? <ActivityIndicator color="#fff" /> : <Text style={styles.buttonText}>{t('auth.login')}</Text>}
      </Pressable>
      <Pressable onPress={() => router.push('/register')}><Text style={styles.link}>{t('auth.needAccount')}</Text></Pressable>
    </View>
  </KeyboardAvoidingView>;
}

const styles=StyleSheet.create({
  screen:{flex:1,backgroundColor:'#263712',justifyContent:'center',padding:20},
  card:{backgroundColor:'#FFFCF6',borderRadius:26,padding:24,width:'100%',maxWidth:520,alignSelf:'center'},
  top:{flexDirection:'row',alignItems:'center',justifyContent:'space-between'},logo:{width:112,height:88},
  eyebrow:{color:BrandColors.accent,fontSize:12,fontWeight:'900',letterSpacing:1.7,marginTop:18},
  title:{color:BrandColors.textPrimary,fontSize:34,fontWeight:'900',marginTop:5},
  subtitle:{color:BrandColors.textSecondary,fontSize:15,lineHeight:21,marginTop:6,marginBottom:18},
  label:{color:BrandColors.textPrimary,fontWeight:'800',fontSize:14,marginTop:12,marginBottom:7},
  input:{borderWidth:1,borderColor:'#DCCCAD',backgroundColor:'#fff',borderRadius:13,paddingHorizontal:14,paddingVertical:13,fontSize:16,color:BrandColors.textPrimary},
  error:{color:'#B42318',backgroundColor:'#FEECEB',borderRadius:10,padding:11,marginTop:14},
  button:{backgroundColor:'#D23808',borderRadius:13,padding:15,alignItems:'center',marginTop:20},
  disabled:{opacity:.65},buttonText:{color:'#fff',fontSize:16,fontWeight:'900'},link:{color:BrandColors.primary,textAlign:'center',fontWeight:'800',marginTop:18}
});
