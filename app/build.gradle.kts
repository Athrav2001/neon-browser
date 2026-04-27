import buildlogic.CiDirs
import buildlogic.CiUtils
import buildlogic.versioning.convertToVersionCode
import buildlogic.versioning.getAppName
import buildlogic.versioning.getAppVersion
import buildlogic.versioning.getAppVersionString
import buildlogic.versioning.getApplicationPackageName
import com.android.build.api.artifact.SingleArtifact
import ir.neo.plugin.common_android.task.androidEnableFileTypesGeneratorForManifest
import org.gradle.kotlin.dsl.registering
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    id(Plugins.Android.application)
    id(MyPlugins.kotlinAndroid)
    id(MyPlugins.composeAndroid)
    id(Plugins.ksp)
    id(Plugins.Kotlin.serialization)
    id(Plugins.aboutLibraries)
    id(Plugins.aboutLibrariesAndroid)
}
android {
    defaultConfig {
        minSdk = 26
        targetSdk = 36
        applicationId = getApplicationPackageName()
        versionCode = getAppVersion().convertToVersionCode()
        versionName = getAppVersionString()
    }
    compileSdk = 36
    namespace = "com.neo.downloader.android"
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            resValue("string", "app_short_name", "NEO")
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
kotlin.compilerOptions {
    jvmTarget = JvmTarget.JVM_21
}
dependencies {
    // Compose
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(platform("org.jetbrains.compose:compose-bom"))
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.compose.material.rippleEffect)
    implementation(libs.compose.reorderable)
    
    // Android
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.swipeRefreshLayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore)
    
    // Coroutines & Serialization
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.kotlin.coroutines.android)
    implementation(libs.kotlin.serialization.json)
    
    // OkHttp & Okio
    implementation(libs.okhttp.okhttp)
    implementation(libs.okio.okio)
    
    // Decompose
    implementation(libs.decompose)
    implementation(libs.decompose.jbCompose)
    implementation(libs.essenty.lifecycleCoroutines)
    
    // Koin
    implementation(libs.koin.core)
    implementation(libs.koin.compose)
    ksp(libs.koin.compiler)
    
    // Other
    implementation(libs.arrow.core)
    implementation(libs.arrow.optics)
    implementation(libs.fastscroller.core)
    implementation(libs.markdownRenderer.core)
    implementation(libs.aboutLibraries.core)
    
    // KSP
    ksp(libs.arrow.opticKsp)
}

androidEnableFileTypesGeneratorForManifest(
    targetActivityClass = ".pages.add.AddDownloadActivity",
    fileTypesFile = project.layout.projectDirectory.file("filetypes.txt")
)
