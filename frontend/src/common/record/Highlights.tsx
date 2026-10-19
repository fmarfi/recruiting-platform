import { Card } from '@mantine/core';
import type { ReactNode } from 'react';
import './record.css';

export type HighlightItem = { label: string; value: ReactNode };

/** Highlights panel: 4 to 7 label-over-value pairs under the page header; empty values show a dash. */
export function Highlights({ items }: { items: HighlightItem[] }) {
  return (
    <Card withBorder padding="md" mb="md" data-testid="highlights">
      <dl className="highlights">
        {items.map((i) => (
          <div key={i.label}>
            <dt>{i.label}</dt>
            <dd>{i.value === undefined || i.value === null || i.value === '' ? '–' : i.value}</dd>
          </div>
        ))}
      </dl>
    </Card>
  );
}
