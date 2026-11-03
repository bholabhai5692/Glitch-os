package com.example.vpn

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AdControlAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AdControlService"
        private const val MAX_NODES_PER_SCAN = 600
        private const val MAX_CLICK_ANCESTORS = 4
        private const val MIN_SCAN_INTERVAL_MS = 120L
        private const val AD_END_GRACE_MS = 1_800L
        private val OBSERVED_EVENT_TYPES = setOf(
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED
        )

        @Volatile
        var isActive = false
            private set

        fun isEnabled(context: Context): Boolean {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val expected = "${context.packageName}/${AdControlAccessibilityService::class.java.name}"
            val splitter = TextUtils.SimpleStringSplitter(':')
            splitter.setString(enabledServices)
            while (splitter.hasNext()) {
                if (splitter.next().equals(expected, ignoreCase = true)) return true
            }
            return false
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastScanAt = 0L
    private var mutedPackageName: String? = null
    private var lastAdActionAt = 0L
    private var lastAdActionWindowId = -1

    private val restoreMuteRunnable = Runnable { restoreMuteIfAdEnded() }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isActive = true
        serviceInfo = serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType !in OBSERVED_EVENT_TYPES) return
        val eventPackage = event.packageName?.toString() ?: return
        if (eventPackage !in AdProtectionStore.supportedApps ||
            !AdProtectionStore.isAppEnabled(this, eventPackage)
        ) return

        val now = SystemClock.uptimeMillis()
        if (now - lastScanAt < MIN_SCAN_INTERVAL_MS) return
        lastScanAt = now

        val root = rootInActiveWindow ?: return
        try {
            val rootPackage = root.packageName?.toString()
            if (eventPackage.isNullOrBlank() || eventPackage != rootPackage) return

            if (mutedPackageName != null && mutedPackageName != rootPackage) {
                mainHandler.removeCallbacks(restoreMuteRunnable)
                mutedPackageName = null
            }

            val nodes = collectVisibleNodes(root)
            try {
                val labels = nodes.mapNotNull(::nodeLabel)
                val isVideoAd = AdUiActionClassifier.isVideoAdContext(labels)

                if (isVideoAd) {
                    mainHandler.removeCallbacks(restoreMuteRunnable)
                    handleAdUi(nodes, rootPackage, event.windowId, now)
                } else if (mutedPackageName == rootPackage) {
                    mainHandler.removeCallbacks(restoreMuteRunnable)
                    mainHandler.postDelayed(restoreMuteRunnable, AD_END_GRACE_MS)
                }
            } finally {
                nodes.filter { it !== root }.forEach(AccessibilityNodeInfo::recycle)
            }
        } catch (e: RuntimeException) {
            Log.w(TAG, "Could not inspect current accessibility window", e)
        } finally {
            root.recycle()
        }
    }

    private fun handleAdUi(
        nodes: List<AccessibilityNodeInfo>,
        packageName: String,
        windowId: Int,
        now: Long
    ) {
        if (mutedPackageName != packageName) {
            if (findAndClick(nodes, AdUiAction.MUTE, isVideoAdContext = true)) {
                mutedPackageName = packageName
                AdProtectionStore.recordAdAction(this, packageName, AdUiAction.MUTE)
            }
        }

        if (windowId == lastAdActionWindowId && now - lastAdActionAt < MIN_SCAN_INTERVAL_MS * 4) return

        val action = when {
            findAndClick(nodes, AdUiAction.SKIP, isVideoAdContext = true) -> AdUiAction.SKIP
            findAndClick(nodes, AdUiAction.CLOSE, isVideoAdContext = true) -> AdUiAction.CLOSE
            else -> null
        }
        val acted = action != null
        if (acted) {
            lastAdActionAt = now
            lastAdActionWindowId = windowId
            action?.let { AdProtectionStore.recordAdAction(this, packageName, it) }
        }
    }

    private fun restoreMuteIfAdEnded() {
        val packageName = mutedPackageName ?: return
        val root = rootInActiveWindow ?: run {
            mutedPackageName = null
            return
        }

        try {
            if (root.packageName?.toString() != packageName) {
                mutedPackageName = null
                return
            }

            val nodes = collectVisibleNodes(root)
            try {
                val labels = nodes.mapNotNull(::nodeLabel)
                if (AdUiActionClassifier.isVideoAdContext(labels)) return

                findAndClick(nodes, AdUiAction.UNMUTE, isVideoAdContext = false)
            } finally {
                nodes.filter { it !== root }.forEach(AccessibilityNodeInfo::recycle)
            }
            mutedPackageName = null
        } catch (e: RuntimeException) {
            Log.w(TAG, "Could not restore player audio after ad", e)
            mutedPackageName = null
        } finally {
            root.recycle()
        }
    }

    private fun collectVisibleNodes(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val result = ArrayList<AccessibilityNodeInfo>()
        val pending = ArrayDeque<AccessibilityNodeInfo>()
        pending.add(root)

        while (pending.isNotEmpty() && result.size < MAX_NODES_PER_SCAN) {
            val node = pending.removeFirst()
            result += node
            for (index in 0 until node.childCount) {
                val child = node.getChild(index) ?: continue
                pending.addLast(child)
            }
        }
        while (pending.isNotEmpty()) pending.removeFirst().recycle()
        return result
    }

    private fun nodeLabel(node: AccessibilityNodeInfo): String? =
        node.contentDescription?.toString()
            ?.takeIf(String::isNotBlank)
            ?: node.text?.toString()?.takeIf(String::isNotBlank)

    private fun findAndClick(
        nodes: List<AccessibilityNodeInfo>,
        action: AdUiAction,
        isVideoAdContext: Boolean
    ): Boolean {
        for (node in nodes) {
            val label = nodeLabel(node) ?: continue
            if (AdUiActionClassifier.actionFor(label, isVideoAdContext) != action) continue

            var candidate = node
            var temporaryNode = false
            for (depth in 0..MAX_CLICK_ANCESTORS) {
                val current = candidate
                if (current.isEnabled &&
                    (current.isClickable || current.actionList.any {
                        it.id == AccessibilityNodeInfo.ACTION_CLICK
                    })
                ) {
                    val clicked = click(current)
                    if (temporaryNode) {
                        current.recycle()
                        temporaryNode = false
                    }
                    if (clicked) return true
                    break
                }
                val parent = current.parent ?: break
                if (temporaryNode) current.recycle()
                candidate = parent
                temporaryNode = true
            }
            if (temporaryNode) candidate.recycle()
        }
        return false
    }

    private fun click(target: AccessibilityNodeInfo): Boolean =
        try {
            target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } catch (e: RuntimeException) {
            Log.w(TAG, "Ad control click was rejected by the host app", e)
            false
        }

    override fun onInterrupt() {
        mainHandler.removeCallbacks(restoreMuteRunnable)
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(restoreMuteRunnable)
        isActive = false
        mutedPackageName = null
        super.onDestroy()
    }
}
