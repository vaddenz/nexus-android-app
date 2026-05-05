package com.nexus.feature.im.domain.adapter

import com.nexus.feature.im.domain.model.NodeSnapshot
import com.nexus.feature.im.domain.model.RawMessage

/**
 * Strategy interface for parsing IM messages from an accessibility node tree.
 *
 * Implementations are package-specific (WeChat, DingTalk, etc.).
 */
interface ImAdapter {

    /** Package names this adapter can handle. */
    val supportedPackages: Set<String>

    /**
     * Returns true if this adapter believes it can parse the given tree.
     *
     * The default implementation checks [packageName] against [supportedPackages],
     * but concrete adapters may inspect the node tree for stronger confidence.
     */
    fun canHandle(root: NodeSnapshot, packageName: String): Boolean {
        return packageName in supportedPackages
    }

    /**
     * Attempt to extract a [RawMessage] from the node tree.
     *
     * @return A message if parsing succeeds, or `null` if the tree does not
     *         represent a message widget (e.g. title bar, input field).
     */
    fun parseMessage(root: NodeSnapshot): RawMessage?
}
