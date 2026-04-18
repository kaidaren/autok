import java.net.URI

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-kapt")
    id("de.undercouch.download") version "5.6.0"
}
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(versions.javaVersionInt))
    }
}
android {
    namespace = "com.aiselp.autojs.codeeditor"
    compileSdk = versions.compile

    defaultConfig {
        minSdk = versions.mini

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = compose_version
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.constraintlayout)
    implementation(libs.kotlinx.coroutines.android)
    api(libs.androidx.webkit)
    implementation(libs.google.gson)
    implementation(libs.core.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(project(":autojs"))

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

abstract class DownloadEditorTask : DefaultTask() {
    @get:Input
    abstract val tag: Property<String>

    @get:OutputDirectory
    abstract val assetsDir: DirectoryProperty

    @TaskAction
    fun download() {
        val tagValue = tag.get()
        val dir = assetsDir.get().asFile
        val versionFile = File(dir, "version.txt")
        dir.mkdirs()
        if (versionFile.isFile && versionFile.readText() == tagValue) {
            logger.lifecycle("downloadEditor: already up to date, skipping")
            return
        }
        val url = URI("https://github.com/aiselp/vscode-mobile/releases/download/$tagValue/dist.zip").toURL()
        val destFile = File(dir, "dist.zip")
        logger.lifecycle("downloadEditor: downloading $url")
        url.openStream().use { input ->
            destFile.outputStream().use { output -> input.copyTo(output) }
        }
        versionFile.writeText(tagValue)
        logger.lifecycle("downloadEditor: complete")
    }
}

tasks.register<DownloadEditorTask>("downloadEditor") {
    tag.set("v0.4.0")
    assetsDir.set(layout.projectDirectory.dir("src/main/assets/codeeditor"))
}
tasks.findByName("preBuild")?.dependsOn("downloadEditor")
tasks.findByName("preDebugBuild")?.dependsOn("downloadEditor")