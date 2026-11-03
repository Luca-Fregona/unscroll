# Phase 1: Rules and Web Client Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship v0.1: a validated rules format, plus userscripts that hide YouTube Shorts and the Instagram Reels and Explore feeds in Safari on iOS and in desktop browsers.

**Architecture:** Each service is described by a JSON rule set in `rules/`, checked by a JSON Schema and by semantic checks in `core/`. A small dependency-free engine in `web/` applies a rule set inside a page: it injects hide-CSS and handles redirects on load and on SPA navigation. A build step bundles the engine and each rule set into one userscript with esbuild. GitHub Actions validates every push and publishes the userscripts on tagged releases.

**Tech Stack:** Node.js 22, TypeScript 5 (strict, ESM), Vitest 5 + jsdom, Ajv 8 (JSON Schema 2020-12), css-what 8, esbuild 0.28, tsx 4.

**Spec:** `docs/superpowers/specs/2026-11-03-unscroll-design.md`

## Global Constraints

- Node `>=22`, npm, `"type": "module"`. TypeScript `strict: true` with `moduleResolution: "Bundler"`.
- The userscript bundle has **zero runtime dependencies** and makes **no network requests**. It uses no storage and no analytics. Ajv and css-what are used only at validation and build time.
- Rule set file name equals its `id`: `rules/<id>.json`. The schema is at `rules/schema/ruleset.schema.json`.
- Output file: `dist/unscroll-<id>.user.js`. Userscript `@version` equals the `package.json` `version`.
- Repo URL comes only from `package.json` → `repository.url` (`https://github.com/Luca-Fregona/unscroll`).
- Licenses: code is `GPL-3.0-or-later`, `rules/` is `CC0-1.0`, and the userscript `@license` is `GPL-3.0-or-later`.
- Tests sit next to the code as `*.test.ts`. DOM tests start with `// @vitest-environment jsdom`. **esbuild tests must use the default node environment**, because esbuild throws under jsdom (verified on 2026-11-03).
- Commit messages follow Conventional Commits (`feat:`, `fix:`, `test:`, `chore:`, `docs:`, `ci:`).

## Review Focus

1. **iPhone Safari loads `m.youtube.com`, not `www`.** YouTube rules must match both hosts and include the mobile `ytm-*` selectors. Pinned in Task 8.
2. **Shorts links from share sheets carry query strings** (`?feature=share`, `&t=10`). The redirect must still keep the right 11-character video ID. Pinned in Tasks 4 and 8.
3. **A reel a friend sends in DMs (`/reel/<code>/`) must still open, and so must search.** Only the Reels tab feed (`/reels…`) and the bare Explore grid redirect. The grid goes to `/explore/search/`, and `/direct/` stays reachable. Pinned in Task 8.
4. **Tapping a Short inside the SPA changes the URL without a page load.** It must still redirect. Pinned in Task 7.
5. **One stale or invalid selector must not disable the other hide rules.** Each rule is its own CSS block, and comma lists are rejected. Pinned in Tasks 2 and 5.

---

## File Structure

```
package.json, tsconfig.json, vitest.config.ts
core/types.ts                 RuleSet types shared by every module
core/validate.ts              validateRuleSet(): schema + semantic checks
core/load.ts                  loadRuleSets(): read rules/*.json, validate, name check
core/cli/validate-rules.ts    `npm run validate`
core/fixtures/valid-full.json fixture covering every field
rules/schema/ruleset.schema.json
rules/youtube.json, rules/instagram.json, rules/rules.test.ts
web/engine/redirect.ts        resolveRedirect()
web/engine/hide.ts            buildHideCss()
web/engine/watch.ts           watchUrl()
web/runtime.ts                run(): wires the engine into a page
web/build/header.ts           buildHeader()
web/build/build-userscripts.ts buildUserscripts()
web/build/cli.ts              `npm run build`
.github/workflows/ci.yml, .github/workflows/release.yml
web/README.md                 install guide (iOS, desktop, Android Firefox)
```

---

### Task 1: Toolchain, types and schema validation

**Files:**
- Create: `package.json`, `tsconfig.json`, `vitest.config.ts`, `core/types.ts`, `rules/schema/ruleset.schema.json`, `core/validate.ts`, `core/fixtures/valid-full.json`
- Test: `core/validate.test.ts`

