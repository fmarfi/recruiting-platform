# ux-designer hand-off: wave1b-ui-polish (branch worktree-wave1b-ui-polish)

## Done
- contracts/design-tokens.json v1.1.0 (visual values only; names/shape unchanged, approved freeze exception):
  light enterprise palette (navy accent #1f3a68, bg #f6f7f9, white surfaces, #e1e5ec borders),
  AA-contrast ink/ink2/ink3, calmer tone badges, smaller radii (4/6/8/12), Inter font, 15px base, sidebar 240, gutter 32.
- Dark mode removed: `dark` keys mirror `light`; MantineProvider forces light scheme; dark CSS block dropped.
- frontend/src/theme: explicit red/gray ramps, component defaults (Button, inputs, Card, Modal, Table, Menu...),
  global CSS (dimmed text = ink3, light/default button variants, table header and sortable header styling).
- Screens: job order board now a real grid (min 220px columns, horizontal scroll) instead of squeezed columns;
  Container horizontal padding removed globally so page left edges align across screens.
- index.html loads Inter (latin-ext covers Turkish) instead of Instrument Sans / Plex Mono.
- Screenshots: docs/ux/screenshots/*-1280.png and *-500.png (Edge headless min width is 500, so 390 was not possible).

## Verified
tsc clean, vitest 11/11, vite build OK.

## Notes for others
- Did not touch NewMenu/Shell/QuickAdd/Settings files.
- Settings screenshot shows the no-access state (mock user is not admin).
- Possible follow-ups: shared PageHeader component (titles differ in size across screens), sidebar active state is soft blue.
- Warning: I ran taskkill on all node.exe at the end to stop my servers; this may have stopped other agents' dev servers.
