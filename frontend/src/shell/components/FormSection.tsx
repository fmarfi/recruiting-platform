import { Stack, Text } from '@mantine/core';
import type { ReactNode } from 'react';

/** A titled group of fields inside a dialog or form, with an optional one-line helper. */
export function FormSection({ title, hint, children }: { title: string; hint?: string; children: ReactNode }) {
  return (
    <Stack gap="xs" component="fieldset" style={{ border: 0, margin: 0, padding: 0, minWidth: 0 }}>
      <Text component="legend" size="xs" fw={700} tt="uppercase" c="dimmed" p={0}>
        {title}
      </Text>
      {hint && (
        <Text size="xs" c="dimmed" mt={-4}>
          {hint}
        </Text>
      )}
      {children}
    </Stack>
  );
}

/** "Fields marked * are required" line shown once at the top of a form. */
export function RequiredNote({ text }: { text: string }) {
  return (
    <Text size="xs" c="dimmed">
      <span aria-hidden>* </span>
      {text}
    </Text>
  );
}
