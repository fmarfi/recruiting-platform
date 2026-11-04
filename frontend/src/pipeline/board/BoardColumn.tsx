// One stage column; a drop target that dims when the dragged card may not go here.
import { useDroppable } from '@dnd-kit/core';
import { Badge, Box, Group, Stack, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { useLabel } from '../shared';
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
  const { label } = useLabel();
  const legal = activeApp ? !!canMove(props.ref, activeApp.stage, stage) : undefined;
  const { setNodeRef, isOver } = useDroppable({ id: stage, disabled: legal === false });
  return (
    <Box
      ref={setNodeRef}
      className="pipe-board-col"
      data-testid={`col-${stage}`}
      data-droppable={legal === undefined ? undefined : String(legal)}
      data-legal={legal === undefined ? undefined : String(legal)}
      data-over={isOver ? 'true' : undefined}
      style={{ minHeight: '7.5rem' }}
    >
      <Group className="pipe-board-colhead" justify="space-between" wrap="nowrap" gap="xs">
        <Text component="span" className="pipe-board-colname" title={label('pipelineStage', stage)}>
          {label('pipelineStage', stage)}
        </Text>
        <Badge variant="light" color="gray" radius="sm" aria-label={`${apps.length}`}>
          {apps.length}
        </Badge>
      </Group>
      <Stack gap="xs">
        {apps.length === 0 && (
          <Text size="xs" c="dimmed" ta="center" className="pipe-board-empty">
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
