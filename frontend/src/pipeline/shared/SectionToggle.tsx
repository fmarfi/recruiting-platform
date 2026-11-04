// Collapsible panel (visual system v3, sections 4 and 5): a shared Panel whose header is the toggle. Used for "Rejected" and "History".
import { Collapse, UnstyledButton } from '@mantine/core';
import { IconChevronDown, IconChevronRight } from '@tabler/icons-react';
import type { ReactNode } from 'react';
import { Panel } from '../../shell/components/Panel';
import { ICON_SIZE, ICON_STROKE } from '../../shell/components/objectIcons';

export function SectionToggle({
  title,
  count,
  open,
  onToggle,
  children,
  testId,
}: {
  title: string;
  count?: number;
  open: boolean;
  onToggle: () => void;
  children?: ReactNode;
  testId?: string;
}) {
  const Chevron = open ? IconChevronDown : IconChevronRight;
  return (
    <Panel
      title={
        <UnstyledButton
          type="button"
          onClick={onToggle}
          aria-expanded={open}
          style={{ display: 'inline-flex', alignItems: 'center', gap: 'var(--space-2)', color: 'var(--color-ink)' }}
        >
          <Chevron size={ICON_SIZE} stroke={ICON_STROKE} aria-hidden />
          {title}
        </UnstyledButton>
      }
      count={count}
      padding="none"
      testId={testId}
    >
      <Collapse expanded={open}>{children}</Collapse>
    </Panel>
  );
}
