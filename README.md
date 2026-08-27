# LauncherPluginHost

An Android Launcher Host application that supports dynamic loading of widget plugins using a custom plugin architecture.

## Project Structure

This project is divided into several modules:

- **`host`**: The main Launcher application (`com.android.launcher3`). it acts as the plugin host and handles the lifecycle of various plugins.
- **`plugin-api`**: Defines the shared interfaces and contracts between the host and the plugins.
- **`media-plugin`**: An example plugin providing a media playback control widget.
- **`weather-plugin`**: An example plugin providing a weather information widget.

## Key Features

- **Plugin Architecture**: Decouples widget implementation from the main launcher process.
- **Dynamic Loading**: Host can discover and load plugins from separate APKs.
- **System API Access**: The host module uses a provided `framework.jar` to access `@hide` system APIs, allowing for deep integration.
- **Signed with Platform Key**: Configured to be signed with a platform signature (`keystore/platform.p12`).

## Getting Started

### Prerequisites

- Android SDK with API 36 platforms.
- Java 11 or higher for the build process.

### Build Instructions

To build the project and generate the APKs, run:

```bash
./gradlew assembleDebug
```

The output APKs will be located in the respective module `build/outputs/apk/debug/` directories.

### Signing

The project is configured to use a platform signing key located at `keystore/platform.p12`.
- **Password**: `android`
- **Alias**: `platform`

## Development

### Adding a New Plugin

1. Implement the interfaces defined in the `plugin-api` module.
2. In your plugin's `AndroidManifest.xml`, use the appropriate intent filters (e.g., `com.android.systemui.action.PLUGIN_WIDGET_VIEW`).
3. Ensure the plugin is signed with the same platform key as the host.

### Accessing Hidden APIs

The `host` module is configured to place `libs/framework.jar` at the beginning of the `bootstrapClasspath` during compilation. This allows the use of internal Android APIs.
