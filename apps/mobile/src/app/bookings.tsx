import { MaterialCommunityIcons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  ActivityIndicator,
  Alert,
  Modal,
  Pressable,
  RefreshControl,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';

import { EmptyState, ScreenShell } from '@/components/screen-shell';
import { BrandColors } from '@/constants/brand';
import { useAuth } from '@/features/auth/auth-context';
import { api } from '@/lib/api';
import { completePayment } from '@/lib/payments';

type Facility = {
  id: string;
  code: string;
  name: string;
  sportCode: string;
  sportName: string;
  locationLabel?: string;
  bookingPrice: number;
};

type Sport = { code: string; name: string };

type Slot = {
  id: string;
  startTime: string;
  endTime: string;
  available: number;
};

type Booking = {
  id: string;
  bookingNumber: string;
  sportName: string;
  facilityName: string;
  date: string;
  startTime: string;
  endTime: string;
  status: string;
  paymentOption?: string;
  paymentStatus: string;
};

type PaymentOrder = {
  id: string;
  provider: string;
};

const terminalStatuses = new Set(['CANCELLED', 'COMPLETED', 'NO_SHOW']);

function dateValue(offset: number) {
  const value = new Date();
  value.setDate(value.getDate() + offset);
  return value.toISOString().slice(0, 10);
}

function idempotencyKey() {
  return `mobile-${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

type SelectOption = { value: string; label: string; subtitle?: string };

function SelectField({
  label,
  placeholder,
  value,
  options,
  onChange,
  disabled,
}: {
  label: string;
  placeholder: string;
  value: string;
  options: SelectOption[];
  onChange(value: string): void;
  disabled?: boolean;
}) {
  const [open, setOpen] = useState(false);
  const selected = options.find((option) => option.value === value);
  return (
    <>
      <Text style={styles.fieldLabel}>{label}</Text>
      <Pressable
        disabled={disabled}
        onPress={() => setOpen(true)}
        style={[styles.selectField, disabled && styles.disabled]}
      >
        <View style={styles.selectCopy}>
          <Text style={[styles.selectValue, !selected && styles.placeholder]}>
            {selected?.label || placeholder}
          </Text>
          {!!selected?.subtitle && <Text style={styles.selectSubtitle}>{selected.subtitle}</Text>}
        </View>
        <MaterialCommunityIcons name="chevron-down" size={24} color={BrandColors.primary} />
      </Pressable>
      <Modal visible={open} transparent animationType="fade" onRequestClose={() => setOpen(false)}>
        <Pressable style={styles.modalBackdrop} onPress={() => setOpen(false)}>
          <View style={styles.optionSheet}>
            <Text style={styles.optionTitle}>{label}</Text>
            <ScrollView>
              {options.map((option) => (
                <Pressable
                  key={option.value}
                  style={[styles.optionRow, option.value === value && styles.optionSelected]}
                  onPress={() => {
                    onChange(option.value);
                    setOpen(false);
                  }}
                >
                  <View style={styles.selectCopy}>
                    <Text style={styles.optionLabel}>{option.label}</Text>
                    {!!option.subtitle && <Text style={styles.selectSubtitle}>{option.subtitle}</Text>}
                  </View>
                  {option.value === value && (
                    <MaterialCommunityIcons name="check-circle" size={22} color={BrandColors.accent} />
                  )}
                </Pressable>
              ))}
            </ScrollView>
          </View>
        </Pressable>
      </Modal>
    </>
  );
}

export default function BookingsScreen() {
  const { t, i18n } = useTranslation();
  const { user } = useAuth();
  const dates = useMemo(() => Array.from({ length: 7 }, (_, index) => dateValue(index + 1)), []);
  const [date, setDate] = useState(dates[0]);
  const [sports, setSports] = useState<Sport[]>([]);
  const [sportCode, setSportCode] = useState('');
  const [facilities, setFacilities] = useState<Facility[]>([]);
  const [facilityId, setFacilityId] = useState('');
  const [slots, setSlots] = useState<Slot[]>([]);
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [loading, setLoading] = useState(false);
  const [searching, setSearching] = useState(false);
  const [processing, setProcessing] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    if (!user) return;
    setLoading(true);
    setError('');
    try {
      const [availableSports, availableFacilities, myBookings] = await Promise.all([
        api<Sport[]>('/api/v1/public/sports'),
        api<Facility[]>('/api/v1/public/facilities'),
        api<Booking[]>('/api/v1/bookings/me'),
      ]);
      setSports(availableSports);
      setFacilities(availableFacilities);
      setBookings(myBookings);
      setSportCode((current) => current || availableSports[0]?.code || availableFacilities[0]?.sportCode || '');
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : t('common.tryAgain'));
    } finally {
      setLoading(false);
    }
  }, [t, user]);

  useEffect(() => {
    void load();
  }, [load]);

  const filteredFacilities = useMemo(
    () => facilities.filter((facility) => facility.sportCode === sportCode),
    [facilities, sportCode],
  );

  useEffect(() => {
    if (!filteredFacilities.some((facility) => facility.id === facilityId)) {
      setFacilityId(filteredFacilities[0]?.id || '');
      setSlots([]);
    }
  }, [facilityId, filteredFacilities]);

  async function findSlots() {
    if (!facilityId) {
      setError(t('bookings.selectFacility'));
      return;
    }
    setSearching(true);
    setError('');
    setMessage('');
    try {
      const result = await api<Slot[]>(
        `/api/v1/public/facilities/${facilityId}/slots?date=${date}`,
      );
      setSlots(result);
      if (!result.length) setMessage(t('bookings.noSlots'));
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : t('bookings.loadError'));
    } finally {
      setSearching(false);
    }
  }

  async function book(slotId: string, paymentOption: 'PAY_NOW' | 'PAY_AT_VENUE') {
    const key = `${slotId}-${paymentOption}`;
    setProcessing(key);
    setError('');
    setMessage('');
    try {
      const hold = await api<{ id: string }>('/api/v1/bookings/holds', {
        method: 'POST',
        body: JSON.stringify({ slotId, quantity: 1 }),
      });
      const booking = await api<Booking>('/api/v1/bookings/confirm', {
        method: 'POST',
        body: JSON.stringify({ holdId: hold.id, paymentOption }),
      });

      if (paymentOption === 'PAY_NOW') {
        await completePayment('BOOKING', booking.id);
        setMessage(t('bookings.paidSuccess', { number: booking.bookingNumber }));
      } else {
        setMessage(t('bookings.venueSuccess', { number: booking.bookingNumber }));
      }
      await Promise.all([load(), findSlots()]);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : t('bookings.bookError'));
    } finally {
      setProcessing('');
    }
  }

  function askCancel(booking: Booking) {
    Alert.alert(
      t('bookings.cancelTitle'),
      t('bookings.cancelQuestion', { number: booking.bookingNumber }),
      [
        { text: t('common.keep'), style: 'cancel' },
        {
          text: t('common.cancelBooking'),
          style: 'destructive',
          onPress: () => void cancelBooking(booking),
        },
      ],
    );
  }

  async function cancelBooking(booking: Booking) {
    setProcessing(`cancel-${booking.id}`);
    setError('');
    try {
      await api(`/api/v1/bookings/${booking.id}/cancel`, { method: 'POST' });
      setMessage(t('bookings.cancelled', { number: booking.bookingNumber }));
      await load();
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : t('bookings.cancelError'));
    } finally {
      setProcessing('');
    }
  }

  if (!user) {
    return (
      <ScreenShell title={t('navigation.bookings')}>
        <EmptyState
          icon="calendar-lock"
          title={t('auth.loginRequired')}
          description={t('bookings.loginDescription')}
        />
        <Pressable style={styles.primaryButton} onPress={() => router.push('/login')}>
          <Text style={styles.primaryButtonText}>{t('auth.login')}</Text>
        </Pressable>
      </ScreenShell>
    );
  }

  return (
    <ScreenShell title={t('navigation.bookings')}>
      <ScrollView
        horizontal={false}
        refreshControl={<RefreshControl refreshing={loading} onRefresh={load} />}
      >
        {!!error && <Text style={styles.error}>{error}</Text>}
        {!!message && <Text style={styles.message}>{message}</Text>}

        <Text style={styles.sectionTitle}>{t('bookings.bookFacility')}</Text>
        {loading && !sports.length ? (
          <ActivityIndicator color={BrandColors.accent} />
        ) : (
          <>
            <SelectField
              label={t('bookings.chooseSport')}
              placeholder={t('bookings.sportPlaceholder')}
              value={sportCode}
              options={sports.map((sport) => ({ value: sport.code, label: sport.name }))}
              onChange={(value) => {
                setSportCode(value);
                setFacilityId('');
                setSlots([]);
              }}
            />
            <SelectField
              label={t('bookings.chooseFacility')}
              placeholder={t('bookings.facilityPlaceholder')}
              value={facilityId}
              disabled={!sportCode}
              options={filteredFacilities.map((facility) => ({
                value: facility.id,
                label: facility.name,
                subtitle: `₹${Number(facility.bookingPrice).toLocaleString('en-IN')}${facility.locationLabel ? ` · ${facility.locationLabel}` : ''}`,
              }))}
              onChange={(value) => {
                setFacilityId(value);
                setSlots([]);
              }}
            />
            {!!sportCode && !filteredFacilities.length && (
              <Text style={styles.inlineNotice}>{t('bookings.noFacilities')}</Text>
            )}
          </>
        )}

        <Text style={styles.sectionTitle}>{t('bookings.chooseDate')}</Text>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.horizontal}>
          {dates.map((value) => {
            const parsed = new Date(`${value}T00:00:00`);
            return (
              <Pressable
                key={value}
                onPress={() => {
                  setDate(value);
                  setSlots([]);
                }}
                style={[styles.dateChip, date === value && styles.selectedChip]}
              >
                <Text style={[styles.dateDay, date === value && styles.selectedText]}>
                  {parsed.toLocaleDateString(i18n.language === 'kn' ? 'kn-IN' : 'en-IN', {
                    weekday: 'short',
                  })}
                </Text>
                <Text style={[styles.dateNumber, date === value && styles.selectedText]}>
                  {parsed.getDate()}
                </Text>
              </Pressable>
            );
          })}
        </ScrollView>

        <Pressable
          style={[styles.primaryButton, searching && styles.disabled]}
          disabled={searching}
          onPress={() => void findSlots()}
        >
          {searching ? (
            <ActivityIndicator color="#fff" />
          ) : (
            <Text style={styles.primaryButtonText}>{t('bookings.findSlots')}</Text>
          )}
        </Pressable>

        {!!slots.length && (
          <>
            <Text style={styles.sectionTitle}>{t('bookings.availableSlots')}</Text>
            <View style={styles.slotList}>
              {slots.map((slot) => (
                <View key={slot.id} style={styles.slotCard}>
                  <View style={styles.slotHeader}>
                    <MaterialCommunityIcons name="clock-outline" size={22} color={BrandColors.primary} />
                    <View style={styles.slotCopy}>
                      <Text style={styles.slotTime}>
                        {slot.startTime.slice(0, 5)}–{slot.endTime.slice(0, 5)}
                      </Text>
                      <Text style={styles.available}>
                        {t('bookings.availableCount', { count: slot.available })}
                      </Text>
                    </View>
                  </View>
                  <View style={styles.actionRow}>
                    <Pressable
                      disabled={!slot.available || !!processing}
                      style={[styles.payButton, (!slot.available || !!processing) && styles.disabled]}
                      onPress={() => void book(slot.id, 'PAY_NOW')}
                    >
                      {processing === `${slot.id}-PAY_NOW` ? (
                        <ActivityIndicator color="#fff" />
                      ) : (
                        <Text style={styles.payText}>{t('bookings.payNow')}</Text>
                      )}
                    </Pressable>
                    <Pressable
                      disabled={!slot.available || !!processing}
                      style={[styles.venueButton, (!slot.available || !!processing) && styles.disabled]}
                      onPress={() => void book(slot.id, 'PAY_AT_VENUE')}
                    >
                      {processing === `${slot.id}-PAY_AT_VENUE` ? (
                        <ActivityIndicator color={BrandColors.primary} />
                      ) : (
                        <Text style={styles.venueText}>{t('bookings.payAtVenue')}</Text>
                      )}
                    </Pressable>
                  </View>
                </View>
              ))}
            </View>
          </>
        )}

        <Text style={styles.sectionTitle}>{t('bookings.myBookings')}</Text>
        {!loading && !bookings.length ? (
          <EmptyState
            icon="calendar-clock"
            title={t('bookings.emptyTitle')}
            description={t('bookings.emptyDescription')}
          />
        ) : (
          <View style={styles.bookingList}>
            {bookings.map((booking) => (
              <View key={booking.id} style={styles.bookingCard}>
                <View style={styles.bookingTop}>
                  <View style={styles.bookingCopy}>
                    <Text style={styles.bookingNumber}>{booking.bookingNumber}</Text>
                    <Text style={styles.bookingFacility}>{booking.facilityName}</Text>
                  </View>
                  <Text style={styles.status}>{booking.status.replaceAll('_', ' ')}</Text>
                </View>
                <Text style={styles.bookingMeta}>
                  {booking.sportName} · {booking.date}
                </Text>
                <Text style={styles.bookingMeta}>
                  {booking.startTime.slice(0, 5)}–{booking.endTime.slice(0, 5)} ·{' '}
                  {booking.paymentStatus.replaceAll('_', ' ')}
                </Text>
                {!terminalStatuses.has(booking.status) && (
                  <Pressable
                    disabled={processing === `cancel-${booking.id}`}
                    style={styles.cancelButton}
                    onPress={() => askCancel(booking)}
                  >
                    <Text style={styles.cancelText}>{t('common.cancelBooking')}</Text>
                  </Pressable>
                )}
              </View>
            ))}
          </View>
        )}
      </ScrollView>
    </ScreenShell>
  );
}

const styles = StyleSheet.create({
  fieldLabel: {
    color: BrandColors.textPrimary,
    fontSize: 14,
    fontWeight: '900',
    marginTop: 12,
    marginBottom: 7,
  },
  selectField: {
    minHeight: 58,
    backgroundColor: BrandColors.surface,
    borderWidth: 1,
    borderColor: BrandColors.border,
    borderRadius: 14,
    paddingHorizontal: 15,
    paddingVertical: 10,
    flexDirection: 'row',
    alignItems: 'center',
  },
  selectCopy: { flex: 1 },
  selectValue: { color: BrandColors.textPrimary, fontSize: 16, fontWeight: '800' },
  placeholder: { color: BrandColors.textSecondary, fontWeight: '600' },
  selectSubtitle: { color: BrandColors.textSecondary, fontSize: 12, marginTop: 3 },
  modalBackdrop: {
    flex: 1,
    justifyContent: 'flex-end',
    backgroundColor: 'rgba(20, 28, 13, 0.55)',
  },
  optionSheet: {
    maxHeight: '65%',
    backgroundColor: BrandColors.surface,
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    padding: 20,
    paddingBottom: 34,
  },
  optionTitle: { color: BrandColors.textPrimary, fontSize: 20, fontWeight: '900', marginBottom: 12 },
  optionRow: {
    minHeight: 58,
    borderBottomWidth: 1,
    borderBottomColor: BrandColors.border,
    paddingVertical: 12,
    paddingHorizontal: 8,
    flexDirection: 'row',
    alignItems: 'center',
  },
  optionSelected: { backgroundColor: '#FFF0E7', borderRadius: 10 },
  optionLabel: { color: BrandColors.textPrimary, fontSize: 16, fontWeight: '800' },
  inlineNotice: { color: BrandColors.textSecondary, marginTop: 10 },
  sectionTitle: {
    color: BrandColors.textPrimary,
    fontSize: 18,
    fontWeight: '900',
    marginTop: 22,
    marginBottom: 12,
  },
  horizontal: { marginHorizontal: -2 },
  facilityCard: {
    width: 218,
    backgroundColor: BrandColors.surface,
    borderWidth: 1,
    borderColor: BrandColors.border,
    borderRadius: 17,
    padding: 16,
    marginRight: 11,
  },
  selectedCard: { borderColor: BrandColors.accent, borderWidth: 2 },
  sportLabel: { color: BrandColors.accent, fontSize: 11, fontWeight: '900', letterSpacing: 0.7 },
  facilityName: { color: BrandColors.textPrimary, fontSize: 17, fontWeight: '900', marginTop: 5 },
  facilityMeta: { color: BrandColors.textSecondary, fontSize: 13, marginTop: 8 },
  dateChip: {
    width: 62,
    alignItems: 'center',
    backgroundColor: BrandColors.surface,
    borderWidth: 1,
    borderColor: BrandColors.border,
    borderRadius: 15,
    paddingVertical: 11,
    marginRight: 9,
  },
  selectedChip: { backgroundColor: BrandColors.primary, borderColor: BrandColors.primary },
  dateDay: { color: BrandColors.textSecondary, fontSize: 11, fontWeight: '800' },
  dateNumber: { color: BrandColors.textPrimary, fontSize: 19, fontWeight: '900', marginTop: 3 },
  selectedText: { color: '#fff' },
  primaryButton: {
    backgroundColor: BrandColors.accent,
    minHeight: 50,
    borderRadius: 14,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 18,
    paddingHorizontal: 18,
  },
  primaryButtonText: { color: '#fff', fontSize: 15, fontWeight: '900' },
  disabled: { opacity: 0.55 },
  error: { color: '#B42318', backgroundColor: '#FEECEB', borderRadius: 12, padding: 12 },
  message: { color: '#246B3D', backgroundColor: '#E9F7EE', borderRadius: 12, padding: 12 },
  slotList: { gap: 11 },
  slotCard: {
    backgroundColor: BrandColors.surface,
    borderWidth: 1,
    borderColor: BrandColors.border,
    borderRadius: 17,
    padding: 16,
  },
  slotHeader: { flexDirection: 'row', alignItems: 'center' },
  slotCopy: { marginLeft: 10 },
  slotTime: { color: BrandColors.textPrimary, fontSize: 17, fontWeight: '900' },
  available: { color: BrandColors.success, fontSize: 13, marginTop: 2 },
  actionRow: { flexDirection: 'row', gap: 9, marginTop: 14 },
  payButton: {
    flex: 1,
    minHeight: 45,
    borderRadius: 12,
    backgroundColor: '#D23808',
    alignItems: 'center',
    justifyContent: 'center',
  },
  payText: { color: '#fff', fontWeight: '900' },
  venueButton: {
    flex: 1,
    minHeight: 45,
    borderRadius: 12,
    backgroundColor: '#FFF8ED',
    borderWidth: 1,
    borderColor: '#DCCCAD',
    alignItems: 'center',
    justifyContent: 'center',
  },
  venueText: { color: BrandColors.primary, fontWeight: '900' },
  bookingList: { gap: 11 },
  bookingCard: {
    backgroundColor: BrandColors.surface,
    borderWidth: 1,
    borderColor: BrandColors.border,
    borderRadius: 17,
    padding: 16,
  },
  bookingTop: { flexDirection: 'row', justifyContent: 'space-between', gap: 12 },
  bookingCopy: { flex: 1 },
  bookingNumber: { color: BrandColors.accent, fontSize: 12, fontWeight: '900' },
  bookingFacility: { color: BrandColors.textPrimary, fontSize: 17, fontWeight: '900', marginTop: 4 },
  status: {
    color: BrandColors.primary,
    backgroundColor: '#E8F2F8',
    borderRadius: 9,
    paddingHorizontal: 8,
    paddingVertical: 5,
    fontSize: 10,
    fontWeight: '900',
    alignSelf: 'flex-start',
  },
  bookingMeta: { color: BrandColors.textSecondary, fontSize: 13, marginTop: 7 },
  cancelButton: {
    alignSelf: 'flex-start',
    borderWidth: 1,
    borderColor: '#D23808',
    borderRadius: 10,
    paddingHorizontal: 13,
    paddingVertical: 8,
    marginTop: 13,
  },
  cancelText: { color: '#D23808', fontWeight: '900', fontSize: 12 },
});
