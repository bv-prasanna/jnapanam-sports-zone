import { getLocales } from 'expo-localization';
import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

import en from './locales/en';
import kn from './locales/kn';

const supportedLanguages = ['en', 'kn'];

const deviceLanguage = getLocales()[0]?.languageCode ?? 'en';

const selectedLanguage = supportedLanguages.includes(deviceLanguage)
  ? deviceLanguage
  : 'en';

i18n.use(initReactI18next).init({
  resources: {
    en: {
      translation: en,
    },
    kn: {
      translation: kn,
    },
  },
  lng: selectedLanguage,
  //lng: 'kn',
  fallbackLng: 'en',
  interpolation: {
    escapeValue: false,
  },
});

export default i18n;