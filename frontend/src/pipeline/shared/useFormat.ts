import { useTranslation } from 'react-i18next';

export function useFormat() {
  const { i18n } = useTranslation();
  const locale = i18n.language === 'en' ? 'en-GB' : 'tr-TR';
  return {
    date: (iso?: string | null) => (iso ? new Date(iso).toLocaleDateString(locale, { day: '2-digit', month: 'short' }) : '–'),
    dateLong: (iso?: string | null) =>
      iso ? new Date(iso).toLocaleDateString(locale, { day: '2-digit', month: 'short', year: 'numeric' }) : '–',
  };
}

export const toIsoDate = (d: Date) => {
  const p = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
};
