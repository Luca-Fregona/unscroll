package io.github.lucafregona.unscroll.rules

enum class RuleAction { BACK, HOME, OVERLAY }

/** One `android.rules[]` entry. Every condition that is set must hold on the same node. */
data class AndroidRule(
    val id: String,
    val description: String,
    val viewId: String?,
    val contentDescription: String?,
    val selected: Boolean,
    val action: RuleAction,
)

/** The `android` block of a rule set, with the identity of the file it came from. */
data class AndroidRuleSet(
    val id: String,
    val version: String,
    val packageName: String,
    val testedVersion: String,
    val rules: List<AndroidRule>,
)
