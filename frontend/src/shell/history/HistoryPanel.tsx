import { Badge, Button, Group, Loader, Stack, Text, Title } from '@mantine/core';
import { useCallback, useEffect, useState } from 'react';
import { api, type components } from '../../api';
import { useText } from '../edit/text';
import { fmtDate } from '../lib/format';

type Schemas = components['schemas'];
export type HistoryEntityType = Schemas['EntityType'];
type Revision = Schemas['Revision'];

export interface HistoryPanelProps {
  entityType: HistoryEntityType;
  entityId: string;
  /** Page size, default 20. */
  pageSize?: number;
  /** Heading level of the panel title, default 3. Pass `null` to hide the title (the host supplies one). */
  headingOrder?: 2 | 3 | 4 | null;
  /** Injected API client for tests, defaults to the shared one. */
  client?: Pick<typeof api, 'GET'>;
}

function show(v: unknown, empty: string): string {
  if (v === null || v === undefined || v === '') return empty;
  return typeof v === 'object' ? JSON.stringify(v) : String(v);
}

/** Change history of one record (`GET /audit/revisions`): who changed which field from what to what, newest first, with "Load more". */
export function HistoryPanel({ entityType, entityId, pageSize = 20, headingOrder = 3, client = api }: HistoryPanelProps) {
  const { tx, lang } = useText();
  const [items, setItems] = useState<Revision[]>([]);
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(0);
  const [state, setState] = useState<'loading' | 'ready' | 'error'>('loading');

  const load = useCallback(
    async (n: number) => {
      setState('loading');
      try {
        const { data } = await client.GET('/audit/revisions', { params: { query: { entityType, entityId, page: n, size: pageSize } } });
        if (!data) throw new Error('history');
        setItems((prev) => (n === 0 ? data.content : [...prev, ...data.content]));
        setPage(n);
        setTotal(data.page.totalElements);
        setState('ready');
      } catch {
        setState('error');
      }
    },
    [client, entityType, entityId, pageSize],
  );

  useEffect(() => {
    setItems([]);
    void load(0);
  }, [load]);

  const none = tx('history.none');
  return (
    <section aria-labelledby={headingOrder ? 'history-title' : undefined} aria-label={headingOrder ? undefined : tx('history.title')} aria-busy={state === 'loading'}>
      {headingOrder && (
        <Title order={headingOrder} size="h5" id="history-title" mb="xs">
          {tx('history.title')}
        </Title>
      )}
      <div role="status" aria-live="polite">
        {state === 'loading' && items.length === 0 && (
          <Group gap="xs">
            <Loader size="xs" aria-hidden />
            <Text size="sm">{tx('history.loading')}</Text>
          </Group>
        )}
        {state === 'ready' && items.length === 0 && (
          <Text size="sm" c="dimmed">
            {tx('history.empty')}
          </Text>
        )}
      </div>
      {state === 'error' && (
        <Group gap="xs" role="alert">
          <Text size="sm" c="red.8">
            {tx('history.error')}
          </Text>
          <Button size="compact-sm" variant="default" onClick={() => void load(items.length === 0 ? 0 : page + 1)}>
            {tx('shell.retry')}
          </Button>
        </Group>
      )}
      {items.length > 0 && (
        <Stack component="ol" gap="sm" m={0} p={0} style={{ listStyle: 'none' }}>
          {items.map((r) => (
            <li key={r.revision}>
              <Group gap="xs" wrap="wrap">
                <Badge variant="outline" color="dark" size="sm">
                  {tx(`history.type.${r.type}`)}
                </Badge>
                <Text size="sm" fw={600}>
                  {r.user?.name ?? tx('history.system')}
                </Text>
                <Text size="xs" c="dimmed" component="time" dateTime={r.at}>
                  {fmtDate(r.at, lang)} {new Date(r.at).toTimeString().slice(0, 5)}
                </Text>
              </Group>
              <Stack component="ul" gap={2} m={0} mt={2} pl="md">
                {r.changes.map((c) => (
                  <li key={c.field}>
                    <Text size="sm">{tx('history.change', { field: c.field, from: show(c.from, none), to: show(c.to, none) })}</Text>
                  </li>
                ))}
              </Stack>
            </li>
          ))}
        </Stack>
      )}
      {state !== 'error' && items.length > 0 && items.length < total && (
        <Button mt="sm" size="compact-sm" variant="default" loading={state === 'loading'} onClick={() => void load(page + 1)}>
          {tx('history.more')}
        </Button>
      )}
    </section>
  );
}
