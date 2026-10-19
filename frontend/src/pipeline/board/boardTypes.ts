import type { Application, Stage } from './boardRules';
import type { RefData } from '../shared';

export type BoardProps = {
  apps: Application[];
  ref: RefData | undefined;
  readOnly: boolean;
  onMove: (app: Application, to: Stage, reason?: string) => void;
  onRemove: (app: Application) => void;
  onLogActivity: (app: Application) => void;
};
