// Relationship map (A-13): the person in the middle, their connections around them. Click a connection to see
// who they know (second degree) and to open their page, e.g. to find a warm introduction.
import { Anchor, Badge, Button, Card, Group, Stack, Text } from '@mantine/core';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { api } from '../../api';
import { type Schemas } from '../../pipeline/shared';

type Summary = Schemas['PersonSummary'];
type Conn = Schemas['PersonConnection'];
type Center = Pick<Summary, 'id' | 'name' | 'type'> & Partial<Summary>;

const W = 640;
const H = 460;
const CX = W / 2;
const CY = H / 2;
const R1 = 140;
const R2 = 205;
const MAX_FIRST = 14;
const MAX_SECOND = 5;

const TYPE_COLOR: Record<Schemas['PersonType'], string> = {
  EXECUTIVE: 'var(--mantine-color-blue-7)',
  CLIENT_CONTACT: 'var(--mantine-color-teal-7)',
  REFERRER: 'var(--mantine-color-grape-7)',
  CANDIDATE: 'var(--mantine-color-orange-7)',
};
const LINK_DASH: Record<Schemas['LinkType'], string | undefined> = { KNOWS: '4 4', WORKED_WITH: undefined, INTRODUCED: '1 5', REFERRED: '8 3 2 3', REPORTS_TO: undefined };
const LINK_WIDTH: Record<Schemas['LinkType'], number> = { KNOWS: 1.5, WORKED_WITH: 2, INTRODUCED: 2, REFERRED: 2, REPORTS_TO: 3 };

const initials = (n: string) => n.split(/\s+/).map((w) => w[0]).slice(0, 2).join('').toUpperCase();
const short = (n: string) => (n.length > 18 ? `${n.slice(0, 17)}…` : n);

type Node = { id: string; person: Center; x: number; y: number; r: number; level: 0 | 1 | 2 };
type Edge = { from: string; to: string; type: Schemas['LinkType'] };

/** Pure layout: ring 1 around the centre, ring 2 fanned out behind the expanded first-degree node. */
export function layoutMap(center: Center, links: Conn[], expanded: Record<string, Conn[]>) {
  const nodes = new Map<string, Node>([[center.id, { id: center.id, person: center, x: CX, y: CY, r: 30, level: 0 }]]);
  const edges: Edge[] = [];
  const first = links.slice(0, MAX_FIRST);
  first.forEach((l, i) => {
    const angle = (2 * Math.PI * i) / first.length - Math.PI / 2;
    if (!nodes.has(l.person.id)) nodes.set(l.person.id, { id: l.person.id, person: l.person, x: CX + R1 * Math.cos(angle), y: CY + R1 * Math.sin(angle), r: 22, level: 1 });
    edges.push({ from: center.id, to: l.person.id, type: l.type });
  });
  first.forEach((l, i) => {
    const kids = (expanded[l.person.id] ?? []).filter((k) => k.person.id !== center.id).slice(0, MAX_SECOND);
    const base = (2 * Math.PI * i) / first.length - Math.PI / 2;
    const spread = Math.min(0.9, (2 * Math.PI) / first.length);
    kids.forEach((k, j) => {
      const angle = kids.length === 1 ? base : base - spread / 2 + (spread * j) / (kids.length - 1);
      if (!nodes.has(k.person.id)) nodes.set(k.person.id, { id: k.person.id, person: k.person, x: CX + R2 * Math.cos(angle) * 1.5, y: CY + R2 * Math.sin(angle) * 1.05, r: 16, level: 2 });
      edges.push({ from: l.person.id, to: k.person.id, type: k.type });
    });
  });
  return { nodes: [...nodes.values()], edges, hidden: Math.max(0, links.length - MAX_FIRST) };
}

