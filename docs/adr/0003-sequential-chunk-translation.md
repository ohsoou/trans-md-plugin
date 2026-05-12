# ADR-0003: Chunk를 순차적으로 번역, 병렬 번역 금지

Date: 2026-05-12  
Status: Accepted

## Context

Google Translate API v2 Basic은 단일 요청당 5,000자를 초과할 수 없다. 긴 Markdown 파일은 Heading 경계(`##`, `###`)로 Chunk로 분할되어 여러 번 요청된다. 이 Chunk들을 병렬로 보낼지, 순차적으로 보낼지 결정이 필요하다.

## Decision

Chunk는 순차적으로 번역한다 (`async`/`awaitAll` 패턴 금지).

## Reasons

- Google Cloud Translation API의 무료 티어 rate limit은 **분당 100 요청**이다. 일반적인 기술 문서 한 페이지(약 2,000자)는 1~2 Chunk로 분할된다. 병렬화의 실질적 이점이 없다.
- 병렬 요청 시 응답 순서가 보장되지 않아 Chunk 재조합 로직이 복잡해진다. 순차 처리에서는 결과 순서가 자동으로 보장된다.
- 진행 상태를 `(N/M 섹션)` 형태로 정확하게 표시할 수 있다.
- 사용자는 번역 속도보다 안정성과 예측 가능한 API 비용을 더 중요하게 여긴다.

## Consequences

- 매우 긴 파일(예: 20개 이상의 Chunk)은 번역에 수십 초 걸릴 수 있다. 이는 허용 가능한 트레이드오프로 본다.
- 나중에 rate limit이 이슈가 된다면 이 ADR을 재검토할 수 있다. 그 시점에 `async`/`awaitAll` + 재시도 로직을 도입하면 된다.
- 병렬 번역 구현 PR은 이 ADR을 인용하고 변경 이유를 명시해야 한다.