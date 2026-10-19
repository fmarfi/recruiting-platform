// Candidates card: Table / Kanban toggle over the same applications (lightning-style P4).
import { Anchor, Card, Group, SegmentedControl, Table, Text } from '@mantine/core';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { Board } from '../../board/Board';
import type { BoardProps } from '../../board/boardTypes';
import { StatusBadge, useFormat } from '../../shared';

const VIEW_KEY = 'pipeline.candidatesView';
type View = 'kanban' | 'table';

function readView(): View {
  try {
    return localStorage.getItem(VIEW_KEY) === 'table' ? 'table' : 'kanban';
  } catch {
    return 'kanban';
  }
}

/** Kanban stays the default (drag to move, Rejected lane); Table is the compact list of the same rows. */
export function CandidatesCard({ board, rejectedCount }: { board: BoardProps; rejectedCount: number }) {
  const { t } = useTranslation();
  const fmt = useFormat();
  const [view, setView] = useState<View>(readView);
  const choose = (v: string) => {
    setView(v as View);
    try {
      localStorage.setItem(VIEW_KEY, v);
    } catch {
      /* storage can be blocked */
    }
  };
  return (
    <Card withBorder padding="md" data-testid="candidates-card">
      <Group justify="space-between" mb="sm">
        <Text fw={600}>
          {t('pipeline.candidates')} ({board.apps.length}
          {rejectedCount > 0 ? `, ${t('pipeline.rejectedLane', { count: rejectedCount })}` : ''})
        </Text>
        <SegmentedControl
          size="xs"
          value={view}
          onChange={choose}
          aria-label={t('pipeline.view')}
          data={[
            { value: 'kanban', label: t('pipeline.kanban') },
            { value: 'table', label: t('pipeline.table') },
          ]}
        />
      </Group>
      {view === 'kanban' ? (
        <Board {...board} />
      ) : (
        <Table.ScrollContainer minWidth={480}>
          <Table highlightOnHover data-testid="candidates-table">
            <Table.Thead>
              <Table.Tr>
                <Table.Th>{t('pipeline.person')}</Table.Th>
                <Table.Th>{t('pipeline.stage')}</Table.Th>
                <Table.Th>{t('pipeline.since')}</Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {board.apps.map((a) => (
                <Table.Tr key={a.id} style={{ cursor: 'pointer' }} onClick={() => board.onOpen?.(a)}>
                  <Table.Td>
                    <Anchor component={Link} to={`/people/${a.person.id}`} onClick={(e) => e.stopPropagation()}>
                      {a.person.name}
                    </Anchor>
                    <Text size="xs" c="dimmed">
                      {[a.person.title, a.person.company].filter(Boolean).join(', ')}
                    </Text>
                  </Table.Td>
                  <Table.Td>
                    <StatusBadge lifecycle="pipelineStage" value={a.stage} />
                  </Table.Td>
                  <Table.Td>{fmt.date(a.stageChangedAt)}</Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Table.ScrollContainer>
      )}
    </Card>
  );
}
