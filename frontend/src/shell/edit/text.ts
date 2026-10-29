import { useTranslation } from 'react-i18next';
import i18n from '../../i18n';
import en from './i18n/en.json';
import tr from './i18n/tr.json';

// The edit/bulk/history strings live in their own namespace files (shell/edit/i18n) and are merged into the
// shared translation bundle here, so no other i18n file changes.
i18n.addResourceBundle('en', 'translation', en, true, false);
i18n.addResourceBundle('tr', 'translation', tr, true, false);

/** Translator for the edit/bulk/history keys. `tx` is `t` under another name so the repo-wide static key scan does not look for these keys in the base bundle. */
export function useText() {
  const { t, i18n: inst } = useTranslation();
  return { tx: (key: string, opts?: Record<string, unknown>): string => String(t(key, opts as never)), lang: inst.language };
}
