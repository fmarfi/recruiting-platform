import { Alert, Button, Card, Center, Group, PasswordInput, Stack, TextInput, Title } from '@mantine/core';
import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import { LanguageSwitch } from '../layout/LanguageSwitch';

export function Login() {
  const { t } = useTranslation();
  const { login, status } = useAuth();
  const nav = useNavigate();
  const loc = useLocation();
  const from = (loc.state as { from?: string } | null)?.from ?? '/';
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  if (status === 'authed') return <Navigate to={from} replace />;

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    const r = await login(email.trim(), password);
    setBusy(false);
    if (r === 'ok') nav(from, { replace: true });
    else setErr(r === 'invalid' ? t('login.invalid') : t('login.error'));
  }

  return (
    <Center mih="100vh" p="md">
      <Card withBorder shadow="sm" padding="lg" w="100%" maw={400}>
        <form onSubmit={(e) => void submit(e)}>
          <Stack>
            <Group justify="space-between">
              <Title order={1} size="h2" data-testid="screen-title">
                {t('login.title')}
              </Title>
              <LanguageSwitch />
            </Group>
            <TextInput label={t('login.email')} type="email" autoComplete="username" value={email} onChange={(e) => setEmail(e.currentTarget.value)} required data-autofocus />
            <PasswordInput label={t('login.password')} autoComplete="current-password" value={password} onChange={(e) => setPassword(e.currentTarget.value)} required />
            {err && (
              <Alert color="red" role="alert">
                {err}
              </Alert>
            )}
            <Button type="submit" loading={busy} fullWidth>
              {t('login.submit')}
            </Button>
          </Stack>
        </form>
      </Card>
    </Center>
  );
}
