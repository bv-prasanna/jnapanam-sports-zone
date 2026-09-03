import { Pressable, StyleSheet, Text, View } from 'react-native';
import { useTranslation } from 'react-i18next';

import { BrandColors } from '@/constants/brand';
import { setStoredValue } from '@/lib/storage';

export function LanguageSwitch() {
  const { i18n } = useTranslation();
  const selected = i18n.resolvedLanguage === 'kn' ? 'kn' : 'en';

  async function choose(language: 'en' | 'kn') {
    await i18n.changeLanguage(language);
    await setStoredValue('jnapanam.language', language);
  }

  return (
    <View style={styles.row}>
      {(['en', 'kn'] as const).map((language) => (
        <Pressable key={language} onPress={() => choose(language)}
          style={[styles.option, selected === language && styles.selected]}>
          <Text style={[styles.text, selected === language && styles.selectedText]}>
            {language === 'en' ? 'English' : 'ಕನ್ನಡ'}
          </Text>
        </Pressable>
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', backgroundColor: '#E9E4D8', borderRadius: 12, padding: 3 },
  option: { paddingHorizontal: 10, paddingVertical: 7, borderRadius: 9 },
  selected: { backgroundColor: BrandColors.primary },
  text: { color: BrandColors.textSecondary, fontSize: 12, fontWeight: '700' },
  selectedText: { color: BrandColors.white },
});
