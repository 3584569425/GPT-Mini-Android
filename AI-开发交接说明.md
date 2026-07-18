# GPT Mini Android 开发交接说明

本文档用于把当前项目交给另一台电脑或另一个 AI 继续开发。它记录当前项目状态、已实现功能、关键文件、构建方式、签名信息和后续开发注意事项。

## 1. 项目概况

项目名称：GPT Mini

项目类型：Android 原生 WebView App

主要语言：Java

构建系统：Gradle / Android Gradle Plugin

当前版本：

- applicationId：`app.gptmini.webview`
- versionCode：`33`
- versionName：`2.0.1`
- App 显示名称：`GPT Mini`

当前定位：

这是一个为 Codex Mini / GPT Mini WebUI 优化的 Android 手机 App。核心仍是加载 WebUI，但针对 Android 手机上浏览器/WebView 使用中的键盘、下载、通知、连接切换、首页入口等问题做了原生层优化。

## 2. 目录结构

重要目录和文件：

- `app/src/main/java/com/coimgrain/codexminiapp/MainActivity.java`
  - App 主逻辑。
  - 包含首页、WebView、悬浮窗、下载界面、通知、局域网探测、下载记录持久化等功能。

- `app/src/main/java/com/coimgrain/codexminiapp/ScanActivity.java`
  - 扫码连接界面。

- `app/src/main/res/values/strings.xml`
  - App 名称、按钮文案、通知文案、下载文案等。

- `app/src/main/res/drawable/`
  - 图标资源。
  - 当前 App 图标使用 ChatGPT 风格图标资源。

- `app/src/main/res/xml/network_security_config.xml`
  - 网络配置。
  - 已允许 HTTP 明文流量，便于访问局域网或公网 HTTP WebUI。

- `GPTMini-应用介绍与使用帮助.md`
  - 面向用户的 App 介绍、优化说明和使用帮助。

- `AI-开发交接说明.md`
  - 当前文档，面向继续开发的 AI 或开发者。

- `scripts/build-debug.sh`
  - debug 构建脚本。

- `scripts/build-release.sh`
  - release 构建脚本。

- `keystore/gpt-mini-release.jks`
  - release 签名证书。
  - 继续发布 `v1.1`、`v1.2` 等版本时必须继续使用这个证书。

- `keystore/release-signing.properties`
  - release 签名配置。
  - 包含证书路径、别名和密码。

## 3. 当前已实现功能

### 3.1 WebView 基础功能

- 加载 Codex Mini / GPT Mini WebUI。
- 支持 HTTP 和 HTTPS。
- 支持文件选择。
- 支持页面权限请求。
- 支持 WebUI 下载触发。
- 支持页面内通知状态回传到原生通知。

### 3.2 首页连接页

首页已经重新设计，用户首次打开时需要输入 WebUI 地址或扫码连接。

首页入口包括：

- 扫码连接。
- 已保存连接。
- 继续上次连接。
- 手动输入地址。
- 连接提示。

连接成功后会保存上次地址。后续打开 App 会默认继续上次连接。

### 3.3 已保存连接

已保存连接用于管理多个 WebUI 地址。

行为：

- 连接成功后自动保存地址。
- 相同地址不会重复保存。
- 重新连接已有地址时，会把该地址移动到列表最上方。
- 用户可以给地址设置备注名称。
- 用户可以删除保存记录。
- 点击保存记录可以直接连接。

### 3.4 键盘适配优化

已处理 Android WebView 中常见的输入框和键盘问题：

- 首次进入后点击输入框无法弹出键盘的问题。
- 点击键盘收起按钮后，输入框和页面位置不回落的问题。
- 复制消息等非输入操作误触发键盘的问题。

目标是让输入框行为更接近原生 App。

### 3.5 悬浮窗

App 使用侧边悬浮窗作为功能入口，避免顶部工具栏占用 WebUI 显示空间。

悬浮窗特性：

- 默认在屏幕侧边。
- 支持拖动。
- 支持侧边吸附。
- 支持半透明。
- 可调整大小。
- 主题支持深色、浅色、跟随系统。

悬浮窗面板按钮名称以实际 UI 为准：

- 下载。
- 回到连接页。
- 刷新当前页面。
- 悬浮窗设置。
- 通知设置。

