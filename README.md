# GPT Mini Android（WebView 内核）

基于系统 `android.webkit.WebView` 的 GPT Mini 客户端。功能对齐 GeckoView 正式版（连接管理、下载、任务通知、桌面模式、界面设置、页面桥接等），浏览器内核保持 WebView，便于轻量安装与系统内核复用。

当前版本：

- versionName：`2.0.1`
- versionCode：`33`
- applicationId：`app.gptmini.webview`（与 Gecko 版 `app.gptmini` 并列安装）
- 最低系统：Android 8.0（API 26）
- 浏览器内核：系统 WebView

## 构建

```bash
chmod +x scripts/build-debug.sh
./scripts/build-debug.sh
```

APK 输出：

```text
app/build/outputs/apk/debug/GPT Mini-WebView-2.0.1-debug.apk
```

## 参考源码

`reference/GeckoView-v1.25.1/` 为 Gecko 正式版只读对照副本，移植功能时查阅，不参与构建。
