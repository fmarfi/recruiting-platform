import { expect, type Page } from '@playwright/test';

export const DEMO = {
  email: 'consultant@demo.example',
  password: 'Demo-Password-2026',
};

/** Logs in through the real login form. Fails if the form is missing, so use only in specs that need it. */
export async function login(page: Page, user = DEMO): Promise<void> {
  await page.goto('/login');
  await page.getByLabel(/e-?mail/i).fill(user.email);
  await page.getByLabel(/password|şifre/i).fill(user.password);
  await page.getByRole('button', { name: /sign in|log in|giriş/i }).click();
  await expect(page.getByRole('heading', { name: /my day|bugün/i })).toBeVisible();
}
