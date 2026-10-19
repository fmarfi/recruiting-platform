import { MantineProvider } from '@mantine/core';
import '@mantine/core/styles.css';
import { AppRoutes } from './routes';
import { theme } from './theme/theme';
import { tokenCss } from './theme/tokens';
import './i18n';

export function App() {
  return (
    <MantineProvider theme={theme} defaultColorScheme="light" forceColorScheme="light">
      <style>{tokenCss()}</style>
      <AppRoutes />
    </MantineProvider>
  );
}
