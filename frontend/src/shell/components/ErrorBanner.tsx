import { Alert, Button } from '@mantine/core';
import { useTranslation } from 'react-i18next';

export function ErrorBanner({ onRetry, message }: { onRetry?: () => void; message?: string }) {
  const { t } = useTranslation();
  return (
    <Alert color="red" role="alert" title={message ?? t('shell.loadError')}>
      {onRetry && (
        <Button size="xs" variant="light" color="red" onClick={onRetry}>
          {t('shell.retry')}
        </Button>
      )}
    </Alert>
  );
}
