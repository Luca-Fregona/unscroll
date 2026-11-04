# Contributing to unscroll

Thanks for helping people get their time back.

## 1. Report a broken rule (no coding needed)

Apps change their layout often. When a Reels or Shorts feed reappears, open an issue with the **Broken rule** template. Tell us the service, your platform and browser or app version, the URL, and what you still see. A screenshot helps.

## 2. Fix a rule

Rules are plain JSON in [`rules/`](rules/). See [`rules/README.md`](rules/README.md) for the format.

**Websites.** Open the page in a desktop browser's developer tools, or use Safari Web Inspector with an iPhone connected to a Mac. Find the element that wraps the feed and write the most stable selector you can. Prefer attributes like `href^="/shorts/"` over generated class names.

**Android apps.** Use an emulator or a phone with USB debugging, open the screen you want to block, then run:

```bash
adb shell uiautomator dump /sdcard/dump.xml && adb pull /sdcard/dump.xml
```

In Git Bash on Windows, put `MSYS_NO_PATHCONV=1` in front of the command, or Git Bash rewrites `/sdcard/...` into a Windows path.

Do the same for **every screen that must keep working** (home, search, messages, a normal video), then list the `resource-id`s of each dump and keep the ones that appear **only** in the screen you want to block:

```bash
node -e 'const x=require("fs").readFileSync(process.argv[1],"utf8");console.log([...new Set([...x.matchAll(/resource-id="([^"]+)"/g)].map(m=>m[1]))].sort().join("\n"))' dump.xml
```

Write the rule against a `viewId` (see [`rules/README.md`](rules/README.md)), and set `testedVersion` to the app's `versionName` (`adb shell dumpsys package <package> | grep versionName`). **Never commit a full dump:** it contains whatever was on the screen, such as names, captions and messages. Put dumps outside the repo.

After editing a rule file, bump its `version` to today's date (`YYYY.MM.DD`) and run:

```bash
npm ci
npm run validate
npm test
```

## 3. Work on the code

- Requirements: Node.js 22 or later, and npm. The Android client also needs JDK 17 or later and the Android SDK (platform 37, build-tools 36); Android Studio is optional. See [`android/README.md`](android/README.md).
- Larger changes follow the [Superpowers](https://github.com/obra/superpowers) flow:
  1. a design spec in `docs/superpowers/specs/`
  2. a plan in `docs/superpowers/plans/`
  3. test-driven implementation, task by task
- Write tests first. Keep files small and focused.
- Commit messages follow Conventional Commits: `feat:`, `fix:`, `docs:`, `test:`, `chore:`, `ci:`.

## Ground rules

These come straight from the project's anti-goals, and pull requests that break them will not be merged:

- **No telemetry, analytics or network calls** in clients, apart from fetching rules where the user opted in.
- **No dark patterns.** Users must always be able to turn unscroll off easily.
- **Don't break messaging.** DMs and links shared by friends keep working.
- **No modified or repackaged official apps.**
