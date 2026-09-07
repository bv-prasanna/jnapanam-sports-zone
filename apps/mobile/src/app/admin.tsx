import { MaterialCommunityIcons } from '@expo/vector-icons';
import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, Pressable, RefreshControl, ScrollView, StyleSheet, Text, View } from 'react-native';

import { BrandColors } from '@/constants/brand';
import { useAuth } from '@/features/auth/auth-context';
import { api, ApiError } from '@/lib/api';

type Dashboard = Record<string, number | string | null | undefined>;
const roles = ['SUPER_ADMIN', 'ADMIN'];

const tiles: { key: string; label: string; icon: keyof typeof MaterialCommunityIcons.glyphMap }[] = [
  { key: 'users', label: 'Users', icon: 'account-group-outline' },
  { key: 'activeSports', label: 'Active sports', icon: 'basketball' },
  { key: 'activeFacilities', label: 'Facilities', icon: 'stadium-outline' },
  { key: 'bookings', label: 'Bookings', icon: 'calendar-check-outline' },
  { key: 'checkedInBookings', label: 'Checked in', icon: 'account-check-outline' },
  { key: 'activeMemberships', label: 'Memberships', icon: 'card-account-details-outline' },
  { key: 'trainingEnrollments', label: 'Training', icon: 'whistle-outline' },
  { key: 'activeTournaments', label: 'Tournaments', icon: 'trophy-outline' },
  { key: 'grossRevenuePaise', label: 'Gross revenue', icon: 'cash-multiple' },
  { key: 'refundsPaise', label: 'Refunds', icon: 'cash-refund' },
  { key: 'netRevenuePaise', label: 'Net revenue', icon: 'finance' },
];

export default function AdminScreen() {
  const { user } = useAuth();
  const allowed = !!user?.roles.some((role) => roles.includes(role));
  const [data, setData] = useState<Dashboard | null>(null);
  const [error, setError] = useState('');
  const [refreshing, setRefreshing] = useState(false);

  const load = useCallback(async () => {
    if (!allowed) return;
    try { setError(''); setData(await api<Dashboard>('/api/v1/admin/dashboard')); }
    catch (cause) { setError(cause instanceof ApiError ? cause.message : 'Unable to load dashboard.'); }
    finally { setRefreshing(false); }
  }, [allowed]);

  useEffect(() => { void load(); }, [load]);

  if (!allowed) return <View style={styles.center}><Text style={styles.title}>Admin access required</Text></View>;

  return <ScrollView style={styles.screen} contentContainerStyle={styles.content}
    refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); void load(); }} />}>
    <View style={styles.heading}><View><Text style={styles.eyebrow}>ADMIN MOBILE</Text><Text style={styles.title}>Dashboard</Text></View>
      <Pressable style={styles.refresh} onPress={() => void load()}><MaterialCommunityIcons name="refresh" size={24} color={BrandColors.primary} /></Pressable></View>
    <Text style={styles.muted}>A mobile overview for administrators. Detailed catalogue, user, finance, and content management remains available in the web console.</Text>
    {!!error && <Text style={styles.error}>{error}</Text>}
    {!data && !error ? <ActivityIndicator color={BrandColors.accent} /> : <View style={styles.grid}>{tiles.map((tile) => {
      const raw = data?.[tile.key];
      const value = tile.key.endsWith('Paise') ? `₹${((Number(raw) || 0) / 100).toLocaleString('en-IN')}` : String(raw ?? 0);
      return <View key={tile.key} style={styles.tile}><View style={styles.icon}><MaterialCommunityIcons name={tile.icon} size={25} color={BrandColors.primary} /></View>
        <Text style={styles.value}>{value}</Text><Text style={styles.label}>{tile.label}</Text></View>;
    })}</View>}
    <View style={styles.notice}><MaterialCommunityIcons name="shield-check-outline" size={25} color={BrandColors.primary} />
      <View style={{ flex: 1 }}><Text style={styles.noticeTitle}>Role-aware access</Text><Text style={styles.muted}>Operations staff see only facility workflows. Admin and Super Admin accounts also see this management overview.</Text></View></View>
  </ScrollView>;
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: BrandColors.background },
  content: { padding: 20, paddingTop: 52, paddingBottom: 110, gap: 16 },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: BrandColors.background },
  heading: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  eyebrow: { color: BrandColors.accent, fontSize: 12, fontWeight: '900', letterSpacing: 1.6 },
  title: { color: BrandColors.textPrimary, fontSize: 31, fontWeight: '900', marginTop: 4 },
  muted: { color: BrandColors.textSecondary, fontSize: 14, lineHeight: 21 },
  refresh: { width: 44, height: 44, borderRadius: 22, backgroundColor: BrandColors.surface, alignItems: 'center', justifyContent: 'center', borderWidth: 1, borderColor: BrandColors.border },
  grid: { flexDirection: 'row', flexWrap: 'wrap', gap: 12 },
  tile: { width: '47.8%', minHeight: 138, backgroundColor: BrandColors.surface, borderWidth: 1, borderColor: BrandColors.border, borderRadius: 18, padding: 15, justifyContent: 'space-between' },
  icon: { width: 45, height: 45, borderRadius: 14, backgroundColor: '#E9F3E6', justifyContent: 'center', alignItems: 'center' },
  value: { color: BrandColors.textPrimary, fontSize: 22, fontWeight: '900', marginTop: 10 },
  label: { color: BrandColors.textSecondary, fontSize: 13, fontWeight: '700' },
  error: { color: '#A32613', backgroundColor: '#FFF0ED', borderRadius: 12, padding: 12 },
  notice: { flexDirection: 'row', gap: 12, padding: 16, borderRadius: 18, backgroundColor: BrandColors.surface, borderWidth: 1, borderColor: BrandColors.border },
  noticeTitle: { color: BrandColors.textPrimary, fontWeight: '900', marginBottom: 4 },
});
