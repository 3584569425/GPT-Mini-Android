# Codex Mini Android

一个最小原生 Android WebView 壳，用来加载 Codex Mini 手机网页，并通过 Android 原生窗口 resize 配合页面脚本缓解键盘遮挡底部输入框的问题。

## 构建

命令行构建：

```bash
chmod +x scripts/build-debug.sh
./scripts/build-debug.sh
```

APK 输出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

也可以用 Android Studio 打开本目录，等待 Gradle 同步完成后连接 Android 手机运行 `app`。

## 入口地址

默认入口在 `app/src/main/res/values/strings.xml` 的 `codex_mini_url`。这个地址包含个人 token，不建议把仓库公开发布。
