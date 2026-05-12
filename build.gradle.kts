import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
    id("org.jetbrains.intellij.platform") version "2.3.0"
}

group = providers.gradleProperty("group").get()
version = providers.gradleProperty("version").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity(providers.gradleProperty("platformVersion"))
        bundledPlugin("org.intellij.plugins.markdown")
        testFramework(TestFrameworkType.Platform)
    }
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.commonmark:commonmark:0.23.0")
    implementation("org.commonmark:commonmark-ext-gfm-tables:0.23.0")
    implementation("org.commonmark:commonmark-ext-gfm-strikethrough:0.23.0")
    implementation("com.google.code.gson:gson:2.11.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

intellijPlatform {
    pluginConfiguration {
        id = providers.gradleProperty("pluginId")
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("version")
        vendor {
            name = providers.gradleProperty("pluginVendorName")
            email = providers.gradleProperty("pluginVendorEmail")
        }
        ideaVersion {
            sinceBuild = "233"
        }
        description = """
            Adds one-click translation to the JetBrains Markdown preview.
            Open any .md file and use the Translated Preview tab — powered by Google Translate.
            Configure your target language directly from the preview toolbar.
        """.trimIndent()
    }
}