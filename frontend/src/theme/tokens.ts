// Brand tokens v2 ("Quiet Authority"), copied from docs/product/brand/design-tokens-v2.json.
// contracts/design-tokens.json (v1.1) is untouched until the architect adopts v2 (see docs/process/handoffs/frontend-shell.md).
import tokens from './design-tokens-v2.json';

export { tokens };
export type ToneKey = keyof typeof tokens.tone.light;
export type Scheme = 'light' | 'dark';

/** Unknown tone falls back to NEUTRAL (design-tokens.json $comment). */
export function toneOf(tone: string | undefined): ToneKey {
  return tone && tone in tokens.tone.light ? (tone as ToneKey) : 'NEUTRAL';
}

/** CSS custom properties: --color-*, --tone-<tone>-bg/fg/dot, --shadow-pop (per scheme). */
function schemeVars(s: Scheme): string {
  const out: string[] = [];
  for (const [k, v] of Object.entries(tokens.color[s])) out.push(`--color-${k}:${v};`);
  for (const [t, v] of Object.entries(tokens.tone[s])) {
    const n = t.toLowerCase();
    out.push(`--tone-${n}-bg:${v.bg};`, `--tone-${n}-fg:${v.fg};`, `--tone-${n}-dot:${v.dot};`);
  }
  out.push(`--shadow-pop:${tokens.shadow.pop[s]};`, `--shadow-raised:${tokens.shadow.raised[s]};`);
  return out.join('');
}

function staticVars(): string {
  const out: string[] = [];
  for (const [k, v] of Object.entries(tokens.spacing.scale)) out.push(`--space-${k}:${v}px;`);
  for (const [k, v] of Object.entries(tokens.radius)) out.push(`--radius-${k}:${v}px;`);
  for (const [k, v] of Object.entries(tokens.badge.sizes)) {
    out.push(`--badge-${k}-height:${v.height}px;`, `--badge-${k}-px:${v.paddingX}px;`, `--badge-${k}-font:${v.fontSize};`);
  }
  out.push(
    `--badge-dot:${tokens.badge.dotSize}px;`,
    `--font-sans:${tokens.typography.fontSans};`,
    `--font-display:${tokens.typography.fontDisplay};`,
    `--mantine-h1-font-family:${tokens.typography.fontDisplay};`,
    `--header-height:${tokens.layout.headerHeight}px;`,
    `--sidebar-collapsed-width:${tokens.spacing.sidebarCollapsedWidth}px;`,
    `--font-mono:${tokens.typography.fontMono};`,
    `--sidebar-width:${tokens.spacing.sidebarWidth}px;`,
    `--content-max-width:${tokens.spacing.contentMaxWidth}px;`,
    `--touch-target-min:${tokens.spacing.touchTargetMin}px;`,
    `--motion-fast:${tokens.motion.fast};`,
    `--motion-base:${tokens.motion.base};`,
  );
  return out.join('');
}

