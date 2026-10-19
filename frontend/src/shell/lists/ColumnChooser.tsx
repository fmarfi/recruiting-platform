// Column chooser: a checkbox list in a popover (S3).
import { Button, Checkbox, Popover, Stack } from '@mantine/core';
import { useTranslation } from 'react-i18next';

export interface ColumnOption {
  key: string;
  label: string;
}

interface Props {
  options: ColumnOption[];
  visible: string[];
  onChange: (visible: string[]) => void;
}

/** Keeps at least one column visible and the original column order. */
export function ColumnChooser({ options, visible, onChange }: Props) {
  const { t } = useTranslation();

  const toggle = (key: string, on: boolean) => {
    const next = options.map((o) => o.key).filter((k) => (k === key ? on : visible.includes(k)));
    if (next.length > 0) onChange(next);
  };

  return (
    <Popover position="bottom-end" withinPortal>
      <Popover.Target>
        <Button variant="default" size="sm">{t('shell.columns')}</Button>
      </Popover.Target>
      <Popover.Dropdown>
        <Stack gap="xs">
          {options.map((o) => (
            <Checkbox key={o.key} label={o.label} checked={visible.includes(o.key)} onChange={(e) => toggle(o.key, e.currentTarget.checked)} />
          ))}
        </Stack>
      </Popover.Dropdown>
    </Popover>
  );
}
