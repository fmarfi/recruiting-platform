// Candidate profile: source, notice period, mobility, languages, education, LinkedIn link, compensation (only when not restricted).
import { Anchor, Group, Stack, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { type Schemas } from '../../pipeline/shared';
import { Panel } from '../../shell/components/Panel';
import { compensationRestrictedNote, compensationVisible, hasProfile, safeLinkedIn } from './profile';

function Row({ label, children, testId }: { label: string; children: React.ReactNode; testId?: string }) {
  return (
    <Group gap="xs" align="baseline" wrap="nowrap" data-testid={testId}>
      <Text size="sm" c="var(--color-ink3)" w={140} style={{ flexShrink: 0 }}>
        {label}
      </Text>
      <Text size="sm" fw={500} c="var(--color-ink)" component="div">
        {children}
      </Text>
    </Group>
  );
}

export function ProfileSection({ person: p }: { person: Schemas['Person'] }) {
  const { t, i18n } = useTranslation();
  if (!hasProfile(p)) return null;
  const link = safeLinkedIn(p.linkedinUrl);
  const c = p.compensation;
  const money = (n?: number | null) => (n == null ? '-' : `${n.toLocaleString(i18n.language === 'en' ? 'en-GB' : 'tr-TR')} ${c?.currency ?? ''}`.trim());
  return (
    <Panel title={t('network.profile.title')} testId="candidate-profile">
      <Stack gap="xs">
        {p.sourceChannel && <Row label={t('network.profile.source')}>{t(`network.pool.channel.${p.sourceChannel}`)}</Row>}
        {p.noticePeriodDays != null && <Row label={t('network.profile.notice')}>{t('network.profile.days', { count: p.noticePeriodDays })}</Row>}
        {p.mobility && <Row label={t('network.profile.mobility')}>{t(`network.profile.mobilities.${p.mobility}`)}</Row>}
        {!!p.languages?.length && (
          <Row label={t('network.profile.languages')}>{p.languages.map((l) => `${l.language.toUpperCase()} (${t(`network.profile.levels.${l.level}`)})`).join(', ')}</Row>
        )}
        {!!p.educations?.length && (
          <Row label={t('network.profile.education')}>
            {p.educations.map((e, i) => (
              <div key={i}>
                {[e.school, [e.degree, e.field].filter(Boolean).join(' '), e.startYear || e.endYear ? `${e.startYear ?? ''}-${e.endYear ?? ''}` : ''].filter(Boolean).join(', ')}
              </div>
            ))}
          </Row>
        )}
        {p.linkedinUrl && (
          <Row label="LinkedIn">
            {link ? (
              <Anchor href={link} target="_blank" rel="noreferrer noopener">
                {p.linkedinUrl}
              </Anchor>
            ) : (
              p.linkedinUrl
            )}
          </Row>
        )}
        {compensationVisible(p) && c && (
          <Row label={t('network.profile.compensation')} testId="compensation">
            {[
              c.currentBase != null && `${t('network.profile.base')}: ${money(c.currentBase)}`,
              c.currentVariable != null && `${t('network.profile.variable')}: ${money(c.currentVariable)}`,
              c.expected != null && `${t('network.profile.expected')}: ${money(c.expected)}`,
            ]
              .filter(Boolean)
              .join(' | ')}{' '}
            ({t(`network.profile.periods.${c.period}`)}, {t(`network.profile.bases.${c.basis}`)})
            {c.benefits ? ` - ${c.benefits}` : ''}
          </Row>
        )}
        {compensationRestrictedNote(p) && (
          <Row label={t('network.profile.compensation')} testId="compensation-restricted">
            <Text span c="dimmed" size="sm">
              {t('network.profile.restricted')}
            </Text>
          </Row>
        )}
      </Stack>
    </Panel>
  );
}
