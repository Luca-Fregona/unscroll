package io.github.lucafregona.unscroll.engine

import io.github.lucafregona.unscroll.rules.AndroidRule
import io.github.lucafregona.unscroll.rules.AndroidRuleSet
import io.github.lucafregona.unscroll.rules.RuleAction

sealed interface Decision {
    /** Nothing to block. An overlay that is showing should go away. */
    data object None : Decision

    /** [rule] matched. [allowed] is false when the cooldown refused a Back or Home. */
    data class Match(val rule: AndroidRule, val allowed: Boolean) : Decision
}

/** Decides what to do with the window of one app. [ruleSets] is read on every call, so new rules apply at once. */
class Engine(
    private val ruleSets: () -> List<AndroidRuleSet>,
    private val matcher: RuleMatcher,
    private val gate: CooldownGate,
) {
    fun evaluate(packageName: String, root: UiNode): Decision {
        val ruleSet = ruleSets().firstOrNull { it.packageName == packageName } ?: return Decision.None
        val rule = matcher.firstMatch(ruleSet.rules, root) ?: return Decision.None
        val allowed = rule.action == RuleAction.OVERLAY || gate.tryAcquire()
        return Decision.Match(rule, allowed)
    }
}
