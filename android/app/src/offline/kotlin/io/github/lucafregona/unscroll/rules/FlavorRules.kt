package io.github.lucafregona.unscroll.rules

/** The offline flavor has no network code and no INTERNET permission, so it never fetches. */
fun createRulesFetcher(): RulesFetcher? = null
