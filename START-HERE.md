# Start here

**Where things stand:** `docs/TASKS.md` · **Project in one page:** `docs/context/brief.md`

## Commands
| What | Command |
| --- | --- |
| Run the demo (http://localhost:8080) | `cd demo` then `mvn spring-boot:run` |
| Build and test the platform | `mvn -q -B verify` |
| See agent branches | `git branch --list "worktree-*"` |
| Recent work | `git log --oneline -10` |

## In Claude Code
- Open Claude Code **in this folder**, so the agents in `.claude/agents/` are loaded.
- **New session for each wave.** Start with: "Read CLAUDE.md and docs/TASKS.md, then continue."
- `/cost` shows spend · `/compact` when a session gets long · `/clear` to switch topics.

## Rules
- Fictional data only. Never real people, never the pilot client's name.
- Install Docker Desktop before Wave 1 (database and tests need it).
