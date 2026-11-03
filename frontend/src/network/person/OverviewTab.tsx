// Overview: career history, former client roles and open next actions. Empty sections name the existing action that fills them.
import { Button, Group, Stack, Text, Title } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { useFormat, type Schemas } from '../../pipeline/shared';

export function OverviewTab({
  person,
  actions,
  roles,
  onAddAction,
  onEdit,
}: {
  person: Schemas['Person'];
  actions: Schemas['NextAction'][];
  roles: Schemas['Contact'][];
  onAddAction: () => void;
  onEdit: () => void;
}) {
  const { t } = useTranslation();
  const fmt = useFormat();
  const year = (d?: string | null) => (d ? new Date(d).getFullYear() : '');
  return (
    <Stack gap="lg">
      {person.notes && <Text size="sm">{person.notes}</Text>}
      <section>
        <Title order={3} size="h5" mb="xs">
          {t('network.career')}
        </Title>
        {person.employments.length === 0 && (
          <Group gap="sm">
            <Text size="sm" c="dimmed">
              {t('network.noCareer')}
            </Text>
            <Button size="compact-sm" variant="light" onClick={onEdit}>
              {t('network.edit.action')}
            </Button>
          </Group>
        )}
        <Stack gap={0}>
          {person.employments.map((e, i) => (
            <Group key={i} justify="space-between" gap="sm" py="xs" wrap="nowrap">
              <Text size="sm" fw={600}>
                {[e.title, e.company].filter(Boolean).join(', ')}
              </Text>
              {(e.startDate || e.endDate) && (
                <Text size="xs" c="dimmed" style={{ fontVariantNumeric: 'tabular-nums', whiteSpace: 'nowrap' }}>
                  {`${year(e.startDate)}–${year(e.endDate) || t('network.currentRole')}`}
                </Text>
              )}
            </Group>
          ))}
        </Stack>
      </section>
      <section>
        <Title order={3} size="h5" mb="xs">
          {t('network.nextActions')}
        </Title>
        {actions.length === 0 && (
          <Group gap="sm">
            <Text size="sm" c="dimmed">
              {t('network.noNextActions')}
            </Text>
            <Button size="compact-sm" variant="light" onClick={onAddAction}>
              {t('network.addAction')}
            </Button>
          </Group>
        )}
        <Stack gap={0}>
          {actions.map((a) => (
            <Group key={a.id} justify="space-between" gap="sm" py="xs" wrap="nowrap">
              <Text size="sm" c={a.overdue ? 'red' : undefined} fw={a.overdue ? 600 : undefined}>
                {a.title}
              </Text>
              <Text size="xs" c={a.overdue ? 'red' : 'dimmed'} style={{ fontVariantNumeric: 'tabular-nums', whiteSpace: 'nowrap' }}>
                {fmt.date(a.dueOn)}
              </Text>
            </Group>
          ))}
        </Stack>
      </section>
      {roles.some((r) => !r.active) && (
        <section>
          <Title order={3} size="h5" mb="xs">
            {t('network.formerRoles')}
          </Title>
          <Stack gap={0}>
            {roles
              .filter((r) => !r.active)
              .map((r) => (
                <Group key={r.id} justify="space-between" gap="sm" py="xs" wrap="nowrap">
                  <Text size="sm" fw={600}>
                    {r.account.name} {r.role ? `(${r.role})` : ''}
                  </Text>
                  <Text size="xs" c="dimmed" style={{ whiteSpace: 'nowrap' }}>
                    {fmt.dateLong(r.endedOn)}
                  </Text>
                </Group>
              ))}
          </Stack>
        </section>
      )}
    </Stack>
  );
}
