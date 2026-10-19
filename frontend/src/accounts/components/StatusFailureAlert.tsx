import { Alert, Anchor, List } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import type { StatusFailure } from '../detail/useStatusChange';

/** 409 under the badge: server detail, plus links to the records that block closing (TRANSITION_GUARD_FAILED). */
export function StatusFailureAlert({ failure }: { failure: StatusFailure }) {
  const { t } = useTranslation();
  return (
    <Alert color="red" role="alert" mt="xs">
      {failure.blocking.length > 0 ? t('accounts.openJobsBlock') : failure.detail}
      {failure.blocking.length > 0 && (
        <List size="sm" mt={4}>
          {failure.blocking.map((b) => (
            <List.Item key={b.id}>
              <Anchor component={Link} to={`/job-orders/${b.id}`}>
                {b.title}
              </Anchor>
            </List.Item>
          ))}
        </List>
      )}
    </Alert>
  );
}
