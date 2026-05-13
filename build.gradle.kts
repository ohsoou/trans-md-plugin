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
        pluginVerifier()
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
            <h2>Trans Md</h2>
            <p>Adds one-click translation to the JetBrains Markdown preview, powered by Google Translate.</p>
            <h3>Features</h3>
            <ul>
                <li>Translated Preview tab next to the standard Markdown preview — no separate window needed</li>
                <li>Side-by-side source editor and translated preview in a single split panel</li>
                <li>Translate and Refresh buttons added directly to the Markdown toolbar</li>
                <li>Translates in chunks so large files work reliably</li>
                <li>Preserves code blocks, inline code, URLs, front matter, and math blocks ($$) untranslated</li>
                <li>Caches translation results per-session to avoid redundant API calls</li>
                <li>API key stored securely in the OS keychain via the IDE credential store</li>
                <li>Target language configurable from Settings → Tools → Trans Md</li>
            </ul>
            <h3>Requirements</h3>
            <ul>
                <li>Google Translate API key with the Cloud Translation API enabled</li>
            </ul>
        """.trimIndent()

        changeNotes = """
            <h2>0.2.0</h2>
            <ul>
                <li>Preview now uses the built-in JetBrains Markdown renderer by default, falling back to commonmark-java when unavailable — output matches the standard Markdown preview</li>
                <li>Block math expressions (<code>$$ ... $$</code>) are now preserved through translation</li>
                <li>Translation pipeline refactored into a typed coroutine Flow — progress states and error reasons are now distinct values instead of exceptions</li>
                <li>Settings: API key is now loaded off the EDT to avoid Keychain-related IDE freezes on macOS</li>
            </ul>
            <h2>0.1.0</h2>
            <ul>
                <li>Initial release</li>
                <li>Translated Preview tab with side-by-side source and translated Markdown</li>
                <li>Google Translate integration with chunked translation for large files</li>
                <li>Preserves code blocks, inline code, URLs, and YAML/TOML front matter</li>
                <li>Per-session translation cache</li>
                <li>API key stored in OS keychain</li>
            </ul>
        """.trimIndent()
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}