**Interfaces:**
- Produces, in `core/types.ts`:
  ```ts
  export interface RedirectRule { id: string; description: string; from: string; to: string }
  export interface HideRule { id: string; description: string; selector: string }
  export interface WebTarget { matches: string[]; redirects: RedirectRule[]; hide: HideRule[] }
  export interface AndroidRule { id: string; description: string;
    when: { viewId?: string; contentDescription?: string }; action: "back" | "home" | "overlay" }
  export interface AndroidTarget { package: string; testedVersion: string; rules: AndroidRule[] }
  export interface RuleSet { schemaVersion: 1; id: string; name: string; description: string;
    version: string; web?: WebTarget; android?: AndroidTarget }
  export type ValidationResult = { ok: true; ruleSet: RuleSet } | { ok: false; errors: string[] }
  ```
- Produces: `validateRuleSet(data: unknown): ValidationResult` in `core/validate.ts`.

- [x] **Step 1: Set up the toolchain**

`package.json`: name `unscroll`, version `0.1.0`, `"private": true`, `"type": "module"`, license `GPL-3.0-or-later`, `repository.url` set to `https://github.com/Luca-Fregona/unscroll`, `engines.node` `>=22`. Scripts:
`"test": "vitest run"`, `"typecheck": "tsc --noEmit"`, `"validate": "tsx core/cli/validate-rules.ts rules"`, `"build": "tsx web/build/cli.ts"`.

Run: `npm i -D typescript@5 vitest@5 jsdom ajv@8 css-what@8 esbuild tsx @types/node`

`tsconfig.json`: `target ES2022`, `module ESNext`, `moduleResolution Bundler`, `lib ["ES2022","DOM","DOM.Iterable"]`, `strict`, `noEmit`, `resolveJsonModule`, `esModuleInterop`, `skipLibCheck`, `types ["node"]`.
`vitest.config.ts`: include `**/*.test.ts`, exclude `node_modules/**`, `dist/**`, `android/**`.

- [x] **Step 2: Create the fixture `core/fixtures/valid-full.json`**

```json
{
  "schemaVersion": 1, "id": "example", "name": "Example",
  "description": "Fixture covering every field.", "version": "2026.11.03",
  "web": {
    "matches": ["*://www.example.com/*", "*://*.example.org/*"],
    "redirects": [{ "id": "feed-to-home", "description": "Feed goes home", "from": "^/feed(/|$)", "to": "/" }],
    "hide": [{ "id": "feed-link", "description": "Feed nav link", "selector": "a[href=\"/feed/\"]" }]
  },
  "android": {
    "package": "com.example.app", "testedVersion": "1.2.3",
    "rules": [{ "id": "feed-screen", "description": "Feed screen",
                "when": { "viewId": "com.example.app:id/feed_pager" }, "action": "back" }]
  }
}
```

- [x] **Step 3: Write the failing tests**

```ts
import { describe, it, expect } from "vitest";
import { validateRuleSet } from "./validate";
import validFull from "./fixtures/valid-full.json";
const clone = (): any => structuredClone(validFull);

describe("validateRuleSet — schema", () => {
  it("accepts the full fixture", () =>
    expect(validateRuleSet(clone())).toEqual({ ok: true, ruleSet: validFull }));
  it("requires web or android", () => {
    const d = clone(); delete d.web; delete d.android;
    expect(validateRuleSet(d).ok).toBe(false);
  });
  it("rejects ids that are not kebab-case", () => {
    const d = clone(); d.id = "You Tube"; expect(validateRuleSet(d).ok).toBe(false);
  });
  it("rejects cross-origin redirect targets", () => {
    const d = clone(); d.web.redirects[0].to = "https://evil.example/";
    expect(validateRuleSet(d).ok).toBe(false);
  });
  it("rejects a malformed version", () => {
    const d = clone(); d.version = "3 Nov 2026"; expect(validateRuleSet(d).ok).toBe(false);
  });
  it("rejects android rules with an empty when", () => {
    const d = clone(); d.android.rules[0].when = {}; expect(validateRuleSet(d).ok).toBe(false);
  });
});
```

- [x] **Step 4: Run the tests and confirm they fail**

Run: `npx vitest run core/validate.test.ts`. Expected: FAIL, because `./validate` cannot be resolved.

- [x] **Step 5: Write the schema and implement `validateRuleSet`**

