import { ActionIcon, Group, Pill, Text, TextInput } from '@mantine/core';
import { useId, useState } from 'react';
import { useText } from './text';

export interface TagInputProps {
  /** Visible label. */
  label: string;
  value: string[];
  onChange: (next: string[]) => void;
  /** Offered through a native datalist. */
  suggestions?: string[];
  /** Backend limit (TOO_MANY_TAGS); the input stops accepting new tags at this count. */
  maxTags?: number;
  disabled?: boolean;
  error?: string;
}

/** Free-text tags: Enter or comma adds, Backspace on an empty input removes the last, each pill has a labelled remove button. Duplicates are ignored (case-insensitive). */
export function TagInput({ label, value, onChange, suggestions, maxTags, disabled, error }: TagInputProps) {
  const { tx } = useText();
  const [draft, setDraft] = useState('');
  const listId = useId();
  const full = maxTags !== undefined && value.length >= maxTags;

  const add = (raw: string) => {
    const tag = raw.trim();
    setDraft('');
    if (!tag || full || value.some((v) => v.toLowerCase() === tag.toLowerCase())) return;
    onChange([...value, tag]);
  };

  return (
    <div>
      <TextInput
        label={label}
        value={draft}
        disabled={disabled || full}
        error={error}
        description={full ? tx('edit.tags.max', { n: maxTags }) : tx('edit.tags.hint')}
        placeholder={tx('edit.tags.add')}
        list={suggestions?.length ? listId : undefined}
        onChange={(e) => {
          const v = e.currentTarget.value;
          if (v.includes(',')) v.split(',').forEach(add);
          else setDraft(v);
        }}
        onKeyDown={(e) => {
          if (e.key === 'Enter') {
            e.preventDefault();
            add(draft);
          } else if (e.key === 'Backspace' && draft === '' && value.length > 0) {
            onChange(value.slice(0, -1));
          }
        }}
        onBlur={() => draft && add(draft)}
      />
      {suggestions?.length ? (
        <datalist id={listId}>
          {suggestions.filter((s) => !value.includes(s)).map((s) => (
            <option key={s} value={s} />
          ))}
        </datalist>
      ) : null}
      {value.length === 0 ? (
        <Text size="xs" c="dimmed" mt={4}>
          {tx('edit.tags.none')}
        </Text>
      ) : (
        <Group component="ul" gap={6} mt={6} aria-label={tx('edit.tags.list')} style={{ listStyle: 'none', padding: 0, margin: 0 }}>
          {value.map((tag) => (
            <li key={tag}>
              <Pill size="md">
                {tag}
                <ActionIcon
                  size="xs"
                  variant="transparent"
                  color="dark"
                  ml={4}
                  disabled={disabled}
                  aria-label={tx('edit.tags.remove', { tag })}
                  onClick={() => onChange(value.filter((v) => v !== tag))}
                >
                  <span aria-hidden>{'×'}</span>
                </ActionIcon>
              </Pill>
            </li>
          ))}
        </Group>
      )}
    </div>
  );
}
