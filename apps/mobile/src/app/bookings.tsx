import { router } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { Pressable, StyleSheet, Text } from 'react-native';
import { EmptyState, ScreenShell } from '@/components/screen-shell';
import { BrandColors } from '@/constants/brand';
import { useAuth } from '@/features/auth/auth-context';

export default function BookingsScreen(){
 const {t}=useTranslation(); const {user}=useAuth();
 return <ScreenShell title={t('navigation.bookings')}>{!user?
  <><EmptyState icon="calendar-lock" title={t('auth.loginRequired')} description={t('bookings.loginDescription')}/><Pressable style={styles.button} onPress={()=>router.push('/login')}><Text style={styles.text}>{t('auth.login')}</Text></Pressable></>:
  <EmptyState icon="calendar-clock" title={t('bookings.emptyTitle')} description={t('bookings.emptyDescription')}/>}</ScreenShell>;
}
const styles=StyleSheet.create({button:{backgroundColor:BrandColors.accent,padding:14,borderRadius:12,alignItems:'center',marginTop:14},text:{color:'#fff',fontWeight:'900'}});
