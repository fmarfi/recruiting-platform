// Talent pool filters (ADR 0011): tag = pool, source channel, CV full text, shareable. Chips and inputs write the URL through the parent.
import { Chip, Group, TagsInput, TextInput } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import type { Schemas } from '../../pipeline/shared';
import { MAX_TAG_LENGTH, normTags, SOURCE_CHANNELS, type ViewFilters } from './networkFilters';

export function PoolFilters({ f, onChange }: { f: ViewFilters; onChange: (patch: Partial<ViewFilters>) => void }) {
  const { t } = useTranslation();
  return (
    <>
      <Group mb="sm" align="flex-end">
        <TagsInput
          label={t('network.pool.tags')}
          description={t('network.pool.tagsHint')}
          value={f.tags}
          maxTags={20}
          onChange={(v) => onChange({ tags: normTags(v) })}
          splitChars={[',']}
          maxLength={MAX_TAG_LENGTH}
          clearable
          style={{ minWidth: 240 }}
        />
        <TextInput
          label={t('network.pool.cv')}
          description={t('network.pool.cvHint')}
          value={f.cv}
          onChange={(e) => onChange({ cv: e.currentTarget.value })}
          style={{ minWidth: 240 }}
        />
      </Group>
      <Group gap="xs" mb="sm" style={{ overflowX: 'auto' }} wrap="nowrap" role="group" aria-label={t('network.pool.channels')}>
        <Chip.Group multiple value={f.channels} onChange={(v) => onChange({ channels: v as Schemas['SourceChannel'][] })}>
          {SOURCE_CHANNELS.map((c) => (
            <Chip key={c} value={c} size="sm">
              {t(`network.pool.channel.${c}`)}
            </Chip>
          ))}
        </Chip.Group>
        <Chip checked={f.shareable} onChange={(v) => onChange({ shareable: v })} size="sm" color="green">
          {t('network.pool.shareable')}
        </Chip>
      </Group>
    </>
  );
}

/** The server marks the match in a CV excerpt with « and »; split it so it can be rendered as text and <mark>, never as HTML. */
export function splitCvMatch(s: string): { text: string; hit: boolean }[] {
  const out: { text: string; hit: boolean }[] = [];
  let rest = s;
  while (rest) {
    const a = rest.indexOf('«');
    const b = a < 0 ? -1 : rest.indexOf('»', a);
    if (a < 0 || b < 0) {
      out.push({ text: rest.replace(/[«»]/g, ''), hit: false });
      break;
    }
    if (a > 0) out.push({ text: rest.slice(0, a), hit: false });
    out.push({ text: rest.slice(a + 1, b), hit: true });
    rest = rest.slice(b + 1);
  }
  return out;
}
