package com.hankeyi.adskipper

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (20 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
        }
        root.addView(TextView(this).apply {
            text = getString(R.string.app_name); textSize = 24f
        })
        status = TextView(this).apply { textSize = 16f; setPadding(0, pad, 0, pad) }
        root.addView(status)
        root.addView(Button(this).apply {
            text = "① 开启无障碍服务（自动点跳过）"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })
        root.addView(TextView(this).apply {
            textSize = 14f
            setPadding(0, pad, 0, 0)
            gravity = Gravity.START
            text = """
                ② 拦截陀螺仪（屏蔽摇一摇广告）需要 Root + LSPosed：
                打开 LSPosed 管理器 → 模块 → 启用“广告跳过助手” → 勾选广告多的应用 → 强行停止这些应用后重新打开。

                没有 Root 的话：在“开发者选项 → 快捷设置开发者图块”里添加“传感器已关闭”，打开广告多的应用前点一下即可（对所有应用生效）。
            """.trimIndent()
        })
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        val on = isServiceOn()
        status.text = (if (on) "✅ 自动跳过已开启" else "⚠️ 自动跳过未开启") +
            "\n已跳过 ${Stats.skipped(this)} 次（其中摇一摇广告 ${Stats.shake(this)} 次）"
    }

    private fun isServiceOn(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val me = ComponentName(this, SkipAdService::class.java).flattenToString()
        return enabled.split(':').any { it.equals(me, ignoreCase = true) }
    }
}
