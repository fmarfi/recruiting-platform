// Overview main column (visual system v3 section 5): Career, candidate profile, Activity (latest 5), Next actions, former roles, notes. Each is a Panel.
import { Button, Group, Text } from '@mantine/core';
import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { useFormat, type Schemas } from '../../pipeline/shared';
import { Panel } from '../../shell/components/Panel';
import { ProfileSection } from './ProfileSection';
import { NextStepLine } from './RelationshipSnapshot';

/** A row of a panel list: 12/16 padding, 1 px divider between rows. */
function Row({ first, children }: { first: boolean; children: ReactNode }) {
  return (
    <Group justify="space-between" align="baseline" gap="sm" wrap="nowrap" style={{ padding: 'var(--space-3) var(--space-4)', borderTop: first ? 0 : '1px solid var(--color-border)' }}>
      {children}
    </Group>
  );
}

export function OverviewTab({
  person,
  actions,
  activities,
  roles,
  onAddAction,
  onEdit,
  onViewAllActivity,
}: {
  person: Schemas['Person'];
  actions: Schemas['NextAction'][];
  activities: Schemas['Activity'][];
  roles: Schemas['Contact'][];
  onAddAction: () => void;
  onEdit: () => void;
  onViewAllActivity: () => void;
}) {
  const { t } = useTranslation();
  const fmt = useFormat();
  const year = (d?: string | null) => (d ? new Date(d).getFullYear() : '');
  const former = roles.filter((r) => !r.active);
  return (
    <>
      <Panel
        title={t('network.career')}
        padding="none"
        actions={
          <Button size="compact-sm" variant="default" onClick={onEdit} disabled={person.anonymised}>
            {t('network.edit.action')}
          </Button>
        }
      >
        {person.employments.length === 0 && (
          <Text size="sm" c="var(--color-ink3)" ta="center" py="md">
            {t('network.noCareer')}
          </Text>
        )}
        {person.employments.map((e, i) => (
          <Row key={i} first={i === 0}>
            <Text size="sm" fw={500} c="var(--color-ink)" style={{ minWidth: 0, overflowWrap: 'anywhere' }}>
              {[e.title, e.company].filter(Boolean).join(', ')}
            </Text>
            {(e.startDate || e.endDate) && (
              <Text size="sm" c="var(--color-ink3)" style={{ fontVariantNumeric: 'tabular-nums', whiteSpace: 'nowrap' }}>
                {`${year(e.startDate)}–${year(e.endDate) || t('network.currentRole')}`}
              </Text>
            )}
          </Row>
        ))}
      </Panel>
      <ProfileSection person={person} />
      <Panel
        title={t('network.tabs.activity')}
        padding="none"
        empty={activities.length === 0 ? t('network.noActivity') : undefined}
        actions={
          activities.length > 0 ? (
            <Button size="compact-sm" variant="subtle" onClick={onViewAllActivity}>
              {t('network.viewAll')}
            </Button>
          ) : undefined
        }
      >
        {activities.slice(0, 5).map((a, i) => (
          <Row key={a.id} first={i === 0}>
            <div style={{ minWidth: 0 }}>
              <Text size="sm" fw={500} c="var(--color-ink)">
                {t(`quickAdd.types.${a.type}`)}
              </Text>
              {a.note && (
                <Text size="sm" c="var(--color-ink2)" lineClamp={2} style={{ overflowWrap: 'anywhere' }}>
                  {a.note}
                </Text>
              )}
              <Text size="xs" c="var(--color-ink3)">
                {a.user.name}
              </Text>
            </div>
            <Text size="sm" c="var(--color-ink3)" style={{ whiteSpace: 'nowrap' }}>
              {fmt.date(a.occurredAt)}
            </Text>
          </Row>
        ))}
      </Panel>
      <Panel
        title={t('network.nextActions')}
        count={actions.length}
        padding="none"
        actions={
          <Button size="compact-sm" variant="default" onClick={onAddAction}>
            {t('network.addAction')}
          </Button>
        }
      >
        <div style={{ padding: 'var(--space-3) var(--space-4)' }}>
          <NextStepLine person={person} activities={activities} actions={actions} />
        </div>
        {actions.length === 0 && (
          <Text size="sm" c="var(--color-ink3)" ta="center" pb="md">
            {t('network.noNextActions')}
          </Text>
        )}
        {actions.map((a) => (
          <Row key={a.id} first={false}>
            <Text size="sm" fw={500} c={a.overdue ? 'var(--tone-warn-fg)' : 'var(--color-ink)'} style={{ minWidth: 0, overflowWrap: 'anywhere' }}>
              {a.title}
            </Text>
            <Text size="sm" c={a.overdue ? 'var(--tone-warn-fg)' : 'var(--color-ink3)'} style={{ fontVariantNumeric: 'tabular-nums', whiteSpace: 'nowrap' }}>
              {fmt.date(a.dueOn)}
            </Text>
          </Row>
        ))}
      </Panel>
      {former.length > 0 && (
        <Panel title={t('network.formerRoles')} padding="none">
          {former.map((r, i) => (
            <Row key={r.id} first={i === 0}>
              <Text size="sm" fw={500} c="var(--color-ink)">
                {r.account.name} {r.role ? `(${r.role})` : ''}
              </Text>
              <Text size="sm" c="var(--color-ink3)" style={{ whiteSpace: 'nowrap' }}>
                {fmt.dateLong(r.endedOn)}
              </Text>
            </Row>
          ))}
        </Panel>
      )}
      {person.notes && (
        <Panel title={t('network.notes')}>
          <Text size="sm" c="var(--color-ink)" style={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>
            {person.notes}
          </Text>
        </Panel>
      )}
    </>
  );
}
