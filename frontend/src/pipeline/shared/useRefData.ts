// /reference-data comes from the shell's ReferenceDataProvider, so it is fetched once per session.
import { useReference } from '../../shell/refdata/useReference';
import type { RefData } from './types';

export function useRefData(): RefData | undefined {
  return useReference().data ?? undefined;
}
