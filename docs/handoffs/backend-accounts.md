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
- Real current user: `ActorResolver` and `UserLookup` are temporary (user id = authentication name; reads app_user by JDBC). Swap to security's CurrentUser/UserDirectory when published.
- Import columns are my choice (see ImportTemplates): ACCOUNTS name,sector,city,website,ownerEmail; CONTACTS accountName,name,title,email,phone,city,role; PEOPLE name,type,title,company,email,phone,city,ownerEmail.
- `createdBy` is null in responses until an AuditorAware bean exists (security).

**Others must know**
- backend-pipeline: implement `com.recruiting.platform.accounts.SearchContributor` (type JOB_ORDER) for /search.
- security-privacy: enable method security (controllers use `@PreAuthorize`; imports need MANAGER); publish AuditorAware and CurrentUser.

**Change requests** (for files you don't own)
- platform-app · main: add the `@RestControllerAdvice` for ApiException/validation/MaxUploadSizeExceeded (ADR 0006); none exists yet (my IT has a stand-in).
- platform-app · application.yml: `spring.servlet.multipart.max-file-size/max-request-size=10MB` (Spring default is 1MB; contract says 10 MB).
- platform-app · pom.xml: I added test deps spring-boot-starter-webmvc-test and spring-security-test.
- platform-app · tests: I renamed PlatformApplicationTests (profile "test", no DB) to persistence/PlatformApplicationIT on Testcontainers, because feature beans now need repositories.

**Verified with**
- `mvn -q -B verify` → green (incl. ModularityTests)
- `mvn -q -B verify -Pintegration` → green (AccountsApiIT 3, other ITs, PlatformApplicationIT)
