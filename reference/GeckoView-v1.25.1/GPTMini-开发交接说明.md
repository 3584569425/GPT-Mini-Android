# GPT Mini Android 开发交接说明

本文档用于把当前项目交给另一台电脑或另一个 AI 继续开发。它记录当前项目状态、已实现功能、关键文件、构建方式、签名信息和后续开发注意事项。

## 1. 项目概况

项目名称：GPT Mini

项目类型：Android 原生 App（Mozilla GeckoView 浏览器内核）

主要语言：Java

构建系统：Gradle / Android Gradle Plugin

当前版本：

- applicationId：`app.gptmini`
- versionCode：`22`
- versionName：`1.25.1`
- App 显示名称：`GPT Mini`

当前定位：

这是一个为 Codex Mini WebUI 优化的 Android 手机 App。核心仍是加载 WebUI，但浏览器内核已经从系统 Android WebView 迁移到 Mozilla GeckoView，主要用于改善 WebUI 原版液态玻璃在 Android Chromium 内核上的滑动卡顿，同时继续提供键盘、下载、通知、连接切换、首页入口等原生层优化。

## 2. 目录结构

重要目录和文件：

- `app/src/main/java/com/coimgrain/codexminiapp/MainActivity.java`
  - App 主逻辑。
  - 包含首页、GeckoView 容器、悬浮窗、下载界面、通知、局域网探测、下载记录持久化等功能。

- `app/src/main/java/com/coimgrain/codexminiapp/AIMiniGeckoEngine.java`
  - GeckoRuntime 和内置 WebExtension 管理。
  - 负责 JavaScript 执行及页面与原生层通信。

- `app/src/main/java/com/coimgrain/codexminiapp/AIMiniGeckoView.java`
  - GeckoView 会话封装。
  - 负责导航、手机/桌面模式、文件选择、下载响应和生命周期。

- `app/src/main/java/com/coimgrain/codexminiapp/AIMiniNotificationService.java`
  - 常驻通知前台服务。
  - 选择常驻模式后立即显示“空闲中”，并降低 App 在后台时任务监听被系统暂停的概率。

- `app/src/main/assets/extensions/ai-mini/`
  - GeckoView 内置 WebExtension。
  - 负责任务状态、键盘辅助、下载和项目字体注入等页面桥接功能。

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

- `GPTMini-开发交接说明.md`
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

### 3.1 GeckoView 基础功能

- 加载 Codex Mini WebUI。
- 支持 HTTP 和 HTTPS。
- 支持文件选择。
- 支持页面权限请求。
- 支持 WebUI 下载触发。
- 支持页面内通知状态回传到原生通知。
- 支持手机模式和桌面模式。
- 支持页面内链接在 App 临时网页层打开。
- WebUI 原版液态玻璃不再使用 Chromium WebView 降级脚本，由 Gecko 原生渲染。

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

已处理 Android 手机浏览器容器中常见的输入框和键盘问题：

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
- 可开启液态玻璃样式。

悬浮窗面板按钮名称以实际 UI 为准：

- 下载管理。
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

任务通知模式有两种：

- 实时通知。
- 仅任务完成时通知。

仅任务完成时通知：

- 不显示任务运行中的动态状态。
- 只有任务完成、失败或异常时发送结果通知。

实时通知：

- App 打开后使用前台服务维持基础常驻通知。
- 没有任务时显示空闲状态。
- 任务运行、完成、失败/异常时更新通知。
- 原生层会尝试通过任务状态接口轮询后台任务，不应依赖 WebUI 保持前台。

多任务通知：

- 每个会话任务使用独立通知。
- 同一个会话的运行中、完成、失败/异常会覆盖更新同一个通知，不重复新建多个通知。

后台横幅提醒：

- 任务完成、失败或异常时，如果 App 不在前台，会通过高重要级别通知频道显示类似 QQ/微信的顶部横幅提醒。
- 如果用户正在 App 界面内，只更新状态栏通知，不显示顶部横幅。
- “实时通知”和“仅任务完成时通知”两种模式在后台任务结束时都应触发结果提醒。

当前仍需重点验证：部分系统环境下，App 长时间处于后台后原生任务轮询可能停滞，导致必须重新打开 App、等待 WebUI 刷新最终回复后才出现完成通知。后续开发应优先检查 `AIMiniNotificationService`、`TaskMonitorJobService`、持久化任务信息和后台 HTTP 请求日志。

### 3.9 App 内临时网页

WebUI 中点击普通链接或 `target="_blank" / window.open` 链接时，会在 App 内打开临时网页层。

临时网页层：

- 覆盖显示在原 WebUI 上方。
- 原 WebUI 保持当前会话和页面状态。
- 悬浮窗会显示“关闭当前网页”。
- 关闭时会停止加载、移除 WebView 并调用 `destroy()`，不会把临时网页长期留在后台占用资源。
- 支持手机模式和桌面模式切换。
- 系统返回键会优先在临时网页内后退；没有历史记录时关闭临时网页并返回原 WebUI。

### 3.10 局域网线路辅助识别