The schema uses draft 2020-12 with `additionalProperties: false` on every object. The top level also allows an optional string `$schema`. Values from the spec:
- `schemaVersion`: `const: 1`
- `id`: pattern `^[a-z0-9-]+$`, also used for every rule `id`
- `version`: pattern `^\d{4}\.\d{2}\.\d{2}$`
- `name`, `description`, and each rule `description`: non-empty strings
- `anyOf: [{ "required": ["web"] }, { "required": ["android"] }]`
- `web` requires `matches` (min 1 item, pattern `^(\*|https?)://(\*|(\*\.)?[a-z0-9.-]+)/.*$`), plus `redirects` and `hide` as arrays, which may be empty
- redirect `to`: pattern `^/`
- `android.package`: pattern `^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z][a-zA-Z0-9_]*)+$`
- `testedVersion`: a string, which may be empty
- `action`: enum `back`, `home`, `overlay`
- `when`: `minProperties: 1`, with optional non-empty strings `viewId` and `contentDescription`

`validateRuleSet` compiles the schema once with `new Ajv2020({ allErrors: true })`, imported from `ajv/dist/2020.js`. It maps each Ajv error to `` `${instancePath || "/"} ${message}` ``.

- [x] **Step 6: Run the tests and typecheck**

Run: `npx vitest run core/validate.test.ts && npm run typecheck`. Expected: 6 passed, no type errors.

- [x] **Step 7: Commit**

```bash
git add package.json package-lock.json tsconfig.json vitest.config.ts core rules/schema
git commit -m "feat: rule set types, JSON schema and schema validation"
```

---

### Task 2: Semantic checks

**Files:**
- Modify: `core/validate.ts`
- Test: `core/validate.test.ts`

**Interfaces:**
- Consumes: `validateRuleSet` from Task 1. The signature does not change. The checks below run **only when schema validation passed**, and every failed check adds one error.

- [x] **Step 1: Write the failing tests** (append to `core/validate.test.ts`)

```ts
describe("validateRuleSet — semantics", () => {
  const fail = (errors: string[]) => ({ ok: false, errors });
  it("rejects duplicate rule ids across sections", () => {
    const d = clone(); d.web.hide[0].id = "feed-to-home";
    expect(validateRuleSet(d)).toEqual(fail(['duplicate rule id "feed-to-home"']));
  });
  it("rejects an invalid redirect regex", () => {
    const d = clone(); d.web.redirects[0].from = "^/feed(";
    expect(validateRuleSet(d)).toEqual(fail(['redirect "feed-to-home": invalid regex']));
  });
  it("rejects a redirect whose target matches its own pattern", () => {
    const d = clone(); d.web.redirects[0].from = "^/";
    expect(validateRuleSet(d)).toEqual(fail(['redirect "feed-to-home": target matches its own pattern (loop)']));
  });
  it("rejects an invalid CSS selector", () => {
    const d = clone(); d.web.hide[0].selector = "a[href=";
    expect(validateRuleSet(d)).toEqual(fail(['hide "feed-link": invalid selector']));
  });
  it("rejects a comma-separated selector list", () => {
    const d = clone(); d.web.hide[0].selector = "a, b";
    expect(validateRuleSet(d)).toEqual(fail(['hide "feed-link": use one selector per rule']));
  });
});
```

- [x] **Step 2: Run the tests and confirm they fail**

Run: `npx vitest run core/validate.test.ts`. Expected: the 5 new tests FAIL, because each result comes back `ok: true`.

- [x] **Step 3: Implement the checks in `core/validate.ts`**

- **Ids:** collect ids from `web.redirects`, `web.hide` and `android.rules`, and report each repeated id once.
- **Regex:** `new RegExp(from)` inside a try/catch.
- **Loop:** `new RegExp(from).test(new URL(to, "https://unscroll.invalid").pathname)`. Skip this check when the regex is invalid.
- **Selectors:** `parse(selector)` from `css-what`. If it throws, the selector is invalid. If `.length !== 1`, the selector is a comma list.

- [x] **Step 4: Run the tests**

Run: `npx vitest run core/validate.test.ts`. Expected: 11 passed.

- [x] **Step 5: Commit**

`git commit -am "feat: semantic rule checks (ids, regex, loops, selectors)"`

---

### Task 3: Loading rule files and `npm run validate`

**Files:**
- Create: `core/load.ts`, `core/cli/validate-rules.ts`
- Test: `core/load.test.ts`

**Interfaces:**
- Consumes: `validateRuleSet`, `RuleSet`
- Produces: `loadRuleSets(dir: string): Promise<RuleSet[]>`, sorted by `id`. It reads only `*.json` directly inside `dir` and never recurses. On any failure it throws `class RuleSetLoadError extends Error { errors: string[] }`, and every message starts with `<file name>: `.

