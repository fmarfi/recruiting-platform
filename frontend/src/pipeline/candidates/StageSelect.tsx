// Stage cell: a select with the current stage and its legal moves. Keyboard reachable; same control on the search table and the person page.
import { Select } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import type { Stage } from '../board/boardRules';
import type { App } from '../longlist/useLonglist';
import { useLabel } from '../shared';
import { stageOptions } from './useStageControl';

export function StageSelect({ app, readOnly, onChange }: { app: App; readOnly?: boolean; onChange: (app: App, to: Stage) => void }) {
  const { t } = useTranslation();
  const { ref, label } = useLabel();
  const opts = stageOptions(ref, app);
  return (
    <Select
      size="xs"
      w="10.5rem"
      aria-label={t('pipeline.cand.stageOf', { name: app.person.name })}
      data={opts.map((s) => ({ value: s, label: label('pipelineStage', s) }))}
      value={app.stage}
      allowDeselect={false}
      disabled={readOnly || opts.length < 2}
      onChange={(v) => v && onChange(app, v as Stage)}
    />
  );
}
