# Rules

There is one JSON file per service. The file name must equal the `id` inside it, for example `youtube.json` has `"id": "youtube"`. Everything in this folder is released under [CC0-1.0](LICENSE), so any project may reuse it.

The full format is described in [§5.1 of the design](../docs/superpowers/specs/2026-11-03-unscroll-design.md#51-the-rules-format-schemaversion-1). The JSON Schema is `schema/ruleset.schema.json`, added in Phase 1 Task 1.

## At a glance

- `web.matches`: the sites the userscript runs on, in `@match` syntax.
- `web.redirects`: `{ id, description, from, to }`. `from` is a regex tested against the path only. `to` is a same-origin path, and `$1`…`$9` insert capture groups.
- `web.hide`: `{ id, description, selector }`. Use **one CSS selector per rule** and never a comma list, so one stale selector can't break the others.
- `android.rules`: `{ id, description, when: { viewId?, contentDescription?, selected? }, action }`, where `action` is `back`, `home` or `overlay`.
  - Every condition in `when` must hold on the **same** node, and a rule matches when any node of the window satisfies it. The first matching rule in list order wins.
  - Prefer `viewId` (`<package>:id/<name>`). It does not change with the phone's language. `contentDescription` is translated, so a rule that depends on it only works for people who use the language it was written in.
  - Bottom-bar tab buttons exist on every screen of an app, so to tell which tab is showing use `selected: true` next to the tab's `viewId` or `contentDescription`. `selected` can only be `true` and never stands alone.
  - A rule for an app that is not in the app's `packageNames` list never fires. At the moment those are Instagram and YouTube.
  - Android rule ids share one namespace with the web rule ids of the same file. The existing ones start with `android-` to keep them apart.

## Changing a rule

1. Edit the file and bump `version` to today's date (`YYYY.MM.DD`).
2. Run `npm run validate` and `npm test`.
3. Open a pull request that says what changed on the site or app, and on which platform you checked it.
