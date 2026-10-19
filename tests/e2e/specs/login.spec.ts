import { expect, test } from '@playwright/test';
import { DEMO, login } from '../support/auth';

// Needs the real login screen (frontend-shell) and, for the success path, the backend with the demo seed.

test.describe('login (NFR-04)', () => {
  test.fixme('consultant logs in and lands on My day', async ({ page }) => {
    // fixme: /login is still a placeholder; needs frontend-shell login screen + backend + seed (synthetic-data).
    await login(page, DEMO);
    await expect(page).toHaveURL(/\/$/);
  });

  test.fixme('wrong password shows an error and stays on login', async ({ page }) => {
    // fixme: same reason; expects the BAD_CREDENTIALS message from POST /auth/login (401).
    await page.goto('/login');
    await page.getByLabel(/e-?mail/i).fill(DEMO.email);
    await page.getByLabel(/password|şifre/i).fill('wrong-password');
    await page.getByRole('button', { name: /sign in|log in|giriş/i }).click();
    await expect(page.getByRole('alert')).toBeVisible();
    await expect(page).toHaveURL(/\/login/);
  });

  test.fixme('unauthenticated visit to /accounts redirects to login', async ({ page }) => {
    // fixme: route guard not built yet (frontend-shell).
    await page.goto('/accounts');
    await expect(page).toHaveURL(/\/login/);
  });
});
