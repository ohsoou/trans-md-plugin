# ADR-0001: commonmark-java를 플러그인에 번들, 내부 intellij.markdown 라이브러리 미사용

Date: 2026-05-12  
Status: Accepted

## Context

Preprocessed Document의 번역된 텍스트를 HTML로 변환해야 한다. IntelliJ Platform에는 `org.intellij.plugins.markdown` 번들 플러그인이 `intellij.markdown` 라이브러리를 포함하고 있으며, 이를 통해 `HtmlGenerator` 등을 사용할 수 있다.

## Decision

`org.commonmark:commonmark` 라이브러리를 플러그인 JAR에 직접 번들한다. `intellij.markdown` 내부 API는 사용하지 않는다.

## Reasons

- `intellij.markdown`의 `HtmlGenerator`, `MarkdownProcessor` 등은 `@ApiStatus.Internal`로 표시되어 있으며 공식 안정 보장이 없다. IDE 버전 업그레이드 시 아무 공지 없이 시그니처가 바뀌거나 제거될 수 있다.
- commonmark-java는 CommonMark 표준의 레퍼런스 구현체로, 독립적인 공개 API를 가진다. 버전을 플러그인이 직접 관리하므로 IDE 업데이트에 영향받지 않는다.
- commonmark-java 코어 JAR는 약 200KB로 플러그인 크기 증가가 미미하다.

## Consequences

- `build.gradle.kts`에서 `implementation("org.commonmark:commonmark:...")` 선언 필요.
- commonmark-java가 지원하지 않는 GFM 확장(테이블, 태스크 리스트 등)을 사용하려면 `commonmark-ext-gfm-tables`, `commonmark-ext-task-list-items` 등을 추가로 번들해야 한다.
- 내부 Markdown 라이브러리 의존 코드는 작성하지 않는다. 이 결정에 반하는 PR은 거부한다.