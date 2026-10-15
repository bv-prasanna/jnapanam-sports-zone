import { MaterialCommunityIcons } from '@expo/vector-icons';
import { Tabs } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { BrandColors } from '@/constants/brand';
import { useAuth } from '@/features/auth/auth-context';

const icons = {
  index: 'home-outline',
  bookings: 'calendar-check-outline',
  memberships: 'card-account-details-outline',
  training: 'whistle-outline',
  tournaments: 'trophy-outline',
  shop: 'shopping-outline',
  notifications: 'bell-outline',
  operations: 'clipboard-account-outline',
  admin: 'view-dashboard-outline',
  profile: 'account-outline',
} as const;

export default function AppTabs() {
  const { t } = useTranslation();
  const { initializing, user } = useAuth();
  if (initializing) return null;
  const staff = !!user?.roles.some(r => ['SUPER_ADMIN','ADMIN','OPERATIONS'].includes(r));
  const admin = !!user?.roles.some(r => ['SUPER_ADMIN','ADMIN'].includes(r));
  return <Tabs screenOptions={({route})=>({
    headerShown:false,
    tabBarActiveTintColor:BrandColors.accent,
    tabBarInactiveTintColor:BrandColors.textSecondary,
    tabBarStyle:{height:70,paddingTop:7,paddingBottom:9,backgroundColor:BrandColors.surface,borderTopColor:BrandColors.border},
    tabBarLabelStyle:{fontSize:9,fontWeight:'800'},
    tabBarIcon:({color,size})=><MaterialCommunityIcons name={icons[route.name as keyof typeof icons]||'circle-outline'} size={size} color={color}/>
  })}>
    <Tabs.Screen name="index" options={{title:t('navigation.home')}}/>
    <Tabs.Screen name="bookings" options={{title:t('navigation.bookings'),href:staff?null:'/bookings'}}/>
    <Tabs.Screen name="memberships" options={{title:t('navigation.memberships'),href:staff?null:'/memberships'}}/>
    <Tabs.Screen name="training" options={{title:t('navigation.training'),href:staff?null:'/training'}}/>
    <Tabs.Screen name="tournaments" options={{title:t('navigation.tournaments'),href:staff?null:'/tournaments'}}/>
    <Tabs.Screen name="shop" options={{title:t('navigation.shop'),href:staff?null:'/shop'}}/>
    <Tabs.Screen name="notifications" options={{title:'Messages'}}/>
    <Tabs.Screen name="operations" options={{title:'Operations',href:staff?'/operations':null}}/>
    <Tabs.Screen name="admin" options={{title:'Admin',href:admin?'/admin':null}}/>
    <Tabs.Screen name="profile" options={{title:t('navigation.profile')}}/>
    <Tabs.Screen name="login" options={{href:null}}/>
    <Tabs.Screen name="register" options={{href:null}}/>
    <Tabs.Screen name="explore" options={{href:null}}/>
  </Tabs>;
}
