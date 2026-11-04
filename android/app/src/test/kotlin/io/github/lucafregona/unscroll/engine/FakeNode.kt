package io.github.lucafregona.unscroll.engine

import io.github.lucafregona.unscroll.rules.AndroidRule
import io.github.lucafregona.unscroll.rules.RuleAction

/** A node of a fake accessibility tree. [onViewIdRead] is called every time the matcher looks at its id. */
class FakeNode(
    viewId: String? = null,
    override val contentDescription: String? = null,
    override val selected: Boolean = false,
    override val visible: Boolean = true,
    kids: List<UiNode> = emptyList(),
    private val onViewIdRead: () -> Unit = {},
) : UiNode {
    private val id = viewId
    private val kidList = kids

    override val viewId: String?
        get() {
            onViewIdRead()
            return id
        }

    override val children: Sequence<UiNode>
        get() = kidList.asSequence()
}

fun rule(
    id: String,
    viewId: String? = null,
    contentDescription: String? = null,
    selected: Boolean = false,
    action: RuleAction = RuleAction.BACK,
) = AndroidRule(id, "d", viewId, contentDescription, selected, action)
