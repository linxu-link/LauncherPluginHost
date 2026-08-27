pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "LauncherPluginHost"
include(":host")
include(":plugin-api")
include(":media-plugin")
include(":weather-plugin")
