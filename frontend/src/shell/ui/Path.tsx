// Path: chevron steps for a lifecycle (docs/ux/lightning-style.md section 2). Done, current and upcoming steps are
// told apart by fill AND a check icon / bold text, never by colour alone.
import { Box, Select, Text, Tooltip } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../auth/useAuth';
import { useReference, type LifecycleKind } from '../refdata/useReference';
import type { MoveRequest } from '../components/StatusMenu';

interface Props {
  kind: LifecycleKind;
  /** Current status value. */
  value: string;
  /** Same handler as StatusMenu: the caller opens the reason dialog when `reasonRequired`. */
  onMove: (m: MoveRequest) => void;
}

const CLIP = 'polygon(0 0, calc(100% - 10px) 0, 100% 50%, calc(100% - 10px) 100%, 0 100%, 10px 50%)';

/** Horizontal chevron row on wide screens; current step plus a select on phones. */
export function Path({ kind, value, onMove }: Props) {
  const { t } = useTranslation();
  const ref = useReference();
  const { isManager } = useAuth();
  const steps = ref.data?.lifecycles[kind].statuses ?? [];
  const current = steps.findIndex((s) => s.value === value);
  const nextOf = new Map((ref.def(kind, value)?.next ?? []).map((n) => [n.to, n]));
  if (steps.length === 0) return null;

  const move = (to: string) => {
    const n = nextOf.get(to);
    if (n && !(n.managerOnly && !isManager)) onMove({ to, reasonRequired: n.reasonRequired });
  };

  return (
    <>
      <Box component="ol" aria-label={t('shell.path')} visibleFrom="sm" m={0} p={0} style={{ display: 'flex', listStyle: 'none', gap: 2 }}>
        {steps.map((s, i) => {
          const n = nextOf.get(s.value);
          const blocked = !!n && n.managerOnly && !isManager;
          const isCurrent = i === current;
          const isDone = current >= 0 && i < current;
          const clickable = !!n && !blocked;
          const label = ref.label(kind, s.value);
          const step = (
            <Box
              component="button"
              type="button"
              disabled={!clickable}
              aria-current={isCurrent ? 'step' : undefined}
              onClick={() => move(s.value)}
              style={{
                width: '100%', minHeight: 32, padding: '0 18px', border: 0, clipPath: CLIP, font: 'inherit', fontSize: 13,
                cursor: clickable ? 'pointer' : 'default',
                fontWeight: isCurrent ? 700 : 500,
                background: isCurrent ? 'var(--color-accent)' : isDone ? 'var(--color-accentSoft)' : 'var(--color-surface3)',
                color: isCurrent ? '#fff' : isDone ? 'var(--color-accent)' : 'var(--color-ink2)',
              }}
            >
              {isDone && <span aria-hidden>✓ </span>}
              {label}
            </Box>
          );
          return (
            <li key={s.value} style={{ flex: '1 1 0', minWidth: 0 }}>
              {blocked ? <Tooltip label={t('shell.managerOnly', { action: label })}><div>{step}</div></Tooltip> : step}
            </li>
          );
        })}
      </Box>
      <Box hiddenFrom="sm">
        <Text fz="xs" c="dimmed" mb={4}>{t('shell.pathStep', { n: Math.max(current, 0) + 1, total: steps.length })}</Text>
        <Select
          aria-label={t('shell.path')}
          value={value}
          allowDeselect={false}
          onChange={(v) => v && v !== value && move(v)}
          data={steps.map((s) => ({ value: s.value, label: ref.label(kind, s.value), disabled: s.value !== value && !nextOf.has(s.value) }))}
        />
      </Box>
    </>
  );
}
