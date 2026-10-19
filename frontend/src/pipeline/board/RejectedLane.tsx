// Collapsed lane below the board for rejected candidates; also a drop target (asks for a reason).
import { useDroppable } from '@dnd-kit/core';
import { Anchor, Box, Button, Collapse, Group, Menu, Stack, Text } from '@mantine/core';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { useFormat, useLabel } from '../shared';
import { canMove, legalMoves, type Application } from './boardRules';
import type { BoardProps } from './boardTypes';

export function RejectedLane({ apps, props, activeApp }: { apps: Application[]; props: BoardProps; activeApp: Application | undefined }) {
  const { t } = useTranslation();
  const { label } = useLabel();
  const fmt = useFormat();
  const [open, setOpen] = useState(false);
  const legal = activeApp ? !!canMove(props.ref, activeApp.stage, 'REJECTED') : undefined;
  const { setNodeRef, isOver } = useDroppable({ id: 'REJECTED', disabled: legal === false });
  return (
    <Box
      ref={setNodeRef}
      data-testid="lane-REJECTED"
      mt="md"
      p="xs"
      style={{
        borderRadius: 'var(--radius-md, 8px)',
        outline: legal ? '2px dashed var(--tone-bad-dot)' : isOver ? '2px solid var(--tone-bad-dot)' : '1px solid var(--color-border, #ddd)',
        opacity: legal === false ? 0.45 : 1,
      }}
    >
      <Button variant="subtle" size="compact-sm" onClick={() => setOpen((o) => !o)} aria-expanded={open}>
        {t('pipeline.rejectedLane', { count: apps.length })}
      </Button>
      <Collapse expanded={open}>
        <Stack gap={4} mt="xs">
          {apps.map((a) => (
            <Group key={a.id} justify="space-between" gap="xs">
              <Text size="sm">
                <Anchor component={Link} to={`/people/${a.person.id}`}>
                  {a.person.name}
                </Anchor>
                {a.rejectionReason ? ` – “${a.rejectionReason}”` : ''} – {fmt.date(a.stageChangedAt)}
              </Text>
              {!props.readOnly && (
                <Menu withinPortal>
                  <Menu.Target>
                    <Button size="compact-xs" variant="light">
                      {t('pipeline.reopen')}
                    </Button>
                  </Menu.Target>
                  <Menu.Dropdown>
                    {legalMoves(props.ref, 'REJECTED').map((m) => (
                      <Menu.Item key={m.to} onClick={() => props.onMove(a, m.to)}>
                        {t('pipeline.backTo', { stage: label('pipelineStage', m.to) })}
                      </Menu.Item>
                    ))}
                  </Menu.Dropdown>
                </Menu>
              )}
            </Group>
          ))}
        </Stack>
      </Collapse>
    </Box>
  );
}
