import { Select } from '@mantine/core';
import { useEffect, useState } from 'react';
import { api } from '../../api';

/** Type-ahead account picker for the quick-add sheets (UX-06). */
export function AccountPicker({ value, onChange, label, optional }: { value: string | null; onChange: (id: string | null) => void; label: string; optional?: boolean }) {
  const [q, setQ] = useState('');
  const [opts, setOpts] = useState<{ value: string; label: string }[]>([]);
  useEffect(() => {
    const h = setTimeout(() => {
      void api.GET('/accounts', { params: { query: { q: q || undefined, size: 20 } } }).then(({ data }) => {
        if (data) setOpts(data.content.map((a) => ({ value: a.id, label: a.name })));
      });
    }, 200);
    return () => clearTimeout(h);
  }, [q]);
  return (
    <Select
      label={label}
      data={opts}
      value={value}
      onChange={onChange}
      searchable
      clearable={optional}
      onSearchChange={setQ}
      filter={({ options }) => options}
      nothingFoundMessage="-"
      required={!optional}
    />
  );
}
