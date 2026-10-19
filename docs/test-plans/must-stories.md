# Test plan: Must stories

Owner: qa-engineer. Layers: **C** = contract test (response validated against `contracts/openapi.yaml`, `ContractAssertions.matchesContract()`), **I** = API integration test (`*IT`, Testcontainers, owning backend agent writes it, QA reviews), **E** = Playwright (`tests/e2e/specs/`), **U** = unit/component test (owner). Status: `E:fixme` = written in `must-stories.spec.ts` or planned, blocked on screen or endpoint.
Run: contract+API `mvn -B verify -Pintegration`; E2E `cd tests/e2e && npx playwright test` (see `tests/e2e/playwright.config.ts` for BASE_URL / API_TARGET).
Done when every row marked E has a passing test against the real stack with the demo dataset.

| Story | Criteria | Layers | Happy-path E2E | Notes |
| --- | --- | --- | --- | --- |
| A-01 Accounts and overview | 1 create, 2 name required, 3 duplicate name (409 + link), 4 filter by status, 5 overview shows all | C I E | filter by name (E:fixme) | Duplicate check is case-insensitive; assert error body per `contracts/errors/` |
| A-02 Account status lifecycle | 1 legal move, 2 illegal move (409), 3 reason required, 4 role limit (403), 5 closing blocked by open jobs, 6 history append-only | C I E | Lead to Proposal sent (E:fixme) | Transitions from `docs/status-lifecycles.md`; table-driven IT over all pairs |
| A-03 Contacts per account | 1 add, 2 reuse person, 3 e-mail format, 4 remove | C I | none | Contact reuse must not create a second person |
| A-06 Job orders | 1 create, 2 not under closed account, 3 advance, 4 illegal move, 5 cancel reason, 6 fill needs placement, 7 days open | C I | via board (A-07) | Days open: fix the clock in the test |
| A-07 Candidate pipeline | 1 add by hand, 2 no duplicates, 3 move forward, 4 rejection reason, 5 placed only from offer, 6 reinstate, 7 same person two jobs, 8 closed job order | C I E | Sourced to Screened via Move menu (E:fixme) | Use the menu, not drag (UX-03 criterion 5 covered separately) |
| A-12 Network | 1 create person, 2 company history, 3 how we know, 4 last contact, 5 change owner, 6 duplicate warning | C I E | person page from list (E:fixme) | |
| A-14 Global search | 1 cross-entity, 2 Turkish characters (ozturk finds Oztürk), 3 filters, 4 no results, 5 speed (under 1 s on demo data), 6 permissions | C I E | search "ozturk" (E:fixme) | Needs seeded Oztürk (synthetic-data) |
| S-01 KVKK notice e-mail | 1 sent automatically, 2 consent, 3 refused, 4 withdrawn, 5 no e-mail, 6 reminder, 7 notice for all | I | none (token links tested at API level) | Mail via test sender; reminder uses a fake clock |
| S-02 Website form consent | 1 notice visible, 2 unticked, 3 cannot submit, 4 stored | C I E | later, when the form exists | |
| S-03 Retention and deletion | 1 flagging, 2 delete or anonymise, 3 renewal, 4 configurable, 5 time-shifted | I | none | Time-shift with injected clock, never real waits |
| UX-01 My day | 1 content, 2 empty state, 3 click-through | C E | My day loads (E:fixme) | |
| UX-02 Search and 3 clicks | 1 search everywhere, 2 three clicks, 3 keyboard | E | nav smoke (E, passing on scaffold) | Three-click paths listed in `docs/ux/flows.md` |
| UX-03 Status badges | 1 badge everywhere, 2 colour not alone (text label present), 3 one-click change, 4 reason prompt, 5 drag and drop | E U | account status change (E:fixme) | |
| UX-04 Pipeline board | 1 columns, 2 cards, 3 move, 4 rejection reason, 5 mobile (375 px viewport) | E U | same as A-07 | Add a mobile project to the Playwright config when the board lands |
| UX-05 Account page | 1 header, 2 tabs, 3 timeline, 4 deep link | E | none yet | Deep link: open `/accounts/{id}?tab=...` directly |

## Cross-cutting
- Every controller test adds `matchesContract()` for each documented status (200/201/4xx).
- Auth: login (`/login`), expired session, CSRF header missing (403), role checks per `x-roles`.
- Regression suite: CI runs `mvn verify -Pintegration` and the Playwright smoke project; the full-stack E2E job needs Postgres + seed.
- Bugs are reported in the QA hand-off under change requests to the owning agent; QA does not fix them.
