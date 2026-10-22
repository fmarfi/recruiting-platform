import { Anchor, Button } from '@mantine/core';
import { useState, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { SectionCard } from '../../shell/ui/SectionCard';

interface Props {
  title: string;
  count: number;
  newLabel: string;
  onNew: () => void;
  /** Receives how many rows to show: 5 until "View all" is clicked. */
  children: (limit: number) => ReactNode;
}

const PREVIEW = 5;

/** Related-list card: title with count, New button, first 5 rows and a "View all" footer link. */
export function RelatedCard({ title, count, newLabel, onNew, children }: Props) {
  const { t } = useTranslation();
  const [all, setAll] = useState(false);
  return (
    <SectionCard
      title={title}
      count={count}
      actions={<Button size="compact-sm" variant="light" onClick={onNew}>+ {newLabel}</Button>}
    >
      {children(all ? Infinity : PREVIEW)}
      {count > PREVIEW && (
        <Anchor component="button" type="button" fz="sm" mt="sm" onClick={() => setAll((v) => !v)}>
          {all ? t('accounts.showLess') : t('accounts.viewAll', { n: count })}
        </Anchor>
      )}
    </SectionCard>
  );
}