- [x] **Step 1: Write the failing tests**

Use `fs.mkdtemp(path.join(os.tmpdir(), "unscroll-"))` for each test, plus a helper `rs(id)` that returns the fixture with that `id`.

```ts
it("loads valid files sorted by id and ignores subfolders", async () => {
  // write b.json = rs("b"), a.json = rs("a"), schema/junk.json = "not json"
  expect((await loadRuleSets(dir)).map((r) => r.id)).toEqual(["a", "b"]);
});
it("reports an id that does not match the file name", async () => {
  // write b.json = rs("a")
  await expect(loadRuleSets(dir)).rejects.toMatchObject({
    errors: ['b.json: id "a" does not match file name'] });
});
it("reports invalid JSON", async () => {
  // write bad.json = "{"
  await expect(loadRuleSets(dir)).rejects.toMatchObject({ errors: ["bad.json: invalid JSON"] });
});
it("prefixes validation errors with the file name", async () => {
  // write x.json = { ...rs("x"), version: "nope" }
  await expect(loadRuleSets(dir)).rejects.toMatchObject({
    errors: [expect.stringMatching(/^x\.json: \/version /)] });
});
```

- [x] **Step 2: Run the tests and confirm they fail**

Run: `npx vitest run core/load.test.ts`. Expected: FAIL, because the module is not found.

- [x] **Step 3: Implement `loadRuleSets`, `RuleSetLoadError` and the CLI**

The CLI takes the directory from `process.argv[2]`. On success it prints `✓ <n> rule sets valid` and exits 0. On a `RuleSetLoadError` it prints `✗ <error>` per line and exits 1.

- [x] **Step 4: Run the tests and the CLI**

Run: `npx vitest run core/load.test.ts && npm run validate`. Expected: 4 passed, then `✓ 0 rule sets valid`.

- [x] **Step 5: Commit**

`git add core && git commit -m "feat: load and validate rules directory"`

---

### Task 4: `resolveRedirect`

**Files:**
- Create: `web/engine/redirect.ts`
- Test: `web/engine/redirect.test.ts`

**Interfaces:**
- Consumes: `RedirectRule`
- Produces: `resolveRedirect(rules: RedirectRule[], href: string): string | null`

- [x] **Step 1: Write the failing tests**

```ts
const rules: RedirectRule[] = [
  { id: "shorts", description: "s", from: "^/shorts/([A-Za-z0-9_-]{11})", to: "/watch?v=$1" },
  { id: "reels", description: "r", from: "^/reels(/|$)", to: "/" },
  { id: "self", description: "x", from: "^/$", to: "/" },
];
const go = (href: string) => resolveRedirect(rules, href);
it("sends a Short to the normal player", () =>
  expect(go("https://www.youtube.com/shorts/dQw4w9WgXcQ")).toBe("https://www.youtube.com/watch?v=dQw4w9WgXcQ"));
it("keeps the video id and drops share query strings", () =>
  expect(go("https://m.youtube.com/shorts/dQw4w9WgXcQ?feature=share&t=10")).toBe("https://m.youtube.com/watch?v=dQw4w9WgXcQ"));
it("redirects the Reels feed but not a single reel", () => {
  expect(go("https://www.instagram.com/reels/C1a2B3c4D5e/")).toBe("https://www.instagram.com/");
  expect(go("https://www.instagram.com/reel/C1a2B3c4D5e/")).toBeNull();
});
it("returns null when nothing matches", () => expect(go("https://www.youtube.com/watch?v=dQw4w9WgXcQ")).toBeNull());
it("never redirects a page to itself", () => expect(go("https://www.instagram.com/")).toBeNull());
```

- [x] **Step 2: Run the tests and confirm they fail**

Run: `npx vitest run web/engine/redirect.test.ts`. Expected: FAIL, because the module is not found.

- [x] **Step 3: Implement it**

The first rule whose `from` matches `url.pathname` decides, and no later rules are tried. The target path is `rule.to.replace(/\$([1-9])/g, (_, i) => match[Number(i)] ?? "")`. Do **not** use `pathname.replace`, because that would keep any trailing path. The result is `new URL(target, url.origin).href`, or `null` if that equals `href`.

- [x] **Step 4: Run the tests**

Expected: 5 passed.

- [x] **Step 5: Commit**

`git add web/engine && git commit -m "feat: redirect resolution"`

---

### Task 5: `buildHideCss`

**Files:**
- Create: `web/engine/hide.ts`
- Test: `web/engine/hide.test.ts`

