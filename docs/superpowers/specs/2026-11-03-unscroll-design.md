# unscroll — Design

**Date:** 2026-11-03
**Status:** Draft, waiting for the maintainer's review
**Author:** Luca Fregona

## 1. Intent

Short-video feeds (Instagram Reels, YouTube Shorts, TikTok's For You) are built to keep people scrolling. Paid apps already hide them, but each one is closed, tied to an app store, and often sends telemetry.

unscroll is an open-source project that removes those feeds and leaves the rest of each app usable: messages, posts, search, subscriptions. People install and run it themselves. It does not depend on an app store, needs no account, and collects no data.

The core idea is a **shared, community-maintained set of rules**. The rules describe where the short-video feeds are, and small clients apply them. When Instagram or YouTube changes its interface, someone fixes one rule and every client benefits. The model is the same one uBlock Origin's filter lists follow.

## 2. Goals, non-goals, anti-goals

**Goals**
- Remove short-video feeds while keeping the rest of each service usable.
- Run without any app store: people install, update and fork it themselves.
- Keep the rules as plain data that is easy to review, so a fix for a broken rule can ship within days.
- Provide one rules format that both the web client and the Android client read.

**Non-goals (for now)**
- Blocking whole apps. Screen Time and Digital Wellbeing already do that.
- Usage tracking, statistics, streaks or gamification. Projects like DigiPaws already cover that.
- Ad blocking.
- Parental controls or monitoring other people.

**Anti-goals: the project fails if any of these happen, even if it technically works**
- It collects, sends or stores any personal data. That means no telemetry, analytics or network calls beyond fetching rules.
- It traps the user. Turning unscroll off must always be easy. Any added friction, such as a future strict mode, is opt-in.
- It breaks messaging. Instagram DMs and links friends send must keep working.
- It modifies or repackages official apps. Doing so risks Terms of Service violations and account bans.

## 3. Platform constraints (researched 2026-11-03)

- **Android.** An `AccessibilityService` can read other apps' UI trees and perform the global Back and Home actions. Every open-source Reels and Shorts blocker works this way. Detection relies on resource-ids and content-descriptions, which change when the target app updates.
- **Android, installing outside a store.** On Android 13 and later, the user must allow "restricted settings" before enabling the accessibility service of a sideloaded APK. Google's developer verification has been enforced since 2026-09-30 in BR, ID, SG and TH, with a global rollout planned for 2027. Unregistered apps will then need the "advanced flow" or ADB.
- **iOS.** No API lets an app look inside another app. Screen Time (FamilyControls, ManagedSettings, DeviceActivity) works only on whole apps, categories or web domains. It cannot block `instagram.com/reels` while allowing `instagram.com/direct`. Using FamilyControls outside development requires Apple's approval.
- **iOS, installing outside the App Store.** Apps signed with a free Apple ID expire every 7 days, and at most 3 can be active. That is unusable for ordinary users.
- **iOS, the workable path.** Safari plus the free *Userscripts* extension can run userscripts and CSS loaded from a URL, and updates arrive automatically. Instagram and YouTube both work fully in mobile Safari for browsing and DMs.

## 4. Approaches considered

| | Approach | Trade-offs |
|---|---|---|
| A | A native app per platform (Android accessibility app plus an iOS app using FamilyControls) | iOS cannot block parts of an app at all, and the iOS app would need sideloading or the App Store plus Apple's approval. This is two full codebases with no shared data. |
| **B** | **Shared rules plus thin clients:** userscripts for Safari on iOS and desktop browsers, and an Android accessibility app, all reading one rules format | Works within each platform's real limits, needs no store, and one rule fix reaches every client. On iOS, users browse the website instead of the app. |
| C | Patch the official apps (ReVanced style) | Violates the ToS, puts user accounts at risk, needs a heavy rebuild every time the app updates, and breaks anti-goal 4. |

**Chosen: B.** Luca approved this direction in the conversation ("ottimo, prepara il repo").

## 5. Architecture

```
rules/*.json  ──►  web client (userscripts)   iOS Safari, desktop, Firefox Android
      │
      └────────►  Android client (Kotlin)     official Instagram and YouTube apps
```

### 5.1 The rules format (`schemaVersion: 1`)

There is one file per service: `rules/<id>.json`, and the file name must equal `id`. A JSON Schema lives at `rules/schema/ruleset.schema.json`.

```json
{
  "$schema": "./schema/ruleset.schema.json",
  "schemaVersion": 1,
  "id": "youtube",
  "name": "YouTube",
  "description": "Hides YouTube Shorts and opens Shorts links in the normal player.",
  "version": "2026.11.03",
  "web": {
    "matches": ["*://www.youtube.com/*", "*://m.youtube.com/*"],
    "redirects": [
      { "id": "shorts-to-watch", "description": "Open a Short in the normal player",
        "from": "^/shorts/([A-Za-z0-9_-]{11})", "to": "/watch?v=$1" }
    ],
    "hide": [
      { "id": "shorts-shelf-desktop", "description": "Shorts shelf on the home page",
        "selector": "ytd-rich-shelf-renderer[is-shorts]" }
    ]
  },
  "android": {
    "package": "com.google.android.youtube",
    "testedVersion": "",
    "rules": []
  }
}
```

Field rules:
- `id` matches `^[a-z0-9-]+$`. Rule `id`s are unique across the whole file.
- `version` is the date of the last data change, formatted `YYYY.MM.DD`.
- A file must contain at least one of `web` and `android`.
- `web.matches` follow userscript `@match` syntax: `<scheme>://<host>/<path>`, where the scheme is `*`, `http` or `https`, the host may start with `*.`, and the path may contain `*`.
- `web.redirects[].from` is a JavaScript regex tested against **`location.pathname` only**. `to` is a same-origin path that must start with `/`. `$1`…`$9` insert capture groups, and the original query string is dropped. A rule whose `to` path matches its own `from` is rejected as a loop.
- `web.hide[].selector` is a single CSS selector, never a comma-separated list. Each one becomes its own CSS rule, so one broken selector cannot disable the others.
- `android.rules[]` have the shape `{ id, description, when: { viewId?, contentDescription?, selected? }, action: "back" | "home" | "overlay" }`, and `when` needs at least one key. Every key present must hold on the same node, and a rule matches when any node of the window satisfies it. `selected` can only be `true`, and it needs a `viewId` or `contentDescription` next to it. It exists because bottom-bar tab buttons are on every screen of an app, so "this tab is the selected one" is the only language-proof way to tell which tab is showing. Prefer `viewId`, since `contentDescription` is translated. Phase 1 defines the format. Phase 2 adds the real Android data after dumping the apps' UI trees.

**What gets blocked in v0.1**
- YouTube: Shorts links redirect to the normal player, and Shorts shelves, cards and navigation entries are hidden on both desktop (`www`) and mobile (`m.`) layouts.
- Instagram: the Reels tab feed (`/reels`, `/reels/<code>/`) redirects to `/`, and its navigation link is hidden. The Explore grid (exactly `/explore/`) redirects to the search page `/explore/search/`, and its link stays visible because on mobile it is the search entry. A single reel a friend sends (`/reel/<code>/`) stays reachable.

### 5.2 Web client (Phase 1)

- The build produces one userscript per service, `dist/unscroll-<id>.user.js`. People pick which services to clean up by installing only those scripts. There is no settings UI in v0.1: to turn a service back on, disable its script.
- The engine is dependency-free TypeScript bundled into an IIFE with esbuild. It runs at `document-start`, injects one `<style>`, and applies redirects on load and on every in-page navigation. Redirects use `location.replace`, so the Back button does not bounce the user back into the feed. Navigation is detected by polling `location.href` plus listening for `popstate`, which works in any userscript manager's isolated world.
- Distribution: GitHub Releases. The `@updateURL` and `@downloadURL` values point to `<repo>/releases/latest/download/unscroll-<id>.user.js`, so the Userscripts app, Violentmonkey and Tampermonkey update automatically.
- The userscript `@version` equals the `package.json` version. Every release bumps it, and a rules-only change ships as a patch release.

### 5.3 Android client (Phase 2, its own plan)

- Kotlin, minSdk 26 (Android 8.0). The `AccessibilityService` is scoped with `packageNames` to the target apps only. It listens for window-state and window-content events and matches `when` conditions against the node tree. It acts with `performGlobalAction(BACK | HOME)` or a full-screen `TYPE_ACCESSIBILITY_OVERLAY`, with a cooldown so it cannot loop.
- The rules are bundled as assets at build time, copied from `../rules/`.
- There are two build flavors. `offline` has no INTERNET permission, and users can import a rules file by hand. `online` fetches rules from a base URL the user can change, which defaults to this repo. That makes forks first-class. The fetch layout is `<base>/<id>.json` for every rule set that is bundled, with no index file: a new service also needs a new `packageNames` entry and so a new release anyway. Only HTTPS is accepted (plain HTTP to the emulator's host in debug builds only), a file is at most 256 KiB, it must describe the id it was asked for and be for one of the target packages, and the newest `version` wins, with a fetched or imported copy winning a tie. Rules refresh at most every 24 hours, checked when the app opens and when the service connects, plus a manual button. The app stores only the disclosure flag, the rules address, the last refresh time and the rule files, and backups are off.
- An in-app disclosure explains exactly what the accessibility service reads and does, before the user enables it.
- Distribution: signed APKs on GitHub Releases, installed through Obtainium, with an F-Droid submission later. The maintainer registers for Android developer verification before the 2027 global rollout.

## 6. Phases

0. **Repo and design.** This document, the plan, and the scaffolding. *(done 2026-11-03)*
1. **Rules and web client.** Schema, validator, engine, YouTube and Instagram rules, CI, releases. → **v0.1**
2. **Android client.** Instagram Reels and YouTube Shorts in the official apps. → **v0.2**
3. **Coverage and maintenance flow.** TikTok (web block), Facebook Reels, X. A UI-dump-to-rule helper. A fast "broken rule" triage routine.
4. **Optional extras, if people ask for them.** An iOS Shortcuts guide (open the website instead of the app), an opt-in strict mode, and a native Safari-extension app for App Store users.

## 7. Calls made without the maintainer (please check)

1. **Name:** `unscroll`, provisional and easy to rename.
2. **Licenses:** code under GPL-3.0-or-later, like DigiPaws, AwayDoomscrollin' and Slowth. `rules/` under CC0-1.0, so any project, open or not, can reuse the rule data, the way filter lists are shared.
3. **Language:** English for code and docs, to reach international contributors.
4. **Single reels stay viewable.** `/reel/<code>/` links from DMs are not blocked. Only the endless feeds are.
5. **Stack:** Node 22 and TypeScript, with Vitest, Ajv, css-what and esbuild for Phase 1, and Kotlin for Phase 2.
6. **Repo URL:** `https://github.com/Luca-Fregona/unscroll`. It lives in one place, `package.json` → `repository.url`.
7. **Android application id:** `io.github.lucafregona.unscroll`. It is permanent once an APK is published.
8. **Android UI and libraries:** Jetpack Compose Material 3. Beyond AndroidX and Compose the app uses `kotlinx-serialization-json` and `kotlinx-coroutines-android` only. Release builds are minified with R8.
9. **Android rule condition `selected`** (§5.1): added in Phase 2 because tab buttons exist on every screen and the selected state is the only language-proof way to tell which tab is showing.
10. **Android and the "no network, storage or analytics" rule:** `CLAUDE.md` states it for the web client. On Android, §5.3 explicitly allows fetching rule files and importing them, so the app keeps those and its own few settings, and nothing else.
11. **Android reels from messages:** on the web a single reel from a DM stays viewable (call 4). In the Android app a reel opened in the same full-screen player as the feed cannot be told apart by a single-node rule, so it may be closed too. Whether Instagram offers a signal that separates them is open until its UI is dumped.

## 8. Left to the builder

Internal module layout beyond what the plan pins, error message wording beyond what tests pin, the exact CSS selectors (they are data and get verified on real devices), and CI caching details.

## 9. Prior art

DigiPaws (GPLv3, F-Droid), AwayDoomscrollin' (GPLv3, F-Droid), Shorts Blocker (Kotlin/Compose), MindGuard, and Tapback are Android accessibility blockers. Slowth (GPLv3) is a Safari web extension with optional FamilyControls. "No Shorts" is a userscript for iPhone Safari. All of them hard-code their selectors and IDs. unscroll's difference is the shared, forkable rules layer.
