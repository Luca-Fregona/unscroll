# CLAUDE.md

unscroll removes short-video feeds (Reels, Shorts) and keeps the rest of each app usable. Shared JSON rules in `rules/` are applied by a web client (userscripts) and, from Phase 2, by an Android client.

## Read first
- Design: `docs/superpowers/specs/2026-11-03-unscroll-design.md`
- Current plan: `docs/superpowers/plans/2026-11-03-phase-1-rules-and-userscripts.md`
- Roadmap: `ROADMAP.md`

## Workflow
- Use the Superpowers skills: brainstorming → spec → writing-plans → subagent-driven-development or executing-plans.
- TDD. Commit after each task using Conventional Commits.
- Tick plan checkboxes and `ROADMAP.md` items as tasks finish.

## Commands (available after Phase 1 Task 1)
- `npm test` runs Vitest. DOM tests use `// @vitest-environment jsdom`. esbuild tests must stay in the node environment.
- `npm run typecheck`
- `npm run validate` checks every `rules/*.json`.
- `npm run build` writes `dist/unscroll-<id>.user.js`.

## Hard constraints
- No network requests, storage or analytics in client code.
- Rule data lives only in `rules/` (CC0-1.0). Code is GPL-3.0-or-later.
- The repo URL appears only in `package.json` → `repository.url`.
