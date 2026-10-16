# ADR 0006: REST API conventions — paging, errors, updates, versioning

- Status: accepted (Wave 0)
- Date: 2026-10-16

## Decision

- **Contract first**: `contracts/openapi.yaml` (OpenAPI 3.1) is the source; controllers implement it; frontends generate types from it; QA tests against it. `x-owner` names the implementing module, `x-roles` the allowed roles.
- **Base path** `/api/v1`. A breaking change gets `/api/v2` for the affected resources; additive changes stay in v1.
- **Resources**: plural nouns, nested only where the child cannot exist without the parent and is listed per parent (`/accounts/{id}/contacts`, `/job-orders/{id}/applications`). Cross-cutting lists use filters (`/activities?accountId=`, `/job-orders?accountId=`) so one endpoint serves the account tab, the person page and My day.
- **Paging**: `page` (0-based), `size` (default 20, max 100), `sort=prop,asc|desc` repeatable — Spring Data `Pageable`. Response is Spring Data's stable `PagedModel` JSON: `{ "content": [...], "page": { "size", "number", "totalElements", "totalPages" } }` (`@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)`). Bounded sub-lists return plain arrays.
- **Filtering**: query parameters named after the field; repeat a parameter for "any of" (`status=LEAD&status=ACTIVE`); dates as `…Before`/`…After`.
- **Updates**: `PATCH` with JSON merge semantics (absent = unchanged, `null` = clear where nullable). Status moves are separate sub-resources (`PATCH /…/status`, `/stage`) with `{ status, reason?, version? }` so permissions, reasons and events have one entry point.
- **Optimistic locking**: every mutable resource returns `version`; clients send it back; mismatch → 409 `VERSION_CONFLICT`. Omitting it skips the check (quick edits on a phone).
- **Creation**: 201 with the created resource and a `Location` header; async work (consent e-mails) → 202 with the queued resource.
- **Errors**: RFC 9457 `application/problem+json` produced by one `@RestControllerAdvice` in `platform-app` from `ApiException` (contracts) and Bean Validation errors. Members: `type` (`urn:recruiting:problem:<code>`), `title`, `status`, `detail`, `instance`, plus extensions `code` (`ErrorCode` enum), `errors[]` (`field`, `code`, `message`), and for status moves `currentStatus`, `requestedStatus`, `allowed[]`; for duplicates `existingId`. `detail` is English; the UI translates by `code`.
- **Formats**: ids UUID strings; `date` = `YYYY-MM-DD`; `date-time` = RFC 3339 UTC (`Instant`); money as decimal string plus ISO currency; enums as Java names.
- **Not found vs forbidden**: 403 when the role cannot do the action at all, 404 for missing records.

## Consequences

- Every list looks the same to the frontend (UX-07 "same list, filter and detail patterns").
- One error shape, so the UI has one error component keyed by `code`.
- Spring Boot 4 / Spring Framework 7 support all of this out of the box (`ProblemDetail`, `PagedModel`).
