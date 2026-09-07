import { MaterialCommunityIcons } from '@expo/vector-icons';
import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  Pressable,
  RefreshControl,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';

import { BrandColors } from '@/constants/brand';
import { useAuth } from '@/features/auth/auth-context';
import { api, ApiError } from '@/lib/api';

type Booking = {
  id: string;
  bookingNumber: string;
  customerName?: string;
  customerEmail?: string;
  customerMobile?: string;
  sportName?: string;
  facilityName?: string;
  date?: string;
  startTime?: string;
  endTime?: string;
  status: string;
  paymentStatus?: string;
  checkedInAt?: string | null;
  checkedOutAt?: string | null;
};

const staffRoles = ['SUPER_ADMIN', 'ADMIN', 'OPERATIONS'];

export default function OperationsScreen() {
  const { user } = useAuth();
  const allowed = !!user?.roles.some((role) => staffRoles.includes(role));
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [query, setQuery] = useState('');
  const [busy, setBusy] = useState('');
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    if (!allowed) return;
    try {
      setError('');
      setBookings(await api<Booking[]>('/api/v1/admin/bookings'));
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'Unable to load bookings.');
    } finally {
      setRefreshing(false);
    }
  }, [allowed]);

  useEffect(() => { void load(); }, [load]);

  const visible = useMemo(() => {
    const value = query.trim().toLowerCase();
    if (!value) return bookings;
    return bookings.filter((booking) =>
      [booking.bookingNumber, booking.customerName, booking.customerMobile, booking.facilityName]
        .some((field) => field?.toLowerCase().includes(value)));
  }, [bookings, query]);

  async function run(booking: Booking, action: 'check-in' | 'check-out' | 'no-show' | 'cancel') {
    try {
      setBusy(booking.id + action);
      setError('');
      const path = action === 'check-in'
        ? `/api/v1/operations/bookings/${booking.bookingNumber}/check-in`
        : `/api/v1/operations/bookings/${booking.id}/${action}`;
      const body = action === 'check-in'
        ? { identityVerified: true, verificationNote: 'Identity verified by operations mobile app' }
        : action === 'no-show'
          ? { reason: 'Customer did not arrive within the permitted grace period' }
          : action === 'cancel'
            ? { reason: 'Cancelled by operations user' }
            : undefined;
      await api(path, { method: 'POST', body: body ? JSON.stringify(body) : undefined });
      await load();
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : `Unable to ${action.replace('-', ' ')} booking.`);
    } finally {
      setBusy('');
    }
  }

  function confirm(booking: Booking, action: 'check-in' | 'check-out' | 'no-show' | 'cancel') {
    const messages = {
      'check-in': 'Confirm that you verified the booked customer using an acceptable ID.',
      'check-out': 'Confirm that the customer has completed the facility session.',
      'no-show': 'Mark this customer as no-show? Use this only after the configured grace period.',
      cancel: 'Cancel this booking? Its history will be retained.',
    };
    Alert.alert(action.replace('-', ' ').toUpperCase(), messages[action], [
      { text: 'Back', style: 'cancel' },
      { text: 'Confirm', style: action === 'cancel' || action === 'no-show' ? 'destructive' : 'default',
        onPress: () => void run(booking, action) },
    ]);
  }

  if (!allowed) {
    return <View style={styles.center}><Text style={styles.title}>Operations access required</Text>
      <Text style={styles.muted}>Sign in with an Operations, Admin, or Super Admin account.</Text></View>;
  }

  return (
    <ScrollView style={styles.screen} contentContainerStyle={styles.content}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); void load(); }} />}>
      <View style={styles.heading}>
        <View><Text style={styles.eyebrow}>STAFF WORKSPACE</Text><Text style={styles.title}>Facility operations</Text></View>
        <Pressable style={styles.refresh} onPress={() => void load()}><MaterialCommunityIcons name="refresh" size={24} color={BrandColors.primary} /></Pressable>
      </View>
      <Text style={styles.muted}>Verify customers and manage arrivals, sessions, no-shows, and cancellations.</Text>
      <View style={styles.search}><MaterialCommunityIcons name="magnify" size={22} color={BrandColors.textSecondary} />
        <TextInput value={query} onChangeText={setQuery} placeholder="Booking number, customer, mobile or facility" style={styles.searchInput} /></View>
      {!!error && <Text style={styles.error}>{error}</Text>}
      <Text style={styles.count}>{visible.length} bookings</Text>
      {bookings.length === 0 && !error ? <ActivityIndicator color={BrandColors.accent} /> : visible.map((booking) => {
        const checkedIn = !!booking.checkedInAt;
        const checkedOut = !!booking.checkedOutAt;
        const active = booking.status === 'CONFIRMED';
        return <View key={booking.id} style={styles.card}>
          <View style={styles.cardTop}><Text style={styles.number}>{booking.bookingNumber}</Text><Text style={styles.status}>{booking.status}</Text></View>
          <Text style={styles.facility}>{booking.facilityName || 'Facility'}</Text>
          <Text style={styles.detail}>{booking.sportName || 'Sport'} · {booking.date || '—'} · {booking.startTime || '—'}–{booking.endTime || '—'}</Text>
          <Text style={styles.customer}>{booking.customerName || booking.customerEmail || 'Customer'} {booking.customerMobile ? `· ${booking.customerMobile}` : ''}</Text>
          <Text style={styles.detail}>Payment: {booking.paymentStatus || '—'}</Text>
          <View style={styles.actions}>
            {active && !checkedIn && <Action label="Verify & check in" icon="account-check-outline" busy={busy === booking.id + 'check-in'} onPress={() => confirm(booking, 'check-in')} />}
            {active && checkedIn && !checkedOut && <Action label="Check out" icon="logout-variant" busy={busy === booking.id + 'check-out'} onPress={() => confirm(booking, 'check-out')} />}
            {active && !checkedIn && <Action label="No-show" icon="account-off-outline" danger busy={busy === booking.id + 'no-show'} onPress={() => confirm(booking, 'no-show')} />}
            {active && <Action label="Cancel" icon="calendar-remove-outline" danger busy={busy === booking.id + 'cancel'} onPress={() => confirm(booking, 'cancel')} />}
          </View>
        </View>;
      })}
      {bookings.length > 0 && visible.length === 0 && <Text style={styles.empty}>No matching bookings.</Text>}
    </ScrollView>
  );
}

