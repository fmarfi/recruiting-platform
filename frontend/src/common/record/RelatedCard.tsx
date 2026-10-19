import { Anchor, Button, Card, Group, Text } from '@mantine/core';
import { useState, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import './record.css';

export type RelatedCardProps<T> = {
  title: string;
  rows: T[];
  rowKey: (row: T) => string;
  /** Renders the 2 to 3 key columns of one row. */
  renderRow: (row: T) => ReactNode;
  /** Text of the empty state line. */
  empty: string;
  onNew?: () => void;
  newLabel?: string;
  /** Rows shown before "View all"; default 5. */
  limit?: number;
};

/** Related-list card: title with count, New button, first rows, "View all" footer, empty state line. */
export function RelatedCard<T>({ title, rows, rowKey, renderRow, empty, onNew, newLabel, limit = 5 }: RelatedCardProps<T>) {
  const { t } = useTranslation();
  const [all, setAll] = useState(false);
  const shown = all ? rows : rows.slice(0, limit);
  return (
    <Card withBorder padding="md" data-testid="related-card">
      <Group justify="space-between" mb="xs">
        <Text fw={600}>
          {title} ({rows.length})
        </Text>
        {onNew && (
          <Button size="compact-sm" variant="light" onClick={onNew}>
            {newLabel ?? t('record.new')}
          </Button>
        )}
      </Group>
      {rows.length === 0 && (
        <Text size="sm" c="dimmed">
          {empty}
        </Text>
      )}
      {shown.map((r) => (
        <div key={rowKey(r)} className="related-row">
          {renderRow(r)}
        </div>
      ))}
      {rows.length > limit && (
        <Anchor component="button" type="button" size="sm" mt="xs" onClick={() => setAll(!all)}>
          {all ? t('record.showLess') : t('record.viewAll')}
        </Anchor>
      )}
    </Card>
  );
}
