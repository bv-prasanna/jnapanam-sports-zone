import { Tabs } from 'expo-router';
import { useAuth } from '@/features/auth/auth-context';
import { MobileHeader } from '@/components/mobile-header';

const screens = [
  'index','bookings','memberships','training','growth','tournaments','shop','notifications',
  'shop-counter','operations','profile','billing','subscriptions','login','register','forgot-password','explore',
  'checkin','admin'
] as const;

/** Screen routing is kept, but the overcrowded Android tab bar is replaced with one accessible hamburger drawer. */
export default function AppTabs() {
  const {initializing}=useAuth();
  if(initializing)return null;
  return <Tabs tabBar={()=>null} screenOptions={{header:()=> <MobileHeader/>,animation:'none'}}>
    {screens.map(screen=><Tabs.Screen name={screen} key={screen} options={{
      headerShown:!['login','register','forgot-password'].includes(screen),
      href:null
    }}/>)}
  </Tabs>;
}
