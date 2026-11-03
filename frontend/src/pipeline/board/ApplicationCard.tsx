// One candidate card: name, consent badge, days in stage, Move menu. Draggable when a legal move exists.
import { useDraggable } from '@dnd-kit/core';
import { ActionIcon, Anchor, Card, Group, Menu, Stack, Text } from '@mantine/core';
import { IconDots, IconShieldCheck } from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { ToneBadge } from '../../shell/components/ToneBadge';
import { useFormat, useLabel } from '../shared';
import { legalMoves, type Application } from './boardRules';
import type { BoardProps } from './boardTypes';

const DAY_MS = 86_400_000;
// Days in stage that turn the age cue amber, then red.
const STAGE_WARN_DAYS = 30;
const STAGE_DANGER_DAYS = 60;
// Consent states that block or delay a move to the client: shown with an icon and words, never a colour chip alone.
const CONSENT_PROBLEM = ['NOT_SENT', 'REFUSED', 'WITHDRAWN'];

export function ApplicationCard({ app, props, dragging }: { app: Application; props: BoardProps; dragging: boolean }) {
  const { t } = useTranslation();
  const { label, def } = useLabel();
  const fmt = useFormat();
  const consentProblem = CONSENT_PROBLEM.includes(app.person.consentStatus);
  const moves = props.readOnly ? [] : legalMoves(props.ref, app.stage);
  const draggable = moves.length > 0;
  const { attributes, listeners, setNodeRef, transform } = useDraggable({ id: app.id, disabled: !draggable });
  const p = app.person;
  const since = app.stageChangedAt ? new Date(app.stageChangedAt).getTime() : NaN;
  const days = Number.isNaN(since) ? undefined : Math.max(0, Math.floor((Date.now() - since) / DAY_MS));
  const ageTone =
    days === undefined || days <= STAGE_WARN_DAYS
      ? undefined
      : days > STAGE_DANGER_DAYS
        ? 'var(--tone-bad-fg)'
        : 'var(--tone-warn-fg)';
  return (
    <Card
      ref={setNodeRef}
      withBorder
      className="pipe-board-card"
      data-dragging={dragging ? 'true' : undefined}
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
          <Anchor component={Link} to={`/people/${p.id}`} fw={500} c="var(--color-ink)" underline="never" size="sm" onPointerDown={(e) => e.stopPropagation()}>
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
                <IconDots size={16} stroke={1.75} aria-hidden />
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
      <Group justify="space-between" mt="xs" gap="xs" wrap="nowrap">
        <Group gap={6} wrap="nowrap" style={{ minWidth: 0 }}>
          <Text size="xs" c="dimmed" style={{ whiteSpace: 'nowrap' }}>
            {t('pipeline.consentLabel')}:
          </Text>
          <ToneBadge
            tone={def('consent', p.consentStatus)?.tone ?? 'NEUTRAL'}
            label={label('consent', p.consentStatus)}
            icon={consentProblem ? <IconShieldCheck size={14} stroke={1.75} aria-hidden /> : undefined}
          />
        </Group>
        <Text
          size="xs"
          c="dimmed"
          className="pipe-board-days"
          style={{ color: ageTone }}
          title={fmt.dateLong(app.stageChangedAt)}
        >
          {days === undefined ? '–' : t('pipeline.daysInStage', { count: days })}
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
