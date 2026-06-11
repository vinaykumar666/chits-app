import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import en from './locales/en';
import te from './locales/te';
import hi from './locales/hi';
import kn from './locales/kn';
import ta from './locales/ta';

const STORAGE_KEY = 'ygc_locale';

export const supportedLocales = [
  { code: 'en', label: 'EN' },
  { code: 'hi', label: 'हि' },
  { code: 'te', label: 'తె' },
  { code: 'kn', label: 'ಕ' },
  { code: 'ta', label: 'த' },
] as const;

export type LocaleCode = (typeof supportedLocales)[number]['code'];

i18n.use(initReactI18next).init({
  resources: {
    en: { translation: en },
    te: { translation: te },
    hi: { translation: hi },
    kn: { translation: kn },
    ta: { translation: ta },
  },
  lng: localStorage.getItem(STORAGE_KEY) || 'en',
  fallbackLng: 'en',
  interpolation: { escapeValue: false },
});

i18n.on('languageChanged', (lng) => {
  localStorage.setItem(STORAGE_KEY, lng);
  document.documentElement.lang = lng;
});

export default i18n;

export function setLocale(code: LocaleCode) {
  i18n.changeLanguage(code);
}