export function tokenCss(): string {
  return [
    `:root{${staticVars()}${schemeVars('light')}}`,
    `:root{--badge-sm-font:0.8125rem;--badge-md-font:0.875rem;--badge-lg-font:0.9375rem;--prose-max:70ch;--mantine-font-size-md:1rem;}`,
    `body{background:var(--color-bg);color:var(--color-ink);font-size:16px;line-height:1.5;-webkit-font-smoothing:antialiased;}`,
    `:root{--mantine-color-dimmed:var(--color-ink3);--mantine-color-text:var(--color-ink);--mantine-color-body:var(--color-bg);--mantine-color-error:var(--color-danger);--mantine-color-placeholder:var(--color-ink3);}`,
    `.mantine-Button-root[data-variant="light"]{background:var(--color-accentSoft);color:var(--color-accent);}`,
    `.mantine-Button-root[data-variant="light"]:hover{background:var(--color-surface3);}`,
    `.mantine-Button-root[data-variant="default"]{border-color:var(--color-borderStrong);color:var(--color-ink);background:var(--color-surface);}`,
    `.mantine-Button-root[data-variant="default"]:hover{background:var(--color-surface2);}`,
    `.mantine-Button-root:not([data-variant]):not([data-disabled]){background:var(--color-accent);color:var(--color-accentInk);}`,
    `.mantine-Button-root:not([data-variant]):not([data-disabled]):hover{background:var(--color-accentHover);}`,
    `h1,h2,h3,h4{letter-spacing:-0.01em;color:var(--color-ink);line-height:1.3;overflow-wrap:break-word;}`,
    `h1,h1.mantine-Title-root{font-family:var(--font-display);font-size:1.75rem;font-weight:600;line-height:1.2;letter-spacing:-0.01em;}h2{font-size:1.25rem;font-weight:600;}h3{font-size:1.0625rem;font-weight:600;}`,
    `@media (max-width:${tokens.breakpoints.phone}px){h1{font-size:1.5rem;}}`,
    `.eyebrow{font-size:0.75rem;font-weight:600;text-transform:uppercase;letter-spacing:0.06em;color:var(--color-ink3);}`,
    `.kpi-value{font-family:var(--font-display);font-size:2rem;font-weight:600;line-height:1.1;font-variant-numeric:tabular-nums;}`,
    `:focus-visible{outline:${tokens.focusRing.width}px solid var(--color-focus);outline-offset:${tokens.focusRing.offset}px;}`,
    `.chrome :focus-visible,.chrome:focus-visible{outline-color:var(--color-focusOnChrome);}`,
    `.prose,.mantine-Text-root[data-prose],p{max-width:var(--prose-max);}`,
    `td.num,th.num,[data-numeric]{text-align:right;font-variant-numeric:tabular-nums;}`,
    `.mantine-Table-td,.mantine-Table-th{padding-inline:14px;}`,
    `.mantine-Card-root{padding:24px;}`,
    `@media (max-width:${tokens.breakpoints.phone}px){.mantine-Card-root{padding:16px;}}`,
    `.mantine-InputWrapper-root + .mantine-InputWrapper-root{margin-top:4px;}`,
    `.mantine-Input-input{font-size:1rem;}.mantine-Input-input::placeholder{color:var(--color-ink3);opacity:1;}`,
    `.mantine-Paper-root,.mantine-Card-root{border-color:var(--color-border);box-shadow:var(--shadow-raised);}`,
    `.mantine-Tabs-tab[data-active]{color:var(--color-accent);background:var(--color-accentSoft);border-color:var(--color-accent);}`,
    `.mantine-Tabs-tab{color:var(--color-ink2);}`,
    `.mantine-Input-input,.mantine-Select-input,.mantine-Textarea-input{border-color:var(--color-borderStrong);background:var(--color-surface);}`,
    `.mantine-Table-th{text-transform:none;border-bottom:1px solid var(--color-borderStrong);}`,
    `.mantine-Table-th button{font:inherit;font-size:inherit;font-weight:inherit;text-transform:inherit;letter-spacing:inherit;color:inherit;}`,
    `.mantine-Table-tr:hover{background:var(--color-surface2);}`,
    `@media (max-width:${tokens.breakpoints.phone}px){body{font-size:16px;}}`,
    `.tone-badge{display:inline-flex;align-items:center;gap:6px;line-height:1.3;border-radius:4px;font-weight:${tokens.badge.fontWeight};}`,
    ...Object.keys(tokens.tone.light).map(
      (t) => `.tone-${t.toLowerCase()}{background:var(--tone-${t.toLowerCase()}-bg);color:var(--tone-${t.toLowerCase()}-fg);}`,
    ),
    `.kpi-tile{transition:border-color .12s,box-shadow .12s;box-shadow:inset 0 3px 0 var(--color-accent);}`,
    `.kpi-tile:hover{border-color:var(--color-borderStrong);box-shadow:inset 0 3px 0 var(--color-accent),0 2px 8px rgba(22,24,29,.10);}`,
    `.kpi-tile:focus-visible,.nav-link:focus-visible,.skip-link:focus-visible,.brand-link:focus-visible{outline:3px solid var(--color-focus);outline-offset:2px;}`,
    `.nav-link:not([aria-current="page"]):hover{background:var(--color-chromeActive)!important;color:var(--color-chromeInk)!important;}`,
    `.nav-link{position:relative;}`,
    `.nav-link[aria-current="page"]::before{content:"";position:absolute;left:0;top:10px;bottom:10px;width:3px;border-radius:2px;background:var(--color-brassDeco);}`,
    `.chrome-rail{background:var(--color-chrome);color:var(--color-chromeInk);border-right:0;}`,
    `.chrome-header{background:var(--color-chrome);color:var(--color-chromeInk);border-bottom:0;}`,
    `.chrome-header .mantine-Input-input{background:rgba(255,255,255,.10);border-color:var(--color-chromeInk2);color:var(--color-chromeInk);}`,
    `.chrome-header .mantine-Input-input::placeholder{color:var(--color-chromeInk2);}`,
    `.chrome-header .mantine-Button-root[data-variant="default"],.chrome-header .mantine-ActionIcon-root[data-variant="default"]{background:transparent;color:var(--color-chromeInk);border-color:var(--color-chromeInk2);}`,
    `.chrome-header .mantine-Button-root[data-variant="default"]:hover,.chrome-header .mantine-ActionIcon-root[data-variant="default"]:hover{background:var(--color-chromeActive);}`,
    `.chrome-header .mantine-Button-root:not([data-variant]):not([data-disabled]){background:var(--color-brass);color:#fff;}`,
    `.chrome-header .mantine-Button-root:not([data-variant]):not([data-disabled]):hover{background:#6e4b12;}`,
    `.chrome-header :focus-visible,.chrome-rail :focus-visible{outline-color:var(--color-focusOnChrome);}`,
    `.rail-toggle{margin-top:auto;align-self:flex-end;min-height:var(--touch-target-min);min-width:var(--touch-target-min);display:flex;align-items:center;justify-content:center;background:transparent;border:0;border-radius:var(--radius-md);color:var(--color-chromeInk2);cursor:pointer;}`,
    `.rail-toggle:hover{background:var(--color-chromeActive);color:var(--color-chromeInk);}`,
    `.chrome-header .mantine-SegmentedControl-root{background:rgba(255,255,255,.14);}`,
    `.chrome-header .mantine-SegmentedControl-label{color:var(--color-chromeInk);}`,
    `.chrome-header .mantine-SegmentedControl-label[data-active]{color:var(--color-accent);}`,
    `.chrome-header .brand-link{color:var(--color-chromeInk);display:inline-flex;align-items:center;}`,
    `.skip-link{position:absolute;left:8px;top:-48px;z-index:1000;background:var(--color-surface);color:var(--color-accent);padding:8px 14px;border-radius:var(--radius-md);border:1px solid var(--color-accent);font-weight:600;text-decoration:none;}`,
    `.skip-link:focus{top:8px;}`,
    `.brand-link{color:var(--color-accent);font-weight:600;text-decoration:none;white-space:nowrap;border-radius:var(--radius-sm);}`,
    `th,td{overflow-wrap:anywhere;}`,
    tokens.motion.honourReducedMotion ? `@media (prefers-reduced-motion:reduce){*{transition:none!important;animation:none!important;}}` : '',
  ].join('\n');
}
