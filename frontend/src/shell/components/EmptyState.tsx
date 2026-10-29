import { Card, Group, Stack, Text, Title } from '@mantine/core';
import type { ReactNode } from 'react';

/** UX-08: every empty list says what to do next. */
export function EmptyState({ title, hint, actions }: { title: string; hint?: string; actions?: ReactNode }) {
  return (
    <Card withBorder padding="xl" data-testid="empty-state" style={{ background: 'var(--color-surface)', borderStyle: 'dashed', borderColor: 'var(--color-borderStrong)' }}>
      <Stack align="center" gap="xs" ta="center">
        <svg width="56" height="56" viewBox="0 0 56 56" fill="none" aria-hidden focusable="false">
          <rect x="8" y="14" width="40" height="28" rx="6" stroke="var(--color-ink3)" strokeWidth="1.5" />
          <path d="M18 34V22l6 8 6-8v12" stroke="var(--color-ink3)" strokeWidth="1.5" strokeLinejoin="round" />
          <path d="M16 47l24-3" stroke="var(--color-brassDeco)" strokeWidth="2.5" strokeLinecap="round" />
        </svg>
        <Title order={2} size="h3">{title}</Title>
        {hint && <Text c="dimmed" maw="60ch">{hint}</Text>}
        {actions && <Group justify="center" mt="xs">{actions}</Group>}
      </Stack>
    </Card>
  );
}
