# ADR 0007: Authentication with a session cookie and three roles

- Status: accepted (Wave 0)
- Date: 2026-10-16

## Context

NFR-05 asks for role-based access (consultant, manager, admin). The SPA is served from the same origin as the API (ADR 0003). SSO (Keycloak) is optional and not requested for the pilot. Candidates never log in in Phase 1; they only use the one-time consent link.

## Decision

- **Spring Security form-style JSON login**: `POST /api/v1/auth/login` with e-mail and password creates an HTTP session (`JSESSIONID`, `HttpOnly`, `Secure`, `SameSite=Lax`); `POST /auth/logout` ends it; `GET /auth/me` returns the current user. Session timeout 8 h idle. Sessions in memory for the pilot (one instance); Spring Session JDBC if we scale out.
- **CSRF** on: cookie `XSRF-TOKEN` (readable by JS) and header `X-XSRF-TOKEN` on state-changing requests (Spring Security SPA CSRF configuration). `/public/**` and `/auth/login` are exempt.
- **Passwords**: `DelegatingPasswordEncoder` (bcrypt default), min 12 characters, admin-set initial password with `mustChangePassword`.
- **Roles**: `Role` enum CONSULTANT < MANAGER < ADMIN (hierarchical, `RoleHierarchy`). Endpoint rules follow `x-roles` in the contract, enforced with `@PreAuthorize` in controllers or one `SecurityFilterChain` matcher table in `security`. Transition rules that are manager-only come from `Lifecycle.requiresElevatedRole` and are checked in services (403).
- **No record-level restrictions in Phase 1** (ASSUMPTION): every consultant sees all accounts and people, as in the demo. If the client wants "own accounts only", that is a query filter added later.
- **Public endpoints** (`security: []`): `/auth/login`, `/public/consent/{token}`; rate-limited by IP.
- **Keycloak / OIDC** later: replace the login endpoint with the OAuth2 login flow; roles map from token claims. The rest of the API does not change.

## Consequences

- Simple, no tokens in browser storage, works with the Prism mock (any `JSESSIONID` cookie value passes).
- Requires same origin (or a proxy) for the SPA; the Vite dev proxy covers development.
- Brute-force protection: lock a user for 15 minutes after 10 failed logins (security-privacy).

## Alternatives considered

- **JWT bearer tokens in the SPA**: stateless, but token storage in the browser and refresh handling add risk and work without a benefit for one same-origin app.
- **HTTP Basic**: no logout, credentials on every request.

## Addendum: pilot ownership rule (review fix)

- For the pilot, any consultant may change any record, whoever its `owner` is. There are no record-level ownership checks. `owner` is for assignment, filtering and reminders only.
- Manager-only moves stay manager-only (account to CLOSED, CLOSED to LEAD, job order CANCELLED to OPEN, from `Lifecycle.requiresElevatedRole`). A 403 `FORBIDDEN` therefore never means "not your record".
- Reasons for status changes are trimmed and must be 3 to 500 characters.
