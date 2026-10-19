import './path.css';
import type { StepState } from './pathSteps';

type Props = {
  state: StepState;
  label: string;
  /** Tone of the status; only used to colour a current terminal or negative step. */
  tone: string;
  onClick?: () => void;
  blocked?: boolean;
};

/** One chevron. State is shown by a check mark and text as well as colour. */
export function PathStep({ state, label, tone, onClick, blocked }: Props) {
  const negative = state === 'current' && (tone === 'BAD' || tone === 'MUTED');
  return (
    <button
      type="button"
      className={`path-step path-${state}${negative ? ' path-negative' : ''}`}
      aria-current={state === 'current' ? 'step' : undefined}
      aria-disabled={!onClick}
      disabled={blocked}
      onClick={onClick}
      title={label}
    >
      {state === 'done' && <span aria-hidden>{'✓'}</span>}
      <span>{label}</span>
    </button>
  );
}
