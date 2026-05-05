package com.nexus.feature.im.data.parser

import android.view.accessibility.AccessibilityNodeInfo
import com.nexus.feature.im.domain.model.NodeSnapshot

/**
 * Converts Android [AccessibilityNodeInfo] trees into pure-Kotlin [NodeSnapshot]
 * for testable, framework-free adapter logic.
 */
object AccessibilityNodeParser {

    /**
     * Deep-copies an [AccessibilityNodeInfo] into an immutable [NodeSnapshot].
     *
     * The source node is **not** recycled; callers remain responsible for the
     * original tree's lifecycle.
     */
    fun snapshot(node: AccessibilityNodeInfo): NodeSnapshot {
        val children = mutableListOf<NodeSnapshot>()
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                children.add(snapshot(child))
            }
        }

        return NodeSnapshot(
            className = node.className?.toString(),
            text = node.text?.toString(),
            contentDescription = node.contentDescription?.toString(),
            viewIdResourceName = node.viewIdResourceName,
            isClickable = node.isClickable,
            isFocusable = node.isFocusable,
            isEnabled = node.isEnabled,
            isVisible = node.isVisibleToUser,
            children = children,
        )
    }

    /** Depth-first search returning every node matching [predicate]. */
    fun findAll(root: NodeSnapshot, predicate: (NodeSnapshot) -> Boolean): List<NodeSnapshot> {
        val results = mutableListOf<NodeSnapshot>()
        dfs(root, predicate, results)
        return results
    }

    /** Depth-first search returning the first node matching [predicate], or `null`. */
    fun firstOrNull(root: NodeSnapshot, predicate: (NodeSnapshot) -> Boolean): NodeSnapshot? {
        if (predicate(root)) return root
        for (child in root.children) {
            firstOrNull(child, predicate)?.let { return it }
        }
        return null
    }

    /** Collects all text fragments in the tree (pre-order). */
    fun collectText(root: NodeSnapshot): List<String> {
        val texts = mutableListOf<String>()
        dfsText(root, texts)
        return texts
    }

    private fun dfs(node: NodeSnapshot, predicate: (NodeSnapshot) -> Boolean, out: MutableList<NodeSnapshot>) {
        if (predicate(node)) out.add(node)
        for (child in node.children) {
            dfs(child, predicate, out)
        }
    }

    private fun dfsText(node: NodeSnapshot, out: MutableList<String>) {
        node.text?.takeIf { it.isNotBlank() }?.let { out.add(it) }
        for (child in node.children) {
            dfsText(child, out)
        }
    }
}
