# Android client

An app with one job: an accessibility service that closes the Reels and Shorts screens of the official Instagram and YouTube apps, driven by the `android` blocks of [`rules/*.json`](../rules/). Everything else in those apps keeps working.

> **Status (pre-alpha).** YouTube Shorts is verified on an Android 16 emulator with YouTube 20.10.41. **The Instagram rules are not shipped yet**: they need a dump of the real Instagram app, which has not been taken. Until then the service does nothing in Instagram. Nothing here has been tried on a physical phone yet.

## What it does

| App | Screen | Result |
|---|---|---|
| YouTube | The Shorts player (the Shorts tab) | Closed with Back. **Verified.** |
| YouTube | A Short opened from a shelf on the home screen or from a link | It opens the same player, so the same rules apply. Not verified one by one. |
| YouTube | Home, search, subscriptions, a normal video | Untouched. **Verified.** |
| Instagram | Reels | Not shipped yet, see the status above. |
| Any other app | | Invisible to the service. |

How it works, in short:

- The service is scoped to `com.instagram.android` and `com.google.android.youtube` in `res/xml/accessibility_service_config.xml`, so Android only delivers events from those two apps.
- After a burst of events it reads the node tree of the window in front (resource ids and labels, never text typed by the user) and checks it against the rules. A rule matches when **all** its conditions hold on one node.
- On a match it presses Back or Home, or covers the screen with a "Hidden by unscroll" view that has a Go back button. A cooldown allows at most one action per second and three per ten seconds, so it can never loop.
- Rules are written against `viewId`, not against visible text, so they keep working when the phone is not set to English.

## Install

The releases carry two APKs per version, `unscroll-<version>-offline.apk` and `unscroll-<version>-online.apk`. Install one of them, not both:

- **offline** has no internet permission at all. You update the rules by importing a file.
- **online** also downloads the rule files, at most once a day, from an address you can change in the app. The default is this project's `rules/` folder.

The easiest way to get updates is [Obtainium](https://obtainium.imranr.dev/):

1. In Obtainium choose **Add app** and paste this repository's GitHub address.
2. Open the app's additional options and set **Filter APKs by regular expression** to `offline\.apk$` or `online\.apk$`, depending on the version you want.
3. Install. Obtainium will offer later releases as updates.

You can also download the APK from the GitHub release page and open it on the phone.

## Turn it on

1. Open unscroll and read the disclosure. It says exactly what the service reads and does.
2. Tap **Open accessibility settings**, pick unscroll under *Downloaded apps* (or *Installed apps*) and switch it on.
3. Android shows its own warning, *Allow unscroll to have full control of your device?*. That is the standard text for every accessibility service that is not a screen reader. It is why the app explains itself first. Tap **Allow**.

**On Android 13 and later, if the switch is greyed out**, Android is blocking accessibility for apps installed outside a store. Open the app info screen (the app has a button for it), tap the three-dot menu in the top right corner, choose **Allow restricted settings**, then go back and switch the service on. The menu only appears after you tried to switch the service on once.

> Not verified: an install made with `adb` is exempt from this restriction, so the greyed-out switch was never seen on the emulator. The steps above follow Android's documentation.

## Turn it off

Tap **Turn off** on the main screen, or switch unscroll off in the system accessibility settings. Both always work. If the service ever gets stuck, uninstalling the app removes it.

## What the app stores

Only: whether you read the disclosure, the rules address you typed, the time of the last rules refresh, and the rule files you imported or the `online` version downloaded. Backups are switched off. There is no account, no analytics and no crash reporting. The only network request, in the `online` version, is an HTTPS GET of a rule file: no cookies, no identifiers, no redirects, at most 256 KiB.

## Host your own rules

The `online` version asks `<address>/<id>.json` for every rule set that came with the app, for example `https://example.org/my-rules/youtube.json`. Serve those files over HTTPS and type the address in the app (this is how a fork keeps its own rules). A file is only used if it is valid, at most 256 KiB, describes the id it was asked for, and is for one of the two apps above. A newer `version` replaces the bundled one, anything wrong is ignored and the previous rules stay. **Reset to default** goes back to the rules that shipped with the app.

## Build

You need JDK 17 or later and the Android SDK with platform 37 and build-tools 36. Android Studio is optional. Tell Gradle where the SDK is, with `ANDROID_HOME` or a `local.properties` file (ignored by git) containing `sdk.dir=...`. From this folder:

```bash
./gradlew assembleOfflineDebug assembleOnlineDebug   # APKs in app/build/outputs/apk/
./gradlew check                                      # unit tests, lint and the "offline has no INTERNET" check, every variant
```

The version, `versionCode` and the default rules address are all read from the repo's `package.json`. The rules are copied from [`../rules/`](../rules/) into the app at build time.

### Tests on an emulator

```bash
./gradlew connectedOfflineDebugAndroidTest
```

These drive the real apps, so the emulator needs YouTube (English, past its first-run screens) and no other accessibility service you care about: the tests switch the service on and off through `adb`-level settings and force-stop YouTube. Tests that need an app that is not installed are skipped, and a skipped test is not a pass: read the report. The Instagram tests do not exist yet.

## Limitations

- Instagram is not covered yet (see the status).
- The service can only close a screen it can recognize. If an app update renames the ids, Reels or Shorts come back until the rule is fixed: that is what the *Broken rule* issue template is for, and the main screen shows which app version the rules were tested with.
- A reel or Short opened from somewhere else (a message, a profile) is closed too when it opens in the same full-screen player. Whether Instagram separates those cases is one of the things the missing Instagram dump has to answer.
- Rules for other apps need a new `packageNames` entry and therefore a new release.

## Release checklist (for the maintainer)

Nothing here has been done by the build:

1. Create the signing key once and keep it safe: `keytool -genkeypair -keystore unscroll.jks -alias unscroll -keyalg RSA -keysize 4096 -validity 36500`. Losing it means users cannot update.
2. Add four repository secrets: `UNSCROLL_KEYSTORE_BASE64` (`base64 -w0 unscroll.jks`), `UNSCROLL_KEYSTORE_PASSWORD`, `UNSCROLL_KEY_ALIAS`, `UNSCROLL_KEY_PASSWORD`.
3. Bump `version` in `package.json` to `0.2.0`, commit, then `git tag v0.2.0 && git push origin v0.2.0`. The release workflow publishes the userscripts and then attaches both APKs. It has not run yet.
4. The application id `io.github.lucafregona.unscroll` is permanent once an APK is published.
5. Register for Android developer verification before the 2027 global rollout, or users will need the advanced flow or `adb` to install.
