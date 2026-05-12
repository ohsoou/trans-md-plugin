# ADR-0004: 번역 제외 항목에 Placeholder 치환 전략 사용

Date: 2026-05-12  
Status: Accepted

## Context

코드 블록, 인라인 코드, URL, Front matter는 번역되어서는 안 된다. 이를 구현하는 방법은 두 가지가 있다.

**방법 A — Placeholder 치환**: 번역 전에 제외 항목을 `__PLACEHOLDER_N__` 토큰으로 교체하고, 번역 후 원본을 복원한다.

**방법 B — 후처리**: 전체 텍스트를 번역한 뒤 번역된 HTML에서 코드 블록을 찾아 원본으로 교체한다.

## Decision

**방법 A (Placeholder 치환)**을 사용한다. `MarkdownPreprocessor`가 이 책임을 담당한다.

## Reasons

- 번역 API가 `__PLACEHOLDER_N__` 토큰을 그대로 유지해주므로 복원이 단순하다. Google Translate는 HTML 엔티티를 보존하도록 설계되어 있고, 영숫자 밑줄 토큰도 변환하지 않는다.
- 방법 B는 번역된 HTML의 DOM 구조가 원본 Markdown과 다를 수 있어 교체 위치를 정확히 찾기 어렵다.
- `MarkdownPreprocessor`를 순수 함수로 구현할 수 있어 IDE 없이 단위 테스트가 가능하다. `restore(preprocess(text).sanitized, preprocess(text).placeholders) == text` 불변식으로 테스트한다.
- commonmark-java AST를 사용해 코드 노드를 정확히 식별하므로 정규식 기반보다 안정적이다.

## Consequences

- `MarkdownPreprocessor`는 상태 없는 순수 함수 모듈로 구현해야 한다.
- Placeholder 토큰 형식(`__PLACEHOLDER_N__`)은 번역 API가 변환하지 않는다는 전제를 가진다. 다른 Provider 추가 시 이 가정을 검증해야 한다.
- Front matter, 펜스 코드 블록, 인라인 코드, Markdown 링크의 URL 부분이 Exclusion에 해당한다. 새로운 Exclusion 타입 추가는 `MarkdownPreprocessor`만 수정하면 된다.