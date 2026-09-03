import { router } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { EmptyState, ScreenShell } from '@/components/screen-shell';
import { BrandColors } from '@/constants/brand';
import { useAuth } from '@/features/auth/auth-context';

export default function ProfileScreen(){
 const {t}=useTranslation();const {user,logout}=useAuth();
 if(!user)return <ScreenShell title={t('navigation.profile')}><EmptyState icon="account-circle-outline" title={t('auth.loginRequired')} description={t('profile.loginDescription')}/><Pressable style={styles.primary} onPress={()=>router.push('/login')}><Text style={styles.primaryText}>{t('auth.login')}</Text></Pressable></ScreenShell>;
 return <ScreenShell title={t('navigation.profile')}><View style={styles.card}><Text style={styles.name}>{user.fullName}</Text><Text style={styles.detail}>{user.email}</Text>{user.mobile&&<Text style={styles.detail}>{user.mobile}</Text>}<Text style={styles.status}>{t('profile.status')}: {user.status}</Text></View><Pressable style={styles.logout} onPress={async()=>{await logout();router.replace('/');}}><Text style={styles.logoutText}>{t('auth.logout')}</Text></Pressable></ScreenShell>;
}
const styles=StyleSheet.create({card:{backgroundColor:'#fff',borderWidth:1,borderColor:BrandColors.border,borderRadius:18,padding:20},name:{fontSize:22,fontWeight:'900',color:BrandColors.textPrimary},detail:{fontSize:15,color:BrandColors.textSecondary,marginTop:6},status:{fontSize:13,fontWeight:'800',color:BrandColors.success,marginTop:14},primary:{backgroundColor:BrandColors.accent,padding:14,borderRadius:12,alignItems:'center',marginTop:14},primaryText:{color:'#fff',fontWeight:'900'},logout:{borderWidth:1,borderColor:'#C7350A',padding:14,borderRadius:12,alignItems:'center',marginTop:18},logoutText:{color:'#C7350A',fontWeight:'900'}});
