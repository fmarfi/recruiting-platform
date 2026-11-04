import { MantineProvider } from '@mantine/core';
import { render as rtl, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { DetailList } from './DetailList';
import { Panel } from './Panel';
import { RecordHeader } from './RecordHeader';
import { RecordLayout } from './RecordLayout';

const render = (ui: React.ReactElement) => rtl(<MantineProvider>{ui}</MantineProvider>);

describe('record components', () => {
  it('Panel shows title, count, actions and body, or the empty text', () => {
    const { rerender } = render(
      <Panel title="Career" count={2} actions={<button>Add</button>}>
        body
      </Panel>,
    );
    expect(screen.getByRole('heading', { level: 2 }).textContent).toBe('Career');
    expect(screen.getByText('2')).toBeTruthy();
    expect(screen.getByText('body')).toBeTruthy();
    rerender(
      <MantineProvider>
        <Panel title="Career" empty="Nothing yet">
          body
        </Panel>
      </MantineProvider>,
    );
    expect(screen.getByTestId('panel-empty').textContent).toBe('Nothing yet');
    expect(screen.queryByText('body')).toBeNull();
  });

  it('DetailList renders a dash for empty values and links for href', () => {
    render(<DetailList items={[{ label: 'City', value: '' }, { label: 'Email', value: 'a@b.c', href: 'mailto:a@b.c' }]} />);
    expect(screen.getByText('–')).toBeTruthy();
    expect(screen.getByRole('link').getAttribute('href')).toBe('mailto:a@b.c');
  });

  it('RecordLayout puts header, banner, main and aside in order', () => {
    render(
      <RecordLayout
        header={<RecordHeader name="Ada" subtitle="CFO" facts={[{ label: 'City', value: 'Ankara' }]} tabs={<div>tabs</div>} />}
        banner={<div>banner</div>}
        main={<div>main</div>}
        aside={<div>aside</div>}
      />,
    );
    expect(screen.getByTestId('screen-title').textContent).toBe('Ada');
    expect(screen.getByTestId('record-facts').textContent).toContain('Ankara');
    const text = document.body.textContent ?? '';
    expect(text.indexOf('banner')).toBeLessThan(text.indexOf('main'));
    expect(text.indexOf('main')).toBeLessThan(text.indexOf('aside'));
  });
});
