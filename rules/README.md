# Rules

There is one JSON file per service. The file name must equal the `id` inside it, for example `youtube.json` has `"id": "youtube"`. Everything in this folder is released under [CC0-1.0](LICENSE), so any project may reuse it.

The full format is described in [§5.1 of the design](../docs/superpowers/specs/2026-11-03-unscroll-design.md#51-the-rules-format-schemaversion-1). The JSON Schema is `schema/ruleset.schema.json`, added in Phase 1 Task 1.

## At a glance

- `web.matches`: the sites the userscript runs on, in `@match` syntax.
- `web.redirects`: `{ id, description, from, to }`. `from` is a regex tested against the path only. `to` is a same-origin path, and `$1`…`$9` insert capture groups.
- `web.hide`: `{ id, description, selector }`. Use **one CSS selector per rule** and never a comma list, so one stale selector can't break the others.
- `android.rules`: `{ id, description, when: { viewId?, contentDescription? }, action }`, where `action` is `back`, `home` or `overlay`. Added in Phase 2.

## Changing a rule

1. Edit the file and bump `version` to today's date (`YYYY.MM.DD`).
2. Run `npm run validate` and `npm test`.
3. Open a pull request that says what changed on the site or app, and on which platform you checked it.
