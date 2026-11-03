# Android client (Phase 2)

Not started yet. Planned approach, from the [design §5.3](../docs/superpowers/specs/2026-11-03-unscroll-design.md#53-android-client-phase-2-its-own-plan):

- Kotlin, minSdk 26. An `AccessibilityService` scoped with `packageNames` to the target apps.
- It reads the `android` blocks of `rules/*.json`, matches `viewId` and `contentDescription` in the node tree, and acts with Back, Home or an overlay, with a cooldown so it cannot loop.
- Flavors: `offline` (no INTERNET permission, manual rules import) and `online` (fetches rules from a configurable URL, so forks are first-class).
- A clear in-app disclosure of what the service reads, shown before the user enables it.
- Distributed as signed APKs on GitHub Releases, installed through Obtainium, with F-Droid later.

A dedicated Superpowers plan will be written before any code.
