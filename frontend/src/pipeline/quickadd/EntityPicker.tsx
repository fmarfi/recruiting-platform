// Type-ahead picker for an account or a person (used by quick add).
import { Select } from '@mantine/core';
import { useDebouncedValue } from '@mantine/hooks';
import { useEffect, useState } from 'react';
import { api } from '../../api';

export type Option = { value: string; label: string };

export function EntityPicker({
  kind,
  label,
  value,
  onChange,
}: {
  kind: 'account' | 'person';
  label: string;
  value: Option | null;
  onChange: (v: Option | null) => void;
}) {
  const [search, setSearch] = useState('');
  const [q] = useDebouncedValue(search, 200);
  const [options, setOptions] = useState<Option[]>([]);
  useEffect(() => {
    if (q.trim().length < 2) return;
    let live = true;
    const run =
      kind === 'account'
        ? api.GET('/accounts', { params: { query: { q, size: 8 } } }).then((r) => r.data?.content.map((a) => ({ value: a.id, label: a.name })))
        : api.GET('/people', { params: { query: { q, size: 8 } } }).then((r) => r.data?.content.map((p) => ({ value: p.id, label: p.name })));
    run.then((o) => live && setOptions(o ?? [])).catch(() => undefined);
    return () => {
      live = false;
    };
  }, [q, kind]);
  const data = value && !options.some((o) => o.value === value.value) ? [value, ...options] : options;
  return (
    <Select
      label={label}
      searchable
      clearable
      data={data}
      value={value?.value ?? null}
      filter={({ options: o }) => o}
      onSearchChange={setSearch}
      onChange={(v) => onChange(data.find((o) => o.value === v) ?? null)}
      nothingFoundMessage="–"
    />
  );
}
