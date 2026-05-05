package com.nexus.feature.im.domain.model

/**
 * Immutable snapshot of an [android.view.accessibility.AccessibilityNodeInfo] tree.
 *
 * This pure-Kotlin representation removes the Android framework dependency,
 * making adapters 100% unit-testable without Robolectric.
 */
data class NodeSnapshot(
    val className: String? = null,
    val text: String? = null,
    val contentDescription: String? = null,
    val viewIdResourceName: String? = null,
    val isClickable: Boolean = false,
    val isFocusable: Boolean = false,
    val isEnabled: Boolean = true,
    val isVisible: Boolean = true,
    val children: List<NodeSnapshot> = emptyList(),
)