**Interfaces:**
- Produces: `buildHideCss(rules: HideRule[]): string`

- [x] **Step 1: Write the failing tests**

```ts
it("returns an empty string for no rules", () => expect(buildHideCss([])).toBe(""));
it("emits one block per rule so a bad selector only drops itself", () =>
  expect(buildHideCss([
    { id: "a", description: "a", selector: "ytd-reel-shelf-renderer" },
    { id: "b", description: "b", selector: 'a[href="/reels/"]' },
  ])).toBe('ytd-reel-shelf-renderer { display: none !important; }\na[href="/reels/"] { display: none !important; }'));
```

- [x] **Step 2: Run the tests and confirm they fail.** Then **Step 3:** implement it. Then **Step 4:** run them and expect 2 passed. Then **Step 5:** commit with `feat: hide-css builder`.

---

### Task 6: `watchUrl`

**Files:**
- Create: `web/engine/watch.ts`
- Test: `web/engine/watch.test.ts` (jsdom)

**Interfaces:**
- Produces: `watchUrl(win: Window, onChange: (href: string) => void, intervalMs = 300): () => void`. It calls `onChange` only when `win.location.href` differs from the last value it saw. It checks on a `win.setInterval` tick and on `popstate`. The returned function stops both.

- [x] **Step 1: Write the failing tests**

```ts
// @vitest-environment jsdom
beforeEach(() => history.replaceState({}, "", "/"));
afterEach(() => vi.useRealTimers());
it("reports a pushState navigation once, and nothing after stop", () => {
  vi.useFakeTimers();
  const seen: string[] = [];
  const stop = watchUrl(window, (h) => seen.push(h));
  history.pushState({}, "", "/shorts/dQw4w9WgXcQ");
  vi.advanceTimersByTime(600);
  expect(seen).toEqual([`${location.origin}/shorts/dQw4w9WgXcQ`]);
  stop(); history.pushState({}, "", "/other"); vi.advanceTimersByTime(600);
  expect(seen).toHaveLength(1);
});
it("reports popstate without waiting for the timer", () => {
  const seen: string[] = [];
  const stop = watchUrl(window, (h) => seen.push(h));
  history.pushState({}, "", "/a"); window.dispatchEvent(new PopStateEvent("popstate"));
  expect(seen).toEqual([`${location.origin}/a`]); stop();
});
```

- [x] **Step 2: Run the tests and confirm they fail.** Then **Step 3:** implement it. Then **Step 4:** run them and expect 2 passed. Then **Step 5:** commit with `feat: SPA url watcher`.

---

### Task 7: Page runtime `run`

**Files:**
- Create: `web/runtime.ts`
- Test: `web/runtime.test.ts` (jsdom)

**Interfaces:**
- Consumes: `resolveRedirect` (Task 4), `buildHideCss` (Task 5), `watchUrl` (Task 6), `RuleSet`
- Produces:
  ```ts
  export interface RuntimeEnv { win: Window; navigate: (href: string) => void; intervalMs?: number }
  export function run(ruleSet: RuleSet, env: RuntimeEnv): () => void
  ```
  When there is no `web` target, it returns a no-op. When the current URL redirects, it calls `navigate(target)` once, injects nothing, and returns a no-op. Otherwise it injects `<style id="unscroll-<id>">` into `document.head ?? document.documentElement`, waiting with a `MutationObserver` when neither exists yet (Chromium at document-start), then watches the URL and calls `navigate` whenever a navigation resolves to a redirect. The returned function stops the watcher.

- [x] **Step 1: Write the failing tests**

