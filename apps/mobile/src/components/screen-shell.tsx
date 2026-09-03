import { MaterialCommunityIcons } from '@expo/vector-icons';
import { PropsWithChildren } from 'react';
import { useTranslation } from 'react-i18next';
import { ActivityIndicator, ScrollView, StyleSheet, Text, View } from 'react-native';

import { LanguageSwitch } from '@/components/language-switch';
import { BrandColors } from '@/constants/brand';

export function ScreenShell({ title, children }: PropsWithChildren<{ title: string }>) {
  const { t } = useTranslation();
  return (
    <View style={styles.screen}>
      <View style={styles.header}>
        <View>
          <Text style={styles.brand}>{t('common.appName')}</Text>
          <Text style={styles.location}>{t('common.location')}</Text>
        </View>
        <LanguageSwitch />
      </View>
      <ScrollView contentContainerStyle={styles.content}>
        <Text style={styles.title}>{title}</Text>
        {children}
      </ScrollView>
    </View>
  );
}

export function LoadingScreen() {
  return <View style={styles.loading}><ActivityIndicator size="large" color={BrandColors.accent} /></View>;
}

export function EmptyState({ icon, title, description }: { icon: keyof typeof MaterialCommunityIcons.glyphMap; title: string; description: string }) {
  return <View style={styles.empty}>
    <MaterialCommunityIcons name={icon} size={42} color={BrandColors.accent} />
    <Text style={styles.emptyTitle}>{title}</Text>
    <Text style={styles.emptyDescription}>{description}</Text>
  </View>;
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: BrandColors.background },
  header: { backgroundColor: BrandColors.primary, paddingTop: 52, paddingBottom: 18, paddingHorizontal: 20, flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', gap: 12 },
  brand: { color: BrandColors.white, fontWeight: '900', fontSize: 17 },
  location: { color: '#DCE5CF', fontSize: 12, marginTop: 3 },
  content: { padding: 20, paddingBottom: 110, width: '100%', maxWidth: 900, alignSelf: 'center' },
  title: { color: BrandColors.textPrimary, fontSize: 28, fontWeight: '900', marginBottom: 20 },
  loading: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: BrandColors.background },
  empty: { backgroundColor: BrandColors.surface, borderColor: BrandColors.border, borderWidth: 1, borderRadius: 20, padding: 28, alignItems: 'center' },
  emptyTitle: { color: BrandColors.textPrimary, fontSize: 18, fontWeight: '800', marginTop: 13 },
  emptyDescription: { color: BrandColors.textSecondary, fontSize: 14, lineHeight: 20, textAlign: 'center', marginTop: 6 },
});
