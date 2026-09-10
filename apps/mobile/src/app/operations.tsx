import { MaterialCommunityIcons } from '@expo/vector-icons';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { ActivityIndicator, Alert, Pressable, RefreshControl, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';

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
  paymentMode?: string;
  paymentMethod?: string;
  amountDue?: number;
  totalAmount?: number;
  checkedInAt?: string | null;
  checkedOutAt?: string | null;
};

type QueueState = 'PAYMENT_DUE' | 'READY' | 'CHECKED_IN';

const staffRoles = ['SUPER_ADMIN', 'ADMIN', 'OPERATIONS'];
const terminalStatuses = new Set(['CANCELLED', 'COMPLETED', 'NO_SHOW']);

function localDateValue(date = new Date()) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function queueState(booking: Booking): QueueState {
  if (booking.checkedInAt && !booking.checkedOutAt) return 'CHECKED_IN';
  return booking.paymentStatus === 'PAID' ? 'READY' : 'PAYMENT_DUE';
}

function paymentLabel(booking: Booking) {
  if (booking.paymentStatus === 'PAID') return 'PREPAID / PAID';
  const amount = booking.amountDue ?? booking.totalAmount;
  const suffix = typeof amount === 'number' ? ` · ₹${amount.toFixed(2)} DUE` : ' · PAYMENT DUE';
  return `PAY AT VENUE${suffix}`;
}

function queueLabel(state: QueueState) {
  if (state === 'PAYMENT_DUE') return 'WAITING FOR PAYMENT';
  if (state === 'READY') return 'READY FOR CHECK-IN';
  return 'CHECKED IN';
}

