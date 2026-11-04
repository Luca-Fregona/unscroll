package io.github.lucafregona.unscroll.engine

/** The part of an accessibility node the rules can look at. The engine never touches the Android framework. */
interface UiNode {
    val viewId: String?
    val contentDescription: String?
    val selected: Boolean
    val visible: Boolean
    val children: Sequence<UiNode>
}
