# ADR-0005: API 키를 PasswordSafe로 저장, PersistentStateComponent XML에 저장 금지

Date: 2026-05-12  
Status: Accepted

## Context

사용자가 입력한 Google Translate API 키를 플러그인이 저장해야 한다. `PersistentStateComponent`는 설정값을 `~/.config/JetBrains/<IDE>/options/TransMdSettings.xml`에 평문으로 저장한다.

## Decision

API 키는 IntelliJ Platform의 `PasswordSafe` API를 통해 OS 키체인에 저장한다. `TransMdSettings.xml`에는 API 키를 포함하지 않는다.

## Reasons

- `TransMdSettings.xml`은 평문 파일로, dotfiles 형태로 백업되거나 실수로 버전 관리에 포함될 위험이 있다.
- `PasswordSafe`는 macOS Keychain, Windows Credential Manager, Linux Secret Service를 통해 OS 수준 암호화를 제공한다.
- JetBrains Marketplace 심사 가이드라인은 자격증명을 안전하게 저장할 것을 권고한다. PasswordSafe 미사용 시 심사에서 지적받을 수 있다.

## Consequences

- API 키 읽기/쓰기는 반드시 `PasswordSafe.instance.getPassword()` / `PasswordSafe.instance.setPassword()` 를 통해야 한다.
- `TransMdSettings.State`에 `googleApiKey` 필드를 두지 않는다.
- IDE가 처음 실행되는 환경(CI, 새 머신)에서는 PasswordSafe가 비어 있으므로, 키가 없을 때의 에러 처리가 반드시 구현되어야 한다 (Inline 에러 + "설정 열기" 링크).