# GPT Mini Android

一个针对 Codex Mini WebUI 手机使用场景优化的原生 Android App。浏览器层使用 Mozilla GeckoView，提供原版液态玻璃渲染、键盘适配、连接管理、下载管理、任务通知和 App 内临时网页浏览等功能。

当前版本：

- versionName：`1.25.1`
- versionCode：`22`
- applicationId：`app.gptmini`
- 最低系统：Android 8.0（API 26）
- 浏览器内核：GeckoView `152.0.20260706120035`

## 构建

命令行构建：

```bash
chmod +x scripts/build-debug.sh
./scripts/build-debug.sh
```

APK 输出位置：

```text
app/build/outputs/apk/debug/GPT Mini-1.25.1-arm64-v8a-debug.apk
```

也可以用 Android Studio 打开本目录，等待 Gradle 同步完成后连接 Android 手机运行 `app`。

当前发布包仅构建 `arm64-v8a`，适用于绝大多数现代 Android 手机。

GeckoView 自带浏览器内核，因此 APK 会明显大于系统 WebView 版本。

## 连接地址

App 首次打开时由用户手动输入或扫码获取 WebUI 地址，连接成功后保存在本机。不要在公开仓库、截图或日志中提交包含个人 token 的连接地址。
