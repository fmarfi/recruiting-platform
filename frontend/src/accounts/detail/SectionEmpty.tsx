import { Stack, Text } from '@mantine/core';
import type { ReactNode } from 'react';

/** Empty section on the account page: one sentence, centred, and when one exists the primary action for it. No box, no illustration. */
export function SectionEmpty({ text, action }: { text: string; action?: ReactNode }) {
  return (
    <Stack data-testid="empty-state" align="center" gap="sm" py="lg">
      <Text c="dimmed" ta="center">
        {text}
      </Text>
      {action}
    </Stack>
  );
}
