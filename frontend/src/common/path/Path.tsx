// Path: status chevrons for a lifecycle (docs/ux/lightning-style.md section 2).
import { Select, Text, Tooltip } from '@mantine/core';
import { useMediaQuery } from '@mantine/hooks';
import { useTranslation } from 'react-i18next';
import { useLabel, type LifecycleKey } from '../../pipeline/shared';
import { PathStep } from './PathStep';
import { stepsFor } from './pathSteps';

export type PathProps = {
  lifecycle: LifecycleKey;
  /** Current status value. */
  value: string;
  /** Managers may take manager-only transitions; others see them disabled with a tooltip. */
  isManager: boolean;
  /** Called for a legal next status; `reasonRequired` tells the host to ask for a reason first. */
  onChoose: (to: string, reasonRequired: boolean) => void;
  /** Hides click handling, for read-only pages. */
  disabled?: boolean;
};

/**
 * Horizontal chevron row for a status lifecycle. Click on a legal next step runs the same transition
 * as a status menu. On a phone it collapses to "Step n of m" plus a select.
 */
export function Path({ lifecycle, value, isManager, onChoose, disabled }: PathProps) {
  const { t } = useTranslation();
  const { ref, def, label } = useLabel();
  const phone = useMediaQuery('(max-width: 48em)');
  const statuses = ref?.lifecycles[lifecycle].statuses ?? [];
  const steps = stepsFor(statuses, value, (s) => label(lifecycle, s.value));
  const current = steps.findIndex((s) => s.state === 'current');
  const next = def(lifecycle, value)?.next ?? [];

  if (phone) {
    const options = next.map((n) => ({ value: n.to, label: label(lifecycle, n.to), disabled: n.managerOnly && !isManager }));
    return (
      <div data-testid="path" data-current={value}>
        <Text size="sm" fw={600}>
          {label(lifecycle, value)}{' '}
          <Text span c="dimmed" fw={400}>
            {t('path.stepOf', { n: current + 1, total: steps.length })}
          </Text>
        </Text>
        {!disabled && options.length > 0 && (
          <Select
            mt={4}
            size="xs"
            aria-label={t('path.changeStatus')}
            placeholder={t('path.changeStatus')}
            data={options}
            value={null}
            onChange={(to) => {
              const n = next.find((x) => x.to === to);
              if (n) onChoose(n.to, n.reasonRequired);
            }}
          />
        )}
      </div>
    );
  }

  return (
    <ol className="path" data-testid="path" data-current={value} aria-label={t('path.label')}>
      {steps.map((s) => {
        const n = next.find((x) => x.to === s.value);
        const blocked = !!n && n.managerOnly && !isManager;
        const clickable = !!n && !blocked && !disabled && s.state !== 'current';
        const step = (
          <PathStep
            state={s.state}
            label={s.label}
            tone={s.tone}
            onClick={clickable && n ? () => onChoose(n.to, n.reasonRequired) : undefined}
            blocked={blocked}
          />
        );
        return (
          <li key={s.value}>
            {blocked ? <Tooltip label={t('path.managerOnly')}>{step}</Tooltip> : step}
          </li>
        );
      })}
    </ol>
  );
}
