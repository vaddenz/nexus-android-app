package com.nexus.feature.im.data.adapter

import com.nexus.feature.im.data.parser.AccessibilityNodeParser
import com.nexus.feature.im.domain.adapter.ImAdapter
import com.nexus.feature.im.domain.model.NodeSnapshot
import com.nexus.feature.im.domain.model.RawMessage
import javax.inject.Inject

/**
 * Adapter for **钉钉** (`com.alibaba.android.rimet`).
 *
 * DingTalk chat bubbles are often rendered with sender name above/below
 * the message content. We use a positional heuristic: if text nodes are
 * vertically arranged (parent-child or sibling), the upper/shorter one
 * is the sender and the lower/longer one is the content.
 */
class DingtalkAdapter @Inject constructor() : ImAdapter {

    override val supportedPackages: Set<String> = setOf("com.alibaba.android.rimet")

    override fun parseMessage(root: NodeSnapshot): RawMessage? {
        val textNodes = AccessibilityNodeParser.findAll(root) {
            !it.text.isNullOrBlank() && it.isVisible && !it.className.isInputField()
        }

        if (textNodes.size < 2) return null

        // DingTalk often has sender and content as consecutive leaf nodes.
        // Try consecutive pairs.
        var best: Pair<String, String>? = null
        var bestConfidence = 0f

        for (window in textNodes.windowed(2)) {
            val first = window[0].text ?: continue
            val second = window[1].text ?: continue

            if (isSystemText(first) || isSystemText(second)) continue

            val (sender, content) = if (first.length in 1..20 && second.length > first.length) {
                first to second
            } else if (second.length in 1..20 && first.length > second.length) {
                second to first
            } else {
                continue
            }

            val confidence = estimateConfidence(sender, content)
            if (confidence > bestConfidence) {
                bestConfidence = confidence
                best = sender to content
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
        return lower in SYSTEM_TEXTS || text.length > 200
    }

    private fun estimateConfidence(sender: String, content: String): Float {
        var score = 0.5f
        if (sender.length in 2..20) score += 0.2f
        if (content.length in 1..500) score += 0.2f
        if (!sender.all { it.isDigit() }) score += 0.1f
        return score.coerceIn(0f, 1f)
    }

    companion object {
        private val SYSTEM_TEXTS = setOf(
            "钉钉", "消息", "文档", "工作台", "通讯录", "我的",
            "返回", "发送", "更多", "+", "搜索",
        )
    }
}

private fun String?.isInputField(): Boolean {
    if (this == null) return false
    return this.endsWith("EditText") || this.endsWith("AutoCompleteTextView")
}
