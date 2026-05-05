package com.nexus.feature.im.domain.desensitizer

/**
 * A single regex-based desensitization rule.
 *
 * @property id Unique rule identifier.
 * @property name Human-readable name.
 * @property pattern Regex used for matching.
 * @property replacement Replacement string (may include `$1` groups).
 * @property isEnabled Whether this rule is active.
 */
data class DesensitizationRule(
    val id: String,
    val name: String,
    val pattern: Regex,
    val replacement: String,
    val isEnabled: Boolean = true,
)
