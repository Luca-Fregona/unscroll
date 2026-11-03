package io.github.lucafregona.unscroll.service

import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import io.github.lucafregona.unscroll.engine.UiNode

/**
 * A window's node tree as the engine sees it. Nodes are fetched lazily as the matcher walks the tree, and
 * [recycle] gives them back afterwards. Recycling only matters below API 33, where it is still required.
 */
class NodeTree(root: AccessibilityNodeInfo) {
    private val fetched = mutableListOf(root)
    val root: UiNode = NodeAdapter(root, this)

    fun add(node: AccessibilityNodeInfo): AccessibilityNodeInfo = node.also { fetched += it }

    @Suppress("DEPRECATION")
    fun recycle() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) fetched.forEach { it.recycle() }
        fetched.clear()
    }
}

class NodeAdapter(private val node: AccessibilityNodeInfo, private val tree: NodeTree) : UiNode {
    override val viewId: String? get() = node.viewIdResourceName
    override val contentDescription: String? get() = node.contentDescription?.toString()
    override val selected: Boolean get() = node.isSelected

    override val children: Sequence<UiNode>
        get() = sequence {
            for (index in 0 until node.childCount) {
                val child = node.getChild(index) ?: continue
                yield(NodeAdapter(tree.add(child), tree))
            }
        }
}
