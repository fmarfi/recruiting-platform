// List-view selector: built-in views plus browser-saved ones, with save and delete (S3).
import { Button, Group, Menu, TextInput } from '@mantine/core';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';

export interface ViewOption {
  value: string;
  label: string;
  /** Saved views can be deleted; built-in ones cannot. */
  custom?: boolean;
}

interface Props {
  options: ViewOption[];
  /** Value of the selected view, or empty when the current filters match none. */
  value: string;
  onSelect: (value: string) => void;
  onSave: (name: string) => void;
  onDelete: (value: string) => void;
}

/** Dropdown of views; the button shows the selected view or a generic label. */
export function ViewSelector({ options, value, onSelect, onSave, onDelete }: Props) {
  const { t } = useTranslation();
  const [name, setName] = useState('');
  const current = options.find((o) => o.value === value);

  const submit = () => {
    if (!name.trim()) return;
    onSave(name.trim());
    setName('');
  };

  return (
    <Menu position="bottom-start" withinPortal>
      <Menu.Target>
        <Button variant="default" size="sm" aria-label={t('shell.views')}>
          {current?.label ?? t('shell.views')} ▾
        </Button>
      </Menu.Target>
      <Menu.Dropdown miw={240}>
        {options.map((o) => (
          <Menu.Item
            key={o.value}
            fw={o.value === value ? 600 : 400}
            onClick={() => onSelect(o.value)}
            rightSection={
              o.custom ? (
                <Button
                  component="span"
                  size="compact-xs"
                  variant="subtle"
                  color="red"
                  aria-label={`${t('shell.deleteView')}: ${o.label}`}
                  onClick={(e) => {
                    e.stopPropagation();
                    onDelete(o.value);
                  }}
                >
                  x
                </Button>
              ) : undefined
            }
          >
            {o.label}
          </Menu.Item>
        ))}
        <Menu.Divider />
        <Group p="xs" gap="xs" wrap="nowrap">
          <TextInput
            size="xs"
            placeholder={t('shell.viewName')}
            aria-label={t('shell.viewName')}
            value={name}
            onChange={(e) => setName(e.currentTarget.value)}
            onKeyDown={(e) => e.stopPropagation()}
          />
          <Button size="compact-sm" onClick={submit}>{t('shell.saveView')}</Button>
        </Group>
      </Menu.Dropdown>
    </Menu>
  );
}
