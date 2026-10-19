# Hand-off: backend-accounts · W1-10 · 2026-10-19

**Branch / commits:** `worktree-wave1b-backend-accounts` · 6 commits (main merged in)

**Done**
- Reference data: GET /reference-data built from the lifecycle enums (labels, tone, next, reason/manager flags, sectors in use).
- Accounts, status moves (A-02, open-jobs guard from the account_open_job projection), contacts, people, person-contact-roles.
- Network links: CRUD, duplicate rule (reverse counts for KNOWS/WORKED_WITH), /people/{id}/links, /accounts/{id}/connections.
- Search: accounts and people here; job orders via `SearchContributor` SPI (pipeline implements it).
- Import: upload/validate, commit, discard, list, get, templates. CSV (comma or semicolon, UTF-8, BOM) and XLSX read with the JDK only (no POI). Kinds ACCOUNTS, CONTACTS, PEOPLE. File and errors are nulled when a job ends (ADR 0009).
- Events published: AccountCreated/Updated/StatusChanged, PersonCreated/Updated, ContactAdded/Removed, ImportCompleted. Listens to JobOrder*/ActivityLogged/consent/retention facts (account/person event listeners).
- Tests: TableReaderTest (unit); platform-app `AccountsApiIT` (reference data, account lifecycle, contacts, links, search, import).

**Not done / left**
- Import columns are my choice (see ImportTemplates): ACCOUNTS name,sector,city,website,ownerEmail; CONTACTS accountName,name,title,email,phone,city,role; PEOPLE name,type,title,company,email,phone,city,ownerEmail.

**Update 2026-10-19 (security merged)**
- `ActorResolver`, `UserLookup` and the local `UserRef` are gone: accounts uses security's `CurrentUser` (via `Actor.of`), `UserDirectory`, `UserRef`. I added `UserDirectory.isActive(id)` and `activeIdByEmail(email)` (owner checks, import owner e-mail); security please keep them.
- New public API `com.recruiting.platform.accounts.PersonDataApi` (`anonymise(personId, actorId)`, `delete(personId, actorId)`, ADR 0009 step 2, publishes `PersonAnonymised`, delete goes through JPA so Envers writes the revtype 2 row) and SPI `PersonExportContributor` (`section()`, `collect(personId)`).
- `GET /people/{id}/export` (MANAGER+) in accounts: person, contactRoles, links; `activities`/`applications` = [] and `consent` omitted until contributors exist. Writes the EXPORT access-log row.
- AccountsApiIT now extends AbstractApiIT (real login, method security): imports (all endpoints) are 403 for CONSULTANT; export 401/403/404/200 + access log; anonymise and delete via PersonDataApi.

**How security swaps the adapter**
1. privacy/pom.xml: add dependency on `crm-accounts` (module already allows `accounts`).
2. Replace `JdbcPersonGateway` by a gateway using `PersonApi` (find/findByIds: PersonSummary has no city/lastContactAt/createdAt, ask me if needed; `idsWithoutPrivacy` has no API yet, keep one SQL or request `PersonApi.idsWithoutPrivacy`) and `PersonDataApi.anonymise/delete`. Remove event publishing and Envers SQL from privacy: accounts publishes `PersonAnonymised` now.
3. Add `@Component class ConsentExportContributor implements PersonExportContributor` with `section()="consent"` returning `ConsentQueryService.overview(id)`. Pipeline should add `activities` and `applications` the same way.

**Others must know**
- backend-pipeline: implement `com.recruiting.platform.accounts.SearchContributor` (type JOB_ORDER) for /search.

**Change requests** (for files you don't own)
- platform-app · main: add the `@RestControllerAdvice` for ApiException/validation/MaxUploadSizeExceeded (ADR 0006); none exists yet (my IT has a stand-in).
- platform-app · application.yml: `spring.servlet.multipart.max-file-size/max-request-size=10MB` (Spring default is 1MB; contract says 10 MB).
- platform-app · pom.xml: I added test deps spring-boot-starter-webmvc-test and spring-security-test.
- platform-app · tests: I renamed PlatformApplicationTests (profile "test", no DB) to persistence/PlatformApplicationIT on Testcontainers, because feature beans now need repositories.

**Verified with**
- `mvn -q -B verify` → green (incl. ModularityTests)
- `mvn -q -B -Pintegration verify` → green, 49 tests (AccountsApiIT 4)