function Action({ label, icon, danger, busy, onPress }: { label: string; icon: keyof typeof MaterialCommunityIcons.glyphMap; danger?: boolean; busy: boolean; onPress(): void }) {
  return <Pressable disabled={busy} onPress={onPress} style={[styles.action, danger && styles.dangerAction]}>
    {busy ? <ActivityIndicator size="small" color={danger ? '#A32613' : BrandColors.primary} /> : <MaterialCommunityIcons name={icon} size={18} color={danger ? '#A32613' : BrandColors.primary} />}
    <Text style={[styles.actionText, danger && styles.dangerText]}>{label}</Text>
  </Pressable>;
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: BrandColors.background },
  content: { padding: 20, paddingTop: 52, paddingBottom: 110, gap: 14 },
  center: { flex: 1, padding: 28, alignItems: 'center', justifyContent: 'center', backgroundColor: BrandColors.background, gap: 8 },
  heading: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  eyebrow: { color: BrandColors.accent, fontSize: 12, fontWeight: '900', letterSpacing: 1.6 },
  title: { color: BrandColors.textPrimary, fontSize: 30, fontWeight: '900', marginTop: 4 },
  muted: { color: BrandColors.textSecondary, fontSize: 15, lineHeight: 22 },
  refresh: { width: 44, height: 44, borderRadius: 22, backgroundColor: BrandColors.surface, alignItems: 'center', justifyContent: 'center', borderWidth: 1, borderColor: BrandColors.border },
  search: { flexDirection: 'row', alignItems: 'center', backgroundColor: BrandColors.surface, borderWidth: 1, borderColor: BrandColors.border, borderRadius: 16, paddingHorizontal: 14 },
  searchInput: { flex: 1, minHeight: 52, marginLeft: 8, color: BrandColors.textPrimary },
  error: { color: '#A32613', backgroundColor: '#FFF0ED', borderRadius: 12, padding: 12 },
  count: { color: BrandColors.textSecondary, fontWeight: '800' },
  card: { backgroundColor: BrandColors.surface, borderWidth: 1, borderColor: BrandColors.border, borderRadius: 18, padding: 16, gap: 7 },
  cardTop: { flexDirection: 'row', justifyContent: 'space-between', gap: 10 },
  number: { color: BrandColors.accent, fontWeight: '900' },
  status: { color: BrandColors.primary, fontSize: 11, fontWeight: '900', backgroundColor: '#E9F3E6', paddingHorizontal: 9, paddingVertical: 4, borderRadius: 12 },
  facility: { color: BrandColors.textPrimary, fontSize: 19, fontWeight: '900' },
  customer: { color: BrandColors.textPrimary, fontWeight: '700', marginTop: 3 },
  detail: { color: BrandColors.textSecondary, lineHeight: 20 },
  actions: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginTop: 8 },
  action: { flexDirection: 'row', alignItems: 'center', gap: 6, borderWidth: 1, borderColor: BrandColors.primary, borderRadius: 12, paddingHorizontal: 11, paddingVertical: 9 },
  dangerAction: { borderColor: '#D86955', backgroundColor: '#FFF8F6' },
  actionText: { color: BrandColors.primary, fontSize: 12, fontWeight: '900' },
  dangerText: { color: '#A32613' },
  empty: { textAlign: 'center', color: BrandColors.textSecondary, padding: 30 },
});
