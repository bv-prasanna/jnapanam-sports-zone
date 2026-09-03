import { MaterialCommunityIcons } from '@expo/vector-icons';
import { Tabs } from 'expo-router';
import { useTranslation } from 'react-i18next';

import { BrandColors } from '@/constants/brand';
import { useAuth } from '@/features/auth/auth-context';

const icons = {
  index: 'home-outline',
  bookings: 'calendar-check-outline',
  training: 'whistle-outline',
  tournaments: 'trophy-outline',
  profile: 'account-outline',
} as const;

export default function AppTabs() {
  const { t } = useTranslation();
  const { initializing } = useAuth();
  if (initializing) return null;

  return (
    <Tabs screenOptions={({ route }) => ({
      headerShown: false,
      tabBarActiveTintColor: '#C7350A',
      tabBarInactiveTintColor: BrandColors.textSecondary,
      tabBarStyle: { height: 68, paddingTop: 7, paddingBottom: 8, backgroundColor: '#FFFCF6', borderTopColor: '#E6DAC5' },
      tabBarLabelStyle: { fontSize: 11, fontWeight: '700' },
      tabBarIcon: ({ color, size }) => {
        const name = icons[route.name as keyof typeof icons] || 'circle-outline';
        return <MaterialCommunityIcons name={name} size={size} color={color} />;
      },
    })}>
      <Tabs.Screen name="index" options={{ title: t('navigation.home') }} />
      <Tabs.Screen name="bookings" options={{ title: t('navigation.bookings') }} />
      <Tabs.Screen name="training" options={{ title: t('navigation.training') }} />
      <Tabs.Screen name="tournaments" options={{ title: t('navigation.tournaments') }} />
      <Tabs.Screen name="profile" options={{ title: t('navigation.profile') }} />
      <Tabs.Screen name="login" options={{ href: null }} />
      <Tabs.Screen name="register" options={{ href: null }} />
      <Tabs.Screen name="explore" options={{ href: null }} />
    </Tabs>
  );
}
