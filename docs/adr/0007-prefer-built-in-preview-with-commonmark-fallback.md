# ADR-0007: 기본 Markdown Preview 렌더러를 우선 사용하고 CommonMark로 fallback

Date: 2026-05-13  
Status: Accepted  
Supersedes: ADR-0001

## Context

`Translated Preview`가 사용자의 기대를 충족하려면 JetBrains 기본 Markdown Preview와 최대한 같은 결과를 보여줘야 한다. 특히 fenced code block의 info string 처리와 `$$ ... $$` 수식 렌더링은 단순 CommonMark HTML만으로는 기본 Preview와 차이가 난다.

한편 JetBrains Markdown 플러그인의 preview 렌더러는 `@ApiStatus.Internal`에 해당하므로, 직접 의존하면 IDE 업그레이드 시 깨질 수 있다. 즉 렌더링 fidelity와 API 안정성 사이의 선택이 필요하다.

## Decision

JetBrains Markdown preview 렌더러를 **우선 사용**한다. 다만 내부 API 의존 리스크를 그대로 노출하지 않기 위해 `CommonmarkRenderer`를 **안정적인 fallback**으로 함께 유지한다.

구체적으로:

- 1차 렌더링은 JetBrains preview 렌더러로 수행한다.
- JetBrains renderer 또는 preview panel 생성/갱신 중 recoverable failure가 발생하면 세션 동안 CommonMark fallback으로 전환한다.
- fallback 이후에는 `JTextPane` 기반 HTML preview를 사용한다.

## Reasons

- 기본 Preview와 같은 수식/코드펜스 동작은 사용자 가치가 크다.
- internal API 리스크는 현실적이지만, fallback이 있으면 기능 전체가 중단되는 상황은 피할 수 있다.
- CommonMark 경로를 유지하면 IDE 업그레이드로 internal API가 깨져도 최소한 번역 preview는 계속 제공된다.

## Consequences

- `Translated Preview`는 두 렌더링 경로를 유지관리해야 한다.
- 정상 환경에서는 기본 Preview와 더 유사한 결과를 보여준다.
- fallback 모드에서는 수식/고급 preview 기능이 기본 Preview와 동일하지 않을 수 있다.
- internal API failure는 로그로 남기고, 사용자에게는 degraded preview를 제공한다.
