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
  operations: 'clipboard-account-outline',
  admin: 'view-dashboard-outline',
  profile: 'account-outline',
} as const;

export default function AppTabs() {
  const { t } = useTranslation();
  const { initializing, user } = useAuth();
  if (initializing) return null;

  const isStaff = !!user?.roles.some((role) => ['SUPER_ADMIN', 'ADMIN', 'OPERATIONS'].includes(role));
  const isAdmin = !!user?.roles.some((role) => ['SUPER_ADMIN', 'ADMIN'].includes(role));

  return (
    <Tabs screenOptions={({ route }) => ({
      headerShown: false,
      tabBarActiveTintColor: BrandColors.accent,
      tabBarInactiveTintColor: BrandColors.textSecondary,
      tabBarStyle: {
        height: 70,
        paddingTop: 7,
        paddingBottom: 9,
        backgroundColor: BrandColors.surface,
        borderTopColor: BrandColors.border,
      },
      tabBarLabelStyle: { fontSize: 11, fontWeight: '800' },
      tabBarIcon: ({ color, size }) => (
        <MaterialCommunityIcons
          name={icons[route.name as keyof typeof icons] || 'circle-outline'}
          size={size}
          color={color}
        />
      ),
    })}>
      <Tabs.Screen name="index" options={{ title: t('navigation.home') }} />
      <Tabs.Screen name="bookings" options={{ title: t('navigation.bookings'), href: isStaff ? null : '/bookings' }} />
      <Tabs.Screen name="training" options={{ title: t('navigation.training'), href: isStaff ? null : '/training' }} />
      <Tabs.Screen name="tournaments" options={{ title: t('navigation.tournaments'), href: isStaff ? null : '/tournaments' }} />
      <Tabs.Screen name="operations" options={{ title: 'Operations', href: isStaff ? '/operations' : null }} />
      <Tabs.Screen name="admin" options={{ title: 'Admin', href: isAdmin ? '/admin' : null }} />
      <Tabs.Screen name="profile" options={{ title: t('navigation.profile') }} />
      <Tabs.Screen name="login" options={{ href: null }} />
      <Tabs.Screen name="register" options={{ href: null }} />
      <Tabs.Screen name="explore" options={{ href: null }} />
    </Tabs>
  );
}
