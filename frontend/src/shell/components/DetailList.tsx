import { Anchor, Text } from '@mantine/core';
import type { ReactNode } from 'react';
import './record.css';

export type DetailItem = { label: string; value?: ReactNode; href?: string };

/** Visual system v3 section 4: label 13/400 ink3 in a fixed 140 px column, value 14/500 ink, empty = "–". */
export function DetailList({ items }: { items: DetailItem[] }) {
  return (
    <dl className="detail-list">
      {items.map((it) => {
        const empty = it.value === undefined || it.value === null || it.value === '' || it.value === false;
        return (
          <div key={it.label} className="detail-row">
            <Text component="dt" fz="0.8125rem" fw={400} c="var(--color-ink3)">
              {it.label}
            </Text>
            <Text component="dd" fz="0.875rem" fw={empty ? 400 : 500} c={empty ? 'var(--color-ink3)' : 'var(--color-ink)'} style={{ margin: 0, overflowWrap: 'anywhere', minWidth: 0 }}>
              {empty ? (
                '–'
              ) : it.href ? (
                <Anchor href={it.href} fz="inherit" fw="inherit" target={it.href.startsWith('http') ? '_blank' : undefined} rel="noreferrer">
                  {it.value}
                </Anchor>
              ) : (
                it.value
              )}
            </Text>
          </div>
        );
      })}
    </dl>
  );
}
