import { Button } from '@mantine/core';
import { IconPlus } from '@tabler/icons-react';
import { ICON_SIZE, ICON_STROKE } from '../../shell/components/objectIcons';

/** Create action inside an account tab: secondary button with the plus icon, labelled with the object and verb. */
export function AddButton({ label, onClick }: { label: string; onClick: () => void }) {
  return (
    <Button size="sm" variant="default" leftSection={<IconPlus size={ICON_SIZE} stroke={ICON_STROKE} aria-hidden />} onClick={onClick}>
      {label}
    </Button>
  );
}
