import { Box, Group, Kbd, Modal, Pill, Stack, Text, TextInput } from '@mantine/core';
import { useEffect, useId, useMemo, useState, type KeyboardEvent, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { NewAccountDialog } from '../../accounts/components/NewAccountDialog';
import { NewPersonDialog } from '../../network/list/NewPersonDialog';
import { QuickAdd } from '../../pipeline/quickadd/QuickAdd';
import { useAuth } from '../auth/useAuth';
import { StatusBadge } from '../components/StatusBadge';
import { useTypeahead } from '../search/useTypeahead';
import { matchesText, moveActive, parseQuery, removeOperator, routeFor, type OperatorKey } from './operators';
import { loadRecentItems, pushRecentItem } from './recentItems';

type Section = 'filter' | 'actions' | 'accounts' | 'people' | 'jobs' | 'recent' | 'search';
type Dialog = null | 'account' | 'person' | 'activity';

interface Item {
  id: string;
  group: Section;
  label: string;
  sub?: string;
  badge?: ReactNode;
  run: () => void;
}

const GROUP_ORDER: Section[] = ['filter', 'actions', 'accounts', 'people', 'jobs', 'recent', 'search'];

/** Global command bar (Ctrl+K): operators, quick actions, grouped /search results, recent items. */
export function CommandPalette({ opened, onClose, onChanged }: { opened: boolean; onClose: () => void; onChanged: () => void }) {
  const { t } = useTranslation();
  const nav = useNavigate();
  const { isManager } = useAuth();
  const uid = useId();
  const listId = `${uid}-list`;
  const [q, setQ] = useState('');
  const [active, setActive] = useState(0);
  const [dialog, setDialog] = useState<Dialog>(null);
  const parsed = useMemo(() => parseQuery(q), [q]);
  const route = useMemo(() => routeFor(parsed), [parsed]);
  const res = useTypeahead(parsed.text);

  useEffect(() => {
    if (opened) {
      setQ('');
      setActive(0);
    }
  }, [opened]);

  const opLabel: Record<OperatorKey, string> = {
    owner: t('command.op.owner'),
    stale: t('command.op.stale'),
    city: t('command.op.city'),
    type: t('command.op.type'),
    consent: t('command.op.consent'),
    company: t('command.op.company'),
    status: t('command.op.status'),
  };

  const items = useMemo<Item[]>(() => {
    const go = (to: string, rec?: { title: string; sub?: string }) => () => {
      if (rec) pushRecentItem({ to, ...rec });
      onClose();
      nav(to);
    };
    const open = (d: Dialog) => () => {
      onClose();
      setDialog(d);
    };
    const out: Item[] = [];
    if (route) out.push({ id: 'filter', group: 'filter', label: t('command.openList', { list: t(route.path === '/network' ? 'nav.network' : route.path === '/accounts' ? 'nav.accounts' : 'nav.jobOrders') }), sub: route.to, run: go(route.to) });

    if (!(parsed.operators.length && !parsed.text)) {
      const actions: [string, string, () => void][] = [
        ['newClient', t('accounts.newAccount'), open('account')],
        ['newPerson', t('network.newPerson'), open('person')],
        ['logActivity', t('accounts.logActivity'), open('activity')],
        ['goMyDay', t('command.goTo', { page: t('nav.myDay') }), go('/')],
        ['goAccounts', t('command.goTo', { page: t('nav.accounts') }), go('/accounts')],
        ['goNetwork', t('command.goTo', { page: t('nav.network') }), go('/network')],
        ['goJobs', t('command.goTo', { page: t('nav.jobOrders') }), go('/job-orders')],
        ...(isManager ? ([['goSettings', t('command.goTo', { page: t('nav.settings') }), go('/settings/users')]] as [string, string, () => void][]) : []),
      ];
      for (const [id, label, run] of actions) if (!parsed.text || matchesText(label, parsed.text)) out.push({ id: `act-${id}`, group: 'actions', label, run });
    }

    if (res && parsed.text.length >= 2) {
      for (const a of res.accounts) {
        const sub = [a.sector, a.city].filter(Boolean).join(' - ');
        out.push({ id: `a-${a.id}`, group: 'accounts', label: a.name, sub, badge: <StatusBadge kind="account" value={a.status} size="sm" />, run: go(`/accounts/${a.id}`, { title: a.name, sub }) });
      }
      for (const p of res.people) {
        const sub = [p.title, p.company].filter(Boolean).join(', ');
        out.push({ id: `p-${p.id}`, group: 'people', label: p.name, sub, run: go(`/people/${p.id}`, { title: p.name, sub }) });
      }
      for (const j of res.jobOrders) out.push({ id: `j-${j.id}`, group: 'jobs', label: j.title, sub: j.account.name, badge: <StatusBadge kind="jobOrder" value={j.status} size="sm" />, run: go(`/job-orders/${j.id}`, { title: j.title, sub: j.account.name }) });
    }

    if (!q.trim()) for (const r of loadRecentItems()) out.push({ id: `r-${r.to}`, group: 'recent', label: r.title, sub: r.sub, run: go(r.to) });
    if (parsed.text.length >= 2) out.push({ id: 'search', group: 'search', label: t('shell.seeAll', { q: parsed.text }), run: go(`/search?q=${encodeURIComponent(parsed.text)}`) });
    return out.sort((a, b) => GROUP_ORDER.indexOf(a.group) - GROUP_ORDER.indexOf(b.group));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [route, parsed, res, q, isManager, t, nav, onClose, opened]);

  const idx = Math.min(active, items.length - 1);
  const optionId = (i: number) => `${uid}-opt-${i}`;
  useEffect(() => {
    document.getElementById(optionId(idx))?.scrollIntoView?.({ block: 'nearest' });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [idx]);

  function onKeyDown(e: KeyboardEvent) {
    if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
      e.preventDefault();
      setActive(moveActive(idx, e.key === 'ArrowDown' ? 1 : -1, items.length));
    } else if (e.key === 'Enter') {
      e.preventDefault();
      items[idx]?.run();
    }
    // Escape is handled by the Modal.
  }

  const groupLabel: Record<Section, string> = {
    filter: t('command.group.filter'),
    actions: t('command.group.actions'),
    accounts: t('command.group.accounts'),
    people: t('command.group.people'),
    jobs: t('command.group.jobs'),
    recent: t('command.group.recent'),
    search: t('command.group.search'),
  };
  const shown = GROUP_ORDER.filter((g) => items.some((i) => i.group === g));

  return (
    <>
      <Modal opened={opened} onClose={onClose} withCloseButton={false} padding={0} size="lg" yOffset="10vh" aria-label={t('command.title')} transitionProps={{ duration: 0 }}>
        <Stack gap={0}>
          <Box p="sm" style={{ borderBottom: '1px solid var(--color-border, #e5e7eb)' }}>
            <TextInput
              data-autofocus
              role="combobox"
              aria-expanded
              aria-haspopup="listbox"
              aria-controls={listId}
              aria-autocomplete="list"
              aria-activedescendant={items.length ? optionId(idx) : undefined}
              aria-label={t('command.placeholder')}
              placeholder={t('command.placeholder')}
              value={q}
              onChange={(e) => {
                setQ(e.currentTarget.value);
                setActive(0);
              }}
              onKeyDown={onKeyDown}
            />
            {parsed.operators.length > 0 && (
              <Group gap={6} mt="xs" aria-label={t('command.filters')}>
                {parsed.operators.map((o) => (
                  <Pill
                    key={`${o.key}-${o.start}`}
                    withRemoveButton
                    onRemove={() => setQ(removeOperator(q, o))}
                    removeButtonProps={{ 'aria-label': t('command.removeFilter', { name: `${opLabel[o.key]} ${o.value}` }) }}
                    style={route?.dropped.includes(o) ? { opacity: 0.5 } : undefined}
                    title={route?.dropped.includes(o) ? t('command.ignored') : undefined}
                  >
                    {opLabel[o.key]}: {o.value}
                  </Pill>
                ))}
              </Group>
            )}
          </Box>
          <Box id={listId} role="listbox" aria-label={t('command.results')} mah="55vh" style={{ overflowY: 'auto' }} p="xs">
            {items.length === 0 && (
              <Text size="sm" c="dimmed" p="sm">
                {t('command.none')}
              </Text>
            )}
            {shown.map((g) => (
              <Box key={g} role="group" aria-labelledby={`${uid}-g-${g}`} mb="xs">
                <Text id={`${uid}-g-${g}`} size="xs" fw={700} c="dimmed" tt="uppercase" px="xs" py={4}>
                  {groupLabel[g]}
                </Text>
                {items.map((it, i) =>
                  it.group !== g ? null : (
                    <Box
                      key={it.id}
                      id={optionId(i)}
                      role="option"
                      aria-selected={i === idx}
                      onMouseEnter={() => setActive(i)}
                      onClick={it.run}
                      px="xs"
                      py={6}
                      style={{ cursor: 'pointer', borderRadius: 'var(--radius-sm)', background: i === idx ? 'var(--color-accentSoft)' : undefined }}
                    >
                      <Group justify="space-between" wrap="nowrap">
                        <div>
                          <Text size="sm" fw={600}>
                            {it.label}
                          </Text>
                          {it.sub && (
                            <Text size="xs" c="dimmed">
                              {it.sub}
                            </Text>
                          )}
                        </div>
                        {it.badge}
                      </Group>
                    </Box>
                  ),
                )}
              </Box>
            ))}
          </Box>
          <Group gap="md" p="xs" px="sm" style={{ borderTop: '1px solid var(--color-border, #e5e7eb)' }}>
            <Text size="xs" c="dimmed">
              <Kbd size="xs">{'\u2191'}</Kbd> <Kbd size="xs">{'\u2193'}</Kbd> {t('command.hintMove')}
            </Text>
            <Text size="xs" c="dimmed">
              <Kbd size="xs">Enter</Kbd> {t('command.hintOpen')}
            </Text>
            <Text size="xs" c="dimmed">
              <Kbd size="xs">Esc</Kbd> {t('command.hintClose')}
            </Text>
          </Group>
        </Stack>
      </Modal>
      <QuickAdd opened={dialog === 'activity'} initialMode="activity" onClose={() => setDialog(null)} onSaved={onChanged} />
      <NewAccountDialog opened={dialog === 'account'} onClose={() => setDialog(null)} onCreated={(id) => nav(`/accounts/${id}`)} />
      <NewPersonDialog opened={dialog === 'person'} onClose={() => setDialog(null)} onCreated={onChanged} />
    </>
  );
}

