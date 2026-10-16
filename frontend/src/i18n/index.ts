import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import tr from './tr.json';
import en from './en.json';

// TR is the default (pilot users are Turkish-speaking); keys follow the "Labels" tables in docs/ux/screens/*.md.
void i18n.use(initReactI18next).init({
  resources: { tr: { translation: tr }, en: { translation: en } },
  lng: 'tr',
  fallbackLng: 'en',
  interpolation: { escapeValue: false },
});

export default i18n;
