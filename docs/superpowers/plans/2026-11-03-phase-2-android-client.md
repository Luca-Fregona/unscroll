# Phase 2: Android Client Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship the v0.2 Android client: an app whose accessibility service closes the Instagram Reels and YouTube Shorts screens in the official apps, driven by the `android` blocks of the shared rules and verified on the Android emulator.

**Architecture:** The `android` blocks of `rules/*.json` are bundled into the app as assets and parsed by a small Kotlin parser. A pure-Kotlin engine (matcher, cooldown gate, decision) decides what to do with an accessibility node tree. A thin `AccessibilityService`, scoped to the two target packages, adapts real nodes to that engine and performs Back, Home or a full-screen overlay. Two Gradle flavors share all code: `offline` has no INTERNET permission and imports rules from a file, `online` also refreshes rules from a user-configurable URL. Everything that does not touch the Android framework is covered by JVM unit tests. The service is verified by instrumented tests that drive the real apps on the emulator.

**Tech Stack:** Kotlin, Gradle (Kotlin DSL, version catalog), Android Gradle Plugin, Jetpack Compose Material 3, kotlinx-serialization-json, kotlinx-coroutines, JUnit 4, AndroidX Test + UiAutomator. Use the latest stable AGP, Kotlin, Gradle and library versions that support `compileSdk 36` at execution time, and record them in `android/gradle/libs.versions.toml`.

