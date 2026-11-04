import { useTranslation } from 'react-i18next';
import { fmtDate } from '../../shell/lib/format';

/** One date format across the app: fmtDate (EN `16 Oct 2026`, TR `16.10.2026`). */
export function useFormat() {
  const { i18n } = useTranslation();
  const date = (iso?: string | null) => (iso ? fmtDate(iso, i18n.language) : '–');
  return { date, dateLong: date };
}

export const toIsoDate = (d: Date) => {
  const p = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
};
