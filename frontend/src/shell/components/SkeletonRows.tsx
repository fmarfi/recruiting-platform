import { Skeleton, Stack } from '@mantine/core';

export function SkeletonRows({ n = 3 }: { n?: number }) {
  return (
    <Stack gap="xs" data-testid="skeleton">
      {Array.from({ length: n }, (_, i) => (
        <Skeleton key={i} height={36} />
      ))}
    </Stack>
  );
}
