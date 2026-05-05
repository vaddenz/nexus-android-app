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
 * nickname and message content text node.
 *
 * Two-phase extraction:
 * 1. Filter out system-level UI text (tabs, hints, dates, toolbar labels).
 * 2. Pair the remaining texts.  The shorter one is treated as the sender
 *    and the longer one as the message body.
 *
 * If no sender candidate exists (e.g. self-sent messages), the top-bar
 * title is used as a fallback sender so the message body can still be
 * captured.
 */
class FeishuAdapter @Inject constructor() : ImAdapter {

    override val supportedPackages: Set<String> = setOf(
        "com.ss.android.lark",
        "com.larksuite.suite",
    )

    override fun parseMessage(root: NodeSnapshot): RawMessage? {
        val textNodes = AccessibilityNodeParser.findAll(root) {
            !it.text.isNullOrBlank() && it.isVisible && !it.className.isInputField()
        }

        if (textNodes.isEmpty()) return null

        // --- Phase 1: collect non-system text candidates ---
        val candidates = textNodes.mapNotNull { it.text }
            .filterNot { isSystemText(it) }
            .distinct()

        if (candidates.isEmpty()) return null

        // Try to find a top-bar title (short text that looks like a
        // person / group / bot name and is not obviously system text).
        val titleSender = candidates
            .filter { it.length in 1..30 && !it.all { c -> c.isDigit() } }
            .firstOrNull()

        // --- Phase 2: pair candidates ---
        var best: Pair<String, String>? = null
        var bestConfidence = 0f

        for (i in candidates.indices) {
            for (j in candidates.indices) {
                if (i == j) continue
                val a = candidates[i]
                val b = candidates[j]

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

        // If no pair was found but we have a title sender and a single
        // long content candidate, use it (handles self-sent messages).
        if (best == null && candidates.size >= 1) {
            val content = candidates.filter { it.length > 2 }.maxByOrNull { it.length }
            if (content != null && titleSender != null && content != titleSender) {
                val confidence = estimateConfidence(titleSender, content)
                if (confidence >= 0.6f) {
                    best = titleSender to content
                    bestConfidence = confidence
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
        val lower = text.lowercase().trim()
        if (lower in SYSTEM_TEXTS) return true
        if (text.length > 500) return true

        // Filter common input-hint patterns
        if (lower.contains("message yourself") ||
            lower.contains("keep a memo") ||
            lower.contains("say something") ||
            lower.contains("type a message")
        ) {
            return true
        }

        // Filter date / time patterns (e.g. "Apr 30", "6:20 PM", "Today")
        if (DATE_TIME_REGEX.matches(text.trim())) return true

        // Filter pure time like "6:20" or "18:30"
        if (PURE_TIME_REGEX.matches(text.trim())) return true

        return false
    }

    private fun estimateConfidence(sender: String, content: String): Float {
        var score = 0.5f
        if (sender.length in 2..30) score += 0.1f
        if (content.length in 3..500) score += 0.2f
        if (content.length in 5..200) score += 0.1f
        if (!sender.all { it.isDigit() }) score += 0.1f
        return score.coerceIn(0f, 1f)
    }

    companion object {
        private val SYSTEM_TEXTS = setOf(
            // Chinese UI
            "飞书", "消息", "通讯录", "工作台", "日历",
            "云文档", "返回", "发送", "更多", "搜索", "会议",
            // English UI
            "lark", "chat", "file", "home", "contacts", "docs",
            "wiki", "mail", "moments", "tasks", "approval",
            "video", "audio", "call", "meet", "join",
            "reply", "forward", "copy", "delete", "pin",
            "add", "create", "invite", "share", "edit",
            "done", "ok", "cancel", "close", "back",
            "today", "yesterday", "tomorrow",
            "jan", "feb", "mar", "apr", "may", "jun",
            "jul", "aug", "sep", "oct", "nov", "dec",
            "mon", "tue", "wed", "thu", "fri", "sat", "sun",
            // Emoji / symbols commonly found in toolbar
            "+", "✓", "✔", "✕", "✖", "❌",
        )

        /**
         * Matches date/time labels such as:
         * "Apr 30", "Apr 30, 2024", "6:20 PM", "Today", "Yesterday"
         */
        private val DATE_TIME_REGEX =
            ("""
            ^(?i)
            (today|yesterday|tomorrow)
            |
            ((jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)\s+\d{1,2}(,\s*\d{4})?)
            |
            (\d{1,2}:\d{2}\s*(am|pm)?)
            $
            """.trimIndent().replace(Regex("\\s+"), ""))
                .toRegex()

        /** Matches pure numeric times like "6:20" or "18:30". */
        private val PURE_TIME_REGEX =
            "^\\d{1,2}:\\d{2}\$".toRegex()
    }
}

/** Returns true if the class name indicates an editable input field. */
private fun String?.isInputField(): Boolean {
    if (this == null) return false
    return this.endsWith("EditText") || this.endsWith("AutoCompleteTextView")
}
