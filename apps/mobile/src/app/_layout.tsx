import '@/i18n';
import { DarkTheme, DefaultTheme, ThemeProvider } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useColorScheme } from 'react-native';
import { AnimatedSplashOverlay } from '@/components/animated-icon';
import AppTabs from '@/components/app-tabs';
import {AppUpdateGate} from '@/components/app-update-gate';
import { AuthProvider } from '@/features/auth/auth-context';
import { getStoredValue } from '@/lib/storage';

void SplashScreen.preventAutoHideAsync();
function LanguageBootstrap(){
  const {i18n}=useTranslation();
  useEffect(()=>{void getStoredValue('jnapanam.language').then(language=>{
    if(language==='en'||language==='kn')void i18n.changeLanguage(language);
  });},[i18n]);
  return null;
}
export default function RootLayout(){
  const scheme=useColorScheme();
  return <ThemeProvider value={scheme==='dark'?DarkTheme:DefaultTheme}>
    <AuthProvider><LanguageBootstrap/><AnimatedSplashOverlay/><AppUpdateGate><AppTabs/></AppUpdateGate></AuthProvider>
  </ThemeProvider>;
}
