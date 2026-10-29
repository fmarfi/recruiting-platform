import { MantineProvider } from '@mantine/core';
import { render } from '@testing-library/react';
import axe from 'axe-core';
import type { ReactElement } from 'react';
import i18n from '../../i18n';
import { UndoToastProvider } from './undo';
import './text';

/** Test helper: renders inside Mantine (test env, no transitions) and the undo toast host, English. */
export function renderShell(ui: ReactElement) {
  void i18n.changeLanguage('en');
  return render(
    <MantineProvider env="test">
      <UndoToastProvider>{ui}</UndoToastProvider>
    </MantineProvider>,
  );
}

/** Runs axe (WCAG 2.x A/AA rules) on the rendered document body and returns the violations. */
export async function axeViolations(root: Element = document.body, off: string[] = []) {
  const res = await axe.run(root, { runOnly: { type: 'tag', values: ['wcag2a', 'wcag2aa', 'wcag21aa', 'wcag22aa'] }, rules: { region: { enabled: false }, 'color-contrast': { enabled: false }, ...Object.fromEntries(off.map((r) => [r, { enabled: false }])) } });
  return res.violations.map((v) => `${v.id}: ${v.nodes.map((n) => n.html).join(' | ')}`);
}

