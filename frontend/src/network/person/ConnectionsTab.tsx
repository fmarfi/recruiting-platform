// Connections (A-13): a list grouped by link type, each row links to the other person. Phase 1 has no graph.
import { Anchor, Button, Group, Stack, Text, Title } from '@mantine/core';
import { EmptyState } from '../../shell/components/EmptyState';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { StatusBadge, type Schemas } from '../../pipeline/shared';

export function ConnectionsTab({ links, onAdd }: { links: Schemas['PersonConnection'][]; onAdd: () => void }) {
  const { t } = useTranslation();
  const groups = [...new Set(links.map((l) => l.type))];
  if (links.length === 0)
    return <EmptyState title={t('network.noConnections')} hint={t('network.noConnectionsAction')} actions={<Button onClick={onAdd}>+ {t('network.addConnection')}</Button>} />;
  return (
    <Stack>
      <Group justify="flex-end">
        <Button variant="light" onClick={onAdd}>
          + {t('network.addConnection')}
        </Button>
      </Group>
      {groups.map((g) => (
        <section key={g}>
          <Title order={3} size="h5">
            {t(`network.linkTypes.${g}`)}
          </Title>
          {links
            .filter((l) => l.type === g)
            .map((l) => (
              <Group key={l.linkId} gap="xs" my={4}>
                <Anchor component={Link} to={`/people/${l.person.id}`}>
                  {l.person.name}
                </Anchor>
                <StatusBadge lifecycle="consent" value={l.person.consentStatus} />
                <Text size="xs" c="dimmed">
                  {l.direction === 'OUTGOING' ? '→' : l.direction === 'INCOMING' ? '←' : '↔'} {l.note}
                </Text>
              </Group>
            ))}
        </section>
      ))}
    </Stack>
  );
}
