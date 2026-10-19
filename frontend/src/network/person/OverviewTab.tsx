// Overview: career history, former client roles and open next actions.
import { Stack, Text, Title } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { useFormat, type Schemas } from '../../pipeline/shared';

export function OverviewTab({
  person,
  actions,
  roles,
}: {
  person: Schemas['Person'];
  actions: Schemas['NextAction'][];
  roles: Schemas['Contact'][];
}) {
  const { t } = useTranslation();
  const fmt = useFormat();
  const year = (d?: string | null) => (d ? new Date(d).getFullYear() : '');
  return (
    <Stack gap="md">
      {person.notes && <Text size="sm">{person.notes}</Text>}
      <section>
        <Title order={3} size="h5">
          {t('network.career')}
        </Title>
        {person.employments.length === 0 && <Text size="sm" c="dimmed">–</Text>}
        {person.employments.map((e, i) => (
          <Text key={i} size="sm">
            {[e.title, e.company].filter(Boolean).join(', ')} {e.startDate || e.endDate ? `${year(e.startDate)}-${year(e.endDate)}` : ''}
          </Text>
        ))}
      </section>
      <section>
        <Title order={3} size="h5">
          {t('network.nextActions')}
        </Title>
        {actions.length === 0 && <Text size="sm" c="dimmed">–</Text>}
        {actions.map((a) => (
          <Text key={a.id} size="sm" c={a.overdue ? 'red' : undefined}>
            {a.title} - {fmt.date(a.dueOn)}
          </Text>
        ))}
      </section>
      {roles.some((r) => !r.active) && (
        <section>
          <Title order={3} size="h5">
            {t('network.formerRoles')}
          </Title>
          {roles
            .filter((r) => !r.active)
            .map((r) => (
              <Text key={r.id} size="sm">
                {r.account.name} {r.role ? `(${r.role})` : ''} - {fmt.dateLong(r.endedOn)}
              </Text>
            ))}
        </section>
      )}
    </Stack>
  );
}