### 3.6 下载管理

针对原 WebUI 点击下载后不知道文件在哪的问题，新增了 App 内下载界面。

下载界面能力：

- 显示下载记录。
- 显示下载状态。
- 显示下载时间。
- 最新下载显示在最上面。
- 长文件名最多两行显示，超出省略。
- 支持打开文件。
- 支持分享文件。
- 支持删除文件。
- 支持管理模式、全选、取消全选、批量删除。
- 支持深色、浅色主题，跟随悬浮窗主题设置。

重要行为：

- 删除按钮是真实删除手机本地文件，不只是隐藏列表记录。
- DownloadManager 下载使用 `DownloadManager.remove(id)` 删除。
- App 自己保存的文件使用 `ContentResolver.delete(uri)` 或 `File.delete()` 删除。
- 删除成功后才从下载列表移除。

### 3.7 下载记录持久化

下载记录已做成类似浏览器下载记录的保存方式。

当前保存内容包括：

- DownloadManager id。
- 是否为 App 手动保存文件。
- 文件名。
- mimeType。
- manualUri。
- localUri。
- 下载状态。
- 已下载大小。
- 总大小。
- 下载时间。

正常覆盖安装或升级 App 后，下载界面会恢复之前的下载记录。

注意：

- 如果用户手动卸载 App 再安装，Android 可能清除 App 本地配置，下载记录可能丢失。
- 文件本身通常仍在系统下载目录中。
- 如果用户在文件管理器里手动删除文件，App 下载记录可能仍显示，但打开/分享会失败，需要用户在 App 下载界面删除记录。

### 3.8 通知功能

通知模式有两种：

- 任务结束通知。
- 常驻通知。

任务结束通知：

- 只有任务完成、失败或异常时发送通知。

常驻通知：

- App 连接后常驻显示任务通知。
- 没有任务时显示空闲状态。
- 任务运行、完成、失败/异常时更新通知。

多任务通知：

- 每个会话任务使用独立通知。
- 同一个会话的运行中、完成、失败/异常会覆盖更新同一个通知，不重复新建多个通知。

### 3.9 局域网线路辅助识别

App 不会直接把 WebView 地址栏改成局域网地址。

当前策略：

- WebView 页面地址仍保持用户输入的公网 URL。
- 原生层探测局域网可用性。
- 探测可用后，辅助 WebUI 使用本地线路。
- 尽量让 WebUI 自己的右上角状态显示真实线路状态。

这不是系统浏览器的完全原生自动切换，而是 App 针对该 WebUI 做的辅助识别和注入。

### 3.10 纯文本/错误页适配

对一些纯文本、JSON、错误信息页面做了移动端显示适配。

目标：

- 避免超长文本撑出屏幕。
- 避免横向超宽显示。
- 让错误信息在手机屏幕上可读。

## 4. 当前 release 包状态

当前正式包：

- 输出位置：`outputs/GPTMini-1.0-release.apk`
- 原始 Gradle 输出：`app/build/outputs/apk/release/GPT Mini-1.0-release.apk`

已验证：

- release v1/v2 签名通过。
- zipalign 通过。
- 包名：`app.gptmini`
- 版本：`1.0`

如果用户之前安装的是 debug 包，可能因为签名不同不能直接覆盖安装，需要先卸载 debug 版。

之后只要一直使用当前 release 证书打包，就可以正常覆盖升级。

## 5. 构建说明

当前项目没有提交 Gradle Wrapper，而是使用本机 `.tools` 目录里的 Gradle、JDK 和 Android SDK。

本源码压缩包通常不包含 `.tools`，因为该目录很大且属于本机工具依赖。迁移到新电脑后可选两种方式：

### 方式一：使用 Android Studio

1. 用 Android Studio 打开项目根目录。
2. 安装 Android SDK 35。
3. 等待 Gradle 同步。
4. 构建 debug 或 release。

### 方式二：使用命令行

需要准备：

- JDK。
- Android SDK。
- Gradle 8.x。
- Android Gradle Plugin 可正常解析依赖。

然后根据新电脑环境调整：

- `local.properties`
- `scripts/build-debug.sh`
- `scripts/build-release.sh`

