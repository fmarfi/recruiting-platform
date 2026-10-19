import tokens from '../../../contracts/design-tokens.json';

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
  out.push(`--shadow-pop:${tokens.shadow.pop[s]};`);
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
    `body{background:var(--color-bg);color:var(--color-ink);font-size:${tokens.typography.base.desktop};line-height:${tokens.typography.base.lineHeight};-webkit-font-smoothing:antialiased;}`,
    `h1,h2,h3,h4{letter-spacing:-0.01em;color:var(--color-ink);}`,
    `.mantine-Paper-root,.mantine-Card-root{border-color:var(--color-border);}`,
    `.mantine-Table-tr:hover{background:var(--color-surface2);}`,
    `@media (max-width:${tokens.breakpoints.phone}px){body{font-size:${tokens.typography.base.phone};}}`,
    `.tone-badge{display:inline-flex;align-items:center;gap:6px;border-radius:var(--radius-pill);font-weight:${tokens.badge.fontWeight};}`,
    ...Object.keys(tokens.tone.light).map(
      (t) => `.tone-${t.toLowerCase()}{background:var(--tone-${t.toLowerCase()}-bg);color:var(--tone-${t.toLowerCase()}-fg);}`,
    ),
    tokens.motion.honourReducedMotion ? `@media (prefers-reduced-motion:reduce){*{transition:none!important;animation:none!important;}}` : '',
  ].join('\n');
}
