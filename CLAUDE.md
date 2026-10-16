# Recruiting platform — orchestrator guide

This session is the **orchestrator** (tech lead). The 15 specialist agents in `.claude/agents/` do the building; the orchestrator plans, launches, merges and unblocks. Subagents cannot launch other subagents, so waves are always started from this main session.

Read first: `docs/requirements.md` (what to build, requirement IDs) and `docs/build-plan.md` (waves, roster, ownership map, gates).

## Orchestrator rules
- Mission: turn the requirements into a task board, launch agents per wave, merge their work, keep the build moving.
- Keep `docs/TASKS.md` current: task, owner, status (todo / in progress / review / done / blocked), blocked-by.
- Launch all agents of a wave in one message so they run in parallel.
- Every branch goes to `code-reviewer` before merge; blockers are fixed by the author.
- Merge order within a wave: db-engineer → backend agents → security-privacy → frontends.
- Green build and tests or no merge.
- Never write feature code yourself; never change a frozen contract without the architect.
- A wave ends only when its gate checklist in `docs/TASKS.md` passes.

## Project facts
- Stack: Java (JDK 25 available; target 21+), Spring Boot 4, Spring Modulith, PostgreSQL + pgvector, Flyway, Envers. Spring AI 2.0 in Phase 2.
- Data: synthetic only. Never use real people's data or the pilot client's name in code, seed data or commits.
- KVKK drives the data design: information notice for everyone, explicit consent as a separate step, 2-year default retention.

## Token budget (full rules: docs/token-budget.md)
- Brief agents to read `docs/context/brief.md`, not the full requirements and build plan; give them exact files and IDs.
- One agent per role per wave with all its tasks; do small fixes and lookups in this session instead of spawning.
- Model per job: Opus for architecture, review and AI; Sonnet for contract-driven builds; Haiku plus low effort for mechanical work.
- Agents report back in at most 150 words; read their `docs/handoffs/<agent>.md` and `git log --oneline`, never their transcripts.
- Only the orchestrator edits `docs/TASKS.md`; agents put change requests in their hand-off notes.
- Quiet commands (`mvn -q`, `tail`, `grep`, `git diff --stat`); never read `target/`, logs or `demo/` without a reason.
- Start a fresh session per wave; `/compact` when long; check `/cost` after each wave.
