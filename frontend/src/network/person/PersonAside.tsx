// Aside of the person page (visual system v3 section 5): Details (DetailList) and Connections (top 5).
import { Anchor, Button, Group, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { useFormat, type Schemas } from '../../pipeline/shared';
import { DetailList } from '../../shell/components/DetailList';
import { Panel } from '../../shell/components/Panel';
import { safeLinkedIn } from './profile';

export function PersonDetails({ person: p }: { person: Schemas['Person'] }) {
  const { t } = useTranslation();
  const fmt = useFormat();
  return (
    <Panel title={t('network.details')} testId="person-details">
      <DetailList
        items={[
          { label: t('network.fields.email'), value: p.email, href: p.email ? `mailto:${p.email}` : undefined },
          { label: t('network.edit.phone'), value: p.phone, href: p.phone ? `tel:${p.phone}` : undefined },
          { label: 'LinkedIn', value: p.linkedinUrl, href: safeLinkedIn(p.linkedinUrl) ?? undefined },
          { label: t('network.fields.city'), value: p.city },
          { label: t('network.fields.type'), value: t(`network.types.${p.type}`) },
          { label: t('network.fields.owner'), value: p.owner.name },
          { label: t('network.pool.tags'), value: p.tags?.join(', ') },
          { label: t('network.fields.howWeKnow'), value: p.howWeKnow },
          { label: t('network.profile.source'), value: p.sourceChannel ? t(`network.pool.channel.${p.sourceChannel}`) : undefined },
          { label: t('network.lastUpdated'), value: fmt.dateLong(p.updatedAt) },
        ]}
      />
    </Panel>
  );
}

export function PersonConnections({ links, onViewAll }: { links: Schemas['PersonConnection'][]; onViewAll: () => void }) {
  const { t } = useTranslation();
  return (
    <Panel
      title={t('network.tabs.connections')}
      count={links.length}
      padding="none"
      empty={links.length === 0 ? t('network.noConnections') : undefined}
      actions={
        links.length > 5 ? (
          <Button size="compact-sm" variant="subtle" onClick={onViewAll}>
            {t('network.viewAll')}
          </Button>
        ) : undefined
      }
    >
      {links.slice(0, 5).map((l, i) => (
        <Group key={l.linkId} justify="space-between" gap="sm" wrap="nowrap" style={{ padding: 'var(--space-3) var(--space-4)', borderTop: i === 0 ? 0 : '1px solid var(--color-border)' }}>
          <Anchor component={Link} to={`/people/${l.person.id}`} size="sm" fw={500} style={{ minWidth: 0, overflowWrap: 'anywhere' }}>
            {l.person.name}
          </Anchor>
          <Text size="sm" c="var(--color-ink3)" style={{ whiteSpace: 'nowrap' }}>
            {t(`network.linkTypes.${l.type}`)}
          </Text>
        </Group>
      ))}
    </Panel>
  );
}
