import { expect, test } from '@playwright/test';

// Runs against the scaffold (placeholder screens) and later the real shell. The default UI language is TR,
// so links are located by href, not by label. Once the shell guards routes behind login, call login() in beforeEach.

const NAV = [
  ['/', 'My day'],
  ['/accounts', 'Accounts'],
  ['/job-orders', 'Job orders'],
  ['/network', 'Network'],
] as const;

test.describe('navigation (UX-02, UX-07)', () => {
  test('app loads and shows the main navigation', async ({ page }) => {
    await page.goto('/');
    for (const [href] of NAV) {
      await expect(page.locator(`nav a[href="${href}"]`)).toBeVisible();
    }
  });

  for (const [href, label] of NAV) {
    test(`nav link ${label} opens its screen`, async ({ page }) => {
      await page.goto('/network');
      await page.locator(`nav a[href="${href}"]`).click();
      await expect(page).toHaveURL(new RegExp(`${href === '/' ? '/$' : href + '$'}`));
      await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
    });
  }

  test('language switch sets the page language (UX-07)', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('html')).toHaveAttribute('lang', 'tr');
    await page.getByRole('button', { name: 'EN', exact: true }).click();
    await expect(page.locator('html')).toHaveAttribute('lang', 'en');
  });
});
