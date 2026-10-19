export interface DialogProps {
  opened: boolean;
  onClose: () => void;
  /** Fixed account (account page) or undefined (global + New: pick one). */
  accountId?: string;
  onDone?: () => void;
}
