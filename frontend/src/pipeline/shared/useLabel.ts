import { useTranslation } from 'react-i18next';
import { useRefData } from './useRefData';
import type { LifecycleKey } from './types';

export function useLabel() {
  const { i18n } = useTranslation();
  const ref = useRefData();
  const en = i18n.language === 'en';
  const def = (lc: LifecycleKey, value: string) => ref?.lifecycles[lc].statuses.find((s) => s.value === value);
  return {
    ref,
    def,
    label: (lc: LifecycleKey, value: string) => {
      const d = def(lc, value);
      return d ? (en ? d.labelEn : d.labelTr) : value;
    },
  };
}

