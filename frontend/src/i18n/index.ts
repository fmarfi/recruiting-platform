import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import tr from './tr.json';
import en from './en.json';
import pipelineTr from '../pipeline/i18n/tr.json';
import pipelineEn from '../pipeline/i18n/en.json';
import networkTr from '../network/i18n/tr.json';
import networkEn from '../network/i18n/en.json';

// TR is the default (pilot users are Turkish-speaking); keys follow the "Labels" tables in docs/ux/screens/*.md.
void i18n.use(initReactI18next).init({
  resources: { tr: { translation: tr }, en: { translation: en } },
  lng: 'tr',
  fallbackLng: 'en',
  interpolation: { escapeValue: false },
});

// Feature strings live next to their screens; deep-merge so areas can share top-level keys (e.g. common).
for (const [lng, bundles] of [['tr', [pipelineTr, networkTr]], ['en', [pipelineEn, networkEn]]] as const) {
  for (const b of bundles) i18n.addResourceBundle(lng, 'translation', b, true, true);
}

export default i18n;