export default function OperationsScreen() {
  const { user } = useAuth();
  const allowed = !!user?.roles.some((role) => staffRoles.includes(role));
  const isAdmin = !!user?.roles.some((role) => ['SUPER_ADMIN', 'ADMIN'].includes(role));
  const today = useMemo(() => localDateValue(), []);
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [query, setQuery] = useState('');
  const [sportFilter, setSportFilter] = useState('ALL');
  const [facilityFilter, setFacilityFilter] = useState('ALL');
  const [busy, setBusy] = useState('');
  const [refreshing, setRefreshing] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    if (!allowed) return;
    try {
      setError('');
      const rows = await api<Booking[]>(`/api/v1/admin/bookings?date=${today}`);
      setBookings(rows.filter((booking) => booking.date === today && !terminalStatuses.has(booking.status)));
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'Unable to load today’s bookings.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [allowed, today]);

  useEffect(() => { void load(); }, [load]);

  const sports = useMemo(() => Array.from(new Set(bookings.map((booking) => booking.sportName).filter(Boolean) as string[])).sort(), [bookings]);
  const facilities = useMemo(() => Array.from(new Set(bookings
    .filter((booking) => sportFilter === 'ALL' || booking.sportName === sportFilter)
    .map((booking) => booking.facilityName).filter(Boolean) as string[])).sort(), [bookings, sportFilter]);

  useEffect(() => {
    if (facilityFilter !== 'ALL' && !facilities.includes(facilityFilter)) setFacilityFilter('ALL');
  }, [facilities, facilityFilter]);

  const visible = useMemo(() => {
    const value = query.trim().toLowerCase();
    return bookings
      .filter((booking) => sportFilter === 'ALL' || booking.sportName === sportFilter)
      .filter((booking) => facilityFilter === 'ALL' || booking.facilityName === facilityFilter)
      .filter((booking) => !value || [booking.bookingNumber, booking.customerName, booking.customerMobile, booking.facilityName, booking.sportName]
        .some((field) => field?.toLowerCase().includes(value)))
      .sort((a, b) => {
        const rank: Record<QueueState, number> = { PAYMENT_DUE: 0, READY: 1, CHECKED_IN: 2 };
        const stateDiff = rank[queueState(a)] - rank[queueState(b)];
        return stateDiff || (a.startTime || '').localeCompare(b.startTime || '');
      });
  }, [bookings, query, sportFilter, facilityFilter]);

  const paymentDue = visible.filter((booking) => queueState(booking) === 'PAYMENT_DUE').length;
  const ready = visible.filter((booking) => queueState(booking) === 'READY').length;
  const checkedIn = visible.filter((booking) => queueState(booking) === 'CHECKED_IN').length;

  type OperationAction = 'check-in' | 'check-out' | 'check-in/reverse' | 'no-show' | 'cancel' | 'cancel-and-refund';

  async function run(booking: Booking, action: OperationAction) {
    try {
      setBusy(booking.id + action);
      setError('');
      const path = action === 'check-in'
        ? `/api/v1/operations/bookings/${booking.bookingNumber}/check-in`
        : `/api/v1/operations/bookings/${booking.id}/${action}`;
      const body = action === 'check-in'
        ? { identityVerified: true, verificationNote: 'Identity verified by operations mobile app' }
        : ['no-show', 'cancel', 'cancel-and-refund'].includes(action)
          ? { reason: action === 'no-show' ? 'Customer did not arrive within the permitted grace period' : 'Cancelled by admin' }
          : undefined;
      await api(path, { method: 'POST', body: body ? JSON.stringify(body) : undefined });
      await load();
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : `Unable to ${action.replaceAll('-', ' ')} booking.`);
    } finally { setBusy(''); }
  }

  async function collectPayment(booking: Booking, paymentMethod: 'CASH' | 'UPI' | 'CARD') {
    try {
      setBusy(booking.id + 'payment');
      setError('');
      await api(`/api/v1/operations/bookings/${booking.id}/collect-payment`, {
        method: 'POST',
        body: JSON.stringify({ paymentMethod, reference: `${paymentMethod} collected by operations mobile` }),
      });
      await load();
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'Unable to collect onsite payment.');
    } finally { setBusy(''); }
  }

  function askPayment(booking: Booking) {
    Alert.alert('Collect payment before entry', `${paymentLabel(booking)}\n\nChoose the method only after receiving and verifying the payment.`, [
      { text: 'Cash', onPress: () => void collectPayment(booking, 'CASH') },
      { text: 'UPI', onPress: () => void collectPayment(booking, 'UPI') },
      { text: 'Card', onPress: () => void collectPayment(booking, 'CARD') },
      { text: 'Back', style: 'cancel' },
    ]);
  }

  function confirm(booking: Booking, action: OperationAction) {
    const messages: Record<OperationAction, string> = {
      'check-in': 'Confirm that payment is cleared and you verified the booked customer using an acceptable ID.',
      'check-out': 'Confirm that the customer has completed the facility session.',
      'check-in/reverse': 'Reverse this check-in? Use this only to correct an admin mistake.',
      'no-show': 'Mark this customer as no-show after the configured grace period?',
      'cancel': 'Cancel this unpaid booking?',
      'cancel-and-refund': 'Cancel this paid booking and initiate its refund?',
    };
    Alert.alert(action.replaceAll('-', ' ').replace('/', ' ').toUpperCase(), messages[action], [
      { text: 'Back', style: 'cancel' },
      { text: 'Confirm', style: ['cancel', 'cancel-and-refund', 'no-show'].includes(action) ? 'destructive' : 'default', onPress: () => void run(booking, action) },
    ]);
  }

  if (!allowed) {
    return <View style={styles.center}><Text style={styles.title}>Operations access required</Text><Text style={styles.muted}>Sign in with an Operations, Admin, or Super Admin account.</Text></View>;
  }

  return <ScrollView style={styles.screen} contentContainerStyle={styles.content}
    refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); void load(); }} />}>
    <View style={styles.heading}>
      <View><Text style={styles.eyebrow}>TODAY · {today}</Text><Text style={styles.title}>Facility operations</Text></View>
      <Pressable style={styles.refresh} onPress={() => void load()}><MaterialCommunityIcons name="refresh" size={24} color={BrandColors.primary} /></Pressable>
    </View>
    <Text style={styles.muted}>Today’s queue for the selected sport/facility. Payment must be cleared before the customer can enter.</Text>

    <Text style={styles.filterTitle}>Sport</Text>
    <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.filters}>
      {['ALL', ...sports].map((sport) => <FilterChip key={sport} label={sport === 'ALL' ? 'All sports' : sport} selected={sportFilter === sport} onPress={() => { setSportFilter(sport); setFacilityFilter('ALL'); }} />)}
    </ScrollView>
    <Text style={styles.filterTitle}>Facility</Text>
    <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.filters}>
      {['ALL', ...facilities].map((facility) => <FilterChip key={facility} label={facility === 'ALL' ? 'All facilities' : facility} selected={facilityFilter === facility} onPress={() => setFacilityFilter(facility)} />)}
    </ScrollView>

    <View style={styles.summaryRow}>
      <View style={styles.summary}><Text style={styles.summaryValue}>{paymentDue}</Text><Text style={styles.summaryLabel}>Payment due</Text></View>
      <View style={styles.summary}><Text style={styles.summaryValue}>{ready}</Text><Text style={styles.summaryLabel}>Ready</Text></View>
      <View style={styles.summary}><Text style={styles.summaryValue}>{checkedIn}</Text><Text style={styles.summaryLabel}>Checked in</Text></View>
    </View>
    <View style={styles.search}><MaterialCommunityIcons name="magnify" size={22} color={BrandColors.textSecondary} />
      <TextInput value={query} onChangeText={setQuery} placeholder="Booking no., customer, mobile, facility" style={styles.searchInput} /></View>
    {!!error && <Text style={styles.error}>{error}</Text>}
    <Text style={styles.count}>{visible.length} today’s bookings</Text>
    {loading ? <ActivityIndicator color={BrandColors.accent} /> : visible.length === 0 ? <Text style={styles.empty}>{query ? 'No matching booking for today.' : 'No active bookings for this sport/facility today.'}</Text> : visible.map((booking) => {
      const state = queueState(booking);
      const checkedInNow = !!booking.checkedInAt;
      const checkedOut = !!booking.checkedOutAt;
      const active = booking.status === 'CONFIRMED';
      const paid = booking.paymentStatus === 'PAID';
      return <View key={booking.id} style={styles.card}>
        <View style={styles.cardTop}><Text style={styles.number}>{booking.bookingNumber}</Text><Text style={styles.status}>{queueLabel(state)}</Text></View>
        <View style={[styles.paymentBadge, paid ? styles.paymentPaid : styles.paymentDue]}>
          <MaterialCommunityIcons name={paid ? 'cash-check' : 'cash-clock'} size={18} color={paid ? '#146C43' : '#9A3412'} />
          <Text style={[styles.paymentText, paid ? styles.paymentPaidText : styles.paymentDueText]}>{paymentLabel(booking)}</Text>
        </View>
        <Text style={styles.facility}>{booking.facilityName || 'Facility'}</Text>
        <Text style={styles.detail}>{booking.sportName || 'Sport'} · {booking.startTime || '—'}–{booking.endTime || '—'}</Text>
        <Text style={styles.customer}>{booking.customerName || booking.customerEmail || 'Customer'} {booking.customerMobile ? `· ${booking.customerMobile}` : ''}</Text>
        <View style={styles.actions}>
          {active && !paid && !checkedInNow && <Action label="Collect payment" icon="cash-check" busy={busy === booking.id + 'payment'} onPress={() => askPayment(booking)} />}
          {active && !checkedInNow && paid && <Action label="Verify & check in" icon="account-check-outline" busy={busy === booking.id + 'check-in'} onPress={() => confirm(booking, 'check-in')} />}
          {active && checkedInNow && !checkedOut && <Action label="Check out" icon="logout-variant" busy={busy === booking.id + 'check-out'} onPress={() => confirm(booking, 'check-out')} />}
          {active && !checkedInNow && <Action label="No-show" icon="account-off-outline" danger busy={busy === booking.id + 'no-show'} onPress={() => confirm(booking, 'no-show')} />}
          {isAdmin && active && checkedInNow && !checkedOut && <Action label="Reverse check-in" icon="undo-variant" busy={busy === booking.id + 'check-in/reverse'} onPress={() => confirm(booking, 'check-in/reverse')} />}
          {isAdmin && active && (paid
            ? <Action label="Cancel & refund" icon="cash-refund" danger busy={busy === booking.id + 'cancel-and-refund'} onPress={() => confirm(booking, 'cancel-and-refund')} />
            : <Action label="Cancel" icon="calendar-remove-outline" danger busy={busy === booking.id + 'cancel'} onPress={() => confirm(booking, 'cancel')} />)}
        </View>
      </View>;
    })}
  </ScrollView>;
}

