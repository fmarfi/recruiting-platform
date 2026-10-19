// One stage column; a drop target that dims when the dragged card may not go here.
import { useDroppable } from '@dnd-kit/core';
import { Badge, Box, Group, Stack, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { StatusBadge } from '../shared';
import { ApplicationCard } from './ApplicationCard';
import { canMove, type Application, type Stage } from './boardRules';
import type { BoardProps } from './boardTypes';

export function BoardColumn({
  stage,
  apps,
  props,
  activeApp,
}: {
  stage: Stage;
  apps: Application[];
  props: BoardProps;
  activeApp: Application | undefined;
}) {
  const { t } = useTranslation();
  const legal = activeApp ? !!canMove(props.ref, activeApp.stage, stage) : undefined;
  const { setNodeRef, isOver } = useDroppable({ id: stage, disabled: legal === false });
  return (
    <Box
      ref={setNodeRef}
      data-testid={`col-${stage}`}
      data-droppable={legal === undefined ? undefined : String(legal)}
      p="sm"
      style={{
        borderRadius: 'var(--radius-md, 8px)',
        background: 'var(--color-surface2)',
        outline: legal ? '2px dashed var(--tone-info-dot)' : isOver ? '2px solid var(--tone-info-dot)' : undefined,
        opacity: legal === false ? 0.45 : 1,
        cursor: legal === false ? 'not-allowed' : undefined,
        minHeight: 120,
        minWidth: 0,
        border: '1px solid var(--color-border)',
      }}
    >
      <Group justify="space-between" wrap="nowrap" mb="sm">
        <StatusBadge lifecycle="pipelineStage" value={stage} />
        <Badge variant="light" color="gray" aria-label={`${apps.length}`}>
          {apps.length}
        </Badge>
      </Group>
      <Stack gap="xs">
        {apps.length === 0 && (
          <Text size="xs" c="dimmed" ta="center" style={{ border: '1px dashed var(--color-borderStrong)', borderRadius: 6, padding: 12 }}>
            {t('pipeline.emptyColumn')}
          </Text>
        )}
        {apps.map((a) => (
          <ApplicationCard key={a.id} app={a} props={props} dragging={activeApp?.id === a.id} />
        ))}
      </Stack>
    </Box>
  );
}
