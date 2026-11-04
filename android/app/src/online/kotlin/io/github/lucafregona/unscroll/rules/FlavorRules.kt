package io.github.lucafregona.unscroll.rules

/** The online flavor refreshes rules over HTTPS. */
fun createRulesFetcher(): RulesFetcher? = HttpRulesFetcher()
