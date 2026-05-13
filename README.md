# Trans MD

A JetBrains IDE plugin that adds one-click translation to the Markdown preview.

Open any `.md` file and switch to the **Translated Preview** tab — the content is translated via Google Translate and rendered as HTML side-by-side with the source editor.

<img width="1339" height="754" alt="Image" src="https://github.com/user-attachments/assets/2aa167ff-d952-4218-bc8a-b46a9976f9eb" />

## Features

- **Translated Preview tab** — appears next to the default Preview tab for every `.md` file
- **Google Translate backend** — translation is done chunk-by-chunk (split on heading boundaries) to stay within API limits
- **Placeholder preservation** — fenced code blocks, inline code, URLs, and front matter are excluded from translation and restored verbatim afterward
- **Session-level LRU cache** — repeated views of the same file/language combination skip redundant API calls
- **Configurable target language** — set the BCP-47 target language code (default: `ko`) in IDE Settings

## Requirements

- IntelliJ IDEA 2023.3+ (Community or Ultimate)
- A Google Translate API key

## Installation

1. Build the plugin:
   ```bash
   ./gradlew buildPlugin
   ```
2. In the IDE: **Settings → Plugins → Install Plugin from Disk** and select the `.zip` from `build/distributions/`.

## Configuration

**Settings → Tools → Trans MD**

| Field | Description | Default |
|-------|-------------|---------|
| Google Translate API Key | Your GCP API key with the Cloud Translation API enabled | _(empty)_ |
| Target Language | BCP-47 language code for the translation output | `ko` |

## Usage

1. Open any `.md` file in the IDE.
2. Click the **Translated Preview** tab in the editor tab bar.
3. The plugin preprocesses the document, translates each section, and renders the result as HTML.
4. Use the **Refresh** action in the toolbar to re-translate after editing.

## Architecture

```
User opens .md file
        │
        ▼
TranslatedPreviewFileEditorProvider
  └─ registers "Translated Preview" tab
        │
        ▼
TranslatedPreviewFileEditor  (JSplitPane: left = source editor, right = translated HTML)
  └─ selectNotify() → starts Translation Job (coroutine)
        │
        ├─ 1. MarkdownPreprocessor.preprocess()   → sanitizedText + placeholders
        ├─ 2. Split by heading boundaries          → List<Chunk>
        ├─ 3. GoogleTranslateService.translate()   per chunk (sequential)
        ├─ 4. MarkdownPreprocessor.restore()       → full translated Markdown
        ├─ 5. JetBrains preview render             → preferred HTML fragment
        │    └─ fallback: commonmark-java render
        └─ 6. MarkdownJCEFHtmlPanel.setHtml()      [EDT]
```

## Tech Stack

| Library | Purpose |
|---------|---------|
| [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/) | Plugin framework |
| [OkHttp 4](https://square.github.io/okhttp/) | HTTP client for Google Translate API |
| JetBrains bundled Markdown plugin | Preferred preview rendering to match the built-in Markdown preview |
| [commonmark-java](https://github.com/commonmark/commonmark-java) | Stable fallback renderer when internal preview APIs break |
| [Gson](https://github.com/google/gson) | JSON parsing for API responses |

## Development

```bash
./gradlew runIde       # launch a sandboxed IDE instance with the plugin loaded
./gradlew test         # run unit tests
./gradlew buildPlugin  # produce the distributable zip
```

## License

MIT
