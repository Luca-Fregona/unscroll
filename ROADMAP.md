# Roadmap

The full rationale lives in [`docs/superpowers/specs/2026-11-03-unscroll-design.md`](docs/superpowers/specs/2026-11-03-unscroll-design.md). Each phase gets its own Superpowers plan in `docs/superpowers/plans/` before any work starts.

## Phase 0: Repo and design ✅
- [x] Research platform constraints (Android accessibility, iOS Screen Time, sideloading rules)
- [x] Design spec
- [x] Phase 1 plan
- [x] Repo scaffolding

## Phase 1: Rules and web client → v0.1
Plan: [`2026-11-03-phase-1-rules-and-userscripts.md`](docs/superpowers/plans/2026-11-03-phase-1-rules-and-userscripts.md)
- [x] Rules schema and validator (`npm run validate`)
- [x] Web engine: redirects, hide-CSS, SPA navigation
- [x] YouTube and Instagram rule sets
- [x] Userscript build
- [ ] Check the rules on iPhone Safari and desktop
- [x] CI and tagged GitHub Releases
- [ ] Tag `v0.1.0`

## Phase 2: Android client → v0.2
Plan: [`2026-11-03-phase-2-android-client.md`](docs/superpowers/plans/2026-11-03-phase-2-android-client.md)
- [x] Write the Phase 2 plan
- [ ] Dump the UI trees of Instagram and YouTube (`adb shell uiautomator dump`) and write the `android` blocks in the rule sets
- [ ] Kotlin `AccessibilityService` scoped to the target packages: Back, Home, overlay, cooldown
- [ ] In-app disclosure screen, and an onboarding flow for Android 13+ restricted settings
- [ ] `offline` and `online` flavors (rules from a user-configurable URL)
- [ ] Signed APKs on GitHub Releases, with Obtainium instructions
- [ ] Register for Android developer verification before the 2027 global rollout

## Phase 3: Coverage and maintenance
- [ ] TikTok (web: full block), Facebook Reels, X video feed
- [ ] Helper that turns a UI dump or a pasted DOM snippet into a candidate rule
- [ ] Broken-rule triage routine with a target turnaround of days, not weeks
- [ ] F-Droid submission

## Phase 4: Optional, if people ask for it
- [ ] iOS Shortcuts guide that opens the website instead of the app
- [ ] Opt-in strict mode with a time lock
- [ ] Native Safari-extension app for people who prefer the App Store
