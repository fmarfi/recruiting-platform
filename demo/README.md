# Recruiting CRM demo

A clickable demo of Phase 1 (Module A, the CRM app) for collecting ideas and feedback before the real build. Spring Boot 4.1 backend with an in-memory store and fully fictional data; plain HTML/JS frontend. No database or Docker needed.

This is a throwaway prototype, separate from the agent build in the rest of the repo.

## Run

```
cd demo
mvn spring-boot:run
```

Open http://localhost:8080. Every restart resets the data.

## What you can try

- **My day**: next actions (overdue in red), your open jobs, recent accounts.
- **Accounts**: filter by status, sector or owner; add a client.
- **Account page**: change the status with the badge, see contacts, job orders, proposals and the timeline.
- **Job order**: drag candidates across the pipeline board; illegal moves are blocked, and rejecting asks for a reason.
- **Network**: people, their connections, and every pipeline they are in.
- **Search**: one box for accounts, people and jobs (also finds "ozturk" for "Öztürk").
- **Quick add**: log a call, visit or note from any account or person.
- **TR / EN** switch in the top bar.

## Code

- `API.md`: the REST contract the UI is built against.
- `src/main/java/com/recruiting/demo/domain`: records and status lifecycles (allowed transitions per status).
- `src/main/java/com/recruiting/demo/crm`: `CrmService` (rules and timeline), `DemoStore`, `SeedData`.
- `src/main/java/com/recruiting/demo/web`: REST controller and error handling.
- `src/main/resources/static`: the UI.

Javadoc: `mvn javadoc:javadoc`, then open `target/reports/apidocs/index.html`.

Status lists are demo assumptions until the client confirms the statuses they use today.
