import { router } from 'expo-router';
import { useState } from 'react';
import { enableEventLocation } from '@/lib/event-location';
import { useTranslation } from 'react-i18next';
import { Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { EmptyState, ScreenShell } from '@/components/screen-shell';
import { BrandColors } from '@/constants/brand';
import { useAuth } from '@/features/auth/auth-context';
import { api } from '@/lib/api';

export default function ProfileScreen() {
  const { t } = useTranslation();
  const { user, logout } = useAuth();
  const [geoStatus, setGeoStatus] = useState('GPS not enabled for event audits');
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [changingPassword, setChangingPassword] = useState(false);
  const [passwordError, setPasswordError] = useState('');

  async function changePassword() {
    if (changingPassword) return;
    setPasswordError('');
    if (newPassword.length < 12) {
      setPasswordError('New password must contain at least 12 characters.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setPasswordError('New passwords do not match.');
      return;
    }
    setChangingPassword(true);
    try {
      await api<void>('/api/v1/profile/password', {
        method: 'PUT',
        body: JSON.stringify({ currentPassword, newPassword }),
      });
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
      await logout();
      router.replace('/login');
    } catch (error) {
      setPasswordError(error instanceof Error ? error.message : 'Unable to update password.');
    } finally {
      setChangingPassword(false);
    }
  }

  if (!user) {
    return <ScreenShell title={t('navigation.profile')}>
      <EmptyState icon="account-circle-outline" title={t('auth.loginRequired')} description={t('profile.loginDescription')} />
      <Pressable style={styles.primary} onPress={() => router.push('/login')}>
        <Text style={styles.primaryText}>{t('auth.login')}</Text>
      </Pressable>
    </ScreenShell>;
  }

  return <ScreenShell title={t('navigation.profile')}>
    <View style={styles.card}>
      <Text style={styles.name}>{user.fullName}</Text>
      <Text style={styles.detail}>{user.email}</Text>
      {user.mobile && <Text style={styles.detail}>{user.mobile}</Text>}
      <Text style={styles.status}>{t('profile.status')}: {user.status}</Text>
    </View>
    <View style={styles.securityCard}>
      <Text style={styles.sectionTitle}>Account security</Text>
      <Text style={styles.detail}>Change password. You will need to sign in again afterwards.</Text>
      <Text style={styles.label}>Current password</Text>
      <TextInput style={styles.input} secureTextEntry autoComplete="current-password"
        value={currentPassword} onChangeText={setCurrentPassword} editable={!changingPassword} />
      <Text style={styles.label}>New password</Text>
      <TextInput style={styles.input} secureTextEntry autoComplete="new-password"
        value={newPassword} onChangeText={setNewPassword} editable={!changingPassword} />
      <Text style={styles.label}>Confirm new password</Text>
      <TextInput style={styles.input} secureTextEntry autoComplete="new-password"
        value={confirmPassword} onChangeText={setConfirmPassword} editable={!changingPassword} />
      {!!passwordError && <Text style={styles.error}>{passwordError}</Text>}
      <Pressable style={[styles.primary,changingPassword && styles.disabled]} disabled={changingPassword} onPress={() => void changePassword()}>
        <Text style={styles.primaryText}>{changingPassword ? 'Updating…' : 'Change password'}</Text>
      </Pressable>
    </View>
    <Pressable style={styles.primary} onPress={async () => {
      const status = await enableEventLocation();
      setGeoStatus(status === 'CAPTURED' ? 'Location enabled while using app'
        : status === 'DENIED' ? 'Permission denied: events still logged without coordinates'
          : 'Device location unavailable');
    }}>
      <Text style={styles.primaryText}>Enable location for activity history</Text>
    </Pressable>
    <Text style={styles.detail}>{geoStatus}</Text>
    <Pressable style={styles.primary} onPress={() => router.push('/subscriptions')}><Text style={styles.primaryText}>Manage recurring subscriptions</Text></Pressable>
    <Pressable style={styles.logout} onPress={async () => { await logout(); router.replace('/'); }}>
      <Text style={styles.logoutText}>{t('auth.logout')}</Text>
    </Pressable>
  </ScreenShell>;
}

const styles = StyleSheet.create({
  card: { backgroundColor: '#fff', borderWidth: 1, borderColor: BrandColors.border, borderRadius: 18, padding: 20 },
  name: { fontSize: 22, fontWeight: '900', color: BrandColors.textPrimary },
  detail: { fontSize: 15, color: BrandColors.textSecondary, marginTop: 6 },
  status: { fontSize: 13, fontWeight: '800', color: BrandColors.success, marginTop: 14 },
  securityCard: { marginTop: 18, padding: 18, borderRadius: 18, backgroundColor: '#fff', borderWidth: 1, borderColor: BrandColors.border },
  sectionTitle: { color: BrandColors.primary, fontWeight: '900', fontSize: 19 },
  label: { color: BrandColors.textPrimary, fontWeight: '700', fontSize: 14, marginTop: 12, marginBottom: 6 },
  input: { borderWidth: 1, borderColor: BrandColors.border, borderRadius: 10, paddingHorizontal: 12, minHeight: 48, color: BrandColors.textPrimary },
  error: { color: '#A32613', fontWeight: '700', marginTop: 12 },
  primary: { backgroundColor: BrandColors.accent, padding: 14, borderRadius: 12, alignItems: 'center', marginTop: 14 },
  primaryText: { color: '#fff', fontWeight: '900' },
  disabled: { opacity: 0.55 },
  logout: { borderWidth: 1, borderColor: '#C7350A', padding: 14, borderRadius: 12, alignItems: 'center', marginTop: 18 },
  logoutText: { color: '#C7350A', fontWeight: '900' },
});
