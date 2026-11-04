package io.github.lucafregona.unscroll.ui

import io.github.lucafregona.unscroll.rules.AndroidRuleSet
import io.github.lucafregona.unscroll.rules.TargetPackages

/** The apps the active rules cover, so the screen never claims more than the rules deliver. */
fun coveredAppNames(ruleSets: List<AndroidRuleSet>): List<String> =
    ruleSets.map { TargetPackages.displayName(it.packageName) }.distinct().sorted()
