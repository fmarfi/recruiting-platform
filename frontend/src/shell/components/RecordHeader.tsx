import { Box, Group, Stack, Text, Title } from '@mantine/core';
import type { ReactNode } from 'react';
import './record.css';

export type RecordFact = { label: string; value: ReactNode };

/** Content of the RecordLayout band: breadcrumb, 40 px avatar or icon, serif name with badge, subtitle, actions, fact strip, tabs. */
export function RecordHeader({
  breadcrumb,
  avatar,
  name,
  badge,
  subtitle,
  actions,
  facts,
  tabs,
}: {
  breadcrumb?: ReactNode;
  avatar?: ReactNode;
  name: ReactNode;
  badge?: ReactNode;
  subtitle?: ReactNode;
  actions?: ReactNode;
  facts?: RecordFact[];
  tabs?: ReactNode;
}) {
  return (
    <>
      <Box className="record-head">
        {breadcrumb && (
          <Text fz="0.8125rem" c="var(--color-ink3)" mb="xs" data-testid="screen-breadcrumb">
            {breadcrumb}
          </Text>
        )}
        <Group justify="space-between" align="flex-start" gap="md" wrap="wrap">
          <Group gap="md" align="center" wrap="nowrap" style={{ flex: '1 1 280px', minWidth: 0 }}>
            {avatar && <Box className="record-avatar">{avatar}</Box>}
            <Stack gap={4} style={{ minWidth: 0 }}>
              <Group gap="sm" align="center" wrap="wrap">
                <Title order={1} data-testid="screen-title" ff="var(--font-display)" fz="1.5rem" fw={600} style={{ overflowWrap: 'anywhere' }}>
                  {name}
                </Title>
                {badge}
              </Group>
              {subtitle && (
                <Text fz="0.875rem" c="var(--color-ink3)" data-testid="screen-subtitle">
                  {subtitle}
                </Text>
              )}
            </Stack>
          </Group>
          {actions && (
            <Group gap="xs" wrap="wrap" justify="flex-end" data-testid="page-actions">
              {actions}
            </Group>
          )}
        </Group>
        {!!facts?.length && (
          <div className="fact-strip" data-testid="record-facts">
            {facts.map((f) => (
              <div key={f.label} className="fact">
                <Text fz="0.75rem" c="var(--color-ink3)">
                  {f.label}
                </Text>
                <Text component="div" fz="0.875rem" fw={500} c="var(--color-ink)">
                  {f.value}
                </Text>
              </div>
            ))}
          </div>
        )}
      </Box>
      {tabs && <div className="record-tabs">{tabs}</div>}
    </>
  );
}