App 不会直接把 WebView 地址栏改成局域网地址。

当前策略：

- WebView 页面地址仍保持用户输入的公网 URL。
- 原生层探测局域网可用性。
- 探测可用后，辅助 WebUI 使用本地线路。
- 尽量让 WebUI 自己的右上角状态显示真实线路状态。

这不是系统浏览器的完全原生自动切换，而是 App 针对该 WebUI 做的辅助识别和注入。

### 3.11 纯文本/错误页适配

对一些纯文本、JSON、错误信息页面做了移动端显示适配。

目标：

- 避免超长文本撑出屏幕。
- 避免横向超宽显示。
- 让错误信息在手机屏幕上可读。

## 4. 当前 release 包状态

当前版本使用 GeckoView，正式包仅构建 `arm64-v8a`。

已验证：

- release v1/v2 签名通过。
- zipalign 通过。
- 包名：`app.gptmini`
- 版本：`1.25.1`

如果用户之前安装的是 debug 包，可能因为签名不同不能直接覆盖安装，需要先卸载 debug 版。

之后只要一直使用当前 release 证书打包，就可以正常覆盖升级。

## 5. 构建说明

当前项目没有提交 Gradle Wrapper，而是使用本机 `.tools` 目录里的 Gradle、JDK 和 Android SDK。

本源码压缩包通常不包含 `.tools`，因为该目录很大且属于本机工具依赖。迁移到新电脑后可选两种方式：

### 方式一：使用 Android Studio

1. 用 Android Studio 打开项目根目录。
2. 安装 Android SDK 36。
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
- `.tools/gradle/gradle-8.11.1/bin/gradle`

如果新电脑没有 `.tools`，脚本需要改成系统自己的 `JAVA_HOME`、`ANDROID_HOME` 和 `gradle`。

当前依赖的 GeckoView Maven 包已经缓存在：

- `.tools/local-maven/`

GeckoView 152 要求最低 Android 8.0/API 26。由于 GeckoView 自带 native 内核，不能再按原系统 WebView 版本的几 MB 体积预估 APK。

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

- App 显示名称、首页、悬浮窗和通知文案统一为 GPT Mini。
- App、首页、悬浮窗与 Launcher 图标统一使用 GPT Mini 图标。
- 修复 GeckoView 输入框合成层导致液态玻璃丢失的问题，并保持输入框随键盘升降。
- 完成 WebUI 全屏延伸到状态栏和刘海区域，顶部安全区域继续允许用户自定义。
- 修复浅色模式下悬浮窗文字对比度。
- 修复 HTML、Blob、Data URL、APK 等 WebUI 下载处理。
- 缓解 App 返回前台、页面加载和模式切换时的闪白。
- 增加原生任务状态轮询、前台服务与任务补充唤醒机制。
- 修复任务正常完成前短暂误报“失败/异常”的问题。
- 修复“任务结束通知”与“常驻通知”切换不能立即生效的问题。
- 新增后台任务完成、失败/异常顶部横幅通知。
- 新增 App 内临时网页层。
- 新增临时网页手机模式/桌面模式切换。
- 新增悬浮窗“关闭当前网页”入口。
- 新增 release 签名配置。
- 生成 release 签名证书。
- 打包 `GPT Mini v1.25.1 arm64-v8a release`。
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
- 后续升级版本时继续递增 `versionCode`，并保持使用同一个 keystore。
- 为项目补充 Gradle Wrapper，减少新电脑环境配置成本。
- 公开仓库不得提交 keystore、签名密码、`local.properties`、真实 WebUI 地址和连接 Token。
- 优先修复 App 长时间处于后台时任务完成通知延迟的问题。
- 真机测试覆盖：
  - 覆盖安装后下载记录是否保留。
  - 下载后打开、分享、删除是否正常。
  - 多会话通知是否按会话分别更新。
  - 悬浮窗深色/浅色/跟随系统是否一致。
  - 扫码权限首次授权后是否正常进入扫码页。

## 10. 给继续开发 AI 的提示

继续修改前，建议优先阅读：

1. `GPTMini-开发交接说明.md`
2. `GPTMini-应用介绍与使用帮助.md`
3. `app/src/main/java/com/coimgrain/codexminiapp/MainActivity.java`
4. `app/src/main/java/com/coimgrain/codexminiapp/AIMiniNotificationService.java`
5. `app/src/main/assets/extensions/ai-mini/page.js`
6. `输入框液态玻璃补丁说明.md`
7. `app/src/main/res/values/strings.xml`
8. `app/build.gradle`

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
.tools/android-sdk/build-tools/35.0.0/apksigner verify --verbose "app/build/outputs/apk/release/GPT Mini-1.15-arm64-v8a-release.apk"
.tools/android-sdk/build-tools/35.0.0/zipalign -c -p 4 "app/build/outputs/apk/release/GPT Mini-1.15-arm64-v8a-release.apk"
```

如果新电脑没有 `.tools`，请改用新电脑 Android SDK 对应的 `apksigner` 和 `zipalign` 路径。
