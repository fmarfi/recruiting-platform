# Code structure rules (all agents)

Goal: anyone can open a folder and see what it does. Folders stay small and each class does one thing.

## Backend (Java)
- **Package by feature, then by layer.** Inside a module, one sub-package per feature (aggregate). Each feature gets a few layer sub-packages, and only the ones it needs:
  ```
  com.recruiting.platform.accounts
  ├── package-info.java            (@ApplicationModule)
  ├── AccountsApi.java             (public API other modules may call; only if needed)
  ├── account/
  │   ├── web/      AccountController, AccountStatusController, dto/ (request/response records)
  │   ├── app/      AccountService, AccountStatusService, AccountMapper
  │   └── domain/   Account, AccountRepository
  ├── contact/ ...
  ├── person/ ...
  ├── importing/ ...
  └── search/ ...
  ```
- **At most about 8 classes per folder.** Past that, split by feature or add a sub-package (`dto/`, `event/`, `query/`).
- **Small classes:** one public type per file. Split a controller per resource (not one controller per module). A service holds one use-case area. Keep classes under about 250 lines and methods under about 30.
- DTOs are Java `record`s in `web/dto/`. Entities never leave the module and are never returned from controllers.
- Spring Modulith: sub-packages are module-internal. Only the module's base package (and packages marked `@NamedInterface`) are visible to other modules, so put the public API there.
- Tests mirror the main package structure.
- **Naming:** `XController`, `XService`, `XRepository`, `XMapper`, `XRequest`/`XResponse`, `XListener` (event listeners), `XIT` (integration tests).

## Frontend (TypeScript)
- Inside your area (`src/accounts`, `src/pipeline`, ...): one folder per screen or feature, e.g. `accounts/list/`, `accounts/detail/`, `accounts/components/`, `accounts/hooks/`, `accounts/i18n/`.
- One component per file, under about 200 lines. Data fetching goes in hooks (`useAccounts.ts`), not inside components.
- Tests sit next to the file (`AccountList.test.tsx`).

## Comments
- A short header comment on each class or file says what it is for when the name isn't enough. Explain *why* in comments, not *what*.

## Readability (all code)
- Consistent indentation: 4 spaces in Java, 2 in TypeScript, no tabs. Blank line between methods and between logical blocks inside long methods. One statement per line; wrap lines over ~120 characters.
- Java: Javadoc on every public class, interface and public method (one-sentence purpose; `@param`/`@return`/`@throws` only when not obvious). Package-level `package-info.java` for each module's public API package. Inline `//` comments explain *why*, business rules (KVKK, status lifecycle) and non-obvious choices.
- TypeScript: a short header comment per file; JSDoc on exported hooks, components and helpers.
- Don't comment the obvious, and don't leave commented-out code.

## Known follow-up
- The flat `domain` packages from Wave 1a (13–15 entities each) move into feature folders after Wave 1b merges. This is a mechanical refactor done by the orchestrator, so don't move them now.
- `contracts/enums` (28) and `contracts/events` (20) get sub-packages at the same time (architect).
