// Network list (A-12): who do we know and who should I contact?
import { Alert, Button, Chip, Container, Group, Pagination, Select, SegmentedControl, Skeleton, Text, TextInput, Title } from '@mantine/core';
import { useMediaQuery } from '@mantine/hooks';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useCurrentUser, useLabel, type Schemas } from '../../pipeline/shared';
import { PersonCards, PersonTable } from '../components/PersonRows';
import { NewPersonDialog } from './NewPersonDialog';
import { usePeople } from './usePeople';

const TYPES: Schemas['PersonType'][] = ['EXECUTIVE', 'CLIENT_CONTACT', 'REFERRER', 'CANDIDATE'];

export function NetworkList() {
  const { t } = useTranslation();
  const phone = useMediaQuery('(max-width: 48em)');
  const me = useCurrentUser();
  const { ref, label } = useLabel();
  const [q, setQ] = useState('');
  const [types, setTypes] = useState<Schemas['PersonType'][]>([]);
  const [mine, setMine] = useState(false);
  const [consent, setConsent] = useState<Schemas['ConsentStatus']>();
  const [staleDays, setStaleDays] = useState(0);
  const [page, setPage] = useState(0);
  const [newOpen, setNewOpen] = useState(false);
  const filters = { q, types, ownerId: mine ? me?.id : undefined, consent, staleDays, page };
  const res = usePeople(filters);
  const people = res.data?.content ?? [];
  const reset = (fn: () => void) => {
    fn();
    setPage(0);
  };
  const filtered = !!(q || types.length || mine || consent || staleDays);

  return (
    <Container py="md" fluid>
      <Group justify="space-between" mb="md">
        <Title order={1} data-testid="screen-title">
          {t('nav.network')}
          {res.data ? ` (${res.data.page.totalElements})` : ''}
        </Title>
        <Button onClick={() => setNewOpen(true)}>+ {t('network.newPerson')}</Button>
      </Group>
      <Group mb="sm" align="flex-end">
        <TextInput label={t('network.search')} value={q} onChange={(e) => reset(() => setQ(e.currentTarget.value))} />
        <SegmentedControl
          value={mine ? 'me' : 'all'}
          onChange={(v) => reset(() => setMine(v === 'me'))}
          data={[
            { value: 'all', label: t('network.all') },
            { value: 'me', label: t('network.mine') },
          ]}
        />
        <Select
          label={t('network.filterNotContacted')}
          clearable
          value={staleDays ? String(staleDays) : null}
          onChange={(v) => reset(() => setStaleDays(v ? Number(v) : 0))}
          data={[30, 60, 90, 180].map((d) => ({ value: String(d), label: t('network.daysAgo', { count: d }) }))}
        />
        <Select
          label={t('network.fields.consent')}
          clearable
          value={consent ?? null}
          onChange={(v) => reset(() => setConsent((v as Schemas['ConsentStatus']) ?? undefined))}
          data={(ref?.lifecycles.consent.statuses ?? []).map((s) => ({ value: s.value, label: label('consent', s.value) }))}
        />
      </Group>
      <Chip.Group multiple value={types} onChange={(v) => reset(() => setTypes(v as Schemas['PersonType'][]))}>
        <Group gap="xs" mb="md" style={{ overflowX: 'auto' }} wrap="nowrap">
          {TYPES.map((x) => (
            <Chip key={x} value={x} size="sm">
              {t(`network.types.${x}`)}
            </Chip>
          ))}
        </Group>
      </Chip.Group>

      {res.error ? (
        <Alert color="red" role="alert" title={t('network.loadFailed')}>
          <Button variant="light" onClick={res.reload}>
            {t('common.retry')}
          </Button>
        </Alert>
      ) : res.loading ? (
        <Skeleton height={200} data-testid="list-loading" />
      ) : people.length === 0 ? (
        <Group data-testid="list-empty">
          <Text c="dimmed">{t(filtered ? 'network.emptyFiltered' : 'network.empty')}</Text>
          {filtered && (
            <Button
              variant="light"
              onClick={() => {
                setQ('');
                setTypes([]);
                setMine(false);
                setConsent(undefined);
                setStaleDays(0);
                setPage(0);
              }}
            >
              {t('network.clear')}
            </Button>
          )}
        </Group>
      ) : phone ? (
        <PersonCards people={people} />
      ) : (
        <PersonTable people={people} />
      )}
      {(res.data?.page.totalPages ?? 0) > 1 && (
        <Pagination mt="md" total={res.data!.page.totalPages} value={page + 1} onChange={(p) => setPage(p - 1)} />
      )}
      <NewPersonDialog opened={newOpen} onClose={() => setNewOpen(false)} onCreated={res.reload} />
    </Container>
  );
}
