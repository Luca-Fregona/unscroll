package io.github.lucafregona.unscroll.rules

const val YOUTUBE = "com.google.android.youtube"
const val INSTAGRAM = "com.instagram.android"

/** A valid rule set with one back rule. [testedVersion] doubles as a marker to tell two copies apart. */
fun ruleSetJson(
    id: String,
    packageName: String,
    version: String = "2026.11.04",
    testedVersion: String = "bundled",
): String = """
    {
      "schemaVersion": 1, "id": "$id", "name": "$id", "description": "d", "version": "$version",
      "android": { "package": "$packageName", "testedVersion": "$testedVersion",
        "rules": [ { "id": "r1", "description": "d", "when": { "viewId": "$packageName:id/x" }, "action": "back" } ] }
    }
""".trimIndent()

fun webOnlyJson(id: String): String = """
    { "schemaVersion": 1, "id": "$id", "name": "$id", "description": "d", "version": "2026.11.04",
      "web": { "matches": ["*://example.com/*"], "redirects": [], "hide": [] } }
""".trimIndent()