**Spec:** `docs/superpowers/specs/2026-11-03-unscroll-design.md` (§5.3 is this plan's scope, §5.1 defines the rule format)

## Global Constraints

- Kotlin, `minSdk 26`, `compileSdk 36`, `targetSdk 36`. Code is `GPL-3.0-or-later`, rule data stays `CC0-1.0` and lives only in `rules/`.
- The `AccessibilityService` is scoped with `packageNames` to `com.instagram.android` and `com.google.android.youtube` only. It listens to window-state and window-content events, matches `when` conditions against the node tree, and acts with `performGlobalAction(BACK | HOME)` or a full-screen `TYPE_ACCESSIBILITY_OVERLAY`, with a cooldown so it cannot loop.
- Rules are bundled as assets at build time, copied from `../rules/`. They are never edited under `android/`.
- Two flavors on one dimension: `offline` (no `INTERNET` permission, rules imported by hand) and `online` (fetches rules from a base URL the user can change, default is this repo).
- A disclosure of exactly what the service reads and does is shown before the user is sent to enable it.
- No telemetry, analytics or crash reporting. The `online` flavor's only network request is an HTTPS GET of a rule file. The app stores only: the disclosure flag, the rules base URL, the last-refresh time and the rule files it fetched or imported. `allowBackup` is `false`.
- Turning unscroll off must always be one tap away. DMs and links friends send must keep working.
- The repo URL comes only from `package.json` → `repository.url`. Gradle reads it from there to build the default rules URL.
- `versionName` equals the `package.json` version. `versionCode` is `major*10000 + minor*100 + patch`.
- Tests: JVM unit tests next to the code under `src/test*`, instrumented tests under `src/androidTest*`. `npm test` ignores `android/**`.
- Commit messages follow Conventional Commits. Never commit full UI dumps, because they contain the signed-in user's feed and messages.

## Calls made without the maintainer (please check)

1. **Application id:** `io.github.lucafregona.unscroll`. It is permanent once an APK is published.
2. **UI:** Jetpack Compose Material 3. Runtime libraries beyond AndroidX and Compose: `kotlinx-serialization-json` and `kotlinx-coroutines-android` only.
3. **Test device:** a new AVD `unscroll_api36`, so the existing `GymApp_Pixel` emulator stays untouched.
4. **Fetch layout:** `<baseUrl>/<id>.json` for each rule set id that is bundled. No index file is needed, because a new service also needs a new `packageNames` entry and therefore an app update. Default base: `https://raw.githubusercontent.com/<owner>/<repo>/main/rules`, derived from `package.json`.
5. **Refresh:** at most every 24 hours, checked when the service connects and when the app opens, plus a manual button. No WorkManager.
6. **Schema amendment:** an optional `android.rules[].when.selected: true`. Bottom-bar tab buttons exist on every screen, so the active-tab state is the only language-proof way to tell which tab is showing. Task 2 records it in the spec.
7. **CLAUDE.md** says "no network requests, storage or analytics in client code". It is read as applying to the web client, because design §5.3 explicitly allows rule fetching and imports on Android. Task 12 adds a clarifying sentence.
8. **Release builds** use R8 minification, and Task 11 verifies the minified APK on the emulator.
9. **Not done by this plan:** tagging or bumping to `v0.2.0`, creating the real signing keystore, setting GitHub secrets, registering for Android developer verification. These need the maintainer and are listed in `android/README.md` by Task 12.

## Needs the maintainer during execution

- **Task 3:** sign in to Google Play and to Instagram inside the emulator. Use a throwaway Instagram account, because Meta may flag logins from emulators. Nobody but the maintainer enters credentials.
- **Task 12 / release:** keystore, GitHub secrets, version bump, tag, developer verification.

## Review Focus

1. **The phone is not in English.** Rules written against translated text (`Reels`, `Shorts`) would silently stop working. Android rules must be anchored on `viewId`. Pinned in Task 3 (TS test) and Task 12 (docs).
2. **A reel a friend sends in DMs.** Opening it in the app must not be blocked by accident. Task 3 compares the dumps of the Reels tab and of a reel opened from a DM, and Task 7 has a guarded instrumented test. If no rule can tell them apart, the limitation is written down instead of hidden.
3. **A broken or hostile rules file** (an HTML 404 page, a 5 MB body, `schemaVersion: 2`, a rule set for `com.android.settings`, an id like `../x`) never replaces working rules and can never widen the scope beyond the two target apps. Pinned in Tasks 4 and 6.
4. **The blocker never traps the user.** Cooldown (Task 5), the overlay disappearing when the user leaves the app (Task 8), and the Turn off button (Task 9).
5. **A playing video floods the service with content-changed events.** Evaluation is coalesced and tree traversal has a node budget. Pinned in Tasks 5 and 7.

---

## File Structure

`<src>` is `android/app/src/main/kotlin/io/github/lucafregona/unscroll`. Tests mirror it under `src/test/kotlin/...`, `src/testOnline/...`, `src/androidTest/...`.

```
android/settings.gradle.kts, build.gradle.kts, gradle.properties, gradle/libs.versions.toml
android/gradlew, gradlew.bat, gradle/wrapper/*       committed, with distributionSha256Sum
android/app/build.gradle.kts                         flavors, BuildConfig, rules asset copy, signing, verifyOfflineManifest
android/app/src/main/AndroidManifest.xml
android/app/src/main/res/xml/accessibility_service_config.xml
android/app/src/main/res/values/strings.xml
<src>/UnscrollApp.kt                 Application: owns RulesRepository and SettingsStore
<src>/rules/Model.kt                 RuleAction, AndroidRule, AndroidRuleSet
<src>/rules/RuleSetParser.kt         ParseResult, RuleSetParser.parse()
<src>/rules/TargetPackages.kt        the two package names
<src>/rules/RulesRepository.kt       bundled + stored rule sets, newest wins, import
<src>/rules/BaseUrl.kt, RulesFetcher.kt, RulesUpdater.kt, RefreshPolicy.kt
<src>/engine/UiNode.kt, RuleMatcher.kt, CooldownGate.kt, Engine.kt
<src>/service/UnscrollService.kt, NodeAdapter.kt, OverlayController.kt
<src>/ui/MainActivity.kt, MainScreen.kt, OnboardingState.kt, ServiceStatus.kt, SettingsStore.kt
android/app/src/online/AndroidManifest.xml            adds INTERNET
android/app/src/online/kotlin/.../rules/HttpRulesFetcher.kt, FlavorRules.kt
android/app/src/online/kotlin/.../ui/RulesSourceSection.kt
android/app/src/offline/kotlin/.../rules/FlavorRules.kt
android/app/src/offline/kotlin/.../ui/RulesSourceSection.kt
android/app/src/debug/AndroidManifest.xml, res/xml/network_security_config.xml   cleartext to 10.0.2.2 in debug only
.github/workflows/ci.yml, release.yml                 Android jobs
android/README.md, rules/README.md, CONTRIBUTING.md, README.md, CLAUDE.md, ROADMAP.md
```

---

### Task 1: Android project skeleton, flavors and the emulator

**Files:**
- Create: everything under `android/` listed above up to the manifests, plus a placeholder `<src>/ui/MainActivity.kt`
- Test: `android/app/src/test/kotlin/.../VersionTest.kt`, `android/app/src/testOnline/kotlin/.../OnlineBuildTest.kt`, `android/app/src/testOffline/kotlin/.../OfflineBuildTest.kt`

**Interfaces:**
- Produces: `applicationId` / `namespace` `io.github.lucafregona.unscroll`; flavor dimension `distribution` with `offline` and `online`; `BuildConfig.ONLINE: Boolean`; `BuildConfig.DEFAULT_RULES_BASE_URL: String` (`""` for offline); Gradle task `verifyOfflineManifest` wired into `check`; assets directory `rules/` containing every `rules/*.json` of the repo root (not `schema/`).

- [ ] **Step 1: Prepare the emulator** (not part of the repo)

PowerShell: set `$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"` and add `platform-tools`, `emulator` and `cmdline-tools\latest\bin` to `PATH`. Create the AVD:
`avdmanager create avd -n unscroll_api36 -k "system-images;android-36.1;google_apis_playstore;x86_64" -d pixel_8`.
Start it in the background with `emulator -avd unscroll_api36 -no-snapshot-save`, then `adb wait-for-device` and poll `adb shell getprop sys.boot_completed` until it prints `1`.
Check `adb shell pm list packages` for `com.android.vending` (Play Store) and write down whether `com.google.android.youtube` is present. If the Play Store is missing, edit the AVD's `config.ini` so `PlayStore.enabled=yes`, or recreate the AVD with a Play Store device definition.

- [ ] **Step 2: Bootstrap Gradle and a buildable empty app**

Download the Gradle binary distribution that the chosen AGP requires into a new empty scratch directory (never into the repo). From a different directory run `gradle wrapper --gradle-version <v> --gradle-distribution-sha256-sum <sha256 from services.gradle.org>` inside `android/`. Commit `gradlew`, `gradlew.bat` and `gradle/wrapper/*`. Create `settings.gradle.kts` (`rootProject.name = "unscroll"`, `include(":app")`, google() and mavenCentral()), the version catalog, `local.properties` (already ignored) with `sdk.dir`, and an `:app` module with `minSdk 26`, `compileSdk 36`, `targetSdk 36`, `android:allowBackup="false"` and a `MainActivity` that only sets an empty content view. Run `./gradlew assembleDebug` from `android/` and expect success.

- [ ] **Step 3: Write the failing tests**

```kotlin
// VersionTest.kt (src/test)
@Test fun versionNameEqualsPackageJson() {
    val text = File("../../package.json").readText()
    val version = Regex("\"version\"\\s*:\\s*\"([^\"]+)\"").find(text)!!.groupValues[1]
    assertEquals(version, BuildConfig.VERSION_NAME)
}
// OnlineBuildTest.kt (src/testOnline)
@Test fun onlineDefaults() {
    assertTrue(BuildConfig.ONLINE)
    assertEquals("https://raw.githubusercontent.com/Luca-Fregona/unscroll/main/rules", BuildConfig.DEFAULT_RULES_BASE_URL)
}
// OfflineBuildTest.kt (src/testOffline)
@Test fun offlineHasNoUrl() { assertFalse(BuildConfig.ONLINE); assertEquals("", BuildConfig.DEFAULT_RULES_BASE_URL) }
```

- [ ] **Step 4: Run them and confirm they fail**

Run: `./gradlew testOnlineDebugUnitTest testOfflineDebugUnitTest`. Expected: compile errors, because `ONLINE` and `DEFAULT_RULES_BASE_URL` do not exist.

- [ ] **Step 5: Implement the build configuration in `android/app/build.gradle.kts`**

- Read `../../package.json` with `groovy.json.JsonSlurper`. Set `versionName` and `versionCode` as in Global Constraints.
- Derive the default URL from `repository.url` (`https://github.com/<owner>/<repo>`, with an optional `.git` suffix or trailing slash) to `https://raw.githubusercontent.com/<owner>/<repo>/main/rules`. Declare `buildConfigField` for `ONLINE` and `DEFAULT_RULES_BASE_URL` in both flavors.
- `src/online/AndroidManifest.xml` adds `<uses-permission android:name="android.permission.INTERNET" />`. The main manifest must not declare it.
- Copy `../rules/*.json` (top level only) into a generated assets directory under `rules/`, using `androidComponents` generated-assets sources. The copy task must run before asset merging.
- Register `verifyOfflineManifest` per `offline` variant with `androidComponents.onVariants` and `SingleArtifact.MERGED_MANIFEST`. It fails if the merged manifest contains `android.permission.INTERNET`. Hook it into `check`.
- Enable `buildConfig = true` and lint `abortOnError = true`.

- [ ] **Step 6: Run the tests, then prove the manifest check can fail**

Run: `./gradlew testOnlineDebugUnitTest testOfflineDebugUnitTest`. Expected: 3 passed.
Temporarily add the INTERNET permission to the main manifest, run `./gradlew verifyOfflineDebugManifest` and confirm it fails. Remove the line and confirm it passes.

- [ ] **Step 7: Install on the emulator**

Run `./gradlew installOfflineDebug`, then `adb shell dumpsys package io.github.lucafregona.unscroll | findstr INTERNET` (expect no output). Run `./gradlew installOnlineDebug` and expect INTERNET listed. Check that the assets exist: `unzip -l android/app/build/outputs/apk/offline/debug/*.apk | grep assets/rules/` lists `instagram.json` and `youtube.json`.

- [ ] **Step 8: Commit**

`git add android .gitignore && git commit -m "feat(android): project skeleton with offline and online flavors"`

---

### Task 2: `when.selected` in the rules schema

**Files:**
- Modify: `rules/schema/ruleset.schema.json`, `core/types.ts`, `core/validate.ts`, `core/fixtures/valid-full.json`, `docs/superpowers/specs/2026-11-03-unscroll-design.md` (§5.1)
- Test: `core/validate.test.ts`

**Interfaces:**
- Produces: `AndroidRule.when` becomes `{ viewId?: string; contentDescription?: string; selected?: true }` in `core/types.ts`. Kotlin (Task 4) reads the same field.

- [ ] **Step 1: Write the failing tests** (append to `core/validate.test.ts`)

```ts
describe("validateRuleSet — android selected", () => {
  it("accepts selected: true next to a viewId", () => {
    const d = clone(); d.android.rules[0].when = { viewId: "com.example.app:id/tab", selected: true };
    expect(validateRuleSet(d).ok).toBe(true);
  });
  it("rejects selected: false", () => {
    const d = clone(); d.android.rules[0].when = { viewId: "com.example.app:id/tab", selected: false };
    expect(validateRuleSet(d).ok).toBe(false);
  });
  it("rejects selected on its own", () => {
    const d = clone(); d.android.rules[0].when = { selected: true };
    expect(validateRuleSet(d)).toEqual({ ok: false,
      errors: ['android rule "feed-screen": selected needs a viewId or contentDescription'] });
  });
});
```

- [ ] **Step 2: Run the tests and confirm they fail**

Run: `npx vitest run core/validate.test.ts`. Expected: the first test fails (`selected` is an unknown property), the third returns a schema error instead of the semantic message.

- [ ] **Step 3: Implement**

Schema: add `"selected": { "const": true }` to `when.properties`. Types: add `selected?: true`. Validator: add a semantic check after the schema passes, producing the exact message above. Fixture: add a second Android rule `{ id: "feed-tab", description: "Feed tab is selected", when: { viewId: "com.example.app:id/feed_tab", selected: true }, action: "back" }`. Spec §5.1: extend the `android.rules[]` shape line with `selected?: true`, one sentence saying why (tab buttons exist on every screen), and that `when` conditions must all hold on one node.

- [ ] **Step 4: Run everything**

Run: `npm test && npm run typecheck && npm run validate`. Expected: all green.

- [ ] **Step 5: Commit**

`git add core rules docs && git commit -m "feat: android when.selected condition"`

---

### Task 3: UI trees of the official apps become `android` rules

**Files:**
- Modify: `rules/youtube.json`, `rules/instagram.json` (add `android` blocks, bump `version` to the execution date)
- Test: `rules/rules.test.ts`

**Interfaces:**
- Produces: for each file, `android: { package, testedVersion, rules[] }` with `package` `com.google.android.youtube` / `com.instagram.android`, `testedVersion` set to the installed `versionName`, and rules whose `when.viewId` starts with `<package>:id/`. Tasks 4, 6 and 7 read them.

- [ ] **Step 1: Write the failing tests** (append to `rules/rules.test.ts`)

```ts
const android = (id: string) => byId[id].android!;
const apps = ["youtube", "instagram"];

it("ships Android rules for the official apps", () => {
  expect(android("youtube").package).toBe("com.google.android.youtube");
  expect(android("instagram").package).toBe("com.instagram.android");
  for (const id of apps) {
    expect(android(id).testedVersion).not.toBe("");
    expect(android(id).rules.length).toBeGreaterThan(0);
  }
});

it("anchors every Android rule on a resource id, which survives a change of phone language", () => {
  for (const id of apps)
    for (const rule of android(id).rules)
      expect(rule.when.viewId).toMatch(/^[\w.]+:id\/\w+$/);
});

it("only references resource ids of its own app", () => {
  for (const id of apps)
    for (const rule of android(id).rules)
      expect(rule.when.viewId!.startsWith(`${android(id).package}:id/`)).toBe(true);
});
```

- [ ] **Step 2: Run them and confirm they fail**

Run: `npx vitest run rules/rules.test.ts`. Expected: 3 failures, because both files have no `android` block.

- [ ] **Step 3: Set up the apps on the emulator (checkpoint, needs the maintainer)**

Ask the maintainer to sign in to Google Play in the emulator and then to Instagram (throwaway account recommended). Install or update YouTube and Instagram from the Play Store. If Instagram is not installable or refuses to run on the emulator, stop and ask for dumps taken on the maintainer's own phone with the `adb shell uiautomator dump` procedure from `CONTRIBUTING.md`. Do not ship Instagram rules that were not checked against a dump.

- [ ] **Step 4: Dump the screens**

Work in a scratch directory outside the repo. For each screen, navigate by hand (or with `adb shell input tap` using bounds read from the previous dump), then run `adb shell uiautomator dump /sdcard/<name>.xml` and `adb pull /sdcard/<name>.xml <scratch>`. Screens to capture:
- YouTube: `yt-shorts` (the Shorts player, entered from the Shorts tab), `yt-home`, `yt-search-results`, `yt-subscriptions`, `yt-watch` (a normal video).
- Instagram: `ig-reels-tab` (Reels tab, viewer showing), `ig-home`, `ig-explore`, `ig-dm-inbox`, `ig-dm-thread`, `ig-dm-reel` (a reel opened from a DM), `ig-profile-reel` (a reel opened from a profile grid).

List resource ids per dump with `([xml](Get-Content $f)).SelectNodes('//node[@resource-id!=""]') | ForEach-Object { $_.'resource-id' } | Sort-Object -Unique`, and for the active-tab check also read the `selected` attribute of the bottom-bar nodes.

- [ ] **Step 5: Choose the conditions**

For each app pick the smallest `when` that is present in the blocked screen (`yt-shorts`, `ig-reels-tab`) and absent from every allowed screen (`yt-home`, `yt-search-results`, `yt-subscriptions`, `yt-watch`, `ig-home`, `ig-explore`, `ig-dm-inbox`, `ig-dm-thread`). Prefer a container id that only exists in the blocked screen. Use `selected: true` on a bottom-bar tab id when no such container exists. Set `action` to `back` unless the dump shows that Back cannot leave that screen.
Then check `ig-dm-reel` and `ig-profile-reel` against the chosen condition. A friend's reel must stay openable (Review Focus 2). If the condition also matches them, prefer a signal that does not (for example the selected Reels tab), even when that blocks less. If every available signal also matches them, keep the broader rule, and say so in that rule's `description`: "Also closes reels opened from DMs or profiles". Record the result for Task 12.
Never use a translated string alone. `contentDescription` may only be an extra condition on the same node as a `viewId`.

- [ ] **Step 6: Write the rules**

Edit both JSON files. Rule `id`s are kebab-case and unique across the file, `description`s say what the screen is. Set `testedVersion` from `adb shell dumpsys package <package> | findstr versionName`. Bump `version` to today in `YYYY.MM.DD`.

- [ ] **Step 7: Run the checks**

Run: `npm run validate && npm test && npm run typecheck`. Expected: all green, 3 more tests than before.

- [ ] **Step 8: Commit** (rules only; the dumps stay in the scratch directory)

`git add rules && git commit -m "feat(rules): android rules for Instagram Reels and YouTube Shorts"`

---

### Task 4: Rule set model and parser

**Files:**
- Create: `<src>/rules/Model.kt`, `<src>/rules/RuleSetParser.kt`, `<src>/rules/TargetPackages.kt`
- Modify: `android/app/build.gradle.kts` (add `kotlinx-serialization-json`)
- Test: `.../rules/RuleSetParserTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  enum class RuleAction { BACK, HOME, OVERLAY }
  data class AndroidRule(val id: String, val description: String, val viewId: String?,
      val contentDescription: String?, val selected: Boolean, val action: RuleAction)
  data class AndroidRuleSet(val id: String, val version: String, val packageName: String,
      val testedVersion: String, val rules: List<AndroidRule>)
  sealed interface ParseResult {
      data class Ok(val ruleSet: AndroidRuleSet) : ParseResult
      data object NoAndroidBlock : ParseResult
      data class Invalid(val reason: String) : ParseResult
  }
  object RuleSetParser { fun parse(json: String): ParseResult }
  object TargetPackages { val ALL: Set<String> }   // com.instagram.android, com.google.android.youtube
  ```

- [ ] **Step 1: Write the failing tests**

Parse with `Json.parseToJsonElement` and read fields by hand. Do not use `ignoreUnknownKeys` shortcuts for the `android` block. The `web` block is ignored entirely. Tests:
- `parsesTheSharedFixture`: `../../core/fixtures/valid-full.json` gives `Ok` with package `com.example.app`, two rules, and the second rule has `selected == true`.
- `parsesEveryShippedRuleSet`: each `../../rules/*.json` (top level) is `Ok` or `NoAndroidBlock`, and `instagram` and `youtube` are `Ok`.
- `noAndroidBlock`: a valid rule set with only `web` returns `NoAndroidBlock`.
- `invalid` cases, each asserting `is ParseResult.Invalid`: malformed JSON, `schemaVersion` 2, `id` `"../x"`, `version` `"3 Nov 2026"`, package `"nodots"`, action `"swipe"`, `when` `{}`, `selected: false`, `selected` alone, duplicate rule ids, a rule missing `description`.

- [ ] **Step 2: Run them and confirm they fail**

Run: `./gradlew testOfflineDebugUnitTest --tests "*RuleSetParserTest"`. Expected: compile error, `RuleSetParser` does not exist.

- [ ] **Step 3: Implement**

Mirror the checks of `rules/schema/ruleset.schema.json` for every field the Android client consumes: `schemaVersion == 1`, `id` `^[a-z0-9-]+$`, `version` `^\d{4}\.\d{2}\.\d{2}$`, package pattern, rule `id` unique within `android.rules`, `when` has at least one key and `selected` only as `true` next to `viewId` or `contentDescription`. `Invalid.reason` is a short English sentence naming the field.

- [ ] **Step 4: Run the tests**

Run: `./gradlew testOfflineDebugUnitTest testOnlineDebugUnitTest`. Expected: all pass.

- [ ] **Step 5: Commit**

`git add android && git commit -m "feat(android): parse android rule sets"`

---

### Task 5: Matching engine, cooldown and decisions

**Files:**
- Create: `<src>/engine/UiNode.kt`, `RuleMatcher.kt`, `CooldownGate.kt`, `Engine.kt`
- Test: `.../engine/RuleMatcherTest.kt`, `CooldownGateTest.kt`, `EngineTest.kt`

**Interfaces:**
- Consumes: `AndroidRule`, `AndroidRuleSet`, `RuleAction`
- Produces:
  ```kotlin
  interface UiNode { val viewId: String?; val contentDescription: String?
      val selected: Boolean; val children: Sequence<UiNode> }
  class RuleMatcher(private val maxNodes: Int = 2_000) {
      fun firstMatch(rules: List<AndroidRule>, root: UiNode): AndroidRule? }
  class CooldownGate(private val minIntervalMs: Long = 1_000, private val maxActions: Int = 3,
      private val windowMs: Long = 10_000, private val clock: () -> Long) { fun tryAcquire(): Boolean }
  sealed interface Decision { data object None : Decision
      data class Match(val rule: AndroidRule, val allowed: Boolean) : Decision }
  class Engine(private val ruleSets: () -> List<AndroidRuleSet>, private val matcher: RuleMatcher,
      private val gate: CooldownGate) { fun evaluate(packageName: String, root: UiNode): Decision }
  ```

- [ ] **Step 1: Write the failing tests** (a `FakeNode` test double counts every `children` access)

`RuleMatcherTest`:
- `matchesAViewIdAnywhereInTheTree`
- `allKeysMustHoldOnTheSameNode`: viewId on one node and contentDescription on another gives `null`, both on one node matches
- `selectedRequiresASelectedNode`
- `theFirstRuleInListOrderWins`
- `noMatchReturnsNull`
- `stopsAfterMaxNodes`: 5,000 nodes, the target is node 4,000, `maxNodes = 2_000` gives `null` and the fake counted at most 2,000 visits

`CooldownGateTest` (fake clock):
- first call is allowed
- a second call 500 ms later is refused, 1,000 ms later is allowed
- a fourth call inside 10 s is refused even when each is more than 1 s apart, and allowed once the first has left the 10 s window

`EngineTest`:
- `ignoresPackagesWithoutARuleSet`: another package returns `Decision.None` and never touches the tree
- `noMatchIsNone`
- `backAndHomeConsumeTheGate`: a match returns `Match(rule, allowed = true)` once, then `allowed = false` inside the cooldown
- `overlayIgnoresTheGate`: an overlay match is always `allowed = true` and does not use up the gate
- `ruleSetsAreReadOnEveryCall`: replacing the lambda's result changes the next decision

- [ ] **Step 2: Run them and confirm they fail**

Run: `./gradlew testOfflineDebugUnitTest --tests "*engine*"`. Expected: compile error.

- [ ] **Step 3: Implement**

`RuleMatcher`: iterative depth-first traversal (no recursion) that stops after `maxNodes` visited nodes, evaluates every rule against each node, and returns the matching rule that comes first in list order. `viewId` and `contentDescription` use exact string equality. `CooldownGate`: a sliding window of action timestamps. `Engine`: looks up the rule set whose `packageName` equals the argument, then delegates. `allowed` is `true` for `OVERLAY` without touching the gate, otherwise `gate.tryAcquire()`.

- [ ] **Step 4: Run the tests**

Run: `./gradlew testOfflineDebugUnitTest`. Expected: all pass.

- [ ] **Step 5: Commit**

`git add android && git commit -m "feat(android): rule matcher, cooldown gate and engine"`

---

### Task 6: Rules repository, import and online refresh

**Files:**
- Create: `<src>/rules/RulesRepository.kt`, `BaseUrl.kt`, `RulesFetcher.kt`, `RulesUpdater.kt`, `RefreshPolicy.kt`, `<src>/UnscrollApp.kt` (skeleton), online/offline `FlavorRules.kt`, online `HttpRulesFetcher.kt`
- Modify: `AndroidManifest.xml` (`android:name=".UnscrollApp"`)
- Test: `.../rules/RulesRepositoryTest.kt`, `BaseUrlTest.kt`, `RulesUpdaterTest.kt`, `RefreshPolicyTest.kt`, `src/testOnline/.../HttpRulesFetcherTest.kt`, `src/androidTest/.../BundledRulesTest.kt`

**Interfaces:**
- Consumes: `RuleSetParser`, `ParseResult`, `AndroidRuleSet`, `TargetPackages`
- Produces:
  ```kotlin
  const val MAX_RULES_BYTES = 262_144
  sealed interface ImportResult { data class Stored(val id: String, val version: String) : ImportResult
      data class Rejected(val reason: String) : ImportResult }
  class RulesRepository(bundledJson: List<String>, storeDir: File) {
      fun current(): List<AndroidRuleSet>            // cached; sorted by id
      fun import(json: String): ImportResult         // validates, stores atomically, refreshes the cache
      fun bundledIds(): List<String> }
  class BaseUrl private constructor(val value: String) {
      fun fileUrl(id: String): String                // "<value>/<id>.json"
      companion object { fun parse(input: String, allowLocalCleartext: Boolean): BaseUrl? } }
  interface RulesFetcher { fun get(url: String): String }   // throws java.io.IOException
  sealed interface RefreshResult { val id: String
      data class Updated(override val id: String, val version: String) : RefreshResult
      data class UpToDate(override val id: String) : RefreshResult
      data class Failed(override val id: String, val reason: String) : RefreshResult }
  class RulesUpdater(repo: RulesRepository, fetcher: RulesFetcher) { fun refresh(base: BaseUrl): List<RefreshResult> }
  object RefreshPolicy { fun shouldRefresh(nowMs: Long, lastMs: Long, intervalMs: Long = 86_400_000): Boolean }
  fun createRulesFetcher(): RulesFetcher?            // flavor-specific: online returns HttpRulesFetcher, offline returns null
  ```
  `UnscrollApp` exposes `val rules: RulesRepository` (bundled JSON read from the `rules/` assets, stored files in `filesDir/rules`).

- [ ] **Step 1: Write the failing tests**

`RulesRepositoryTest` (JUnit `TemporaryFolder`):
- bundled rule sets are returned for both apps
- a stored rule set with a newer `version` replaces the bundled one, an older one is ignored, an equal one wins over bundled
- an invalid stored file is ignored and the bundled rules stay
- a stored file for a package outside `TargetPackages.ALL` is ignored by `current()`
- `import` is `Rejected` for: invalid JSON, an HTML page, `schemaVersion` 2, no android block, a body over `MAX_RULES_BYTES`, id `../evil`, package `com.android.settings`
- `import` of a valid newer file returns `Stored` and `current()` reflects it without any extra call

`BaseUrlTest`: accepts `https://example.org/rules`, removes a trailing slash, rejects `http://example.org/rules`, `ftp://x`, an empty string, `https://`, `https://user:pw@example.org`, and any query or fragment; accepts `http://10.0.2.2:8080/rules` only with `allowLocalCleartext = true`; `fileUrl("youtube")` equals `https://example.org/rules/youtube.json`.

`RulesUpdaterTest` (fake fetcher): `Updated` for a newer version, `UpToDate` for the same version, `Failed` on `IOException`, `Failed` when the file's id differs from the requested id, `Failed` on invalid content, a failure for one id does not stop the others, and active rules are unchanged after every failure.

`RefreshPolicyTest`: never fetched (`last = 0`) is due, 23 h is not, 24 h is due, a clock that moved backwards (`now < last`) is due.

`HttpRulesFetcherTest` (online unit test, `com.sun.net.httpserver.HttpServer` on a free local port): 200 returns the body, 404 throws `IOException`, a 302 is not followed and throws, a body above `MAX_RULES_BYTES` throws.

`BundledRulesTest` (instrumented): `UnscrollApp.rules.current()` contains ids `instagram` and `youtube`.

- [ ] **Step 2: Run them and confirm they fail**

Run: `./gradlew testOnlineDebugUnitTest`. Expected: compile errors.

- [ ] **Step 3: Implement**

- `RulesRepository`: parse bundled JSON and throw if any is `Invalid` (that is a build bug); stored files are `storeDir/<id>.json`. Per id the highest `version` wins and a tie goes to the stored file. Drop rule sets whose package is not in `TargetPackages.ALL`. Write imports to a temp file and rename.
- `HttpRulesFetcher`: `HttpURLConnection`, GET, 10 s connect and read timeouts, `instanceFollowRedirects = false`, any status other than 200 throws, read at most `MAX_RULES_BYTES + 1` bytes and throw above the limit.
- `RulesUpdater.refresh` requests `base.fileUrl(id)` for every id in `repo.bundledIds()` and goes through `repo.import`, so the same validation guards both paths. Compare the fetched `version` against `repo.current()` to tell `Updated` from `UpToDate`.

- [ ] **Step 4: Run the tests**

Run: `./gradlew testOnlineDebugUnitTest testOfflineDebugUnitTest` and, with the emulator running, `./gradlew connectedOfflineDebugAndroidTest`. Expected: all pass.

- [ ] **Step 5: Commit**

`git add android && git commit -m "feat(android): rules repository, import and online refresh"`

---

### Task 7: The accessibility service (Back and Home)

**Files:**
- Create: `<src>/service/UnscrollService.kt`, `NodeAdapter.kt`, `android/app/src/main/res/xml/accessibility_service_config.xml`
- Modify: `AndroidManifest.xml`, `strings.xml`, `android/app/build.gradle.kts` (androidTest dependencies)
- Test: `src/test/.../service/ServiceConfigTest.kt`, `src/androidTest/.../ServiceHarness.kt`, `YoutubeShortsTest.kt`, `InstagramReelsTest.kt`

**Interfaces:**
- Consumes: `Engine`, `Decision`, `UiNode`, `RulesRepository` (through `UnscrollApp`), `TargetPackages`
- Produces:
  ```kotlin
  class UnscrollService : AccessibilityService() {
      override fun onServiceConnected(); override fun onAccessibilityEvent(event: AccessibilityEvent); override fun onInterrupt()
      companion object { @Volatile var instance: UnscrollService? } }
  class NodeAdapter(private val node: AccessibilityNodeInfo) : UiNode
  ```
  Component name: `io.github.lucafregona.unscroll/.service.UnscrollService`. `ServiceHarness` (androidTest) offers `enable()`, `disable()`, `open(url: String)`, `waitUntil(timeoutMs: Long, check: () -> Boolean): Boolean`.

- [ ] **Step 1: Write the failing tests**

`ServiceConfigTest` (JVM): parse `src/main/res/xml/accessibility_service_config.xml` and assert `packageNames` equals `TargetPackages.ALL` and equals the set of `android.package` values of every `../../rules/*.json` that has an android block; `canRetrieveWindowContent == true`; the event types are exactly window-state and window-content changes; `flagReportViewIds` is set.

Instrumented tests, with `ServiceHarness` enabling the service through `uiAutomation.executeShellCommand("settings put secure enabled_accessibility_services <component>")` plus `accessibility_enabled 1`:
- `YoutubeShortsTest.shortsPlayerIsClosedWithinThreeSeconds`: with instrumentation argument `shortsId`, open `https://www.youtube.com/shorts/<shortsId>` and assert `waitUntil(3000) { !device.hasObject(By.res(<viewId from the youtube rule>)) }`. Skip with `assumeTrue` when the argument is missing.
- `YoutubeShortsTest.aNormalVideoStaysOpen`: with `videoId`, open `https://www.youtube.com/watch?v=<videoId>`, wait 6 s, assert the YouTube package is still in front.
- `InstagramReelsTest.reelsTabIsClosed` and `InstagramReelsTest.aReelFromADmStaysOpen`: guarded with `assumeTrue` on a signed-in Instagram. The second encodes the decision from Task 3 Step 5, and if that decision was "the broader rule also closes DM reels", it asserts the documented behavior instead.

- [ ] **Step 2: Run them and confirm they fail**

Run: `./gradlew testOfflineDebugUnitTest --tests "*ServiceConfigTest"` (compile error) and, once the harness compiles, `./gradlew connectedOfflineDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.shortsId=<id> -Pandroid.testInstrumentationRunnerArguments.videoId=<id>`. Expected: `shortsPlayerIsClosedWithinThreeSeconds` fails because no service exists. Pick the two ids by opening YouTube search on the emulator: any current Short and any ordinary video.

- [ ] **Step 3: Implement the service**

- Config XML: `accessibilityEventTypes="typeWindowStateChanged|typeWindowContentChanged"`, `accessibilityFlags="flagReportViewIds"`, `canRetrieveWindowContent="true"`, `packageNames="com.instagram.android,com.google.android.youtube"`, `notificationTimeout="100"`, `accessibilityFeedbackType="feedbackGeneric"`, `description="@string/service_description"`. Do not set `isAccessibilityTool`.
- Manifest: the service with `android.permission.BIND_ACCESSIBILITY_SERVICE`, `exported="true"`, the `android.accessibilityservice` intent filter and the config meta-data.
- `NodeAdapter`: `children` lazily wraps `getChild(i)`, `viewId` is `viewIdResourceName`, `selected` is `isSelected`. Call `recycle()` on API below 33.
- `UnscrollService.onAccessibilityEvent`: ignore events whose package is not in `TargetPackages.ALL`, coalesce them with a `Handler(Looper.getMainLooper())` so one evaluation runs 150 ms after the first event of a burst, then read `rootInActiveWindow`, skip when it is `null` or its package differs from the event's, call `Engine.evaluate`, and on `Match(rule, allowed = true)` perform `GLOBAL_ACTION_BACK` or `GLOBAL_ACTION_HOME`. `OVERLAY` is handled in Task 8, so here it only logs. Build the `Engine` with `RuleMatcher()`, `CooldownGate(clock = SystemClock::elapsedRealtime)` and `{ app.rules.current() }`. Keep `instance` set between `onServiceConnected` and `onDestroy`.

- [ ] **Step 4: Run the tests**

Run: `./gradlew testOfflineDebugUnitTest`, then `./gradlew connectedOfflineDebugAndroidTest` with the arguments from Step 2. Expected: all pass, and the two Instagram tests are skipped or pass depending on Task 3. Confirm the service is alive with `adb shell dumpsys accessibility | findstr -i unscroll`.

- [ ] **Step 5: Commit**

`git add android && git commit -m "feat(android): accessibility service that closes Reels and Shorts"`

---

### Task 8: Overlay action

**Files:**
- Create: `<src>/service/OverlayController.kt`, `src/test/.../service/OverlayPolicyTest.kt`
- Modify: `UnscrollService.kt`, `strings.xml`
- Test: `src/androidTest/.../OverlayTest.kt`

**Interfaces:**
- Consumes: `Decision.Match` with `rule.action == RuleAction.OVERLAY`, `RulesRepository.import`
- Produces:
  ```kotlin
  class OverlayController(private val service: AccessibilityService, private val onGoBack: () -> Unit) {
      fun show(); fun hide(); val isShowing: Boolean }
  object OverlayPolicy { fun shouldStay(matchPresent: Boolean, foregroundPackage: String?, targets: Set<String>): Boolean }
  ```

- [ ] **Step 1: Write the failing tests**

`OverlayPolicyTest`: stays when a match is present in a target app, goes when there is no match, goes when `foregroundPackage` is `null` or outside `targets` even if the last match was present.
`OverlayTest` (instrumented): import a one-off YouTube rule set (same viewId as the shipped Shorts rule, `action: "overlay"`, a newer `version`) through `UnscrollApp.rules.import`, open the Short, assert the overlay window exists (`device.hasObject(By.text(<overlay title string>))`), press Home with `device.pressHome()`, and assert the overlay is gone within 2 s. A second test taps the "Go back" button and asserts the overlay is gone and YouTube is not showing the Shorts player.

- [ ] **Step 2: Run them and confirm they fail**

Run: `./gradlew testOfflineDebugUnitTest --tests "*OverlayPolicyTest"`. Expected: compile error.

- [ ] **Step 3: Implement**

The overlay is a full-screen opaque view (`TYPE_ACCESSIBILITY_OVERLAY`, `FLAG_LAYOUT_IN_SCREEN or FLAG_NOT_FOCUSABLE`) with a short message and one "Go back" button, which runs `onGoBack` (the service performs `GLOBAL_ACTION_BACK`) and hides the view. It is removed in three ways: the button, an evaluation with no match, and a watchdog that runs every second while it is showing and asks `OverlayPolicy.shouldStay` using `rootInActiveWindow?.packageName`. Its job is to remove the overlay when the user leaves the app, because events from other packages never reach the service.

- [ ] **Step 4: Run the tests**

Run: `./gradlew testOfflineDebugUnitTest connectedOfflineDebugAndroidTest` with the Task 7 arguments. Expected: all pass. If `rootInActiveWindow` reports the overlay's own window instead of the app's, adjust the watchdog until the Home test passes, and keep that test.

- [ ] **Step 5: Commit**

`git add android && git commit -m "feat(android): full-screen overlay action with watchdog"`

---

### Task 9: Disclosure, onboarding and Turn off

**Files:**
- Create: `<src>/ui/OnboardingState.kt`, `ServiceStatus.kt`, `SettingsStore.kt`, `MainScreen.kt`
- Modify: `MainActivity.kt`, `strings.xml`, `android/app/build.gradle.kts` (Compose)
- Test: `src/test/.../ui/OnboardingStateTest.kt`, `ServiceStatusTest.kt`, `src/androidTest/.../TurnOffTest.kt`

**Interfaces:**
- Consumes: `UnscrollService.instance`
- Produces:
  ```kotlin
  enum class OnboardingStep { DISCLOSURE, ENABLE_SERVICE, ACTIVE }
  fun onboardingStep(disclosureAccepted: Boolean, serviceEnabled: Boolean): OnboardingStep
  fun needsRestrictedSettingsHelp(sdkInt: Int, serviceEnabled: Boolean): Boolean   // sdkInt >= 33 && !serviceEnabled
  fun isServiceEnabled(settingValue: String?, flatComponentName: String): Boolean   // handles "pkg/.Cls" and "pkg/pkg.Cls"
  class SettingsStore(context: Context) { var disclosureAccepted: Boolean; var rulesBaseUrl: String?; var lastRefreshMs: Long }
  ```

- [ ] **Step 1: Write the failing tests**

- `onboardingStep`: not accepted gives `DISCLOSURE` whatever the service state, accepted and disabled gives `ENABLE_SERVICE`, accepted and enabled gives `ACTIVE`.
- `needsRestrictedSettingsHelp`: SDK 32 gives `false`, SDK 33 with the service off gives `true`, SDK 36 with the service on gives `false`.
- `isServiceEnabled`: `null` and the empty string give `false`; the short and the full class-name forms both match; a colon-separated list matches when the component is one of the entries; a different service does not match.
- `TurnOffTest` (instrumented): enable the service through the harness, launch `MainActivity`, tap "Turn off" (found by text), then assert `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` no longer lists the component.

- [ ] **Step 2: Run them and confirm they fail**

Run: `./gradlew testOfflineDebugUnitTest --tests "*ui*"`. Expected: compile error.

- [ ] **Step 3: Implement**

Add Compose Material 3 and the Compose compiler plugin through the version catalog. The screen is driven by `onboardingStep`:
- `DISCLOSURE`: states, in plain English, that the service (1) only receives events from Instagram and YouTube, (2) reads the structure of the screen (element ids and labels) to see whether a Reels or Shorts screen is showing, (3) acts only by pressing Back or Home or by drawing a "hidden by unscroll" screen, (4) never stores, logs or sends what it reads, (5) in the `online` flavor downloads rule files and nothing else, (6) can be turned off at any time from this app or from system settings. One button, "I understand".
- `ENABLE_SERVICE`: a button that opens `Settings.ACTION_ACCESSIBILITY_SETTINGS`. When `needsRestrictedSettingsHelp` is true, show the steps for Android 13 and later: open the app info screen (`Settings.ACTION_APPLICATION_DETAILS_SETTINGS`, with a button), tap the three-dot menu, choose "Allow restricted settings", come back and enable unscroll. Re-check the state in `onResume`.
- `ACTIVE`: shows that unscroll is on, the active rule set versions from `UnscrollApp.rules.current()` with their `testedVersion`, a "Turn off" button (`UnscrollService.instance?.disableSelf()`, falling back to opening the accessibility settings when `instance` is `null`), and the flavor-specific rules section from Task 10.
- `service_description` in `strings.xml` is the one-paragraph version of the disclosure, shown by the system under the service name.

- [ ] **Step 4: Run the tests**

Run: `./gradlew testOfflineDebugUnitTest connectedOfflineDebugAndroidTest`. Expected: all pass.

- [ ] **Step 5: Look at it on the emulator**

Install the debug build, clear the app data (`adb shell pm clear io.github.lucafregona.unscroll`), take a screenshot of each step with `adb exec-out screencap -p > <scratch>\stepN.png` and read the images. Check that nothing is cut off and the text matches the points above. Then copy the APK to `/sdcard/Download`, install it from the emulator's Files app, and check whether the accessibility toggle is greyed out ("restricted setting"). Write down what the emulator showed, because `adb install` is exempt and an install from Files is not. If the restricted flow cannot be reproduced, say so in `android/README.md`.

- [ ] **Step 6: Commit**

`git add android && git commit -m "feat(android): disclosure, onboarding and turn off"`

---

### Task 10: Rules management UI and automatic refresh

**Files:**
- Create: `android/app/src/online/kotlin/.../ui/RulesSourceSection.kt`, `android/app/src/offline/kotlin/.../ui/RulesSourceSection.kt`, `android/app/src/debug/AndroidManifest.xml`, `android/app/src/debug/res/xml/network_security_config.xml`
- Modify: `UnscrollApp.kt`, `UnscrollService.kt` (refresh on connect), `MainScreen.kt`
- Test: `src/test/.../rules/RulesRefreshTest.kt`, `src/androidTest/.../RulesImportTest.kt`

**Interfaces:**
- Consumes: `RulesRepository`, `RulesUpdater`, `BaseUrl`, `RefreshPolicy`, `createRulesFetcher`, `SettingsStore`
- Produces: `@Composable fun RulesSourceSection(app: UnscrollApp)` in both flavors. `UnscrollApp.refreshRulesIfDue(nowMs: Long = System.currentTimeMillis())` is a no-op when `createRulesFetcher()` is `null`, when the stored URL is invalid, or when `RefreshPolicy` says it is not due, and it records `lastRefreshMs` after an attempt.

- [ ] **Step 1: Write the failing tests**

`RulesRefreshTest`: a small `RulesRefresher(settings, repo, fetcher, clock)` extracted from `UnscrollApp` is tested with fakes: not due means no fetch, due means one fetch per bundled id and `lastRefreshMs` updated, an invalid stored URL means no fetch, failures still update `lastRefreshMs` and leave the rules unchanged, `fetcher == null` means nothing happens.
`RulesImportTest` (offline instrumented): write a newer `youtube.json` through the same code path the file picker uses and assert `UnscrollApp.rules.current()` reports the new version.

- [ ] **Step 2: Run them and confirm they fail**

Run: `./gradlew testOnlineDebugUnitTest --tests "*RulesRefreshTest"`. Expected: compile error.

- [ ] **Step 3: Implement**

- `offline` section: an "Import rules file" button using `ActivityResultContracts.OpenDocument` for `application/json`, reading at most `MAX_RULES_BYTES + 1` bytes, passing the text to `RulesRepository.import` and showing `Stored` or the rejection reason.
- `online` section: a text field for the base URL (default `BuildConfig.DEFAULT_RULES_BASE_URL`), "Update now" calling `RulesUpdater.refresh` on `Dispatchers.IO` and listing the per-id results, and "Reset to default". Parse with `BaseUrl.parse(input, allowLocalCleartext = BuildConfig.DEBUG)`, and show an inline error for `null`.
- The debug manifest sets `networkSecurityConfig` to a file permitting cleartext only to `10.0.2.2`. Release builds have no such file.
- `UnscrollApp.refreshRulesIfDue()` is called from `MainActivity.onResume` and `UnscrollService.onServiceConnected`, off the main thread.

- [ ] **Step 4: Run the unit and instrumented tests**

Run: `./gradlew testOnlineDebugUnitTest testOfflineDebugUnitTest connectedOfflineDebugAndroidTest`. Expected: all pass.

- [ ] **Step 5: Check the online path end to end on the emulator**

Serve the repo's rules: `npx --yes http-server rules -p 8080` (from a different working directory than any downloaded content). Install `installOnlineDebug`, set the URL to `http://10.0.2.2:8080`, edit a copy of `youtube.json` served by that server to a newer `version`, tap "Update now", and confirm the status list shows the new version. Then serve a 404 and an HTML page and confirm the previous rules stay active and an error is shown.

- [ ] **Step 6: Commit**

`git add android && git commit -m "feat(android): rules import, URL setting and automatic refresh"`

---

### Task 11: Signed release APKs and CI

**Files:**
- Modify: `android/app/build.gradle.kts`, `android/app/proguard-rules.pro`, `.github/workflows/ci.yml`, `.github/workflows/release.yml`, `.gitignore` if needed

**Interfaces:**
- Consumes: environment variables `UNSCROLL_KEYSTORE_FILE`, `UNSCROLL_KEYSTORE_PASSWORD`, `UNSCROLL_KEY_ALIAS`, `UNSCROLL_KEY_PASSWORD`
- Produces: Gradle tasks `assembleOfflineRelease` and `assembleOnlineRelease`. A signed release produces `unscroll-<version>-offline.apk` and `unscroll-<version>-online.apk`. Without the variables the release variants stay unsigned.

- [ ] **Step 1: Configure signing and minification**

A `release` signing config built from the four variables, applied only when all are set. Release: `isMinifyEnabled = true`, `isShrinkResources = true`, with keep rules only for what the manifest and reflection need (the service, the application class).

- [ ] **Step 2: Build and verify with a throwaway keystore**

Create a keystore outside the repo with `keytool -genkeypair ... -keystore <scratch>\test.jks`, export the four variables, and run `./gradlew assembleOfflineRelease assembleOnlineRelease`. Verify the signature with `apksigner verify --print-certs <apk>` from the SDK build-tools. Confirm that the offline APK declares no INTERNET permission: `aapt2 dump permissions <apk>`.

- [ ] **Step 3: Test the minified APK on the emulator**

Install the signed offline release APK, enable the service with the harness commands, and repeat the YouTube Shorts check by hand (or with `connectedOfflineDebugAndroidTest` pointed at the release build if the test APK can be signed accordingly). The Shorts player must close, the app must open, and the disclosure text must show.

- [ ] **Step 4: CI**

In `ci.yml` add an `android` job on `ubuntu-latest`: checkout, Java 21 (Temurin), Gradle setup with caching, and `./gradlew check assembleDebug` in `android/`. It must not need an emulator. Use the latest major of each action, as the existing workflow does.
In `release.yml` add an `android` job with `needs: release`: decode `secrets.UNSCROLL_KEYSTORE_BASE64` into `$RUNNER_TEMP`, export the other three secrets as the variables above, run `./gradlew assembleOfflineRelease assembleOnlineRelease`, fail unless `apksigner verify` succeeds on both APKs, rename them to `unscroll-<version>-<flavor>.apk`, and attach them with `gh release upload "$GITHUB_REF_NAME" <apks>`. The tag-matches-version check from the existing job also applies.

- [ ] **Step 5: Check what can be checked locally**

Run `npm run typecheck && npm test` (must be unchanged) and read both workflow files back for syntax. The workflows cannot run without a push, so state in the commit body that they are unrun.

- [ ] **Step 6: Commit**

`git add android .github .gitignore && git commit -m "ci(android): signed release APKs and Android checks"`

---

### Task 12: Documentation, roadmap and final pass

**Files:**
- Modify: `android/README.md`, `rules/README.md`, `CONTRIBUTING.md`, `README.md`, `CLAUDE.md`, `ROADMAP.md`, `docs/superpowers/specs/2026-11-03-unscroll-design.md` (§5.3, §7)

- [ ] **Step 1: `android/README.md`** (replace the stub)

Sections: what it does and does not block (a table built from the Task 3 results, including the DM and profile behavior and the Shorts shelf on the home screen); install through Obtainium (add the GitHub repo URL, enable the APK filter, pick `online` or `offline`); enabling the service step by step, including "Allow restricted settings" on Android 13 and later; turning it off; building (`./gradlew assembleOfflineDebug` from `android/`, JDK 21, Android SDK); running the tests, including `connected*AndroidTest` and the instrumentation arguments; the rules fetch layout `<base>/<id>.json` for people who host their own rules; and a **Release checklist** for the maintainer: create the keystore, set the four GitHub secrets, bump `package.json` to `0.2.0`, tag `v0.2.0`, register for Android developer verification before the 2027 global rollout.

- [ ] **Step 2: Other docs**

- `rules/README.md`: document `when.selected`, say that conditions all hold on one node, say to prefer `viewId` because `contentDescription` is translated and breaks on phones in another language, and drop "Added in Phase 2".
- `CONTRIBUTING.md`: in the Android section add the Windows form of the dump commands, the list-of-ids one-liner from Task 3, and "never commit a full dump".
- `README.md`: change the Android row of the table to Phase 2 with a link to `android/README.md` and update the status line.
- `CLAUDE.md`: under **Commands** add the Gradle commands (run from `android/`: `./gradlew check`, `./gradlew assembleOfflineDebug`, `./gradlew connectedOfflineDebugAndroidTest`), and under **Hard constraints** one sentence saying the no-network rule is for the web client, while the Android `online` flavor may fetch rule files over HTTPS and the app stores only the items in this plan's Global Constraints.
- Design spec: §5.3 records the fetch layout, the 24-hour refresh and the application id, and §7 gets the numbered calls from this plan.

- [ ] **Step 3: Roadmap**

Tick, in `ROADMAP.md`: the Phase 2 plan, the UI dumps and `android` blocks, the service, the disclosure and onboarding flow, the two flavors, and "Signed APKs ... with Obtainium instructions" only when the pipeline and docs exist. Leave "Register for Android developer verification" unticked.

- [ ] **Step 4: Final verification**

Run `npm run typecheck && npm run validate && npm test` from the repo root and `./gradlew check assembleOfflineDebug assembleOnlineDebug` from `android/`. With the emulator running, run `./gradlew connectedOfflineDebugAndroidTest` with the Task 7 arguments. Everything must pass. Read the final state of `git status` to confirm no dumps, keystores or `local.properties` are tracked.

- [ ] **Step 5: Commit**

`git add -A android README.md CONTRIBUTING.md CLAUDE.md ROADMAP.md rules docs && git commit -m "docs: Android client guide and Phase 2 roadmap"`

---

## Self-review

- **Spec coverage (§5.3):** Kotlin and `minSdk 26` (Task 1); `packageNames` scope, events, Back/Home/overlay, cooldown (Tasks 5, 7, 8); rules as assets (Task 1); both flavors and the fetch layout (Tasks 1, 6, 10); disclosure (Task 9); signed APKs, Obtainium and developer verification (Tasks 11, 12, the last as a maintainer item). The roadmap's seven Phase 2 items map to Plan (this file), Task 3, Tasks 5/7/8, Task 9, Tasks 1/6/10, Tasks 11/12 and Task 12.
- **Type consistency:** `AndroidRule.selected` is defined in Task 4 and read in Task 5. `Decision.Match.allowed` is defined in Task 5 and read in Tasks 7 and 8. `RulesRepository.import` and `ImportResult` come from Task 6 and are used in Tasks 8, 10. `TargetPackages.ALL` is Task 4 and is used in Tasks 6, 7, 8.
- **Review Focus:** every item has an owning test (1: Task 3, 2: Tasks 3 and 7, 3: Tasks 4 and 6, 4: Tasks 5, 8, 9, 5: Tasks 5 and 7).
