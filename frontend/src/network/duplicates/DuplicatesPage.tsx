// Duplicates queue (MANAGER/ADMIN): pairs found after create, update, CV attach and by the nightly scan. Merge or mark as different people.
import { AppPagination } from '../../shell/components/AppPagination';
import { Alert, Anchor, Button, Card, Container, Group, Skeleton, Stack, Text, Title } from '@mantine/core';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { api } from '../../api';
import { StatusBadge, problemText, useAsync, useFormat, type Schemas } from '../../pipeline/shared';
import { useAuth } from '../../shell/auth/useAuth';
import { EmptyState } from '../../shell/components/EmptyState';
import { canMerge } from './duplicateLogic';
import { ReasonBadges } from './DuplicatePrompt';
import { MergeDialog } from './MergeDialog';

function Side({ p }: { p: Schemas['PersonSummary'] }) {
  return (
    <Stack gap={2} style={{ flex: 1, minWidth: 0 }}>
      <Anchor component={Link} to={`/people/${p.id}`} fw={600}>
        {p.name}
      </Anchor>
      <Text size="sm">{[p.title, p.company].filter(Boolean).join(', ')}</Text>
      <Text size="xs" c="dimmed">
        {[p.city, p.email].filter(Boolean).join(' - ')}
      </Text>
      <StatusBadge lifecycle="consent" value={p.consentStatus} />
    </Stack>
  );
}

export function DuplicatesPage() {
  const { t } = useTranslation();
  const fmt = useFormat();
  const { user } = useAuth();
  const allowed = canMerge(user?.role);
  const [page, setPage] = useState(0);
  const res = useAsync<Schemas['DuplicateCandidatePage']>(() => (allowed ? api.GET('/duplicates', { params: { query: { page, size: 20 } } }) : Promise.resolve({})), [page, allowed]);
  const [merging, setMerging] = useState<Schemas['DuplicateCandidate']>();
  const [notice, setNotice] = useState<{ color: string; text: string }>();

  if (!allowed) {
    return (
      <Container py="md">
        <Alert color="gray" role="alert">
          {t('network.dup.managersOnly')}
        </Alert>
      </Container>
    );
  }
  const dismiss = async (id: string) => {
    const r = await api.POST('/duplicates/{id}/dismiss', { params: { path: { id } } });
    if (r.error) setNotice({ color: 'red', text: problemText(r.error, t('network.dup.dismissFailed')) });
    else res.reload();
  };

  return (
    <Container maw="var(--content-max-width)" fluid>
      <Anchor component={Link} to="/network" size="sm">
        {t('nav.network')}
      </Anchor>
      <Title order={1} my="sm" data-testid="screen-title">
        {t('network.dup.title')}
        {res.data ? ` (${res.data.page.totalElements})` : ''}
      </Title>
      {notice && (
        <Alert color={notice.color} mb="sm" withCloseButton onClose={() => setNotice(undefined)} role="alert">
          {notice.text}
        </Alert>
      )}
      {res.loading && <Skeleton height={120} />}
      {!!res.error && (
        <Alert color="red" role="alert">
          {t('network.dup.loadFailed')}{' '}
          <Button size="compact-sm" variant="light" onClick={res.reload}>
            {t('common.retry')}
          </Button>
        </Alert>
      )}
      {res.data && res.data.content.length === 0 && <EmptyState title={t('network.dup.empty')} />}
      <Stack>
        {res.data?.content.map((d) => (
          <Card key={d.id} withBorder data-testid="duplicate-pair">
            <Group align="flex-start" wrap="wrap">
              <Side p={d.personA} />
              <Side p={d.personB} />
              <Stack gap="xs" align="flex-end">
                <ReasonBadges reasons={d.reasons} />
                <Text size="xs" c="dimmed">
                  {t('network.dup.detected', { date: fmt.dateLong(d.detectedAt) })}
                </Text>
                <Group gap="xs">
                  <Button size="compact-sm" variant="default" onClick={() => void dismiss(d.id)}>
                    {t('network.dup.dismiss')}
                  </Button>
                  <Button size="compact-sm" onClick={() => setMerging(d)}>
                    {t('network.dup.merge')}
                  </Button>
                </Group>
              </Stack>
            </Group>
          </Card>
        ))}
        {(res.data?.page.totalPages ?? 0) > 1 && <AppPagination total={res.data!.page.totalPages} value={page + 1} onChange={(p) => setPage(p - 1)} />}
      </Stack>
      <MergeDialog
        key={merging?.id ?? 'none'}
        pair={merging}
        onClose={() => setMerging(undefined)}
        onMerged={() => {
          setNotice({ color: 'green', text: t('network.dup.merged') });
          res.reload();
        }}
      />
    </Container>
  );
}
