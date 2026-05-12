# PRD: Markdown Translator — JetBrains Plugin

Status: ready-for-agent

---

## Problem Statement

개발자들은 JetBrains IDE에서 Markdown 파일을 자주 열람한다. 기술 문서, README, 설계 문서 등 대부분이 영어로 작성되어 있으며, 한국어 사용자는 내용을 이해하기 위해 브라우저를 열어 별도로 번역해야 한다. IDE를 떠나지 않고 Markdown 미리보기 안에서 바로 번역된 내용을 볼 수 있는 방법이 없다.

---

## Solution

JetBrains 플러그인 "Trans MD"는 Markdown 미리보기 툴바에 번역 버튼을 추가한다. 버튼을 클릭하면 "Translated Preview" 탭이 열리고, 원본 Markdown이 선택한 언어로 번역되어 렌더링된다. 코드 블록, URL, Front matter는 번역에서 제외되어 내용이 깨지지 않는다. 타겟 언어는 툴바 팝업에서 즉시 변경할 수 있다.

---

## User Stories

1. As a Korean developer, I want to see a translated Markdown preview tab next to the original preview, so that I can read English documentation without leaving the IDE.
2. As a user, I want the translated preview tab to open automatically when I click the translate button, so that I don't have to navigate through menus.
3. As a user, I want the source language to be auto-detected, so that I don't need to manually specify it every time.
4. As a user, I want to select the target language from a toolbar popup, so that I can quickly switch between languages without opening settings.
5. As a user, I want Korean to be the default target language, so that I can start using the plugin immediately without configuration.
6. As a user, I want the top 7 languages (Korean, English, Japanese, Simplified Chinese, Spanish, French, German) to appear at the top of the language list, so that I can reach common languages in one click.
7. As a user, I want a "Other languages..." option in the popup, so that I can access less common languages when needed.
8. As a user, I want a refresh button in the toolbar, so that I can retranslate after editing the source file.
9. As a user, I want an in-progress translation to be cancelled when I click refresh, so that I don't have to wait for the old translation to finish.
10. As a user, I want to see "⏳ 번역 중... (N/M 섹션)" while translation is in progress, so that I know the plugin is working and how far along it is.
11. As a user, I want code blocks (fenced and inline) to remain untranslated, so that code is never corrupted by the translation process.
12. As a user, I want URLs inside Markdown links to remain untranslated, so that links don't break after translation.
13. As a user, I want YAML/TOML front matter to remain untranslated, so that metadata fields are not corrupted.
14. As a user, I want large Markdown files to be translated in sections (split by headings), so that translation works reliably on long documents.
15. As a user, I want to see an inline error message with a "Open Settings" link when my API key is missing or invalid, so that I know exactly what to fix without searching through menus.
16. As a user, I want to configure my Google Translate API key in Settings → Tools → Markdown Translator, so that I can provide my own API credentials.
17. As a user, I want my API key to be stored securely using the OS keychain, so that it is not exposed in plaintext config files.
18. As a user, I want the plugin to work in IntelliJ IDEA, PyCharm, WebStorm, GoLand, and all other JetBrains IDEs, so that I don't need to install different plugins for different IDEs.
19. As a user, I want the plugin to work on IDE version 2023.3 and later, so that I don't need to upgrade my IDE to use the plugin.
20. As a user, I want previously translated content to be cached during the session, so that switching between tabs doesn't trigger redundant API calls.
21. As a user, I want the translated preview to use a clean, readable HTML layout, so that the rendered output is easy to read.
22. As a user, I want the plugin to handle environments where the JCEF browser is unavailable (some Linux setups), so that the plugin degrades gracefully instead of crashing.
23. As a developer, I want the plugin to be open source under Apache 2.0, so that I can contribute or fork it freely.
24. As a developer, I want the plugin to be available on the JetBrains Marketplace for free, so that I can install it with one click.

---

## Implementation Decisions

### Modules

**MarkdownPreprocessor** (deep module — pure, stateless)
- Input: raw Markdown string
- Output: `PreprocessedMarkdown(sanitized: String, placeholders: Map<String, String>)`
- Extracts fenced code blocks, inline code spans, bare URLs, and front matter; replaces each with a stable placeholder token (`__PLACEHOLDER_N__`)
- Provides a `restore(translated: String, placeholders: Map)` function to reinsert originals after translation
- Uses commonmark-java AST to locate code nodes precisely; regex for front matter

**TranslationService** (interface, deep module)
```
interface TranslationService {
    suspend fun translate(text: String, sourceLang: String, targetLang: String): Result<String>
    val providerId: String
}
```
- `GoogleTranslateService` implements this via Google Cloud Translation API v2 Basic (`/language/translate/v2`)
- All HTTP calls run on `Dispatchers.IO`
- Large documents are split on `##`/`###` heading boundaries before calling this interface; each chunk is translated sequentially

**TranslationCache** (deep module — pure data structure)
- LRU, 200 entries, app-scoped singleton
- Key: `"$providerId:$sourceLang:$targetLang:${content.hashCode()}"`
- Invalidation: none (session-only, clears on IDE restart)

