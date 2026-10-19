import { expect, test } from '@playwright/test';
import { login } from '../support/auth';

// Happy paths of the Must stories against the real app plus the demo dataset (backend, seed and screens needed).
// Each test is test.fixme with its blocker; remove the fixme when that screen and endpoint land.

test.describe('UX-01 My day', () => {
  test.fixme('My day loads with next actions and jobs', async ({ page }) => {
    // fixme: My day screen (frontend-shell) + GET /home (backend-pipeline) + login.
    await login(page);
    await expect(page.getByRole('heading', { name: /my day/i })).toBeVisible();
    await expect(page.getByRole('region', { name: /next actions|today/i })).toBeVisible();
  });
});

test.describe('A-01 accounts list', () => {
  test.fixme('accounts list filters by name', async ({ page }) => {
    // fixme: accounts list screen (frontend-shell) + GET /accounts (backend-accounts) + login.
    await login(page);
    await page.getByRole('link', { name: 'Accounts', exact: true }).click();
    await expect(page.getByRole('row').nth(1)).toBeVisible();
    await page.getByLabel(/filter by name/i).fill('zzzz-no-such-client');
    await expect(page.getByText(/no clients match/i)).toBeVisible();
  });
});

test.describe('A-02 / UX-03 account status', () => {
  test.fixme('consultant moves an account Lead to Proposal sent', async ({ page }) => {
    // fixme: account page status badge (frontend-shell) + PATCH /accounts/{id}/status (backend-accounts) + login.
    await login(page);
    await page.goto('/accounts');
    await page.getByRole('row', { name: /Lead/ }).first().getByRole('link').first().click();
    await page.getByRole('button', { name: /status/i }).click();
    await page.getByRole('menuitem', { name: 'Proposal sent' }).click();
    await expect(page.getByRole('button', { name: /proposal sent/i })).toBeVisible();
  });
});

test.describe('A-06 / A-07 / UX-04 pipeline board', () => {
  test.fixme('candidate card moves Sourced to Screened', async ({ page }) => {
    // fixme: board (frontend-pipeline) + PATCH /applications/{id}/stage (backend-pipeline) + login. Uses the Move menu, not drag.
    await login(page);
    await page.goto('/job-orders');
    await page.getByRole('row', { name: /Supply Chain Director/ }).getByRole('link').first().click();
    const sourced = page.getByRole('region', { name: /sourced/i });
    const card = sourced.getByRole('listitem').first();
    const name = (await card.getByRole('link').first().innerText()).trim();
    await card.getByRole('button', { name: /move to/i }).click();
    await page.getByRole('menuitem', { name: 'Screened' }).click();
    await expect(page.getByRole('region', { name: /screened/i }).getByText(name)).toBeVisible();
  });
});

test.describe('A-12 network', () => {
  test.fixme('person page opens from the network list', async ({ page }) => {
    // fixme: network + person screens (frontend-shell) + GET /people/{id} (backend-accounts) + login.
    await login(page);
    await page.getByRole('link', { name: 'Network', exact: true }).click();
    await page.getByRole('row').nth(1).getByRole('link').first().click();
    await expect(page).toHaveURL(/\/people\/[0-9a-f-]+/);
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
  });
});

test.describe('A-14 / UX-02 global search', () => {
  test.fixme('searching "ozturk" finds Öztürk', async ({ page }) => {
    // fixme: search box + results (frontend-shell) + GET /search (backend-accounts); needs a seeded Öztürk + login.
    await login(page);
    await page.getByRole('searchbox', { name: /search/i }).fill('ozturk');
    await page.keyboard.press('Enter');
    await expect(page).toHaveURL(/\/search\?q=ozturk/);
    await expect(page.getByText(/Öztürk/).first()).toBeVisible();
  });
});

test.describe('UX-06 quick add', () => {
  test.fixme('quick add creates an account from the + New menu', async ({ page }) => {
    // fixme: + New menu (frontend-shell) + POST /accounts (backend-accounts) + login.
    await login(page);
    const name = `QA Test Client ${Date.now()}`;
    await page.getByRole('button', { name: /new/i }).click();
    await page.getByRole('menuitem', { name: /client|account/i }).click();
    await page.getByLabel(/name/i).fill(name);
    await page.getByRole('button', { name: /save|add|create/i }).click();
    await expect(page.getByText(name)).toBeVisible();
  });
});
