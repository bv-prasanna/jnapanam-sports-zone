import { router } from 'expo-router';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  ActivityIndicator,
  Image,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';

import { LanguageSwitch } from '@/components/language-switch';
import { BrandColors } from '@/constants/brand';
import { useAuth } from '@/features/auth/auth-context';

export default function LoginScreen() {
  const { t } = useTranslation();
  const { login } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit() {
    if (!/^\S+@\S+\.\S+$/.test(email.trim()) || !password) {
      setError(t('auth.validCredentials'));
      return;
    }
    setBusy(true);
    setError('');
    try {
      await login(email, password);
      router.replace('/');
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : t('common.tryAgain'));
    } finally {
      setBusy(false);
    }
  }

  return (
    <KeyboardAvoidingView
      style={styles.screen}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      keyboardVerticalOffset={Platform.OS === 'ios' ? 0 : 18}
    >
      <ScrollView
        contentContainerStyle={styles.scroll}
        keyboardShouldPersistTaps="handled"
        showsVerticalScrollIndicator={false}
      >
        <View style={styles.card}>
          <View style={styles.top}>
            <Image
              source={require('@/assets/images/jnapanam-logo-mobile.jpg')}
              style={styles.logo}
              resizeMode="contain"
            />
            <LanguageSwitch />
          </View>
          <Text style={styles.eyebrow}>{t('auth.welcomeBack')}</Text>
          <Text style={styles.title}>{t('auth.login')}</Text>
          <Text style={styles.subtitle}>{t('auth.loginDescription')}</Text>
          <Text style={styles.label}>{t('auth.email')}</Text>
          <TextInput
            value={email}
            onChangeText={setEmail}
            autoCapitalize="none"
            keyboardType="email-address"
            autoComplete="email"
            returnKeyType="next"
            style={styles.input}
          />
          <Text style={styles.label}>{t('auth.password')}</Text>
          <TextInput
            value={password}
            onChangeText={setPassword}
            secureTextEntry
            autoComplete="current-password"
            returnKeyType="done"
            onSubmitEditing={() => void submit()}
            style={styles.input}
          />
          {!!error && <Text style={styles.error}>{error}</Text>}
          <Pressable
            onPress={() => void submit()}
            disabled={busy}
            style={[styles.button, busy && styles.disabled]}
          >
            {busy ? <ActivityIndicator color="#fff" /> : <Text style={styles.buttonText}>{t('auth.login')}</Text>}
          </Pressable>
          <Pressable onPress={() => router.push('/register')}>
            <Text style={styles.link}>{t('auth.needAccount')}</Text>
          </Pressable>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: BrandColors.primary },
  scroll: { flexGrow: 1, justifyContent: 'center', padding: 20, paddingVertical: 34 },
  card: {
    backgroundColor: BrandColors.surface,
    borderRadius: 26,
    padding: 24,
    width: '100%',
    maxWidth: 520,
    alignSelf: 'center',
  },
  top: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 12 },
  logo: { width: 100, height: 100, borderRadius: 12 },
  eyebrow: { color: BrandColors.accent, fontSize: 12, fontWeight: '900', letterSpacing: 1.7, marginTop: 14 },
  title: { color: BrandColors.textPrimary, fontSize: 34, fontWeight: '900', marginTop: 5 },
  subtitle: { color: BrandColors.textSecondary, fontSize: 15, lineHeight: 21, marginTop: 6, marginBottom: 13 },
  label: { color: BrandColors.textPrimary, fontWeight: '800', fontSize: 14, marginTop: 12, marginBottom: 7 },
  input: {
    borderWidth: 1,
    borderColor: BrandColors.border,
    backgroundColor: '#fff',
    borderRadius: 13,
    paddingHorizontal: 14,
    paddingVertical: 13,
    fontSize: 16,
    color: BrandColors.textPrimary,
  },
  error: { color: '#B42318', backgroundColor: '#FEECEB', borderRadius: 10, padding: 11, marginTop: 14 },
  button: { backgroundColor: BrandColors.accent, minHeight: 52, borderRadius: 13, alignItems: 'center', justifyContent: 'center', marginTop: 20 },
  disabled: { opacity: 0.65 },
  buttonText: { color: '#fff', fontSize: 16, fontWeight: '900' },
  link: { color: BrandColors.primary, textAlign: 'center', fontWeight: '800', marginTop: 18 },
});
