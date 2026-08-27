# LauncherPluginHost

一个支持使用自定义插件架构动态加载组件插件（Widget Plugins）的 Android Launcher 宿主应用。

## 项目结构

本项目分为以下几个模块：

- **`host`**: 主 Launcher 应用 (`com.android.launcher3`)。它作为插件宿主，管理各种插件的生命周期。
- **`plugin-api`**: 定义了宿主与插件之间的共享接口和契约。
- **`media-plugin`**: 提供媒体播放控制组件的示例插件。
- **`weather-plugin`**: 提供天气信息组件的示例插件。

## 核心功能

- **插件化架构**: 将组件的实现与主 Launcher 进程解耦。
- **动态加载**: 宿主可以从独立的 APK 中发现并加载插件。
- **系统 API 访问**: `host` 模块使用提供的 `framework.jar` 来访问 `@hide` 系统 API，从而实现深度集成。
- **平台签名**: 配置为使用平台签名进行签名 (`keystore/platform.p12`)。

## 快速入门

### 前置条件

- Android SDK (包含 API 36 平台)。
- JDK 11 或更高版本。

### 编译说明

运行以下命令编译项目并生成 APK：

```bash
./gradlew assembleDebug
```

生成的 APK 将位于各模块的 `build/outputs/apk/debug/` 目录下。

### 签名配置

项目配置为使用位于 `keystore/platform.p12` 的平台签名密钥：
- **密码**: `android`
- **别名**: `platform`

## 开发指南

### 添加新插件

1. 实现 `plugin-api` 模块中定义的接口。
2. 在插件的 `AndroidManifest.xml` 中使用对应的 Intent Filter (例如 `com.android.systemui.action.PLUGIN_WIDGET_VIEW`)。
3. 确保插件与宿主使用相同的平台密钥签名。

### 访问隐藏 API

`host` 模块配置为在编译期间将 `libs/framework.jar` 置于 `bootstrapClasspath` 的最前面。这允许开发者在代码中使用 Android 内部 API。
