# 广告跳过助手 AdSkipper

一个轻量的安卓工具：

- **自动跳过开屏广告**：通过无障碍服务识别“跳过 / 5s 跳过 / Skip”等按钮并自动点击。
- **对付“摇一摇”广告**：页面出现“摇一摇 / 摇动手机 / 扭一扭”时，更积极地寻找跳过按钮。
- **拦截陀螺仪（LSPosed 模块）**：在你勾选的应用中屏蔽陀螺仪、加速度计、旋转矢量等运动传感器，让摇一摇广告彻底失效。

不联网、不收集任何信息，APK 约 600 KB，支持 Android 7.0+。

## 下载

到 [Releases](../../releases) 下载最新的 `AdSkipper-v*.apk`。

## 使用

1. 安装后打开，点“开启无障碍服务”，找到“广告跳过助手”打开。
2. 华为 / 荣耀等机型：设置 → 应用 → 应用启动管理，把它设为手动管理并全部允许，避免被后台清理。
3. （可选，需要 Root + LSPosed）LSPosed 管理器 → 模块 → 启用“广告跳过助手” → 勾选广告多的应用 → 强行停止这些应用后重新打开。

没有 Root：开发者选项 → 快捷设置开发者图块 → 打开“传感器已关闭”，需要时在下拉栏一键关闭所有传感器。

> 注意：被 LSPosed 勾选的应用里，体感游戏、计步、摇一摇等依赖重力感应的功能也会失效。

## 编译

```bash
./gradlew assembleRelease
```

或者直接 fork 本仓库，GitHub Actions 会自动编译并在 Artifacts 里给出 APK。

技术栈：Kotlin、AccessibilityService、Xposed API 82（compileOnly）、minSdk 24 / targetSdk 34。

## 许可证

MIT
