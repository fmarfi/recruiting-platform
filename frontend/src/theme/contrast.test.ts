import { describe, expect, it } from 'vitest';
import { tokens } from './tokens';

function lum(hex: string): number {
  const ch = [1, 3, 5].map((i) => {
    const v = parseInt(hex.slice(i, i + 2), 16) / 255;
    return v <= 0.03928 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4;
  });
  return 0.2126 * ch[0]! + 0.7152 * ch[1]! + 0.0722 * ch[2]!;
}
export function ratio(a: string, b: string): number {
  const [hi, lo] = [lum(a), lum(b)].sort((x, y) => y - x);
  return (hi! + 0.05) / (lo! + 0.05);
}

const c = tokens.color.light;
type K = keyof typeof c;

// [foreground, background, minimum ratio]: 4.5 for text, 3 for UI components and graphics (WCAG 1.4.3 / 1.4.11).
const PAIRS: [K, K, number][] = [
  ['ink', 'surface', 4.5], ['ink', 'bg', 4.5], ['ink2', 'surface', 4.5], ['ink2', 'bg', 4.5], ['ink2', 'surface2', 4.5], ['ink2', 'surface3', 4.5],
  ['ink3', 'surface', 4.5], ['ink3', 'bg', 4.5], ['ink3', 'surface2', 4.5], ['ink3', 'surface3', 4.5],
  ['accent', 'surface', 4.5], ['accent', 'bg', 4.5], ['accent', 'accentSoft', 4.5], ['accent', 'brassSoft', 4.5],
  ['accentInk', 'accent', 4.5], ['accentInk', 'accentHover', 4.5],
  ['chromeInk', 'chrome', 4.5], ['chromeInk', 'chromeActive', 4.5], ['chromeInk2', 'chrome', 4.5], ['chromeInk2', 'chromeActive', 4.5],
  ['brassDeco', 'chrome', 4.5], ['brass', 'surface', 4.5], ['brass', 'bg', 4.5], ['brass', 'surface2', 4.5], ['brass', 'brassSoft', 4.5],
  ['surface', 'brass', 4.5], ['danger', 'surface', 4.5], ['danger', 'dangerSoft', 4.5], ['surface', 'danger', 4.5],
  ['success', 'surface', 4.5], ['warning', 'surface', 4.5], ['info', 'surface', 4.5],
  ['focus', 'surface', 3], ['focus', 'bg', 3], ['focus', 'accentSoft', 3], ['focus', 'surface2', 3], ['focusOnChrome', 'chrome', 3],
  ['borderStrong', 'surface', 3], ['borderStrong', 'bg', 3], ['brassDeco', 'chromeActive', 3],
];

describe('brand token contrast (WCAG AA)', () => {
  it.each(PAIRS)('%s on %s is at least %s:1', (fg, bg, min) => {
    expect(ratio(c[fg], c[bg])).toBeGreaterThanOrEqual(min);
  });

  it('white text on the white-on-brass header button passes', () => {
    expect(ratio('#ffffff', c.brass)).toBeGreaterThanOrEqual(4.5);
    expect(ratio('#ffffff', '#6e4b12')).toBeGreaterThanOrEqual(4.5); // hover shade
  });

  it.each(Object.entries(tokens.tone.light))('tone %s: text on its tint and on white pass AA, dot passes 3:1 as a graphic', (_t, v) => {
    expect(ratio(v.fg, v.bg)).toBeGreaterThanOrEqual(4.5);
    expect(ratio(v.fg, '#ffffff')).toBeGreaterThanOrEqual(4.5);
    expect(ratio(v.dot, '#ffffff')).toBeGreaterThanOrEqual(3);
  });
});
