// Job order page header: breadcrumb, title, actions, status Path and highlights panel (lightning-style P1, P4).
import { Anchor, Button, Group, Text, Title } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { Path } from '../../../common/path/Path';
import { Highlights } from '../../../common/record/Highlights';
import { useFormat, type Schemas } from '../../shared';

type Props = {
  job: Schemas['JobOrder'];
  isManager: boolean;
  readOnly: boolean;
  onStatus: (to: string, reasonRequired: boolean) => void;
  onAdd: () => void;
  onLog: () => void;
};

/** Everything above the tabs. Status changes go through the Path, which offers only legal next steps. */
export function JobHeader({ job: j, isManager, readOnly, onStatus, onAdd, onLog }: Props) {
  const { t } = useTranslation();
  const fmt = useFormat();
  return (
    <>
      <Group gap="xs" mb="xs">
        <Anchor component={Link} to={`/accounts/${j.account.id}`} size="sm">
          {j.account.name}
        </Anchor>
        <Text size="sm" c="dimmed">
          / {j.title}
        </Text>
      </Group>
      <Group justify="space-between" align="flex-start" mb="sm">
        <Title order={1} data-testid="screen-title" size="h2">
          {j.title}
        </Title>
        <Group gap="xs">
          {!readOnly && <Button onClick={onAdd}>+ {t('pipeline.addCandidate')}</Button>}
          <Button variant="default" onClick={onLog}>
            {t('pipeline.logActivity')}
          </Button>
        </Group>
      </Group>
      <div style={{ marginBottom: 12 }} data-testid="job-status">
        <Path lifecycle="jobOrder" value={j.status} isManager={isManager} onChoose={onStatus} />
      </div>
      <Highlights
        items={[
          { label: t('pipeline.client'), value: <Anchor component={Link} to={`/accounts/${j.account.id}`}>{j.account.name}</Anchor> },
          { label: t('pipeline.location'), value: j.location },
          { label: t('pipeline.seniority'), value: j.seniority },
          { label: t('pipeline.fee'), value: j.feeTerms },
          { label: t('pipeline.owner'), value: j.owner.name },
          { label: t('pipeline.opened'), value: `${fmt.dateLong(j.openedOn)} (${t('pipeline.daysOpen', { count: j.daysOpen })})` },
          { label: t('pipeline.activeCandidatesLabel'), value: j.activeCandidates },
        ]}
      />
    </>
  );
}
