package com.nexus.feature.im.data.adapter

import com.nexus.feature.im.data.parser.AccessibilityNodeParser
import com.nexus.feature.im.domain.adapter.ImAdapter
import com.nexus.feature.im.domain.model.NodeSnapshot
import com.nexus.feature.im.domain.model.RawMessage
import javax.inject.Inject

/**
 * Adapter for **企业微信** (`com.tencent.wework`).
 *
 * UI structure is very close to WeChat with minor view-id differences.
 * This implementation re-uses the WeChat heuristic but targets the
 * wework package and adjusts confidence thresholds.
 */
class WeworkAdapter @Inject constructor() : ImAdapter {

    override val supportedPackages: Set<String> = setOf("com.tencent.wework")

    override fun parseMessage(root: NodeSnapshot): RawMessage? {
        val textNodes = AccessibilityNodeParser.findAll(root) {
            !it.text.isNullOrBlank() && it.isVisible && !it.className.isInputField()
        }

        if (textNodes.isEmpty()) return null

        var best: Pair<String, String>? = null
        var bestConfidence = 0f

        for (i in textNodes.indices) {
            for (j in textNodes.indices) {
                if (i == j) continue
                val a = textNodes[i].text ?: continue
                val b = textNodes[j].text ?: continue

                if (isSystemText(a) || isSystemText(b)) continue

                val (sender, content) = if (a.length in 1..20 && b.length > a.length) {
                    a to b
                } else if (b.length in 1..20 && a.length > b.length) {
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
        if (sender.length in 2..20) score += 0.2f
        if (content.length in 1..500) score += 0.2f
        if (!sender.all { it.isDigit() }) score += 0.1f
        return score.coerceIn(0f, 1f)
    }

    companion object {
        private val SYSTEM_TEXTS = setOf(
            "企业微信", "消息", "通讯录", "工作台", "我", "返回",
            "更多", "发送", "按住说话", "日程", "会议", "微盘",
        )
    }
}

private fun String?.isInputField(): Boolean {
    if (this == null) return false
    return this.endsWith("EditText") || this.endsWith("AutoCompleteTextView")
}
