# ADR-0006: Split 레이아웃에 EditorFactory.createEditor + JSplitPane 사용

Date: 2026-05-12  
Status: Accepted

## Context

"Translated Preview" 탭을 좌: 소스 에디터, 우: 번역 HTML의 분할 레이아웃으로 변경해야 한다. IntelliJ Platform에는 이를 위한 두 가지 접근법이 있다.

**방법 A — `TextEditorWithPreview`**: JetBrains 내장 클래스. `FileEditorProvider`를 `TextEditorWithPreviewProvider`로 교체하고 `PreviewEditor` 인터페이스를 추가 구현해야 한다. 편집/미리보기/분할 전환 버튼이 자동으로 추가된다.

**방법 B — `EditorFactory.createEditor` + `JSplitPane`**: `TranslatedPreviewFileEditor` 내부에서 `panel`을 `JSplitPane`으로 교체하고, 왼쪽에 `EditorFactory`로 생성한 에디터 컴포넌트를 추가한다.

## Decision

**방법 B**를 사용한다.

## Reasons

- 방법 A는 `FileEditorProvider` 구조를 `TextEditorWithPreviewProvider`로 전면 교체해야 하며, `PreviewEditor` 인터페이스 구현과 레이아웃 전환 상태 관리가 추가된다.
- 방법 B는 기존 `TranslatedPreviewFileEditor` 구조를 유지한 채 `panel` 생성 로직만 변경하면 된다. 기존 Translation Job, 캐시, 에러 처리 코드가 그대로 재사용된다.
- 이 플러그인은 항상 분할 레이아웃만 필요하므로, `TextEditorWithPreview`가 제공하는 편집/미리보기/분할 전환 버튼은 불필요한 UI다.

## Consequences

- `EditorFactory.createEditor()`로 생성한 에디터는 반드시 `dispose()` 시 `EditorFactory.releaseEditor()`로 해제해야 한다. 미해제 시 메모리 누수가 발생한다.
- 같은 `Document`를 공유하므로 왼쪽 에디터에서의 편집은 다른 탭(Markdown Split Editor 등)에도 즉시 반영된다.
