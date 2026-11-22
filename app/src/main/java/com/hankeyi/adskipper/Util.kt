package com.hankeyi.adskipper

import android.content.Context
import android.view.inputmethod.InputMethodManager

/** 已安装输入法的包名 */
fun Settings_InputMethods(ctx: Context): List<String> {
    val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    return imm.inputMethodList.map { it.packageName }
}

/** 简单计数 */
object Stats {
    private const val PREF = "stats"
    fun increment(ctx: Context, shake: Boolean) {
        val p = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        p.edit()
            .putInt("skipped", p.getInt("skipped", 0) + 1)
            .putInt("shake", p.getInt("shake", 0) + if (shake) 1 else 0)
            .apply()
    }
    fun skipped(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt("skipped", 0)
    fun shake(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt("shake", 0)
}
