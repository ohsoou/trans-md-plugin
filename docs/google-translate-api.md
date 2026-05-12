# Google Cloud Translation API v2 (Basic) — 계약 문서

구현 시 이 문서를 참조한다. 공식 문서: https://cloud.google.com/translate/docs/reference/rest/v2/translate

---

## 엔드포인트

```
POST https://translation.googleapis.com/language/translate/v2
```

인증은 URL 쿼리 파라미터로 전달한다:

```
POST https://translation.googleapis.com/language/translate/v2?key={API_KEY}
```

---

## 요청

Content-Type: `application/json`

```json
{
  "q": "Hello, world!",
  "source": "en",
  "target": "ko",
  "format": "text"
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `q` | `string` \| `string[]` | 번역할 텍스트. 단일 문자열 또는 배열. **5,000자 제한 (UTF-8 기준)** |
| `source` | `string` | 입력 언어 BCP-47 코드. 생략하거나 `"auto"` 전달 시 자동 감지. **주의: `"auto"`는 유효한 값이 아님 — 자동 감지는 `source` 필드를 아예 생략해야 함** |
| `target` | `string` | 출력 언어 BCP-47 코드. 필수. |
| `format` | `"text"` \| `"html"` | 입력 형식. Placeholder 토큰이 포함된 텍스트는 `"text"` 사용. |

---

## 응답 (성공: HTTP 200)

```json
{
  "data": {
    "translations": [
      {
        "translatedText": "안녕, 세상!",
        "detectedSourceLanguage": "en"
      }
    ]
  }
}
```

`q`가 배열인 경우 `translations` 배열에 순서대로 결과가 담긴다.

---

## 오류 응답

| HTTP 코드 | 의미 | 처리 방법 |
|-----------|------|-----------|
| 400 | 잘못된 요청 (빈 텍스트, 잘못된 언어 코드 등) | 인라인 에러 표시 |
| 403 | API 키 없음, 잘못된 키, 결제 미설정 | "API 키를 확인하세요" 에러 + 설정 링크 |
| 429 | 할당량 초과 | "번역 한도를 초과했습니다" 에러 |
| 5xx | 서버 오류 | "번역 서비스를 일시적으로 사용할 수 없습니다" 에러 |

---

## 할당량 및 제한

| 항목 | 제한 |
|------|------|
| 요청당 최대 문자 수 | 5,000자 (UTF-8) |
| 분당 최대 요청 수 | 100 요청 (무료 티어) |
| 월간 무료 사용량 | 500,000자 |
| 초과 시 요금 | $20 / 백만 자 |

**구현 주의사항**: Chunk 분할 시 5,000자를 초과하지 않도록 보장해야 한다. 단일 섹션이 5,000자를 넘는 경우 추가 분할이 필요하다 (줄바꿈 기준).

---

## GCP 프로젝트 설정 필수 조건

1. Google Cloud Console에서 프로젝트 생성
2. "Cloud Translation API" 활성화
3. API 키 생성 (결제 계정 연결 필요 — 무료 티어 범위 내 사용 가능)
4. API 키를 Trans MD 플러그인 설정에 입력

설정 UI에서 GCP Console 링크를 제공한다: https://console.cloud.google.com/apis/library/translate.googleapis.com

---

## Source Language 처리

소스 언어는 항상 자동 감지이므로 요청 시 `source` 필드를 **포함하지 않는다**.

```kotlin
// 올바른 방법
val body = """{"q":"$text","target":"$targetLang","format":"text"}"""

// 잘못된 방법 — "auto"는 유효하지 않음
val body = """{"q":"$text","source":"auto","target":"$targetLang","format":"text"}"""
```