import { createTheme, type MantineColorsTuple } from '@mantine/core';
import { tokens } from './tokens';

const brand = tokens.ramp.brand as unknown as MantineColorsTuple;
const brass = tokens.ramp.brass as unknown as MantineColorsTuple;
const gray = tokens.ramp.gray as unknown as MantineColorsTuple;

const c = tokens.color.light;
const px = (n: number) => `${n}px`;

export const theme = createTheme({
  primaryColor: 'brand',
  primaryShade: 6,
  black: c.ink,
  white: c.surface,
  autoContrast: true,
  focusRing: 'auto',
  fontSizes: { xs: '0.8125rem', sm: '0.9375rem', md: '1rem', lg: '1.125rem', xl: '1.375rem' },
  lineHeights: { xs: '1.5', sm: '1.5', md: '1.5', lg: '1.5', xl: '1.4' },
  shadows: { xs: 'none', sm: '0 1px 2px rgba(22,24,29,.06)', md: tokens.shadow.pop.light, lg: tokens.shadow.pop.light, xl: tokens.shadow.pop.light },
  colors: {
    brand,
    brass,
    red: ['#fdecea', '#fbd9d5', '#f5b5ae', '#ee8b81', '#e0645a', '#cf4a40', '#b42318', '#9a1d14', '#82170f', '#6b120b'],
    // Text-safe shades: Mantine paints c="green" / light badges with shade 6, which must pass WCAG AA on white and on its light tint.
    green: ['#e8f5ee', '#dff0e6', '#bfe0cc', '#8fc8a6', '#5aa87c', '#2f8f5a', '#17573a', '#134a31', '#0f3d29', '#0a2d1e'],
    yellow: ['#fdf6e3', '#fbf0d9', '#f6e0b0', '#eecb80', '#e2b04d', '#c98a12', '#7a4e00', '#694200', '#583700', '#432a00'],
    blue: ['#eef3fb', '#e6eefa', '#c7d9f2', '#9dbce6', '#6f9bd6', '#3b78c0', '#1b4682', '#173c70', '#12315b', '#0d2443'],
    gray,
  },
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
      h1: { fontSize: tokens.typography.scale.h1.desktop, fontWeight: String(tokens.typography.scale.h1.weight), lineHeight: String(tokens.typography.scale.h1.lineHeight) },
      h2: { fontSize: tokens.typography.scale.h2.size, fontWeight: String(tokens.typography.scale.h2.weight) },
      h3: { fontSize: tokens.typography.scale.h3.size, fontWeight: String(tokens.typography.scale.h3.weight) },
    },
  },
  components: {
    Container: { defaultProps: { px: 0 } },
    Button: { defaultProps: { radius: 'md' }, styles: { root: { fontWeight: 500, height: 'auto', minHeight: 'var(--button-height)', paddingBlock: 6 }, label: { whiteSpace: 'normal', textAlign: 'center', lineHeight: 1.35 } } },
    ActionIcon: { defaultProps: { radius: 'md' } },
    TextInput: { defaultProps: { radius: 'md' }, styles: { label: { fontWeight: 500, color: c.ink2, marginBottom: 6, fontSize: '0.9375rem' }, description: { fontSize: '0.8125rem', color: c.ink3, marginBottom: 6 }, error: { fontSize: '0.8125rem' } } },
    PasswordInput: { styles: { label: { fontWeight: 500, color: c.ink2, marginBottom: 6, fontSize: '0.9375rem' }, description: { fontSize: '0.8125rem', color: c.ink3, marginBottom: 6 }, error: { fontSize: '0.8125rem' } } },
    Select: { styles: { label: { fontWeight: 500, color: c.ink2, marginBottom: 6, fontSize: '0.9375rem' }, description: { fontSize: '0.8125rem', color: c.ink3, marginBottom: 6 }, error: { fontSize: '0.8125rem' } } },
    Textarea: { styles: { label: { fontWeight: 500, color: c.ink2, marginBottom: 6, fontSize: '0.9375rem' }, description: { fontSize: '0.8125rem', color: c.ink3, marginBottom: 6 }, error: { fontSize: '0.8125rem' } } },
    Paper: { defaultProps: { radius: 'lg' }, styles: { root: { borderColor: c.border } } },
    Card: { defaultProps: { radius: 'lg', withBorder: true, padding: 'lg', shadow: 'none' } },
    Modal: { defaultProps: { radius: 'lg', shadow: 'md', overlayProps: { backgroundOpacity: 0.35, color: c.ink } }, styles: { title: { fontWeight: 600, fontSize: '1.1rem' } } },
    Drawer: { defaultProps: { shadow: 'md', overlayProps: { backgroundOpacity: 0.35, color: c.ink } } },
    Table: {
      defaultProps: { verticalSpacing: 'sm', horizontalSpacing: 'md', highlightOnHover: true, borderColor: c.border },
      styles: { th: { color: c.ink2, fontWeight: 600, fontSize: '0.875rem', lineHeight: 1.35, verticalAlign: 'bottom', background: c.surface2 }, td: { fontSize: '0.9375rem', verticalAlign: 'middle' } },
    },
    Tabs: { defaultProps: { color: 'brand' }, styles: { tab: { fontWeight: 500, fontSize: '0.9375rem', height: 'auto', minHeight: 44, whiteSpace: 'normal', textAlign: 'center' } } },
    Badge: { styles: { root: { textTransform: 'none', fontWeight: 500, height: 'auto', minHeight: 22, paddingBlock: 2 }, label: { whiteSpace: 'normal' } } },
    Menu: { defaultProps: { shadow: 'md', radius: 'md' } },
    Popover: { defaultProps: { shadow: 'md', radius: 'md' } },
  },
});
