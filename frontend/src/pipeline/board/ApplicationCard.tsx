// One candidate card: name, consent badge, stage date, Move menu. Draggable when a legal move exists.
import { useDraggable } from '@dnd-kit/core';
import { ActionIcon, Anchor, Card, Group, Menu, Stack, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { StatusBadge, useFormat, useLabel } from '../shared';
import { legalMoves, type Application } from './boardRules';
import type { BoardProps } from './boardTypes';

export function ApplicationCard({ app, props, dragging }: { app: Application; props: BoardProps; dragging: boolean }) {
  const { t } = useTranslation();
  const { label } = useLabel();
  const fmt = useFormat();
  const moves = props.readOnly ? [] : legalMoves(props.ref, app.stage);
  const draggable = moves.length > 0;
  const { attributes, listeners, setNodeRef, transform } = useDraggable({ id: app.id, disabled: !draggable });
  const p = app.person;
  return (
    <Card
      ref={setNodeRef}
      withBorder
      padding="xs"
      radius="md"
      data-testid={`card-${app.id}`}
      {...(draggable ? { ...attributes, ...listeners } : {})}
      style={{
        transform: transform ? `translate3d(${transform.x}px, ${transform.y}px, 0)` : undefined,
        opacity: dragging ? 0.8 : 1,
        cursor: draggable ? 'grab' : 'default',
        zIndex: dragging ? 10 : undefined,
        touchAction: 'manipulation',
      }}
    >
      <Group justify="space-between" wrap="nowrap" align="flex-start" gap="xs">
        <Stack gap={2} style={{ minWidth: 0 }}>
          <Anchor component={Link} to={`/people/${p.id}`} fw={600} size="sm" onPointerDown={(e) => e.stopPropagation()}>
            {p.name}
          </Anchor>
          <Text size="xs" c="dimmed" truncate>
            {[p.title, p.company].filter(Boolean).join(', ')}
          </Text>
        </Stack>
        {!props.readOnly && (
          <Menu position="bottom-end" withinPortal>
            <Menu.Target>
              <ActionIcon
                variant="subtle"
                aria-label={`${t('pipeline.move')}: ${p.name}`}
                style={{ minWidth: 'var(--touch-target-min, 44px)', minHeight: 'var(--touch-target-min, 44px)' }}
                onPointerDown={(e) => e.stopPropagation()}
                onKeyDown={(e) => e.stopPropagation()}
              >
                ⋯
              </ActionIcon>
            </Menu.Target>
            <Menu.Dropdown>
              {moves.length > 0 && <Menu.Label>{t('pipeline.move')}</Menu.Label>}
              {moves.map((m) => (
                <Menu.Item key={m.to} onClick={() => props.onMove(app, m.to)}>
                  {label('pipelineStage', m.to)}
                </Menu.Item>
              ))}
              <Menu.Item component={Link} to={`/people/${p.id}`}>
                {t('pipeline.openProfile')}
              </Menu.Item>
              <Menu.Item onClick={() => props.onLogActivity(app)}>{t('pipeline.logWithPerson')}</Menu.Item>
              {app.stage === 'SOURCED' && (
                <Menu.Item color="red" onClick={() => props.onRemove(app)}>
                  {t('pipeline.removeFromJob')}
                </Menu.Item>
              )}
            </Menu.Dropdown>
          </Menu>
        )}
      </Group>
      <Group justify="space-between" mt={6} gap="xs">
        <StatusBadge lifecycle="consent" value={p.consentStatus} />
        <Text size="xs" c="dimmed">
          {fmt.date(app.stageChangedAt)}
        </Text>
      </Group>
      {app.rejectionReason && (
        <Text size="xs" mt={4} c="dimmed">
          “{app.rejectionReason}”
        </Text>
      )}
    </Card>
  );
}
