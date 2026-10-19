import { Group, Popover, Stack, Text, TextInput, UnstyledButton } from '@mantine/core';
import { useEffect, useRef, useState, type KeyboardEvent, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { StatusBadge } from '../components/StatusBadge';
import { loadRecent, saveRecent } from './recentSearches';
import { useTypeahead } from './useTypeahead';

interface Row {
  key: string;
  to: string;
  title: string;
  sub: string;
  badge?: ReactNode;
}

/** Global type-ahead (UX-02): "/" focuses it, arrows + Enter pick, Enter without a pick lands on /search. */
export function GlobalSearch() {
  const { t } = useTranslation();
  const nav = useNavigate();
  const input = useRef<HTMLInputElement>(null);
  const [q, setQ] = useState('');
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(-1);
  const res = useTypeahead(q);
  const term = q.trim();

  useEffect(() => {
    const onKey = (e: globalThis.KeyboardEvent) => {
      const tag = (e.target as HTMLElement | null)?.tagName;
      if (e.key === '/' && tag !== 'INPUT' && tag !== 'TEXTAREA') {
        e.preventDefault();
        input.current?.focus();
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, []);

  const rows: Row[] = res
    ? [
        ...res.accounts.map((a) => ({ key: `a${a.id}`, to: `/accounts/${a.id}`, title: a.name, sub: [a.sector, a.city].filter(Boolean).join(' - '), badge: <StatusBadge kind="account" value={a.status} size="sm" /> })),
        ...res.people.map((p) => ({ key: `p${p.id}`, to: `/people/${p.id}`, title: p.name, sub: [p.title, p.company].filter(Boolean).join(', ') })),
        ...res.jobOrders.map((j) => ({ key: `j${j.id}`, to: `/job-orders/${j.id}`, title: j.title, sub: j.account.name, badge: <StatusBadge kind="jobOrder" value={j.status} size="sm" /> })),
      ]
    : [];

  function go(to: string) {
    if (term) saveRecent(term);
    setOpen(false);
    setQ('');
    nav(to);
  }

  function onKeyDown(e: KeyboardEvent) {
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setActive((a) => Math.min(a + 1, rows.length - 1));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setActive((a) => Math.max(a - 1, -1));
    } else if (e.key === 'Escape') {
      setOpen(false);
    } else if (e.key === 'Enter') {
      e.preventDefault();
      const row = rows[active];
      if (row) go(row.to);
      else if (term) go(`/search?q=${encodeURIComponent(term)}`);
    }
  }

  const recent = term.length === 0 ? loadRecent() : [];
  const showDropdown = open && (recent.length > 0 || (term.length >= 2 && res !== null));

  return (
    <Popover opened={showDropdown} position="bottom-start" width="target" withinPortal>
      <Popover.Target>
        <TextInput
          ref={input}
          type="search"
          role="searchbox"
          aria-label={t('shell.searchPlaceholder')}
          placeholder={t('shell.searchPlaceholder')}
          value={q}
          onChange={(e) => {
            setQ(e.currentTarget.value);
            setActive(-1);
            setOpen(true);
          }}
          onFocus={() => setOpen(true)}
          onBlur={() => setTimeout(() => setOpen(false), 150)}
          onKeyDown={onKeyDown}
          style={{ flex: 1, maxWidth: 520 }}
        />
      </Popover.Target>
      <Popover.Dropdown p="xs">
        <Stack gap={2}>
          {recent.length > 0 && (
            <Text size="xs" c="dimmed">
              {t('shell.recentSearches')}
            </Text>
          )}
          {recent.map((r) => (
            <UnstyledButton key={r} onMouseDown={() => go(`/search?q=${encodeURIComponent(r)}`)} p="xs">
              {r}
            </UnstyledButton>
          ))}
          {rows.map((r, i) => (
            <UnstyledButton key={r.key} onMouseDown={() => go(r.to)} p="xs" bg={i === active ? 'var(--color-accentSoft)' : undefined} style={{ borderRadius: 'var(--radius-sm)' }}>
              <Group justify="space-between" wrap="nowrap">
                <div>
                  <Text size="sm" fw={600}>
                    {r.title}
                  </Text>
                  <Text size="xs" c="dimmed">
                    {r.sub}
                  </Text>
                </div>
                {r.badge}
              </Group>
            </UnstyledButton>
          ))}
          {term.length >= 2 && res && rows.length === 0 && <Text size="sm" p="xs">{t('shell.searchNone', { q: term })}</Text>}
          {term.length >= 2 && (
            <UnstyledButton onMouseDown={() => go(`/search?q=${encodeURIComponent(term)}`)} p="xs">
              <Text size="sm" c="brand">
                {t('shell.seeAll', { q: term })}
              </Text>
            </UnstyledButton>
          )}
        </Stack>
      </Popover.Dropdown>
    </Popover>
  );
}
