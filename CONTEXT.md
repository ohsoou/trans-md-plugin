# Trans MD — Domain Context

## What this plugin does

Trans MD는 JetBrains IDE의 Markdown 미리보기에 번역 기능을 추가하는 플러그인이다. `.md` 파일을 열면 "Translated Preview" 탭이 추가되고, 사용자가 탭을 열면 원본 Markdown을 지정한 언어로 번역하여 HTML로 렌더링한다.

---

## Glossary

이 프로젝트에서 쓰이는 용어는 아래 정의를 따른다. 동의어(avoid column)는 코드, 커밋, 이슈에서 사용하지 않는다.

| Term | Definition | Avoid |
|------|------------|-------|
| **Translation Job** | 하나의 파일에 대한 단일 번역 작업 전체. 전처리 → 청크 분할 → API 호출 → 복원 → 렌더링의 전 과정을 포함한다. | task, operation, request |
| **Chunk** | Heading 경계(`##`, `###`)로 분할된 Markdown 텍스트 단위. 하나의 번역 API 요청에 대응한다. | segment, part, piece |
| **Preprocessed Document** | 번역 불가 영역을 Placeholder로 치환한 뒤의 Markdown 문서. `sanitizedText`(번역할 텍스트)와 `placeholders`(복원용 맵)로 구성된다. | cleaned text, stripped markdown |
| **Placeholder** | 번역에서 제외할 원본 내용을 대체하는 토큰. 형식: `__PLACEHOLDER_N__`. 번역 완료 후 원본으로 복원된다. | token, marker, tag |
| **Exclusion** | 번역하지 않아야 하는 Markdown 요소의 총칭. 펜스 코드 블록, 인라인 코드, URL, Front matter가 해당된다. | skip, ignore |
| **Provider** | 번역 API 백엔드. 현재는 Google Translate만 구현. `TranslationService` 인터페이스로 추상화된다. | service, engine, backend |
| **Target Language** | 번역 결과 언어. BCP-47 코드로 저장 (예: `ko`, `en`, `ja`). 기본값은 `ko`. | destination language, output language |
| **Source Language** | 번역 입력 언어. 항상 `auto` (자동 감지). 사용자가 변경할 수 없다. | input language, from language |
| **Translated Preview** | 좌: 원본 소스 에디터, 우: 번역된 HTML을 나란히 보여주는 커스텀 `FileEditor` 탭. 기본 "Preview" 탭 옆에 추가된다. | translation tab, preview tab |
| **Translation Cache** | 세션 범위의 LRU 캐시. 동일 파일/언어/Provider 조합의 재번역을 방지한다. | result cache, response cache |
| **Front Matter** | Markdown 파일 상단의 `---`로 감싼 YAML/TOML 메타데이터 블록. 번역에서 제외되며, 미리보기에도 표시되지 않는다. | header, metadata |

---

## Architecture Overview

```
User opens .md file
        │
        ▼
TranslatedPreviewFileEditorProvider
  └─ registers "Translated Preview" tab (PLACE_AFTER_DEFAULT_EDITOR)
        │
        ▼
TranslatedPreviewFileEditor  (JSplitPane: 좌=소스 에디터, 우=번역 HTML)
  ├─ sourceEditor (EditorFactory.createEditor — 편집 가능)
  ├─ JBCefBrowser (or JTextPane fallback)
  └─ selectNotify() → starts Translation Job
        │
        ▼
Translation Job (coroutine, cancellable)
  ├─ 1. MarkdownPreprocessor.preprocess(rawText)
  │       → Preprocessed Document (sanitizedText + placeholders)
  ├─ 2. Split sanitizedText by heading boundaries → List<Chunk>
  ├─ 3. For each Chunk (sequential):
  │       GoogleTranslateService.translate(chunk, "auto", targetLang)
  │       → update loading state "⏳ 번역 중... (N/M 섹션)"
  ├─ 4. Join translated chunks
  ├─ 5. MarkdownPreprocessor.restore(translated, placeholders)
  ├─ 6. commonmark: MD → HTML
  └─ 7. browser.loadHTML(html)  [EDT]
```

---

## Threading Model

IntelliJ Platform의 threading 규칙을 반드시 따라야 한다. 위반 시 EDT assertion error가 발생한다.

| Context | What runs here |
|---------|----------------|
| **EDT** (Event Dispatch Thread) | UI 업데이트만. `browser.loadHTML()`, Swing 컴포넌트 변경 |
| **Dispatchers.IO** | 파일 읽기, HTTP 호출 (OkHttp), 모든 블로킹 I/O |
| **Coroutine scope** | Job 조율, 청크 순회 루프. `selectNotify()`에서 launch. |

규칙:
- `withContext(Dispatchers.Main.immediate)` 또는 `withContext(Dispatchers.EDT)` 로만 UI 업데이트
- `Job.cancel()` 은 EDT에서 안전하게 호출 가능
- OkHttp 호출은 반드시 `withContext(Dispatchers.IO)` 안에서

---

## Module Dependency Graph

```
TranslateMarkdownAction
RefreshTranslationAction
        │
        ▼
TranslatedPreviewFileEditor
        │
        ├──► MarkdownPreprocessor   (pure, no deps)
        │
        ├──► GoogleTranslateService
        │         └──► TransMdSettings (API key, targetLang)
        │         └──► TranslationCache
        │
        └──► commonmark-java        (bundled, MD→HTML)

TransMdSettingsConfigurable
        └──► TransMdSettings
```

---

## Placeholder Contract

`MarkdownPreprocessor`의 입출력 계약:

```
Input:  "# Title\n\n```python\nx = 1\n```\nSee [link](https://example.com)"

Sanitized:
        "# Title\n\n__PLACEHOLDER_0__\nSee [link](__PLACEHOLDER_1__)"

Placeholders:
        {
          "__PLACEHOLDER_0__": "```python\nx = 1\n```",
          "__PLACEHOLDER_1__": "https://example.com"
        }
```

복원(restore)은 `sanitized`에서 각 placeholder 토큰을 원본 값으로 치환한다.  
`restore(preprocess(text).sanitized, preprocess(text).placeholders) == text` 가 항상 성립해야 한다.

---

## What is NOT in scope (v1)

- DeepL, Papago, LibreTranslate Provider
- 파일 저장 시 자동 재번역
- 디스크 캐시 (세션 메모리만)
- 소스 언어 수동 선택 (항상 `auto`)