export function RelationshipMap({ center, links }: { center: Center; links: Conn[] }) {
  const { t } = useTranslation();
  const [expanded, setExpanded] = useState<Record<string, Conn[]>>({});
  const [selected, setSelected] = useState<string>();
  const [loading, setLoading] = useState(false);
  const [failed, setFailed] = useState(false);
  const { nodes, edges, hidden } = layoutMap(center, links, expanded);
  const byId = new Map(nodes.map((n) => [n.id, n]));
  const sel = selected ? byId.get(selected) : undefined;

  const expand = async (id: string) => {
    setLoading(true);
    setFailed(false);
    const r = await api.GET('/people/{id}/links', { params: { path: { id } } }).catch(() => undefined);
    setLoading(false);
    if (r?.data) setExpanded((e) => ({ ...e, [id]: r.data! }));
    else setFailed(true);
  };

  if (links.length === 0) return <Text c="dimmed">{t('network.noConnections')}</Text>;

  return (
    <Stack gap="sm">
      <Group gap="md">
        {(Object.keys(TYPE_COLOR) as Schemas['PersonType'][]).map((k) => (
          <Group key={k} gap={4}>
            <svg width="12" height="12" aria-hidden>
              <circle cx="6" cy="6" r="6" fill={TYPE_COLOR[k]} />
            </svg>
            <Text size="xs">{t(`network.types.${k}`)}</Text>
          </Group>
        ))}
      </Group>
      <Card withBorder p={0} style={{ overflow: 'hidden' }}>
        <svg viewBox={`0 0 ${W} ${H}`} width="100%" role="group" aria-label={t('network.map.label', { name: center.name })} data-testid="relationship-map">
          {edges.map((e, i) => {
            const a = byId.get(e.from)!;
            const b = byId.get(e.to)!;
            const hot = selected && (e.from === selected || e.to === selected);
            return <line key={i} x1={a.x} y1={a.y} x2={b.x} y2={b.y} stroke={hot ? 'var(--mantine-color-blue-6)' : 'var(--mantine-color-gray-5)'} strokeWidth={LINK_WIDTH[e.type]} strokeDasharray={LINK_DASH[e.type]} />;
          })}
          {nodes.map((n) => (
            <g
              key={n.id}
              transform={`translate(${n.x} ${n.y})`}
              role="button"
              tabIndex={0}
              aria-label={`${n.person.name}${n.person.company ? `, ${n.person.company}` : ''}`}
              aria-pressed={selected === n.id}
              style={{ cursor: 'pointer', outline: 'none' }}
              onClick={() => setSelected(n.id)}
              onKeyDown={(e) => (e.key === 'Enter' || e.key === ' ') && (e.preventDefault(), setSelected(n.id))}
            >
              <circle r={n.r} fill={TYPE_COLOR[n.person.type]} stroke={selected === n.id ? 'var(--mantine-color-dark-9)' : '#fff'} strokeWidth={selected === n.id ? 3 : 2} />
              <text textAnchor="middle" dy="0.35em" fontSize={n.level === 0 ? 14 : 11} fill="#fff" fontWeight={600} pointerEvents="none">
                {initials(n.person.name)}
              </text>
              <text textAnchor="middle" y={n.r + 13} fontSize={11} fill="var(--mantine-color-dark-7)" pointerEvents="none">
                {short(n.person.name)}
              </text>
            </g>
          ))}
        </svg>
      </Card>
      {hidden > 0 && <Text size="xs" c="dimmed">{t('network.map.more', { count: hidden })}</Text>}
      <Group gap="md">
        {(Object.keys(LINK_WIDTH) as Schemas['LinkType'][]).map((k) => (
          <Group key={k} gap={4}>
            <svg width="28" height="8" aria-hidden>
              <line x1="0" y1="4" x2="28" y2="4" stroke="var(--mantine-color-gray-6)" strokeWidth={LINK_WIDTH[k]} strokeDasharray={LINK_DASH[k]} />
            </svg>
            <Text size="xs">{t(`network.linkTypes.${k}`)}</Text>
          </Group>
        ))}
      </Group>
      {sel ? (
        <Card withBorder padding="sm" data-testid="map-detail">
          <Group justify="space-between" align="flex-start">
            <div>
              <Group gap="xs">
                <Text fw={600}>{sel.person.name}</Text>
                <Badge variant="light" size="sm">
                  {t(`network.types.${sel.person.type}`)}
                </Badge>
              </Group>
              <Text size="sm">{[sel.person.title, sel.person.company, sel.person.city].filter(Boolean).join(' - ')}</Text>
              {sel.person.owner && (
                <Text size="xs" c="dimmed">
                  {t('network.fields.owner')}: {sel.person.owner.name}
                </Text>
              )}
              {failed && <Text size="xs" c="red">{t('network.map.expandFailed')}</Text>}
            </div>
            <Group gap="xs">
              {sel.level !== 0 && (
                <Anchor component={Link} to={`/people/${sel.id}`} size="sm">
                  {t('network.map.open')}
                </Anchor>
              )}
              {sel.level === 1 && !expanded[sel.id] && (
                <Button size="compact-sm" loading={loading} onClick={() => void expand(sel.id)}>
                  {t('network.map.expand')}
                </Button>
              )}
              {sel.level === 1 && expanded[sel.id]?.length === 0 && <Text size="xs" c="dimmed">{t('network.map.noMore')}</Text>}
            </Group>
          </Group>
        </Card>
      ) : (
        <Text size="sm" c="dimmed">{t('network.map.hint')}</Text>
      )}
    </Stack>
  );
}
