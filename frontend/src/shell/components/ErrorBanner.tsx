import { Alert, Button, Stack, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';

/** Friendly load error: says what failed, what to do (retry) and never shows a raw code. */
export function ErrorBanner({ onRetry, message, hint }: { onRetry?: () => void; message?: string; hint?: string }) {
  const { t } = useTranslation();
  return (
    <Alert color="red" role="alert" title={message ?? t('shell.loadError')}>
      <Stack gap="xs" align="flex-start">
        <Text size="sm">{hint ?? t('shell.loadErrorHint')}</Text>
        {onRetry && (
          <Button size="xs" variant="light" color="red" onClick={onRetry}>
            {t('shell.retry')}
          </Button>
        )}
      </Stack>
    </Alert>
  );
}
