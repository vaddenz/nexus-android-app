package com.nexus.feature.im.data.adapter

import com.nexus.feature.im.data.parser.AccessibilityNodeParser
import com.nexus.feature.im.domain.adapter.ImAdapter
import com.nexus.feature.im.domain.model.NodeSnapshot
import com.nexus.feature.im.domain.model.RawMessage
import javax.inject.Inject

/**
 * Adapter for **飞书 / Lark** (`com.ss.android.lark` / `com.larksuite.suite`).
 *
 * Feishu chat lists use RecyclerViews where each item contains a sender
 * nickname and message content text node. The heuristic is similar to
 * Slack: the shorter visible text is the sender, the longer one is content.
 */
class FeishuAdapter @Inject constructor() : ImAdapter {

    override val supportedPackages: Set<String> = setOf(
        "com.ss.android.lark",
        "com.larksuite.suite",
    )

    override fun parseMessage(root: NodeSnapshot): RawMessage? {
        val textNodes = AccessibilityNodeParser.findAll(root) {
            !it.text.isNullOrBlank() && it.isVisible
        }

        if (textNodes.size < 2) return null

        var best: Pair<String, String>? = null
        var bestConfidence = 0f

        for (i in textNodes.indices) {
            for (j in textNodes.indices) {
                if (i == j) continue
                val a = textNodes[i].text ?: continue
                val b = textNodes[j].text ?: continue

                if (isSystemText(a) || isSystemText(b)) continue

                val (sender, content) = if (a.length in 1..30 && b.length > a.length) {
                    a to b
                } else if (b.length in 1..30 && a.length > b.length) {
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
        return lower in SYSTEM_TEXTS || text.length > 500
    }

    private fun estimateConfidence(sender: String, content: String): Float {
        var score = 0.5f
        if (sender.length in 2..30) score += 0.2f
        if (content.length in 1..1000) score += 0.2f
        if (!sender.all { it.isDigit() }) score += 0.1f
        return score.coerceIn(0f, 1f)
    }

    companion object {
        private val SYSTEM_TEXTS = setOf(
            "飞书", "lark", "消息", "通讯录", "工作台", "日历",
            "云文档", "返回", "发送", "更多", "搜索", "会议",
        )
    }
}
