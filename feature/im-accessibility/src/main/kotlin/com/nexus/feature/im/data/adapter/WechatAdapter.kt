package com.nexus.feature.im.data.adapter

import com.nexus.feature.im.data.parser.AccessibilityNodeParser
import com.nexus.feature.im.domain.adapter.ImAdapter
import com.nexus.feature.im.domain.model.NodeSnapshot
import com.nexus.feature.im.domain.model.RawMessage

/**
 * Adapter for **微信** (`com.tencent.mm`).
 *
 * WeChat chat lists are typically RecyclerViews or ListViews where each item
 * contains an avatar [ImageView], a nickname [TextView], and a message-content
 * [TextView]. This adapter walks the tree looking for leaf text nodes that are
 * siblings under a common list-item container.
 *
 * Heuristics (may need tuning per WeChat version):
 * - The sender nickname is a short text node near the top of the item.
 * - The message content is a longer text node below the nickname.
 * - Both are children of the same list-item root.
 */
import javax.inject.Inject

class WechatAdapter @Inject constructor() : ImAdapter {

    override val supportedPackages: Set<String> = setOf("com.tencent.mm")

    override fun parseMessage(root: NodeSnapshot): RawMessage? {
        // Collect all visible text nodes
        val textNodes = AccessibilityNodeParser.findAll(root) {
            !it.text.isNullOrBlank() && it.isVisible && !it.className.isInputField()
        }

        if (textNodes.isEmpty()) return null

        // Heuristic: find pairs of (short text, longer text) that look like
        // (nickname, message). WeChat typically has 2+ text nodes per item.
        // We pick the pair with the highest confidence.
        var best: Pair<String, String>? = null
        var bestConfidence = 0f

        for (i in textNodes.indices) {
            for (j in textNodes.indices) {
                if (i == j) continue
                val a = textNodes[i].text ?: continue
                val b = textNodes[j].text ?: continue

                // Skip system-level texts that are clearly not messages
                if (isSystemText(a) || isSystemText(b)) continue

                // Heuristic: sender name is short (1-12 chars), message is longer
                val (sender, content) = if (a.length in 1..12 && b.length > a.length) {
                    a to b
                } else if (b.length in 1..12 && a.length > b.length) {
                    b to a
                } else {
                    continue
                }

                val confidence = estimateConfidence(sender, content)
                if (confidence > bestConfidence) {
                    bestConfidence = confidence
                    best = sender to content
                }
            }
        }

        val (sender, content) = best ?: return null

        return RawMessage(
            packageName = supportedPackages.first(),
            senderName = sender.trim(),
            content = content.trim(),
            timestampMs = System.currentTimeMillis(),
            confidence = bestConfidence,
        )
    }

    private fun isSystemText(text: String): Boolean {
        val lower = text.lowercase()
        return lower in SYSTEM_TEXTS || lower.startsWith("android:") || text.length > 200
    }

    private fun estimateConfidence(sender: String, content: String): Float {
        var score = 0.5f
        if (sender.length in 2..12) score += 0.2f
        if (content.length in 1..500) score += 0.2f
        if (!sender.any { it.isDigit() && sender.all { c -> c.isDigit() } }) score += 0.1f
        return score.coerceIn(0f, 1f)
    }

    companion object {
        private val SYSTEM_TEXTS = setOf(
            "微信", "wechat", "通讯录", "发现", "我", "聊天", "返回",
            "更多", "发送", "语音输入", "按住说话", "朋友圈",
        )
    }
}

private fun String?.isInputField(): Boolean {
    if (this == null) return false
    return this.endsWith("EditText") || this.endsWith("AutoCompleteTextView")
}
