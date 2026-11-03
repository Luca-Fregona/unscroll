package io.github.lucafregona.unscroll.engine

import io.github.lucafregona.unscroll.rules.AndroidRule

/**
 * Finds the first rule, in list order, that holds on some node of a window. A rule holds on a node when
 * every condition it sets holds on that same node. The traversal looks at no more than [maxNodes] nodes, so
 * a huge tree cannot make the service burn CPU.
 */
class RuleMatcher(private val maxNodes: Int = 2_000) {
    fun firstMatch(rules: List<AndroidRule>, root: UiNode): AndroidRule? {
        if (rules.isEmpty()) return null
        val matched = BooleanArray(rules.size)
        val pending = ArrayDeque<UiNode>().apply { addLast(root) }
        var visited = 0

        while (pending.isNotEmpty() && visited < maxNodes) {
            val node = pending.removeLast()
            visited++
            for (i in rules.indices) {
                if (!matched[i] && rules[i].holdsOn(node)) matched[i] = true
            }
            if (matched[0]) break // nothing can outrank the first rule
            if (visited < maxNodes) node.children.toList().asReversed().forEach(pending::addLast)
        }
        return rules.indices.firstOrNull { matched[it] }?.let(rules::get)
    }

    private fun AndroidRule.holdsOn(node: UiNode): Boolean =
        (viewId == null || node.viewId == viewId) &&
            (contentDescription == null || node.contentDescription == contentDescription) &&
            (!selected || node.selected)
}
