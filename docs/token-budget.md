# Token budget playbook

How we keep the multi-agent build cheap. Every agent starts with an empty context and pays for everything it reads, so most savings come from **reading less**, **choosing the right model**, and **passing results through small files instead of long messages**.

Rough size of what an agent can read (1 token ≈ 4 characters):

| File | Tokens |
| --- | --- |
| docs/requirements.md | ~8,000 |
| docs/build-plan.md | ~5,000 |
| docs/context/brief.md | ~1,000 |
| One requirement row (grep by ID) | ~60 |
| demo/src/main/resources/static/app.js | ~25,000 |

Reading the brief instead of both full docs saves about 12,000 tokens per agent launch. That is about 180,000 tokens over the 15 agents, before anything has been built.

## 1. Read less

- **Start from the brief.** Agents read `docs/context/brief.md`, then only the files their task names.
- **Look up by ID, not by file.** Use `grep -n "| A-07 |" docs/requirements.md` or `grep -n -A 8 "### 7." docs/build-plan.md` instead of reading the whole document.
- **Use contracts, not code.** To learn an interface, read `contracts/openapi.yaml` (and only the tags you need), not another module's source.
- **Read ranges.** Use the Read tool with `offset` and `limit`, or Grep with `-A`/`-B`, once you know where the code is.
- **Never read generated output.** `.claude/settings.json` blocks reads of `target/`, `node_modules/`, `dist/`, `*.log` and `.git/`.
- **Keep the demo out unless asked.** `demo/` is large (the UI alone is ~25k tokens) and is not the spec.

## 2. Keep command output short

- Maven: `mvn -q -B verify`. On failure, look only at the errors: `mvn -q -B verify 2>&1 | grep -E "ERROR|FAIL" | head -30`.
- Tests: run the one class you changed first (`mvn -q -B -pl crm-pipeline test -Dtest=StatusTransitionTest`), then the full suite once.
- Logs: `tail -n 40` or `grep` for the line you need, never `cat`.
- Git: `git diff --stat` first, then the diff of a single file.
- Long-running servers: log to a file and wait for the ready line, rather than streaming output.

## 3. Pick the cheapest model that can do the job

| Work | Model | Why |
| --- | --- | --- |
| Architecture, cross-module review, AI design | Opus | Mistakes here are expensive to undo |
| Well-specified build work from a contract | Sonnet | The contract removes most judgement |
| Seed data, renames, boilerplate, formatting | Haiku | Mechanical |

When launching an agent for mechanical work, also pass a lower `effort`. Don't run Opus on work Sonnet can do: on large tasks the price gap per token adds up fast.

## 4. Spawn fewer, better-briefed agents

- **Batch tasks.** One agent per role per wave, with all of that role's tasks, rather than one agent per small task. Every launch re-reads its brief and context.
- **Brief completely once.** A precise prompt (owned paths, inputs, outputs, how to verify) avoids a second agent run to fix misunderstandings.
- **Don't delegate small things.** A one-file fix, a lookup or a grep is cheaper in the main session than in a new agent.
- **Continue instead of restarting.** For follow-up work on the same task, send the existing agent a message rather than launching a fresh one that must rebuild its context.
- **Read-only reviewer.** `code-reviewer` reads the diff (`git diff main...<branch>`), not the whole repo.

## 5. Pass results through files, not messages

- Agents end with a hand-off note in `docs/handoffs/<agent>.md` using `docs/handoffs/TEMPLATE.md` (under 40 lines).
- The final reply to the orchestrator is **at most 150 words**: branch, commits, verification result, blockers. Details live in the hand-off note.
- The orchestrator reads hand-off notes and `git log --oneline`, never agent transcripts.

## 6. Keep the orchestrator session small

- **One session per wave.** Start a fresh session for each wave. `CLAUDE.md`, `docs/TASKS.md` and the hand-off notes carry the state, so nothing is lost.
- **Compact or clear between tasks.** Use `/compact` when a session gets long, and `/clear` when switching to unrelated work.
- **Keep stable files stable.** CLAUDE.md and the agent files are re-sent on every call and benefit from prompt caching. Edit them in batches rather than in many small changes, since each change invalidates the cache.
- **Check spend** with `/cost` after each wave, and note large outliers in `docs/TASKS.md`.

## 7. Spend tokens where they prevent rework

Some spending saves more than it costs:
- Freezing contracts in Wave 0, so agents don't build against guesses.
- A clear "done when" with an exact command, so agents stop when finished instead of polishing.
- Small pull requests, so reviews are short and fixes are local.
