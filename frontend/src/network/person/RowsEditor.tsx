// Repeatable rows (career, languages, education) for the person edit drawer. The value is a JSON string so it fits the drawer's string form.
import { ActionIcon, Button, Fieldset, Group, NativeSelect, Stack, Text, TextInput } from '@mantine/core';
import { useTranslation } from 'react-i18next';

export interface Column {
  key: string;
  label: string;
  kind?: 'text' | 'date' | 'number' | 'select';
  options?: { value: string; label: string }[];
  maxLength?: number;
  required?: boolean;
}

type Row = Record<string, string | number | null | undefined>;

export const parseRows = (json: string): Row[] => {
  try {
    const v = JSON.parse(json || '[]') as unknown;
    return Array.isArray(v) ? (v as Row[]) : [];
  } catch {
    return [];
  }
};

/** Rows to send: numbers as numbers, blank cells as null, rows with every cell blank dropped. */
export function cleanRows(json: string, cols: Column[]): Row[] {
  return parseRows(json)
    .map((r) => Object.fromEntries(cols.map((c) => {
      const raw = r[c.key];
      const blank = raw === undefined || raw === null || String(raw).trim() === '';
      return [c.key, blank ? null : c.kind === 'number' ? Number(raw) : String(raw).trim()];
    })))
    .filter((r) => Object.values(r).some((v) => v !== null));
}

interface Props {
  legend: string;
  columns: Column[];
  value: string;
  onChange: (json: string) => void;
  addLabel: string;
  removeLabel: (n: number) => string;
  error?: string;
  max?: number;
}

export function RowsEditor({ legend, columns, value, onChange, addLabel, removeLabel, error, max = 20 }: Props) {
  const { t } = useTranslation();
  const rows = parseRows(value);
  const write = (next: Row[]) => onChange(JSON.stringify(next));
  const blank: Row = Object.fromEntries(columns.map((c) => [c.key, c.kind === 'select' ? c.options?.[0]?.value ?? '' : '']));
  return (
    <Fieldset legend={legend} p="xs">
      <Stack gap="sm">
        {rows.length === 0 && (
          <Text size="sm" c="dimmed">
            {t('network.edit.noRows')}
          </Text>
        )}
        {rows.map((r, i) => (
          <Group key={i} align="flex-end" gap="xs" role="group" aria-label={`${legend} ${i + 1}`}>
            {columns.map((c) =>
              c.kind === 'select' ? (
                <NativeSelect key={c.key} label={c.label} data={c.options ?? []} value={String(r[c.key] ?? '')} onChange={(e) => write(rows.map((x, j) => (j === i ? { ...x, [c.key]: e.currentTarget.value } : x)))} />
              ) : (
                <TextInput
                  key={c.key}
                  label={c.label}
                  type={c.kind === 'date' ? 'date' : c.kind === 'number' ? 'number' : 'text'}
                  maxLength={c.maxLength}
                  withAsterisk={c.required}
                  w={c.kind === 'number' ? 96 : undefined}
                  value={String(r[c.key] ?? '')}
                  onChange={(e) => write(rows.map((x, j) => (j === i ? { ...x, [c.key]: e.currentTarget.value } : x)))}
                />
              ),
            )}
            <ActionIcon variant="subtle" color="red" size="lg" aria-label={removeLabel(i + 1)} onClick={() => write(rows.filter((_, j) => j !== i))}>
              <span aria-hidden>{'×'}</span>
            </ActionIcon>
          </Group>
        ))}
        {error && (
          <Text size="sm" c="red.8" role="alert">
            {error}
          </Text>
        )}
        <Group>
          <Button size="compact-sm" variant="light" disabled={rows.length >= max} onClick={() => write([...rows, blank])}>
            + {addLabel}
          </Button>
        </Group>
      </Stack>
    </Fieldset>
  );
}
