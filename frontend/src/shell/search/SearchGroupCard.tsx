import { Anchor, Button, Card, Group, Text, Title } from '@mantine/core';
import { useState, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';

export interface SearchRow {
  id: string;
  to: string;
  title: string;
  sub: string;
  right?: ReactNode;
}

function Highlight({ text, q }: { text: string; q: string }) {
  const i = q ? text.toLocaleLowerCase().indexOf(q.toLocaleLowerCase()) : -1;
  if (i < 0) return <>{text}</>;
  return (
    <>
      {text.slice(0, i)}
      <mark>{text.slice(i, i + q.length)}</mark>
      {text.slice(i + q.length)}
    </>
  );
}

/** One result group: 5 rows, "Show all N" expands in place. */
export function SearchGroupCard({ title, total, rows, q }: { title: string; total: number; rows: SearchRow[]; q: string }) {
  const { t } = useTranslation();
  const [all, setAll] = useState(false);
  if (rows.length === 0) return null;
  return (
    <Card withBorder>
      <Title order={2} size="h4">
        {title} ({total})
      </Title>
      {(all ? rows : rows.slice(0, 5)).map((r) => (
        <Group key={r.id} justify="space-between" py={6} wrap="nowrap">
          <div>
            <Anchor component={Link} to={r.to} fw={600}>
              <Highlight text={r.title} q={q} />
            </Anchor>
            <Text size="sm" c="dimmed">
              {r.sub}
            </Text>
          </div>
          {r.right}
        </Group>
      ))}
      {rows.length > 5 && !all && (
        <Button variant="subtle" size="xs" onClick={() => setAll(true)}>
          {t('search.showAll', { n: total })}
        </Button>
      )}
    </Card>
  );
}
