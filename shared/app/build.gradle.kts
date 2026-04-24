import buildlogic.versioning.getAppDataDirName
import buildlogic.versioning.getAppName
import buildlogic.versioning.getAppVersionString
import buildlogic.versioning.getApplicationPackageName
import buildlogic.versioning.getPrettifiedAppName
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id(MyPlugins.kotlinMultiplatform)
    id(MyPlugins.composeBase)
    id(Plugins.Kotlin.serialization)
    id(Plugins.Android.library)
    id(Plugins.buildConfig)
}
kotlin {
    jvm("desktop")
    androidTarget("android") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
    sourceSets {
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.ui)

            api(project(":downloader:core"))
            api(project(":downloader:monitor"))

            api(project(":shared:config"))
            api(project(":shared:utils"))
            api(project(":shared:compose-utils"))
            api(project(":shared:resources"))
            api(project(":shared:auto-start"))
            api(project(":shared:updater"))

            api(libs.kotlin.coroutines.core)
            api(libs.kotlin.serialization.json)

            api(libs.decompose)
            api(libs.essenty.lifecycleCoroutines)
            api(libs.koin.core)

            api(libs.androidx.datastore)

            implementation(libs.kotlinFileWatcher)

            //because we don't have material design, but we use ripple effect
            implementation(libs.compose.material.rippleEffect)
            implementation(libs.compose.material.iconsExtended)

            // multiplatform scrollbars
            api(libs.fastscroller.core)
            api(libs.markdownRenderer.core)
            api(libs.compose.reorderable)
        }
        androidMain.dependencies {
            api(libs.androidx.core.ktx)
            api(libs.androidx.activity.compose)
        }
        val desktopMain by getting
        desktopMain.dependencies {
            implementation(libs.osThemeDetector.get().toString()) {
                exclude(group = "net.java.dev.jna")
            }
        }
    }
}

android {
    compileSdk = 36
    namespace = "com.neo.downloader.shared"
    defaultConfig {
        minSdk = 26
    }
}
// generate a file with these constants
buildConfig {
    packageName = "com.neo.downloader.shared"
    buildConfigField(
        "PACKAGE_NAME",
        provider {
            getApplicationPackageName()
        }
    )
    buildConfigField(
        "APP_DISPLAY_NAME",
        provider { getPrettifiedAppName() }
    )
    buildConfigField(
        "DATA_DIR_NAME",
        provider { getAppDataDirName() }
    )
    buildConfigField(
        "APP_VERSION",
        provider { getAppVersionString() }
    )
    buildConfigField(
        "APP_NAME",
        provider { getAppName() }
    )
    buildConfigField(
        "PROJECT_WEBSITE",
        provider {
            "https://neo.downloader.com"
        }
    )
    buildConfigField(
        "PROJECT_SOURCE_CODE",
        provider {
            "https://github.com/amir1376/neo-downloader"
        }
    )
    buildConfigField(
        "DONATE_LINK",
        provider {
            "https://github.com/amir1376/neo-downloader/blob/master/DONATE.md"
        }
    )
    buildConfigField(
        "PROJECT_GITHUB_OWNER",
        provider {
            "amir1376"
        }
    )
    buildConfigField(
        "PROJECT_GITHUB_REPO",
        provider {
            "neo-downloader"
        }
    )
    buildConfigField(
        "PROJECT_TRANSLATIONS",
        provider {
            "https://crowdin.com/project/neo-downloader"
        }
    )
    buildConfigField(
        "INTEGRATION_CHROME_LINK",
        provider {
            "https://chromewebstore.google.com/detail/neo-downloader-brows/bbobopahenonfdgjgaleledndnnfhooj"
        }
    )
    buildConfigField(
        "INTEGRATION_FIREFOX_LINK",
        provider {
            "https://addons.mozilla.org/en-US/firefox/addon/neo-downloader/"
        }
    )
    buildConfigField(
        "TELEGRAM_GROUP",
        provider {
            "https://t.me/neo.downloader_discussion"
        }
    )
    buildConfigField(
        "TELEGRAM_CHANNEL",
        provider {
            "https://t.me/neo.downloader"
        }
    )
}
