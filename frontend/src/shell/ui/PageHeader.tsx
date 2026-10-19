// Page header for list and record pages: breadcrumb, object icon tile, title, actions.
import { Anchor, Box, Breadcrumbs, Group, Text, Title } from '@mantine/core';
import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';

export interface Crumb {
  label: string;
  to?: string;
}

interface Props {
  title: ReactNode;
  /** One or two letters shown in the object icon tile. */
  glyph: string;
  crumbs?: Crumb[];
  /** Status badge or Path shown beside the title. */
  status?: ReactNode;
  actions?: ReactNode;
}

/** Breadcrumb line, icon tile, h1 title and right-aligned actions. */
export function PageHeader({ title, glyph, crumbs, status, actions }: Props) {
  return (
    <Box
      mb="md"
      p="md"
      style={{ background: 'var(--color-surface)', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-md)' }}
    >
      {crumbs && crumbs.length > 0 && (
        <Breadcrumbs separator="/" mb={4} fz="xs">
          {crumbs.map((c) =>
            c.to ? (
              <Anchor key={c.label} component={Link} to={c.to} fz="xs">{c.label}</Anchor>
            ) : (
              <Text key={c.label} fz="xs" c="dimmed">{c.label}</Text>
            ),
          )}
        </Breadcrumbs>
      )}
      <Group justify="space-between" align="center" wrap="wrap" gap="sm">
        <Group gap="sm" wrap="nowrap" style={{ minWidth: 0 }}>
          <Box
            aria-hidden
            style={{
              width: 36, height: 36, flex: '0 0 auto', display: 'grid', placeItems: 'center',
              borderRadius: 'var(--radius-md)', background: 'var(--color-accentSoft)', color: 'var(--color-accent)', fontWeight: 700,
            }}
          >
            {glyph}
          </Box>
          <Title order={1} data-testid="screen-title" style={{ fontSize: '1.375rem' }}>{title}</Title>
          {status}
        </Group>
        {actions && <Group gap="sm">{actions}</Group>}
      </Group>
    </Box>
  );
}
