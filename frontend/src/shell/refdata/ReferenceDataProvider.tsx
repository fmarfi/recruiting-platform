import { useEffect, useMemo, useState, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../api';
import { toneOf } from '../../theme/tokens';
import { useAuth } from '../auth/useAuth';
import { RefContext, type LifecycleKind, type Ref, type ReferenceData } from './useReference';

export function ReferenceDataProvider({ children, initial }: { children: ReactNode; initial?: ReferenceData }) {
  const { status } = useAuth();
  const { i18n } = useTranslation();
  const [data, setData] = useState<ReferenceData | null>(initial ?? null);
  useEffect(() => {
    if (status !== 'authed' || data) return;
    void api.GET('/reference-data').then(({ data: d }) => d && setData(d));
  }, [status, data]);
  const value = useMemo<Ref>(() => {
    const def = (kind: LifecycleKind, v: string) => data?.lifecycles[kind].statuses.find((s) => s.value === v);
    const tr = i18n.language.startsWith('tr');
    return {
      data,
      def,
      label: (kind, v) => {
        const d = def(kind, v);
        return d ? (tr ? d.labelTr : d.labelEn) : v;
      },
      tone: (kind, v) => toneOf(def(kind, v)?.tone),
    };
  }, [data, i18n.language]);
  return <RefContext.Provider value={value}>{children}</RefContext.Provider>;
}
