// Side drawer for one candidate: stage Path, person facts and links (lightning-style P4).
import { Anchor, Button, Drawer, Stack, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { Path } from '../../../common/path/Path';
import type { Application } from '../../board/boardRules';
import { StatusBadge, useFormat } from '../../shared';

type Props = {
  app: Application | undefined;
  readOnly: boolean;
  isManager: boolean;
  onClose: () => void;
  /** Same entry as the board: the page asks for a reason when `reasonRequired`. */
  onStage: (app: Application, to: string, reasonRequired: boolean) => void;
  onLog: (app: Application) => void;
};

export function CandidateDrawer({ app, readOnly, isManager, onClose, onStage, onLog }: Props) {
  const { t } = useTranslation();
  const fmt = useFormat();
  return (
    <Drawer opened={!!app} onClose={onClose} position="right" title={app?.person.name}>
      {app && (
        <Stack gap="sm" data-testid="candidate-drawer">
          <Text size="sm" c="dimmed">
            {[app.person.title, app.person.company].filter(Boolean).join(', ')}
          </Text>
          <Path
            lifecycle="pipelineStage"
            value={app.stage}
            isManager={isManager}
            disabled={readOnly}
            onChoose={(to, reason) => onStage(app, to, reason)}
          />
          <Text size="sm">
            {t('pipeline.since')}: {fmt.dateLong(app.stageChangedAt)}
          </Text>
          <Text size="sm">
            {t('pipeline.consent')}: <StatusBadge lifecycle="consent" value={app.person.consentStatus} />
          </Text>
          {app.rejectionReason && (
            <Text size="sm">
              {t('pipeline.reason.label')}: {app.rejectionReason}
            </Text>
          )}
          <Anchor component={Link} to={`/people/${app.person.id}`}>
            {t('pipeline.openProfile')}
          </Anchor>
          <Button variant="default" onClick={() => onLog(app)}>
            {t('pipeline.logWithPerson')}
          </Button>
        </Stack>
      )}
    </Drawer>
  );
}
