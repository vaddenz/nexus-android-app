package com.nexus.android.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.nexus.feature.im.data.parser.AccessibilityNodeParser
import com.nexus.feature.im.service.ImAdapterRegistry
import com.nexus.feature.im.service.bridge.ImMessageBridge
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * AccessibilityService that listens for window-content changes in IM apps
 * and forwards parsed messages into the Harness framework via [ImMessageBridge].
 *
 * Registered in `AndroidManifest.xml` with
 * `android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"`.
 */
@AndroidEntryPoint
class ImAccessibilityService : AccessibilityService() {

    @Inject lateinit var adapterRegistry: ImAdapterRegistry
    @Inject lateinit var messageBridge: ImMessageBridge

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        ) {
            return
        }

        val root = rootInActiveWindow ?: return
        val packageName = event.packageName?.toString() ?: return

        // Skip if this package is not a known IM app
        if (packageName !in adapterRegistry.supportedPackages) return

        val adapter = adapterRegistry.find(packageName) ?: return
        val snapshot = AccessibilityNodeParser.snapshot(root)
        val message = adapter.parseMessage(snapshot) ?: return

        // Deduplication: drop identical messages within a 2-second window
        if (isDuplicate(message)) return

        messageBridge.emit(message)
        lastMessage = message.content
        lastMessageTime = System.currentTimeMillis()
    }

    override fun onInterrupt() {
        // No-op; the service is passive.
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Keep the service sticky so the system restarts it if killed.
        return START_STICKY
    }

    private fun isDuplicate(message: com.nexus.feature.im.domain.model.RawMessage): Boolean {
        val now = System.currentTimeMillis()
        return message.content == lastMessage && (now - lastMessageTime) < 2000L
    }

    companion object {
        private var lastMessage: String? = null
        private var lastMessageTime: Long = 0L
    }
}