function FilterChip({ label, selected, onPress }: { label: string; selected: boolean; onPress(): void }) {
  return <Pressable onPress={onPress} style={[styles.filterChip, selected && styles.filterChipSelected]}><Text style={[styles.filterChipText, selected && styles.filterChipTextSelected]}>{label}</Text></Pressable>;
}

function Action({ label, icon, danger, busy, onPress }: { label: string; icon: keyof typeof MaterialCommunityIcons.glyphMap; danger?: boolean; busy: boolean; onPress(): void }) {
  return <Pressable disabled={busy} onPress={onPress} style={[styles.action, danger && styles.dangerAction]}>
    {busy ? <ActivityIndicator size="small" color={danger ? '#A32613' : BrandColors.primary} /> : <MaterialCommunityIcons name={icon} size={18} color={danger ? '#A32613' : BrandColors.primary} />}
    <Text style={[styles.actionText, danger && styles.dangerText]}>{label}</Text>
  </Pressable>;
}

const styles = StyleSheet.create({
  screen:{flex:1,backgroundColor:BrandColors.background},content:{padding:20,paddingTop:52,paddingBottom:110,gap:14},center:{flex:1,padding:28,alignItems:'center',justifyContent:'center',backgroundColor:BrandColors.background,gap:8},
  heading:{flexDirection:'row',alignItems:'center',justifyContent:'space-between'},eyebrow:{color:BrandColors.accent,fontSize:12,fontWeight:'900',letterSpacing:1.3},title:{color:BrandColors.textPrimary,fontSize:30,fontWeight:'900',marginTop:4},muted:{color:BrandColors.textSecondary,fontSize:15,lineHeight:22},
  filterTitle:{color:BrandColors.textPrimary,fontSize:12,fontWeight:'900',marginBottom:-7},filters:{gap:8,paddingRight:10},filterChip:{borderWidth:1,borderColor:BrandColors.border,backgroundColor:BrandColors.surface,borderRadius:18,paddingHorizontal:13,paddingVertical:8},filterChipSelected:{borderColor:BrandColors.primary,backgroundColor:BrandColors.primary},filterChipText:{fontSize:12,fontWeight:'800',color:BrandColors.textSecondary},filterChipTextSelected:{color:BrandColors.white},
  refresh:{width:44,height:44,borderRadius:22,backgroundColor:BrandColors.surface,alignItems:'center',justifyContent:'center',borderWidth:1,borderColor:BrandColors.border},summaryRow:{flexDirection:'row',gap:8},summary:{flex:1,backgroundColor:BrandColors.surface,borderWidth:1,borderColor:BrandColors.border,borderRadius:14,padding:12},summaryValue:{fontSize:22,fontWeight:'900',color:BrandColors.primary},summaryLabel:{fontSize:11,fontWeight:'800',color:BrandColors.textSecondary,marginTop:2},
  search:{flexDirection:'row',alignItems:'center',backgroundColor:BrandColors.surface,borderWidth:1,borderColor:BrandColors.border,borderRadius:16,paddingHorizontal:14},searchInput:{flex:1,minHeight:52,marginLeft:8,color:BrandColors.textPrimary},error:{color:'#A32613',backgroundColor:'#FFF0ED',borderRadius:12,padding:12},count:{color:BrandColors.textSecondary,fontWeight:'800'},
  card:{backgroundColor:BrandColors.surface,borderWidth:1,borderColor:BrandColors.border,borderRadius:18,padding:16,gap:7},cardTop:{flexDirection:'row',justifyContent:'space-between',gap:10},number:{color:BrandColors.accent,fontWeight:'900'},status:{color:BrandColors.primary,fontSize:11,fontWeight:'900',backgroundColor:'#E9F3E6',paddingHorizontal:9,paddingVertical:4,borderRadius:12},paymentBadge:{flexDirection:'row',alignItems:'center',alignSelf:'flex-start',gap:6,borderRadius:10,paddingHorizontal:10,paddingVertical:7,borderWidth:1},paymentPaid:{backgroundColor:'#ECFDF3',borderColor:'#86D3A7'},paymentDue:{backgroundColor:'#FFF4E8',borderColor:'#F2B585'},paymentText:{fontSize:12,fontWeight:'900'},paymentPaidText:{color:'#146C43'},paymentDueText:{color:'#9A3412'},facility:{color:BrandColors.textPrimary,fontSize:19,fontWeight:'900'},customer:{color:BrandColors.textPrimary,fontWeight:'700',marginTop:3},detail:{color:BrandColors.textSecondary,lineHeight:20},actions:{flexDirection:'row',flexWrap:'wrap',gap:8,marginTop:8},action:{flexDirection:'row',alignItems:'center',gap:6,borderWidth:1,borderColor:BrandColors.primary,borderRadius:12,paddingHorizontal:11,paddingVertical:9},dangerAction:{borderColor:'#D86955',backgroundColor:'#FFF8F6'},actionText:{color:BrandColors.primary,fontSize:12,fontWeight:'900'},dangerText:{color:'#A32613'},empty:{textAlign:'center',color:BrandColors.textSecondary,padding:30}
});
