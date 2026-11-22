package com.hankeyi.adskipper

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * 无障碍服务：在应用刚打开（开屏广告期间）时寻找“跳过”按钮并自动点击。
 * 对“摇一摇”广告：只要页面出现“摇一摇/摇动手机”等字样，就更积极地寻找跳过按钮。
 */
class SkipAdService : AccessibilityService() {

    // 开屏广告一般在窗口切换后 8 秒内
    private val splashWindowMs = 8_000L
    // 同一应用两次点击的最小间隔，防止连点
    private val clickCooldownMs = 1_500L

    private val skipKeywords = listOf("跳过", "跳過", "Skip", "SKIP", "skip", "关闭广告")
    private val shakeKeywords = listOf("摇一摇", "摇动手机", "扭一扭", "转动手机", "晃一晃")

    private var currentPkg: String? = null
    private var windowChangedAt = 0L
    private var lastClickAt = 0L
    private var ignoredPkgs: Set<String> = emptySet()

    override fun onServiceConnected() {
        super.onServiceConnected()
        ignoredPkgs = buildIgnoreList()
    }

    private fun buildIgnoreList(): Set<String> {
        val set = mutableSetOf(packageName, "com.android.systemui", "com.android.settings", "android")
        // 桌面（启动器）不处理
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        packageManager.queryIntentActivities(home, 0).forEach { set += it.activityInfo.packageName }
        // 输入法不处理
        Settings_InputMethods(this).forEach { set += it }
        return set
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        if (pkg in ignoredPkgs) return

        val now = SystemClock.uptimeMillis()
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (pkg != currentPkg) {
                currentPkg = pkg
                windowChangedAt = now
            } else if (event.className?.toString()?.contains("Splash", true) == true ||
                event.className?.toString()?.contains("Ad", false) == true) {
                windowChangedAt = now
            }
        }
        if (now - windowChangedAt > splashWindowMs) return
        if (now - lastClickAt < clickCooldownMs) return

        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != pkg) return

        val isShakeAd = shakeKeywords.any { root.findAccessibilityNodeInfosByText(it).isNotEmpty() }
        val target = findSkipNode(root, loose = isShakeAd) ?: return

        if (clickNode(target)) {
            lastClickAt = now
            Stats.increment(this, isShakeAd)
        }
    }

    /** 找“跳过”按钮。loose=true 时（摇一摇广告）放宽文字长度限制。 */
    private fun findSkipNode(root: AccessibilityNodeInfo, loose: Boolean): AccessibilityNodeInfo? {
        val maxLen = if (loose) 10 else 6
        for (kw in skipKeywords) {
            val nodes = root.findAccessibilityNodeInfosByText(kw) ?: continue
            for (n in nodes) {
                if (!n.isVisibleToUser) continue
                val raw = (n.text ?: n.contentDescription ?: "").toString()
                // 去掉倒计时数字、“s/秒”、分隔符后判断，例如“跳过 3”、“5s | 跳过”
                val core = raw.replace(Regex("[\\s0-9sS秒|丨/>·]"), "")
                if (core.isEmpty() || core.length > maxLen) continue
                if (!skipKeywords.any { core.contains(it, ignoreCase = true) }) continue
                return n
            }
        }
        return null
    }

    private fun clickNode(node: AccessibilityNodeInfo): Boolean {
        // 1. 节点本身或最多 3 层父节点可点击，直接点击
        var cur: AccessibilityNodeInfo? = node
        repeat(4) {
            val c = cur ?: return@repeat
            if (c.isClickable && c.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            cur = c.parent
        }
        // 2. 否则在按钮中心模拟一次真实点击
        val r = Rect()
        node.getBoundsInScreen(r)
        if (r.isEmpty) return false
        val path = Path().apply { moveTo(r.exactCenterX(), r.exactCenterY()) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 60))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    override fun onInterrupt() {}
}
