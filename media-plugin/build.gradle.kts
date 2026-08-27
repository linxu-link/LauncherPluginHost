import java.util.Properties

plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.plugin.media"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.plugin.media"
        minSdk = 33
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        // -bootclasspath 仅在 -source 8 下可用，必须保持 1.8
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    signingConfigs {
        create("platform") {
            storeFile = rootProject.file("keystore/platform.p12")
            keyAlias = "platform"
            keyPassword = "android"
            storePassword = "android"
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("platform")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("platform")
        }
    }
}

dependencies {
    // The plugin must NOT bundle the interface library: those classes are provided by the host
    // process's classloader via the ClassLoaderFilter at runtime.
    compileOnly(project(":plugin-api"))
}

// 将 framework.jar 置于 javac bootclasspath 最前，使 @hide 系统 API 可编译
afterEvaluate {
    val props = Properties().apply {
        rootProject.file("local.properties").inputStream().use { load(it) }
    }
    val androidJar = File(props.getProperty("sdk.dir"), "platforms/android-36/android.jar")
    tasks.withType<JavaCompile>().configureEach {
        val base = options.bootstrapClasspath ?: files(androidJar)
        options.bootstrapClasspath = files(rootProject.file("host/libs/framework.jar")) + base
    }
}
