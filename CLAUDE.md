# CLAUDE.md

unscroll removes short-video feeds (Reels, Shorts) and keeps the rest of each app usable. Shared JSON rules in `rules/` are applied by a web client (userscripts) and, from Phase 2, by an Android client.

## Read first
- Design: `docs/superpowers/specs/2026-11-03-unscroll-design.md`
- Current plan: `docs/superpowers/plans/2026-11-03-phase-2-android-client.md` (Phase 1: `docs/superpowers/plans/2026-11-03-phase-1-rules-and-userscripts.md`)
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
- Android, from `android/` (JDK 17+, Android SDK with platform 37 and build-tools 36): `./gradlew check` runs unit tests, lint and the offline-manifest check for every variant, `./gradlew assembleOfflineDebug assembleOnlineDebug` builds the APKs, and `./gradlew connectedOfflineDebugAndroidTest` runs the instrumented tests on a running emulator (they drive the real YouTube app). A skipped instrumented test is not a pass.

## Hard constraints
- No network requests, storage or analytics in client code. This is the rule for the web client. The Android client follows the same spirit within the limits of design §5.3: the `online` flavor may fetch rule files over HTTPS and nothing else, and the app stores only its own settings (disclosure flag, rules address, last refresh time) and the rule files it fetched or imported.
- Rule data lives only in `rules/` (CC0-1.0). Code is GPL-3.0-or-later.
- In code, the repo URL comes only from `package.json` → `repository.url`.
