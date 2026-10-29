import { fireEvent, screen } from '@testing-library/react';
import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { TagInput } from './TagInput';
import { axeViolations, renderShell } from './testUtils';

function Host({ initial = [] as string[], max, spy }: { initial?: string[]; max?: number; spy?: (v: string[]) => void }) {
  const [v, setV] = useState(initial);
  return (
    <TagInput
      label="Tags"
      value={v}
      maxTags={max}
      suggestions={['cfo']}
      onChange={(n) => {
        setV(n);
        spy?.(n);
      }}
    />
  );
}

describe('TagInput', () => {
  it('adds with Enter and comma, ignoring duplicates', () => {
    const spy = vi.fn();
    renderShell(<Host spy={spy} />);
    const box = screen.getByLabelText('Tags');
    fireEvent.change(box, { target: { value: 'Retail' } });
    fireEvent.keyDown(box, { key: 'Enter' });
    fireEvent.change(box, { target: { value: 'retail' } });
    fireEvent.keyDown(box, { key: 'Enter' });
    fireEvent.change(box, { target: { value: 'Ankara,' } });
    expect(spy).toHaveBeenLastCalledWith(['Retail', 'Ankara']);
  });

  it('removes with the labelled button and with Backspace', () => {
    renderShell(<Host initial={['a', 'b']} />);
    fireEvent.click(screen.getByRole('button', { name: 'Remove tag a' }));
    expect(screen.queryByText('a')).toBeNull();
    fireEvent.keyDown(screen.getByLabelText('Tags'), { key: 'Backspace' });
    expect(screen.getByText('No tags')).toBeInTheDocument();
  });

  it('stops at maxTags', () => {
    renderShell(<Host initial={['a', 'b']} max={2} />);
    expect(screen.getByLabelText('Tags')).toBeDisabled();
    expect(screen.getByText('At most 2 tags')).toBeInTheDocument();
  });

  it('is axe clean', async () => {
    renderShell(<Host initial={['a']} />);
    expect(await axeViolations()).toEqual([]);
  });
});
