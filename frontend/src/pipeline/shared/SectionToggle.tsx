// Plain collapsible section (v3 6): a 16/600 heading with a chevron, no box around it. Used for "Rejected" and "History".
import { Box, Collapse, Text, UnstyledButton } from '@mantine/core';
import { IconChevronDown, IconChevronRight } from '@tabler/icons-react';
import type { ReactNode } from 'react';
import { ICON_SIZE, ICON_STROKE } from '../../shell/components/objectIcons';

export function SectionToggle({
  title,
  open,
  onToggle,
  children,
  testId,
}: {
  title: string;
  open: boolean;
  onToggle: () => void;
  children?: ReactNode;
  testId?: string;
}) {
  const Chevron = open ? IconChevronDown : IconChevronRight;
  return (
    <Box data-testid={testId}>
      <Text component="h2" m={0} fw={600} size="md">
        <UnstyledButton
          type="button"
          onClick={onToggle}
          aria-expanded={open}
          style={{ display: 'inline-flex', alignItems: 'center', gap: 'var(--space-2)', color: 'var(--color-ink)' }}
        >
          <Chevron size={ICON_SIZE} stroke={ICON_STROKE} aria-hidden />
          {title}
        </UnstyledButton>
      </Text>
      <Collapse expanded={open}>
        <Box mt="sm">{children}</Box>
      </Collapse>
    </Box>
  );
}