```ts
// @vitest-environment jsdom
const ruleSet: RuleSet = { schemaVersion: 1, id: "t", name: "T", description: "d", version: "2026.11.03",
  web: { matches: ["*://localhost/*"],
    redirects: [{ id: "shorts", description: "s", from: "^/shorts/([A-Za-z0-9_-]{11})", to: "/watch?v=$1" }],
    hide: [{ id: "shelf", description: "h", selector: ".shorts-shelf" }] } };
const target = () => `${location.origin}/watch?v=dQw4w9WgXcQ`;
beforeEach(() => {
  history.replaceState({}, "", "/");
  document.getElementById("unscroll-t")?.remove();
  if (!document.head) document.documentElement.prepend(document.createElement("head"));
});
afterEach(() => vi.useRealTimers());
it("injects one style element with the hide rules", () => {
  const stop = run(ruleSet, { win: window, navigate: vi.fn() });
  expect(document.getElementById("unscroll-t")?.textContent).toBe(".shorts-shelf { display: none !important; }");
  stop();
});
it("redirects on load without injecting styles", () => {
  history.replaceState({}, "", "/shorts/dQw4w9WgXcQ");
  const navigate = vi.fn(); run(ruleSet, { win: window, navigate });
  expect(navigate).toHaveBeenCalledExactlyOnceWith(target());
  expect(document.getElementById("unscroll-t")).toBeNull();
});
it("redirects after an in-page navigation", () => {
  vi.useFakeTimers(); const navigate = vi.fn();
  const stop = run(ruleSet, { win: window, navigate });
  history.pushState({}, "", "/shorts/dQw4w9WgXcQ"); vi.advanceTimersByTime(300);
  expect(navigate).toHaveBeenCalledExactlyOnceWith(target()); stop();
});
it("falls back to <html> when <head> does not exist yet", () => {
  document.head.remove();
  const stop = run(ruleSet, { win: window, navigate: vi.fn() });
  expect(document.getElementById("unscroll-t")?.parentElement).toBe(document.documentElement); stop();
});
```

- [x] **Step 2: Run the tests and confirm they fail.** Then **Step 3:** implement it. Then **Step 4:** run them and expect 4 passed. Then **Step 5:** commit with `feat: page runtime`.

---

### Task 8: YouTube and Instagram rule sets

**Files:**
- Create: `rules/youtube.json`, `rules/instagram.json`
- Test: `rules/rules.test.ts`

**Interfaces:**
- Consumes: `loadRuleSets` (Task 3), `resolveRedirect` (Task 4)

- [x] **Step 1: Write the failing regression tests**

```ts
const sets = await loadRuleSets(fileURLToPath(new URL(".", import.meta.url)));
const byId = Object.fromEntries(sets.map((s) => [s.id, s]));
const go = (id: string, href: string) => resolveRedirect(byId[id].web!.redirects, href);
it("YouTube covers desktop and mobile hosts", () =>
  expect(byId.youtube.web!.matches).toEqual(expect.arrayContaining(["*://www.youtube.com/*", "*://m.youtube.com/*"])));
it("YouTube Shorts open in the player with the right id", () =>
  expect(go("youtube", "https://m.youtube.com/shorts/dQw4w9WgXcQ?feature=share")).toBe("https://m.youtube.com/watch?v=dQw4w9WgXcQ"));
it("YouTube hides mobile Shorts surfaces", () =>
  expect(byId.youtube.web!.hide.some((r) => r.selector.startsWith("ytm-"))).toBe(true));
it("Instagram Reels feed goes home and the Explore grid goes to search", () => {
  expect(go("instagram", "https://www.instagram.com/reels/C1a2B3c4D5e/")).toBe("https://www.instagram.com/");
  expect(go("instagram", "https://www.instagram.com/explore/")).toBe("https://www.instagram.com/explore/search/");
});
it("Instagram keeps the Explore link, which is the search entry on mobile", () =>
  expect(byId.instagram.web!.hide.map((r) => r.selector)).not.toContain('a[href="/explore/"]'));
it("Instagram keeps single reels, Explore search and DMs reachable", () => {
  for (const p of ["/reel/C1a2B3c4D5e/", "/explore/search/", "/direct/inbox/"])
    expect(go("instagram", `https://www.instagram.com${p}`)).toBeNull();
});
```

- [x] **Step 2: Run the tests and confirm they fail**

Run: `npx vitest run rules/rules.test.ts`. Expected: FAIL, because `byId.youtube` is undefined.

- [x] **Step 3: Write the two rule sets**

Both have `"$schema": "./schema/ruleset.schema.json"`, `schemaVersion: 1` and `version: "2026.11.03"`. They have **no `android` block yet**, because Phase 2 adds it. Copy the descriptions from spec §5.1.

**youtube.json**
- `matches`: `*://www.youtube.com/*`, `*://m.youtube.com/*`
- Redirect `shorts-to-watch`: `^/shorts/([A-Za-z0-9_-]{11})` → `/watch?v=$1`
- Hide rules. These are **starting values**, to be checked on devices in Task 9:

