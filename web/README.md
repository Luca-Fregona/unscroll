# Web client (userscripts)

Each `rules/<id>.json` becomes a userscript, `unscroll-<id>.user.js`. Install only the services you want to clean up.

| Service | Install link |
|---|---|
| YouTube | https://github.com/Luca-Fregona/unscroll/releases/latest/download/unscroll-youtube.user.js |
| Instagram | https://github.com/Luca-Fregona/unscroll/releases/latest/download/unscroll-instagram.user.js |

Your script manager updates installed scripts automatically from the same links.

## iPhone and iPad

1. Install **Userscripts** from the App Store. It is free and open source.
2. Go to Settings → Apps → Safari → Extensions → Userscripts, turn it on, and allow it on `youtube.com` and `instagram.com`.
3. Open an install link above in Safari, tap the Userscripts button in the address bar, and confirm the install.
4. Use YouTube and Instagram in Safari, at `m.youtube.com` and `instagram.com`. Optionally, remove the apps or put them behind a Screen Time limit, so you don't open them out of habit.

### Shortcuts on the Home Screen

Safari extensions do not run in Home Screen web apps, so a plain "Add to Home Screen" shortcut shows Shorts again. If Chrome is your default browser, or the YouTube and Instagram apps open instead of Safari, use one of these:

- **Shortcuts app:** create a shortcut with the *Open URL* action and the address `x-safari-https://m.youtube.com` (or `x-safari-https://www.instagram.com`), then add it to the Home Screen. The `x-safari-` prefix forces Safari.
- **Safari Favorites:** open the site in Safari and use Share → Add to Favorites. It appears on Safari's start page.

## Desktop

Install **Violentmonkey** or **Tampermonkey** in your browser, then open an install link above and confirm.

## Android

**Firefox** with the **Violentmonkey** add-on works for the websites. The official apps are covered by the Android client in Phase 2.

## Turning it off

Disable or remove the script in your script manager. Nothing else is installed or stored.

## Development

```bash
npm ci
npm test          # Vitest
npm run validate  # check rules/*.json
npm run build     # write dist/unscroll-<id>.user.js
```

To try a local build, install the file from `dist/` in your script manager.
