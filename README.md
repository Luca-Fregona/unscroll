# unscroll

**Take back your attention.** unscroll removes endless short-video feeds (Instagram Reels, YouTube Shorts, and more later) and keeps the parts of each app you actually use: messages, posts, search, subscriptions.

- **Open source and self-managed.** No app store needed, no account, nothing to sign up for.
- **Zero data collection.** No telemetry, no analytics, no tracking. Ever.
- **Community rules.** When an app changes its layout, one small rule fix updates every client.
- **You stay in control.** Turning unscroll off is always one tap away.

> **Status:** pre-alpha. The web client (Phase 1) is built and waiting for its first release. See the [roadmap](ROADMAP.md).

## How it works

```
rules/*.json  ──►  web client (userscripts)   iPhone Safari, desktop browsers, Firefox for Android
      │
      └────────►  Android client (Kotlin)     official Instagram and YouTube apps
```

The **rules** describe where the short-video feeds live: CSS selectors and URL redirects for websites, and UI element IDs for Android apps. **Clients** apply them.

| Platform | How | Status |
|---|---|---|
| iPhone / iPad | Safari plus the free *Userscripts* extension, using instagram.com and m.youtube.com | Phase 1 |
| Desktop | Violentmonkey or Tampermonkey | Phase 1 |
| Android, websites | Firefox plus Violentmonkey | Phase 1 |
| Android, official apps | unscroll app (accessibility service), sideloaded from GitHub Releases | Phase 2 |

**Why not the iPhone apps themselves?** iOS does not let any app look inside another app. Screen Time can only block a whole app, so every "block Reels on iPhone" product works through the website. unscroll does the same, openly and for free.

## Why it is easy to use

- **No Mac, no Xcode, no developer account.** On iPhone you only need the free *Userscripts* app from the App Store.
- **No computer needed.** Install and update everything from your phone.
- **No sideloading and no 7-day expiry.** Nothing is signed with a personal certificate, so nothing stops working after a week.
- **Updates arrive by themselves.** Your script manager fetches new rules from the release page.
- **Easy to turn off.** Disable the script and everything is back as before.

## Install

iPhone, iPad, desktop browsers and Firefox for Android: follow [`web/README.md`](web/README.md). The install links start working with the first release, v0.1.0.

## Contributing

Fixing a broken rule is the most useful contribution and needs no coding experience. See [CONTRIBUTING.md](CONTRIBUTING.md).

The project is planned and built with the [Superpowers](https://github.com/obra/superpowers) workflow:
- design specs live in [`docs/superpowers/specs/`](docs/superpowers/specs/)
- implementation plans live in [`docs/superpowers/plans/`](docs/superpowers/plans/)

## License

- Code: [GPL-3.0-or-later](LICENSE)
- Rule data in `rules/`: [CC0-1.0](rules/LICENSE), so any project can reuse it

unscroll is an independent project. It is not affiliated with, sponsored by or endorsed by Meta, Google, ByteDance, Instagram, YouTube or TikTok. Their names are used only to describe compatibility.
