package io.github.lucafregona.unscroll.rules

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

sealed interface ParseResult {
    data class Ok(val ruleSet: AndroidRuleSet) : ParseResult
    data object NoAndroidBlock : ParseResult
    data class Invalid(val reason: String) : ParseResult
}

/**
 * Reads the `android` block of a rule set. It checks everything the Android client consumes, with the
 * same patterns as `rules/schema/ruleset.schema.json`, and ignores the `web` block. Anything it does not
 * understand makes the whole rule set `Invalid`, so a rule can never end up broader than its author meant.
 */
object RuleSetParser {
    private val ID = Regex("^[a-z0-9-]+$")
    private val VERSION = Regex("^\\d{4}\\.\\d{2}\\.\\d{2}$")
    private val PACKAGE = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")
    private val WHEN_KEYS = setOf("viewId", "contentDescription", "selected")

    private class Bad(val reason: String) : Exception(reason)

    fun parse(json: String): ParseResult = try {
        read(json)
    } catch (bad: Bad) {
        ParseResult.Invalid(bad.reason)
    }

    private fun read(json: String): ParseResult {
        val root = try {
            Json.parseToJsonElement(json)
        } catch (_: IllegalArgumentException) {
            throw Bad("not valid JSON")
        }
        if (root !is JsonObject) throw Bad("the top level must be an object")

        if (root["schemaVersion"]?.asInt() != 1) throw Bad("schemaVersion must be 1")
        val id = root.text("id", "id").also { if (!ID.matches(it)) throw Bad("id must match ^[a-z0-9-]+$") }
        val version = root.text("version", "version").also {
            if (!VERSION.matches(it)) throw Bad("version must look like YYYY.MM.DD")
        }
        val android = root["android"] ?: return ParseResult.NoAndroidBlock
        if (android !is JsonObject) throw Bad("android must be an object")

        val packageName = android.text("package", "android.package").also {
            if (!PACKAGE.matches(it)) throw Bad("android.package is not a package name")
        }
        val testedVersion = android.text("testedVersion", "android.testedVersion", allowEmpty = true)
        val rules = (android["rules"] as? kotlinx.serialization.json.JsonArray ?: throw Bad("android.rules must be an array"))
            .mapIndexed { index, element -> rule(element, "android.rules[$index]") }

        val duplicate = rules.groupBy { it.id }.entries.firstOrNull { it.value.size > 1 }
        if (duplicate != null) throw Bad("duplicate android rule id \"${duplicate.key}\"")

        return ParseResult.Ok(AndroidRuleSet(id, version, packageName, testedVersion, rules))
    }

    private fun rule(element: JsonElement, path: String): AndroidRule {
        if (element !is JsonObject) throw Bad("$path must be an object")
        val id = element.text("id", "$path.id").also { if (!ID.matches(it)) throw Bad("$path.id must match ^[a-z0-9-]+$") }
        val description = element.text("description", "$path.description")
        val action = when (element.text("action", "$path.action")) {
            "back" -> RuleAction.BACK
            "home" -> RuleAction.HOME
            "overlay" -> RuleAction.OVERLAY
            else -> throw Bad("$path.action must be back, home or overlay")
        }

        val condition = element["when"] as? JsonObject ?: throw Bad("$path.when must be an object")
        condition.keys.firstOrNull { it !in WHEN_KEYS }?.let { throw Bad("$path.when has an unknown key \"$it\"") }
        val viewId = condition["viewId"]?.let { condition.text("viewId", "$path.when.viewId") }
        val contentDescription = condition["contentDescription"]?.let {
            condition.text("contentDescription", "$path.when.contentDescription")
        }
        val selected = condition["selected"]?.let {
            val primitive = it as? JsonPrimitive
            if (primitive == null || primitive.isString || primitive.booleanOrNull != true) {
                throw Bad("$path.when.selected can only be true")
            }
            true
        } ?: false

        if (viewId == null && contentDescription == null) {
            throw Bad("$path.when needs a viewId or a contentDescription")
        }
        return AndroidRule(id, description, viewId, contentDescription, selected, action)
    }

    private fun JsonObject.text(key: String, path: String, allowEmpty: Boolean = false): String {
        val primitive = this[key] as? JsonPrimitive
        if (primitive == null || !primitive.isString) throw Bad("$path must be a string")
        if (!allowEmpty && primitive.content.isEmpty()) throw Bad("$path must not be empty")
        return primitive.content
    }

    private fun JsonElement.asInt(): Int? = (this as? JsonPrimitive)?.takeIf { !it.isString }?.content?.toIntOrNull()
}
