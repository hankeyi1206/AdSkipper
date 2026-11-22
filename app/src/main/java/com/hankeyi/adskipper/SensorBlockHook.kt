package com.hankeyi.adskipper

import android.hardware.Sensor
import android.hardware.SensorManager
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * LSPosed 模块（需要 Root + LSPosed）。
 * 在 LSPosed 管理器里勾选的应用中：
 *  - 让应用“看不到”陀螺仪 / 加速度计等运动传感器
 *  - 拒绝它们注册传感器监听
 * “摇一摇”广告靠这些传感器判断你是否在晃手机，被拦截后就不会误触跳转。
 * 注意：被勾选的应用里，依赖重力感应的功能（体感游戏、计步等）也会失效，所以只勾选广告多的应用。
 */
class SensorBlockHook : IXposedHookLoadPackage {

    private val blockedTypes = setOf(
        Sensor.TYPE_GYROSCOPE,
        Sensor.TYPE_GYROSCOPE_UNCALIBRATED,
        Sensor.TYPE_ACCELEROMETER,
        Sensor.TYPE_ACCELEROMETER_UNCALIBRATED,
        Sensor.TYPE_LINEAR_ACCELERATION,
        Sensor.TYPE_GRAVITY,
        Sensor.TYPE_ROTATION_VECTOR,
        Sensor.TYPE_GAME_ROTATION_VECTOR,
    )

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == "android" || lpparam.packageName == BuildConfig.APPLICATION_ID) return

        // 1. getDefaultSensor(type) 对被屏蔽类型返回 null
        XposedBridge.hookAllMethods(SensorManager::class.java, "getDefaultSensor", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val type = param.args.getOrNull(0) as? Int ?: return
                if (type in blockedTypes) param.result = null
            }
        })

        // 2. getSensorList(...) 过滤掉被屏蔽的传感器
        XposedBridge.hookAllMethods(SensorManager::class.java, "getSensorList", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                @Suppress("UNCHECKED_CAST")
                val list = param.result as? List<Sensor> ?: return
                param.result = list.filter { it.type !in blockedTypes }
            }
        })

        // 3. 所有 registerListener 最终都会走 SystemSensorManager.registerListenerImpl，直接拒绝
        val impl = XposedHelpers.findClassIfExists("android.hardware.SystemSensorManager", lpparam.classLoader)
        if (impl != null) {
            XposedBridge.hookAllMethods(impl, "registerListenerImpl", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val sensor = param.args.firstOrNull { it is Sensor } as? Sensor ?: return
                    if (sensor.type in blockedTypes) param.result = false
                }
            })
        }
        XposedBridge.log("AdSkipper: 已在 ${lpparam.packageName} 屏蔽运动传感器")
    }
}
