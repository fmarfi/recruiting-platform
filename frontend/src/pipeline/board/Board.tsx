import {
  DndContext,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
  type DragEndEvent,
  type DragStartEvent,
} from '@dnd-kit/core';
import { Button, Group, Text } from '@mantine/core';
import { useMediaQuery } from '@mantine/hooks';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useLabel } from '../shared';
import { BoardColumn } from './BoardColumn';
import { canMove, type Application, type Stage } from './boardRules';
import type { BoardProps } from './boardTypes';
import { ReasonDialog } from './ReasonDialog';
import { RejectedLane } from './RejectedLane';

/** Kanban board (UX-04). Drag on desktop, Move menu everywhere; only moves from /reference-data are offered. */
export function Board(props: BoardProps) {
  const { t } = useTranslation();
  const { label } = useLabel();
  const phone = useMediaQuery('(max-width: 48em)');
  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 6 } }), useSensor(KeyboardSensor));
  const [activeId, setActiveId] = useState<string>();
  const [phoneStage, setPhoneStage] = useState<Stage>('SOURCED');
  const [pendingReject, setPendingReject] = useState<Application>();
  const columns = (
    (props.ref?.stageOrder ?? ['SOURCED', 'SCREENED', 'PRESENTED', 'CLIENT_INTERVIEW', 'OFFER', 'PLACED', 'REJECTED']) as Stage[]
  ).filter((s) => s !== 'REJECTED');
  const activeApp = props.apps.find((a) => a.id === activeId);

  // A move needing a reason opens the dialog first; the others go straight to the parent.
  const request = (app: Application, to: Stage) => {
    const m = canMove(props.ref, app.stage, to);
    if (!m) return;
    if (m.reasonRequired) setPendingReject(app);
    else props.onMove(app, to);
  };
  const onDragEnd = (e: DragEndEvent) => {
    setActiveId(undefined);
    const app = props.apps.find((a) => a.id === e.active.id);
    if (app && e.over) request(app, e.over.id as Stage);
  };
  const guarded: BoardProps = { ...props, onMove: (app, to) => request(app, to) };
  const chips = t('pipeline.rejectChips', { returnObjects: true }) as unknown as string[];
  const rejected = props.apps.filter((a) => a.stage === 'REJECTED');
  const visible = phone ? columns.filter((c) => c === phoneStage) : columns;
  return (
    <DndContext
      sensors={props.readOnly || phone ? [] : sensors}
      onDragStart={(e: DragStartEvent) => setActiveId(String(e.active.id))}
      onDragEnd={onDragEnd}
      onDragCancel={() => setActiveId(undefined)}
    >
      {!props.readOnly && !phone && (
        <Text size="xs" c="dimmed" mb="xs">
          {t('pipeline.hint')}
        </Text>
      )}
      {phone && (
        <Group gap="xs" wrap="nowrap" mb="sm" style={{ overflowX: 'auto' }} role="tablist">
          {columns.map((c) => (
            <Button
              key={c}
              role="tab"
              aria-selected={c === phoneStage}
              size="compact-md"
              variant={c === phoneStage ? 'filled' : 'light'}
              onClick={() => setPhoneStage(c)}
            >
              {label('pipelineStage', c)} {props.apps.filter((a) => a.stage === c).length}
            </Button>
          ))}
        </Group>
      )}
      <div style={{ display: 'grid', gridTemplateColumns: phone ? '1fr' : `repeat(${visible.length}, minmax(220px, 1fr))`, gap: 12, overflowX: 'auto', paddingBottom: 8 }}>
        {visible.map((s) => (
          <BoardColumn key={s} stage={s} apps={props.apps.filter((a) => a.stage === s)} props={guarded} activeApp={activeApp} />
        ))}
      </div>
      <RejectedLane apps={rejected} props={guarded} activeApp={activeApp} />
      <ReasonDialog
        opened={!!pendingReject}
        title={t('pipeline.rejectTitle')}
        chips={Array.isArray(chips) ? chips : undefined}
        onCancel={() => setPendingReject(undefined)}
        onConfirm={(reason) => {
          if (pendingReject) props.onMove(pendingReject, 'REJECTED', reason);
          setPendingReject(undefined);
        }}
      />
    </DndContext>
  );
}