**TranslatedPreviewFileEditor** (stateful UI module)
- Embeds a `JBCefBrowser`; falls back to `JTextPane(contentType="text/html")` when `JBCefApp.isSupported()` returns false
- Holds a coroutine `Job`; cancels it on `Job.cancel()` before starting a new translation
- `selectNotify()` triggers translation on first open
- Renders loading HTML immediately, replaces with translated HTML on completion, or inline error HTML on failure
- Uses commonmark-java to convert the final translated Markdown to HTML for rendering

**TranslatedPreviewFileEditorProvider**
- Registered with `FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR` so "Translated Preview" appears alongside the native "Preview" tab
- `accept()` limited to files of `MarkdownFileType`

**TranslateMarkdownAction** (toolbar button)
- Registered in `Markdown.Toolbar.Right` action group
- Label shows current state: `🌐 Auto → KO`
- On click: opens `JBPopupFactory.createActionGroupPopup()` with top-7 language list + "Other languages…" separator item
- Selecting a language updates `TransMdSettings.targetLang` and triggers retranslation of the current file if "Translated Preview" tab is already open

**RefreshTranslationAction** (toolbar button)
- Registered in `Markdown.Toolbar.Right` action group after `TranslateMarkdownAction`
- On click: cancels active `Job` in `TranslatedPreviewFileEditor` and triggers fresh translation

**TransMdSettings** (`PersistentStateComponent`)
- Persists: `targetLang` (default `"ko"`), `sourceLang` (always `"auto"`)
- API key stored via `PasswordSafe` (OS keychain), not in the XML state
- Exposes `getInstance()` companion for access from any component

**TransMdSettingsConfigurable** (Settings UI)
- Path: Settings → Tools → Markdown Translator
- Fields: Google Translate API key (password field), target language dropdown
- "Test Connection" button: translates a short test string and shows success/failure inline

### Key Architectural Decisions

- **commonmark-java bundled in plugin**: avoids depending on the internal `intellij.markdown` library which has no stability guarantee. Adds ~200KB to plugin size.
- **OkHttp bundled**: single shared `OkHttpClient` instance (application service) to share the connection pool across translation calls.
- **Sequential chunking, not parallel**: chunks are translated one-by-one to stay within Google's free-tier rate limits (100 req/min). Chunk boundaries are Markdown heading lines (`^#{1,6} `).
- **Placeholder strategy for exclusions**: front matter, fenced code, inline code, and bare URLs are each replaced with `__PLACEHOLDER_N__` before translation and restored afterward. This is simpler and more reliable than post-processing the translated HTML.
- **`PLACE_AFTER_DEFAULT_EDITOR` policy**: preserves the native Preview tab; our tab is additive. This avoids replacing or conflicting with the bundled Markdown plugin's editor.
- **PasswordSafe for API keys**: OS keychain integration prevents API keys from appearing in `TransMdSettings.xml` in the IDE config directory.

---

## Testing Decisions

A good test covers observable behavior through the module's public interface only. Tests must not assert on internal data structures, private methods, or implementation class names.

### Modules to test

**MarkdownPreprocessor** — highest priority
- Pure input/output, no IDE dependencies, fast
- Test: fenced code blocks are replaced with placeholders and correctly restored
- Test: inline code spans are replaced and restored
- Test: URLs inside `[text](url)` are preserved verbatim
- Test: front matter (`---` block) is excluded from the sanitized output
- Test: a document with no exclusions passes through unchanged
- Test: `restore()` is the left-inverse of `sanitize()`

**TranslationCache**
- Test: a cache hit returns the stored value
- Test: entries beyond capacity (200) evict the least-recently-used entry
- Test: keys with different providers/languages are distinct

**GoogleTranslateService** (with `MockWebServer`)
- Test: a successful response is parsed and returned as `Result.success`
- Test: a 403 response (bad API key) is returned as `Result.failure` with a descriptive message
- Test: a network timeout is returned as `Result.failure`
- Test: text above 5000 chars is split into multiple requests

No tests are planned for `TranslatedPreviewFileEditor`, `TranslateMarkdownAction`, or `TransMdSettingsConfigurable` in v1 — these require a running IDE platform and will be verified manually via `./gradlew runIde`.

---

## Out of Scope

- **DeepL, Papago, LibreTranslate support** — Google Translate only in v1. The `TranslationService` interface is designed for future providers.
- **Real-time / auto-translate on save** — translation is triggered manually only.
- **Split panel view (original + translated side by side)** — separate "Translated Preview" tab is sufficient for v1.
- **Disk-based translation cache** — session memory cache only; persistence across restarts is not needed in v1.
- **Mermaid diagram translation** — diagram source is treated as a code block and excluded from translation.
- **Configurable source language in toolbar** — source is always Auto; only target language is selectable in the toolbar popup.
- **Paid / freemium tier** — plugin is entirely free and open source.

---

## Further Notes

- Plugin ID `io.github.ohsoou.trans-md` is permanent once submitted to the Marketplace and cannot be changed.
- The `Markdown.Toolbar.Right` action group ID should be verified at runtime via `./gradlew runIde` before finalising the `plugin.xml` registration — the group ID may differ across IDE versions.
- Google Cloud Translation API v2 Basic requires a billing-enabled GCP project even for free-tier usage. The settings UI should surface a direct link to the GCP console for first-time setup.
- `since-build = "233"` (IntelliJ 2023.3) is the minimum. `until-build` is intentionally omitted to avoid blocking future IDE versions.