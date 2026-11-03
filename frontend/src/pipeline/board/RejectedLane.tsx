// "Rejected" panel below the pipeline panel (collapsed by default); also a drop target (asks for a reason).
import { useDroppable } from '@dnd-kit/core';
import { Anchor, Box, Button, Group, Menu, Stack, Text } from '@mantine/core';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { useFormat, useLabel } from '../shared';
import { SectionToggle } from '../shared/SectionToggle';
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
      className="pipe-lane"
      data-testid="lane-REJECTED"
      data-legal={legal === undefined ? undefined : String(legal)}
      data-over={isOver ? 'true' : undefined}
      style={{ opacity: legal === false ? 0.45 : 1 }}
    >
      <SectionToggle title={t('pipeline.panelRejected')} count={apps.length} open={open} onToggle={() => setOpen((o) => !o)} testId="rejected-panel">
        <Stack gap="xs" p="md">
          {apps.map((a) => (
            <Group key={a.id} justify="space-between" gap="xs">
              <Text size="sm">
                <Anchor component={Link} to={`/people/${a.person.id}`} c="var(--color-ink)" fw={500} underline="hover">
                  {a.person.name}
                </Anchor>
                {a.rejectionReason ? ` – “${a.rejectionReason}”` : ''} – {fmt.date(a.stageChangedAt)}
              </Text>
              {!props.readOnly && (
                <Menu withinPortal>
                  <Menu.Target>
                    <Button size="compact-xs" variant="default">
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
      </SectionToggle>
    </Box>
  );
}
