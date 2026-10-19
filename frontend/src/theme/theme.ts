import { createTheme, type MantineColorsTuple } from '@mantine/core';
import { tokens } from './tokens';

/** Linear mix of two #rrggbb colours. */
function mix(a: string, b: string, t: number): string {
  const p = (h: string) => [1, 3, 5].map((i) => parseInt(h.slice(i, i + 2), 16));
  const pa = p(a);
  const pb = p(b);
  return '#' + pa.map((v, i) => Math.round(v + ((pb[i] ?? 0) - v) * t).toString(16).padStart(2, '0')).join('');
}

// 10-shade ramp: accentSoft (0) -> accent (6) -> accentHover (7) -> darker (9), from the light-scheme anchors.
const { accent, accentHover, accentSoft, ink } = tokens.color.light;
const brand = [
  accentSoft,
  mix(accentSoft, accent, 0.2),
  mix(accentSoft, accent, 0.4),
  mix(accentSoft, accent, 0.6),
  mix(accentSoft, accent, 0.8),
  mix(accentSoft, accent, 0.93),
  accent,
  accentHover,
  mix(accentHover, ink, 0.4),
  mix(accentHover, ink, 0.7),
] as unknown as MantineColorsTuple;

const c = tokens.color.light;
const px = (n: number) => `${n}px`;

export const theme = createTheme({
  primaryColor: 'brand',
  primaryShade: 6,
  black: c.ink,
  white: c.surface,
  autoContrast: true,
  focusRing: 'auto',
  fontSizes: { xs: '0.8125rem', sm: '0.875rem', md: '0.9375rem', lg: '1.0625rem', xl: '1.25rem' },
  lineHeights: { xs: '1.45', sm: '1.5', md: '1.55', lg: '1.55', xl: '1.5' },
  shadows: { xs: 'none', sm: '0 1px 2px rgba(15,26,43,.05)', md: tokens.shadow.pop.light, lg: tokens.shadow.pop.light, xl: tokens.shadow.pop.light },
  colors: { brand },
  fontFamily: tokens.typography.fontSans,
  fontFamilyMonospace: tokens.typography.fontMono,
  defaultRadius: 'md',
  radius: { xs: px(2), sm: px(tokens.radius.sm), md: px(tokens.radius.md), lg: px(tokens.radius.lg), xl: px(tokens.radius.sheet) },
  spacing: {
    xs: px(tokens.spacing.scale['1']),
    sm: px(tokens.spacing.scale['2']),
    md: px(tokens.spacing.scale['4']),
    lg: px(tokens.spacing.scale['5']),
    xl: px(tokens.spacing.scale['6']),
  },
  breakpoints: {
    xs: '30em',
    sm: `${(tokens.breakpoints.phone + 1) / 16}em`,
    md: `${tokens.breakpoints.tablet / 16}em`,
    lg: `${tokens.breakpoints.laptop / 16}em`,
    xl: '90em',
  },
  headings: {
    fontFamily: tokens.typography.fontSans,
    sizes: {
      h1: { fontSize: tokens.typography.scale.h1.desktop, fontWeight: String(tokens.typography.scale.h1.weight) },
      h2: { fontSize: tokens.typography.scale.h2.size, fontWeight: String(tokens.typography.scale.h2.weight) },
      h3: { fontSize: tokens.typography.scale.h3.size, fontWeight: String(tokens.typography.scale.h3.weight) },
    },
  },
  components: {
    Button: { defaultProps: { radius: 'md' }, styles: { root: { fontWeight: 500 } } },
    ActionIcon: { defaultProps: { radius: 'md' } },
    TextInput: { defaultProps: { radius: 'md' }, styles: { label: { fontWeight: 500, color: c.ink2, marginBottom: 4 } } },
    PasswordInput: { styles: { label: { fontWeight: 500, color: c.ink2, marginBottom: 4 } } },
    Select: { styles: { label: { fontWeight: 500, color: c.ink2, marginBottom: 4 } } },
    Textarea: { styles: { label: { fontWeight: 500, color: c.ink2, marginBottom: 4 } } },
    Paper: { defaultProps: { radius: 'lg' }, styles: { root: { borderColor: c.border } } },
    Card: { defaultProps: { radius: 'lg', withBorder: true, padding: 'lg', shadow: 'none' } },
    Modal: { defaultProps: { radius: 'lg', shadow: 'md', overlayProps: { backgroundOpacity: 0.35, color: c.ink } }, styles: { title: { fontWeight: 600, fontSize: '1.1rem' } } },
    Drawer: { defaultProps: { shadow: 'md', overlayProps: { backgroundOpacity: 0.35, color: c.ink } } },
    Table: {
      defaultProps: { verticalSpacing: 'sm', horizontalSpacing: 'md', highlightOnHover: true, borderColor: c.border },
      styles: { th: { color: c.ink3, fontWeight: 600, fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.04em', background: c.surface2 } },
    },
    Tabs: { styles: { tab: { fontWeight: 500 } } },
    Badge: { styles: { root: { textTransform: 'none', fontWeight: 500 } } },
    Menu: { defaultProps: { shadow: 'md', radius: 'md' } },
    Popover: { defaultProps: { shadow: 'md', radius: 'md' } },
  },
});