| id | selector |
|---|---|
| shorts-shelf-home | `ytd-rich-shelf-renderer[is-shorts]` |
| shorts-shelf | `ytd-reel-shelf-renderer` |
| shorts-grid-shelf | `grid-shelf-view-model:has(a[href^="/shorts/"])` |
| shorts-item-home | `ytd-rich-item-renderer:has(a[href^="/shorts/"])` |
| shorts-item-search | `ytd-video-renderer:has(a[href^="/shorts/"])` |
| shorts-nav-guide | `ytd-guide-entry-renderer:has(a[title="Shorts"])` |
| shorts-nav-mini-guide | `ytd-mini-guide-entry-renderer[aria-label="Shorts"]` |
| m-shorts-shelf | `ytm-reel-shelf-renderer` |
| m-shorts-lockup | `ytm-shorts-lockup-view-model` |
| m-shorts-lockup-v2 | `ytm-shorts-lockup-view-model-v2` |
| m-shorts-section | `ytm-rich-section-renderer:has(a[href^="/shorts/"])` |
| m-shorts-item | `ytm-video-with-context-renderer:has(a[href^="/shorts/"])` |
| m-shorts-nav | `ytm-pivot-bar-item-renderer:has(.pivot-shorts)` |

**instagram.json**
- `matches`: `*://www.instagram.com/*`
- Redirect `reels-feed`: `^/reels(/|$)` → `/`
- Redirect `explore-grid`: `^/explore/?$` → `/explore/search/`
- Hide `reels-nav-link`: `a[href="/reels/"]`. The Explore link stays visible, because on mobile it is the search entry.

- [x] **Step 4: Run the tests and the validator**

Run: `npx vitest run rules/rules.test.ts && npm run validate`. Expected: 6 passed, then `✓ 2 rule sets valid`.

- [x] **Step 5: Commit**

`git add rules && git commit -m "feat: YouTube and Instagram rule sets"`

---

### Task 9: Userscript build and real-device check

**Files:**
- Create: `web/build/header.ts`, `web/build/build-userscripts.ts`, `web/build/cli.ts`
- Test: `web/build/header.test.ts`, `web/build/build-userscripts.test.ts` (node environment)

**Interfaces:**
- Consumes: `loadRuleSets` (Task 3), `run` (Task 7)
- Produces:
  ```ts
  export interface HeaderMeta { id: string; name: string; description: string; matches: string[]; version: string; repoUrl: string }
  export function buildHeader(meta: HeaderMeta): string
  export interface BuildOptions { rulesDir: string; outDir: string; repoUrl: string; version: string }
  export function buildUserscripts(opts: BuildOptions): Promise<string[]> // written paths, sorted
  ```

- [x] **Step 1: Write the failing tests**

```ts
// header.test.ts
const h = buildHeader({ id: "youtube", name: "YouTube", description: "Hides Shorts.",
  matches: ["*://www.youtube.com/*", "*://m.youtube.com/*"], version: "0.1.0", repoUrl: "https://github.com/Luca-Fregona/unscroll" });
const lines = h.split("\n");
it("is a well-formed userscript block", () => {
  expect(lines[0]).toBe("// ==UserScript=="); expect(lines.at(-1)).toBe("// ==/UserScript==");
  expect(h).toMatch(/^\/\/ @name\s+unscroll — YouTube$/m);
  expect(h).toMatch(/^\/\/ @version\s+0\.1\.0$/m);
  expect(h.match(/^\/\/ @match\s+/gm)).toHaveLength(2);
  expect(h).toMatch(/^\/\/ @run-at\s+document-start$/m);
  expect(h).toMatch(/^\/\/ @grant\s+none$/m);
  expect(h).toMatch(/^\/\/ @updateURL\s+https:\/\/github\.com\/Luca-Fregona\/unscroll\/releases\/latest\/download\/unscroll-youtube\.user\.js$/m);
  expect(h).toMatch(/^\/\/ @license\s+GPL-3\.0-or-later$/m);
});

// build-userscripts.test.ts — temp rulesDir holding example.json (fixture)
// and android-only.json (fixture with id "android-only" and no web block)
it("writes one parseable, offline userscript per web rule set", async () => {
  const out = await buildUserscripts({ rulesDir, outDir, repoUrl: "https://github.com/Luca-Fregona/unscroll", version: "0.1.0" });
  expect(out).toEqual([path.join(outDir, "unscroll-example.user.js")]);
  const code = await fs.readFile(out[0], "utf8");
  expect(code.startsWith("// ==UserScript==")).toBe(true);
  expect(code).toContain("feed-to-home");
  expect(() => new Function(code)).not.toThrow();
  expect(code).not.toMatch(/fetch\(|XMLHttpRequest|sendBeacon|localStorage|indexedDB/);
});
```

- [x] **Step 2: Run the tests and confirm they fail**

