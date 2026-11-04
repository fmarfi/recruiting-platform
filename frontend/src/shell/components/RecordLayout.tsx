import type { ReactNode } from 'react';
import './record.css';

/**
 * Visual system v3 section 5: white header band across the content width, optional banner, then main (2fr) and aside
 * (1fr) side by side from 75em; one column below, aside after main.
 */
export function RecordLayout({ header, banner, main, aside }: { header: ReactNode; banner?: ReactNode; main: ReactNode; aside: ReactNode }) {
  return (
    <div className="record-layout">
      <div className="record-band" data-testid="record-band">
        {header}
      </div>
      {banner && <div className="record-banner">{banner}</div>}
      <div className="record-body">
        <div className="record-main">{main}</div>
        <aside className="record-aside">{aside}</aside>
      </div>
    </div>
  );
}
