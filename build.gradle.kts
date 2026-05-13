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
            sinceBuild = "243"
        }
        description = """
            <img src="https://github.com/user-attachments/assets/2aa167ff-d952-4218-bc8a-b46a9976f9eb" width="800"/>
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

            <hr/>

            <h2>Trans Md</h2>
            <p>Google Translate 기반으로 JetBrains Markdown 미리보기에 원클릭 번역 기능을 추가합니다.</p>
            <h3>기능</h3>
            <ul>
                <li>기본 Markdown 미리보기 옆에 번역 미리보기 탭 추가 — 별도 창 불필요</li>
                <li>소스 에디터와 번역 미리보기를 하나의 분할 패널에서 나란히 표시</li>
                <li>Markdown 툴바에 번역(Translate) 및 새로고침(Refresh) 버튼 직접 추가</li>
                <li>대용량 파일도 안정적으로 처리하기 위해 청크 단위로 번역</li>
                <li>코드 블록, 인라인 코드, URL, 프런트 매터, 수식 블록($$)은 번역하지 않고 그대로 유지</li>
                <li>세션 내 번역 결과를 캐시하여 불필요한 API 호출 방지</li>
                <li>API 키는 IDE 자격증명 저장소를 통해 OS 키체인에 안전하게 보관</li>
                <li>Settings → Tools → Trans Md에서 대상 언어 설정 가능</li>
            </ul>
            <h3>요구사항</h3>
            <ul>
                <li>Cloud Translation API가 활성화된 Google Translate API 키</li>
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