当前脚本默认使用：

- `.tools/jdk/Contents/Home`
- `.tools/android-sdk`
- `.tools/gradle/gradle-8.10.2/bin/gradle`

如果新电脑没有 `.tools`，脚本需要改成系统自己的 `JAVA_HOME`、`ANDROID_HOME` 和 `gradle`。

## 6. 签名说明

release 签名文件：

- `keystore/gpt-mini-release.jks`
- `keystore/release-signing.properties`

这两个文件对后续升级非常重要。

注意：

- 不要丢失 `gpt-mini-release.jks`。
- 不要随便换签名证书。
- 如果换证书，同包名 App 将无法覆盖升级。
- 不要把 keystore 和密码公开发布到公开仓库。

当前 `.gitignore` 已忽略 keystore 文件，但源码压缩包为了方便迁移开发，包含了签名文件。

## 7. 当前用户关心的体验要求

继续开发时需要特别注意以下偏好：

- 用户希望界面尽量贴近当前 WebUI 风格。
- 首页必须一屏显示，不要出现明显滚动。
- 悬浮窗不要占用太多屏幕空间。
- 悬浮窗按钮和文档按钮名称必须与真实 UI 一致。
- 下载界面不要全屏，保持半屏抽屉体验。
- 下载文件删除必须是真实删除本地文件。
- 下载记录要像浏览器一样保存。
- 正常覆盖安装或升级后，下载记录、已保存连接、设置都应该保留。
- 文件名、时间、按钮布局要适配手机屏幕，不能溢出。
- 如果打包给用户，必须使用 release 签名包，不要发 debug 包。

## 8. 近期修改记录

最近完成的修改：

- 新增 release 签名配置。
- 生成 release 签名证书。
- 打包 `GPT Mini v1.0 release`。
- 下载界面新增分享按钮。
- 下载文件名改为最多两行。
- 下载完成状态和下载时间合并到一行显示。
- 下载记录改为持久化保存。
- 删除下载时同步删除本地文件和下载记录。
- 新增用户文档 `GPTMini-应用介绍与使用帮助.md`。
- 修正文档中悬浮窗按钮名称，和实际 UI 保持一致。

## 9. 建议后续开发事项

可以考虑继续优化：

- 下载记录文件存在性检测。
- 下载记录失效时显示“文件不存在”并允许清理记录。
- release 包版本升级到 `versionCode 2` / `versionName 1.1` 时，保持使用同一个 keystore。
- 为项目补充 Gradle Wrapper，减少新电脑环境配置成本。
- 如果要公开仓库，移除 keystore 和签名密码，只保留签名配置模板。
- 真机测试覆盖：
  - 覆盖安装后下载记录是否保留。
  - 下载后打开、分享、删除是否正常。
  - 多会话通知是否按会话分别更新。
  - 悬浮窗深色/浅色/跟随系统是否一致。
  - 扫码权限首次授权后是否正常进入扫码页。

## 10. 给继续开发 AI 的提示

继续修改前，建议优先阅读：

1. `AI-开发交接说明.md`
2. `GPTMini-应用介绍与使用帮助.md`
3. `app/src/main/java/com/coimgrain/codexminiapp/MainActivity.java`
4. `app/src/main/res/values/strings.xml`
5. `app/build.gradle`

修改 UI 前建议先搜索相关方法：

- `buildWelcomeView`
- `renderDownloads`
- `showDownloadsPanel`
- `buildMiniMenu`
- `showFloatSettings`
- `showNotificationSettings`
- `startHttpDownload`
- `saveDataUrlDownload`
- `persistDownloads`
- `loadPersistedDownloads`

打包前建议执行：

```bash
./scripts/build-debug.sh
./scripts/build-release.sh
```

验证 release APK：

```bash
.tools/android-sdk/build-tools/35.0.0/apksigner verify --verbose "app/build/outputs/apk/release/GPT Mini-1.0-release.apk"
.tools/android-sdk/build-tools/35.0.0/zipalign -c -p 4 "app/build/outputs/apk/release/GPT Mini-1.0-release.apk"
```

如果新电脑没有 `.tools`，请改用新电脑 Android SDK 对应的 `apksigner` 和 `zipalign` 路径。
