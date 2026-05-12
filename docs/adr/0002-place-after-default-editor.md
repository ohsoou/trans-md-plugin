# ADR-0002: FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR 사용, 기본 에디터 교체 금지

Date: 2026-05-12  
Status: Accepted

## Context

`TranslatedPreviewFileEditorProvider`는 `.md` 파일에 대해 커스텀 `FileEditor`를 등록한다. `FileEditorPolicy`는 커스텀 에디터가 기본 Markdown 에디터와 어떻게 공존할지를 결정한다.

사용 가능한 값:
- `HIDE_DEFAULT_EDITOR`: 기본 에디터를 숨기고 우리 에디터만 표시
- `PLACE_AFTER_DEFAULT_EDITOR`: 기본 에디터 탭 뒤에 우리 탭을 추가
- `NONE`: 탭 추가 없음 (직접 열어야 함)

## Decision

`FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR`를 사용한다.

## Reasons

- 사용자는 원본 Preview와 Translated Preview를 동시에 접근할 수 있어야 한다. `HIDE_DEFAULT_EDITOR`를 쓰면 원본 Preview를 보려면 플러그인을 비활성화해야 한다.
- 기본 Markdown 편집 기능(에디터 탭, 원본 Preview)은 JetBrains가 유지관리한다. 이를 교체하면 버전 업마다 호환성을 검증해야 하는 부담이 생긴다.
- "Translated Preview" 탭이 부가 기능으로 존재하는 것이 사용자 기대에 맞다.

## Consequences

- `.md` 파일을 열면 기본 탭들(Editor, Preview) 뒤에 "Translated Preview" 탭이 자동으로 추가된다.
- Translated Preview 탭은 사용자가 클릭할 때(`selectNotify()`)만 Translation Job을 시작한다. 불필요한 API 호출이 없다.
- 이 정책을 `HIDE_DEFAULT_EDITOR`로 바꾸는 변경은 이 ADR에 반한다.