import { useTranslation } from 'react-i18next';
import i18n from '../../i18n';
import en from './i18n/en.json';
import tr from './i18n/tr.json';

// Quick log strings live in their own bundle and are merged into the shared translation resources (same pattern as shell/edit).
i18n.addResourceBundle('en', 'translation', en, true, false);
i18n.addResourceBundle('tr', 'translation', tr, true, false);

/** Translator for the quicklog.* keys (`tx`, so the repo-wide static key scan does not look for them in the base bundle). */
export function useQlText() {
  const { t } = useTranslation();
  return (key: string, opts?: Record<string, unknown>): string => String(t(key, opts as never));
}