Run: `npx vitest run web/build`. Expected: FAIL, because the modules are not found.

- [x] **Step 3: Implement the header, the build and the CLI**

**Header lines,** in this order, each written as `` `// ${("@" + key).padEnd(13)}${value}` ``:
`name` (`unscroll — <name>`), `namespace` (repoUrl), `version`, `description`, `match` (one line per pattern), `run-at document-start`, `grant none`, `updateURL` and `downloadURL` (both `<repoUrl>/releases/latest/download/unscroll-<id>.user.js`), `license GPL-3.0-or-later`.

**Build:** for each rule set with `web`, call esbuild `build()` with:
- `stdin.contents`: `` `import { run } from "./web/runtime"; run(${JSON.stringify(rs)}, { win: window, navigate: (h) => window.location.replace(h) });` ``
- `stdin.resolveDir`: the repo root, `fileURLToPath(new URL("../..", import.meta.url))`
- `loader: "ts"`, `bundle: true`, `format: "iife"`, `target: "es2020"`, `write: false`, `banner.js`: the header

Write the result to `outDir`.

**CLI:** reads `version` and `repository.url` from `package.json`, builds `rules` into `dist`, and prints `built <n> userscripts` followed by the paths.

- [x] **Step 4: Run the tests and the build**

Run: `npx vitest run web/build && npm run build`. Expected: 2 passed, then `built 2 userscripts`.

- [ ] **Step 5: Check on real devices** (human in the loop, about 20 minutes)

- **Desktop:** install both `dist/*.user.js` files in Violentmonkey. On youtube.com, check that the home page and search show no Shorts shelves or nav entry, and that opening a `/shorts/<id>` link lands in `/watch`. Repeat in responsive mode on m.youtube.com. On instagram.com, check that the Reels and Explore links are gone, that `/reels/` lands on `/`, and that a `/reel/<code>/` link from DMs still opens.
- **iPhone:** install "Userscripts" from the App Store and enable it in Safari for youtube.com and instagram.com. Put both files in its folder in the Files app and repeat the checks.
- For each selector that matched nothing, update it in the rule file and bump `version`. Re-run `npm run validate && npm test`.

- [x] **Step 6: Commit**

`git add web/build rules && git commit -m "feat: userscript build"`

---

### Task 10: CI, releases and install guide

**Files:**
- Create: `.github/workflows/ci.yml`, `.github/workflows/release.yml`
- Modify: `web/README.md`, `README.md` (Install section)

- [ ] **Step 1: Write `ci.yml`**

It runs on push to `main` and on pull requests, using `ubuntu-latest`, `actions/checkout@v5`, and `actions/setup-node@v5` with `node-version: 22` and `cache: npm`. Bump these if newer majors exist. Steps: `npm ci`, `npm run typecheck`, `npm run validate`, `npm test`, `npm run build`.

- [ ] **Step 2: Write `release.yml`**

It runs on tags `v*` with `permissions: contents: write`. Its steps are the same as CI, plus:
- a version guard: `test "v$(node -p "require('./package.json').version")" = "$GITHUB_REF_NAME"`
- `gh release create "$GITHUB_REF_NAME" dist/*.user.js --generate-notes`, with `GH_TOKEN: ${{ github.token }}`

- [ ] **Step 3: Write the install guide in `web/README.md`**

- **iPhone/iPad:** install Userscripts from the App Store, enable it in Settings → Apps → Safari → Extensions, and allow it on youtube.com and instagram.com. Open the `releases/latest/download/unscroll-<id>.user.js` link in Safari and confirm the install. Optional tip: remove the Instagram app or put it behind a Screen Time limit, and use instagram.com instead.
- **Desktop:** Violentmonkey or Tampermonkey, then open the same links.
- **Android:** Firefox with Violentmonkey works for the websites. The official apps are covered in Phase 2.
- **Turning it off:** disable the script in the manager.

Link the guide from the README Install section.

- [ ] **Step 4: Verify**

Run: `npx --yes @action-validator/cli .github/workflows/ci.yml .github/workflows/release.yml` (or push to a branch and watch Actions). Then run `npm run typecheck && npm run validate && npm test && npm run build`. Expected: all green.

- [ ] **Step 5: Commit, then release (maintainer)**

`git add .github web/README.md README.md && git commit -m "ci: validation, tests and tagged releases"`

The maintainer then runs `git tag v0.1.0 && git push --tags`. Expected: a GitHub Release with `unscroll-instagram.user.js` and `unscroll-youtube.user.js`.
