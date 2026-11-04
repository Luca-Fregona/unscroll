package io.github.lucafregona.unscroll.rules

import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** The largest rule file unscroll reads, whether it is imported or fetched. */
const val MAX_RULES_BYTES = 262_144

sealed interface ImportResult {
    data class Stored(val id: String, val version: String) : ImportResult
    data class Rejected(val reason: String) : ImportResult
}

/**
 * The rules the service acts on: the rule sets bundled with the app plus the ones the user imported or the
 * `online` flavor fetched, which live in [storeDir] as `<id>.json`. Per id the newest `version` wins and, on
 * a tie, the stored copy does. Only rule sets for [TargetPackages] are ever used.
 */
class RulesRepository(bundledJson: List<String>, private val storeDir: File) {
    private val bundled: List<AndroidRuleSet> = bundledJson.mapNotNull { json ->
        when (val result = RuleSetParser.parse(json)) {
            is ParseResult.Ok -> result.ruleSet.takeIf { it.packageName in TargetPackages.ALL }
            ParseResult.NoAndroidBlock -> null
            is ParseResult.Invalid -> error("a bundled rule set is invalid: ${result.reason}")
        }
    }

    @Volatile
    private var cache: List<AndroidRuleSet>? = null

    /** Ids of the rule sets that ship with the app. These are the ones the `online` flavor refreshes. */
    fun bundledIds(): List<String> = bundled.map { it.id }.sorted()

    /** The package the bundled rule set with this id is for, or null if there is none. */
    fun bundledPackage(id: String): String? = bundled.firstOrNull { it.id == id }?.packageName

    fun current(): List<AndroidRuleSet> = cache ?: synchronized(this) { cache ?: load().also { cache = it } }

    /** Validates [json] and, if it is acceptable, stores it as `<id>.json`. Nothing is written otherwise. */
    @Synchronized
    fun import(json: String): ImportResult {
        if (json.toByteArray(StandardCharsets.UTF_8).size > MAX_RULES_BYTES) {
            return ImportResult.Rejected("the file is larger than ${MAX_RULES_BYTES / 1024} KiB")
        }
        val ruleSet = when (val result = RuleSetParser.parse(json)) {
            is ParseResult.Ok -> result.ruleSet
            ParseResult.NoAndroidBlock -> return ImportResult.Rejected("the rule set has no android block")
            is ParseResult.Invalid -> return ImportResult.Rejected(result.reason)
        }
        if (ruleSet.packageName !in TargetPackages.ALL) {
            return ImportResult.Rejected("unscroll does not act in ${ruleSet.packageName}")
        }

        val temp = File(storeDir, "${ruleSet.id}.json.tmp")
        try {
            storeDir.mkdirs()
            temp.writeText(json, StandardCharsets.UTF_8)
            Files.move(temp.toPath(), File(storeDir, "${ruleSet.id}.json").toPath(), StandardCopyOption.ATOMIC_MOVE)
        } catch (e: IOException) {
            temp.delete()
            return ImportResult.Rejected("the rule file could not be saved: ${e.message ?: "unknown error"}")
        }
        cache = null
        return ImportResult.Stored(ruleSet.id, ruleSet.version)
    }

    /** Forgets everything that was imported or fetched, so only the bundled rule sets remain. */
    @Synchronized
    fun resetStored() {
        storeDir.listFiles().orEmpty().forEach { it.delete() }
        cache = null
    }

    private fun load(): List<AndroidRuleSet> {
        val candidates = bundled.map { it to false } + stored().map { it to true }
        // One winner per app: the engine looks rules up by package, so two rule sets for one package must not coexist.
        return candidates.groupBy { (ruleSet, _) -> ruleSet.packageName }.values
            .map { group -> group.maxWith(compareBy<Pair<AndroidRuleSet, Boolean>>({ it.first.version }, { it.second }, { it.first.id })).first }
            .sortedBy { it.id }
    }

    private fun stored(): List<AndroidRuleSet> =
        storeDir.listFiles { file -> file.isFile && file.name.endsWith(".json") }.orEmpty().mapNotNull { file ->
            val result = runCatching { RuleSetParser.parse(file.readText(StandardCharsets.UTF_8)) }.getOrNull()
            (result as? ParseResult.Ok)?.ruleSet?.takeIf {
                it.packageName in TargetPackages.ALL && "${it.id}.json" == file.name
            }
        }
}
