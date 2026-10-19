import type { ReactNode } from 'react';
import './record.css';

/**
 * Record page template body: a main column (about 8 of 12) and a right column of stacked cards (4 of 12).
 * Below 960 px the right column drops under the main column (record.css).
 */
export function RecordLayout({ main, aside }: { main: ReactNode; aside: ReactNode }) {
  return (
    <div className="record-grid">
      <div className="record-main">{main}</div>
      <aside className="record-aside">{aside}</aside>
    </div>
  );
}
