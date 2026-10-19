// Laptop-only 36 px footer with Recent items and Quick note; opens a 320 px docked panel (S5).
import { Anchor, Box, Button, Group, Stack, Text, Textarea } from '@mantine/core';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { readNote, readRecents, writeNote, type RecentItem } from './recents';

type Panel = null | 'recent' | 'note';

/** Footer buttons plus the docked panel. Content stays in the browser; nothing is sent to the server. */
export function UtilityBar() {
  const { t } = useTranslation();
  const [panel, setPanel] = useState<Panel>(null);
  const [recents, setRecents] = useState<RecentItem[]>(readRecents);
  const [note, setNote] = useState(readNote);

  useEffect(() => {
    const refresh = () => setRecents(readRecents());
    window.addEventListener('rp-recents', refresh);
    return () => window.removeEventListener('rp-recents', refresh);
  }, []);

  const toggle = (p: Exclude<Panel, null>) => setPanel((cur) => (cur === p ? null : p));
  const onNote = (text: string) => {
    setNote(text);
    writeNote(text);
  };

  return (
    <>
      {panel && (
        <Box
          role="dialog"
          aria-label={panel === 'recent' ? t('shell.recentItems') : t('shell.quickNote')}
          style={{
            position: 'fixed', left: 16, bottom: 36, width: 320, maxHeight: 360, overflow: 'auto', zIndex: 120, padding: 'var(--space-3)',
            background: 'var(--color-surface)', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-md) var(--radius-md) 0 0',
            boxShadow: '0 4px 16px rgba(15,26,43,.12)',
          }}
        >
          {panel === 'recent' ? (
            <Stack gap="xs">
              <Text fw={600}>{t('shell.recentItems')}</Text>
              {recents.length === 0 && <Text c="dimmed" fz="sm">{t('shell.recentEmpty')}</Text>}
              {recents.map((r) => (
                <Anchor key={r.path} component={Link} to={r.path} fz="sm" onClick={() => setPanel(null)}>{r.label}</Anchor>
              ))}
            </Stack>
          ) : (
            <Stack gap="xs">
              <Text fw={600}>{t('shell.quickNote')}</Text>
              <Textarea aria-label={t('shell.quickNote')} autosize minRows={5} value={note} onChange={(e) => onNote(e.currentTarget.value)} placeholder={t('shell.quickNoteHint')} />
              <Text fz="xs" c="dimmed">{t('shell.quickNoteLocal')}</Text>
            </Stack>
          )}
        </Box>
      )}
      <Group
        component="footer"
        h={36}
        px="md"
        gap="xs"
        style={{ position: 'fixed', left: 0, right: 0, bottom: 0, zIndex: 110, background: 'var(--color-surface)', borderTop: '1px solid var(--color-border)' }}
      >
        <Button size="compact-sm" variant={panel === 'recent' ? 'light' : 'subtle'} onClick={() => toggle('recent')}>{t('shell.recentItems')}</Button>
        <Button size="compact-sm" variant={panel === 'note' ? 'light' : 'subtle'} onClick={() => toggle('note')}>{t('shell.quickNote')}</Button>
      </Group>
    </>
  );
}
