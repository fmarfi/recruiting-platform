// Editable job status badge: offers only the legal next statuses from /reference-data; manager-only moves are disabled for consultants.
import { Menu, Tooltip } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { StatusBadge, useLabel, type Schemas } from '../../shared';

export function JobStatusMenu({
  status,
  isManager,
  onChoose,
}: {
  status: Schemas['JobOrderStatus'];
  isManager: boolean;
  onChoose: (to: string, reasonRequired: boolean) => void;
}) {
  const { t } = useTranslation();
  const { def, label } = useLabel();
  const next = def('jobOrder', status)?.next ?? [];
  return (
    <Menu disabled={next.length === 0}>
      <Menu.Target>
        <button type="button" aria-label={t('pipeline.changeStatus')} style={{ border: 0, background: 'none', cursor: 'pointer', padding: 0 }}>
          <StatusBadge lifecycle="jobOrder" value={status} testId="job-status" />
        </button>
      </Menu.Target>
      <Menu.Dropdown>
        {next.map((n) => {
          const blocked = n.managerOnly && !isManager;
          const item = (
            <Menu.Item key={n.to} disabled={blocked} onClick={() => onChoose(n.to, n.reasonRequired)}>
              {label('jobOrder', n.to)}
            </Menu.Item>
          );
          return blocked ? (
            <Tooltip key={n.to} label={t('pipeline.managerOnly', { action: t('pipeline.reopenJob') })}>
              <span>{item}</span>
            </Tooltip>
          ) : (
            item
          );
        })}
      </Menu.Dropdown>
    </Menu>
  );
}
