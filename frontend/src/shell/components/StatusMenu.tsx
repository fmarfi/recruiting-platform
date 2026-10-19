import { Menu, Tooltip, UnstyledButton } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../auth/useAuth';
import { useReference, type LifecycleKind } from '../refdata/useReference';
import { StatusBadge } from './StatusBadge';
import { ToneBadge } from './ToneBadge';

export interface MoveRequest {
  to: string;
  reasonRequired: boolean;
}

/** Badge that is a menu of the legal next statuses (one click change, UX-03). Manager-only moves are disabled with a tooltip. */
export function StatusMenu({ kind, value, size = 'lg', onMove }: { kind: LifecycleKind; value: string; size?: 'sm' | 'md' | 'lg'; onMove: (m: MoveRequest) => void }) {
  const ref = useReference();
  const { isManager } = useAuth();
  const { t } = useTranslation();
  const next = ref.def(kind, value)?.next ?? [];
  if (next.length === 0) return <StatusBadge kind={kind} value={value} size={size} />;
  return (
    <Menu position="bottom-start" withinPortal>
      <Menu.Target>
        <UnstyledButton aria-label={t('shell.changeStatus')} aria-haspopup="menu" style={{ minHeight: 'var(--touch-target-min)', display: 'inline-flex', alignItems: 'center' }}>
          <StatusBadge kind={kind} value={value} size={size} />
          <span aria-hidden style={{ marginLeft: 4 }}>▾</span>
        </UnstyledButton>
      </Menu.Target>
      <Menu.Dropdown>
        {next.map((n) => {
          const blocked = n.managerOnly && !isManager;
          const label = ref.label(kind, n.to);
          const item = (
            <Menu.Item key={n.to} disabled={blocked} onClick={() => onMove({ to: n.to, reasonRequired: n.reasonRequired })}>
              <ToneBadge tone={ref.tone(kind, n.to)} label={label} size="sm" />
            </Menu.Item>
          );
          return blocked ? (
            <Tooltip key={n.to} label={t('shell.managerOnly', { action: label })} withinPortal>
              <div>{item}</div>
            </Tooltip>
          ) : (
            item
          );
        })}
      </Menu.Dropdown>
    </Menu>
  );
}
