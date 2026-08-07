# AI 자체 팩트체크 2-Pass 설계

- **작성일**: 2026-08-08
- **범위**: `EventExtractionService` AI 파이프라인에 검증(verification) 단계 추가
- **명시적 제외**: 지오코딩(위경도 정확도) 개선은 이번 작업에 포함하지 않는다. 좌표는 계속 `AiEventExtractor`가 추정한 값을 그대로 쓴다.

## 1. 배경 / 문제

현재 파이프라인은 `추출(AiEventExtractor) → Event 저장 → (신뢰 소스만) 재작성(AiLocalizer)` 한 방향으로만 흐른다. 이 과정에서 코드베이스에 이미 있는 두 곳이 죽어 있다.

- **`EventEntity`** (`event`, `rawArticle`, `factText`, `verified` 필드를 가진 테이블): 스키마는 있지만 저장하는 코드가 어디에도 없다. `FeedController`의 `/api/events/{id}` 응답 중 `facts`, `sources` 필드는 이 테이블을 읽기만 하므로 **항상 빈 배열**이 나간다.
- **`Event.confidence`** (`Confidence` enum: `VERIFIED` / `UNVERIFIED` / `DISPUTED`): 저신뢰 소스든 신뢰 소스든 명시적으로 값을 세팅하는 곳이 `UNVERIFIED` 하나뿐이라, **`VERIFIED`/`DISPUTED`는 코드 어디서도 실제로 쓰이지 않는다.**

또한 현재 `AiEventExtractor`가 반환하는 `evidence`는 "이 사건이 맞다고 판단한 근거 문장"을 **같은 호출 안에서 자기 자신이 자기 주장에 붙이는 것**이라, 모델이 잘못 추출했을 경우 이를 걸러낼 독립적인 장치가 없다.

## 2. 목표

1. 신뢰 소스든 저신뢰 소스든, 생성된 모든 `Event`에 대해 **별도의 2차 LLM 호출**로 원문 대조 검증을 수행한다.
2. 검증 결과를 죽어있던 `EventEntity` 테이블에 실제로 채워서 `facts`/`sources` API가 의미 있는 데이터를 반환하게 한다.
3. 검증 결과를 집계해 죽어있던 `Confidence.VERIFIED`/`DISPUTED`를 실제로 세팅한다.
4. 기존 파이프라인의 "부분 실패해도 나머지는 계속 진행" 철학(기사 단위/언어 단위 try-catch)을 그대로 따른다.

## 3. 플로우

```
기존: 추출 → Event 저장 → (신뢰 소스만) 재작성
변경: 추출 → Event 저장 → 팩트검증(신규, 모든 이벤트 대상) → (신뢰 소스만) 재작성
```

팩트검증은 신뢰도 분기와 무관하게 생성된 모든 이벤트에 대해 실행된다. `PENDING_REVIEW`로 묶이는 저신뢰 이벤트일수록, 에디터가 승인 여부를 판단할 때 AI가 미리 걸러준 팩트 목록이 유용하기 때문이다.

## 4. 컴포넌트

### 4.1 `AiFactVerifier` (신규)
`AiEventExtractor`/`AiLocalizer`와 동일한 구조(WebClient + system prompt + JSON 강제 응답 파싱, `openai.api-key`/`openai.model` 설정 재사용).

- **입력**: 원문 기사 전문(제목+본문) + 1차 추출 결과(사건 제목/요약)
- **출력**: `List<VerifiedFact>`
- **동작**: 사건 요약을 2~5개의 개별 사실(fact)로 분해한 뒤, 각 사실이 원문에 직접적인 근거가 있는지 개별 판정한다. `temperature=0`으로 고정한다.

### 4.2 `VerifiedFact` (신규 record)
```java
record VerifiedFact(String factText, boolean verified) {}
```

### 4.3 `EventExtractionService` 변경
- 생성자에 `AiFactVerifier factVerifier`, `EventEntityRepository eventEntities` 2개 의존성 추가
- `extractEvents()` 루프에서 Event/EventLocation 저장 직후, 재작성 분기보다 먼저 `verifyAndSave(event, article)` 호출
- `verifyAndSave`:
  1. `factVerifier.verify(article, event)` 호출
  2. 빈 리스트면 아무것도 하지 않고 반환 (기존 기본값 `UNVERIFIED` 유지)
  3. 각 `VerifiedFact`를 `EventEntity(event, rawArticle=article, factText, verified)`로 저장
  4. 전부 `verified=true`면 `event.setConfidence(Confidence.VERIFIED)`, 하나라도 `false`면 `Confidence.DISPUTED`
  5. 전체를 try-catch로 감싸 예외를 밖으로 던지지 않음 (내부에서 swallow)

## 5. 프롬프트 설계 (초안)

**System prompt**
```
너는 재구성된 사건 정보가 원문 기사에 실제로 근거하는지 검증하는 팩트체커다.
아래 원문 기사와, 그 기사에서 추출된 사건 요약을 비교해서, 요약에 담긴 핵심 주장을
2~5개의 개별 사실(fact)로 쪼갠 뒤 각각이 원문에 직접적인 근거가 있는지 판정한다.
원문에 없는 내용을 지어냈거나 과장했다면 verified를 false로 표시한다.
설명, 코드블록, 다른 텍스트 없이 아래 JSON 스키마로만 응답한다.

{
  "facts": [
    { "fact_text": "...", "verified": true }
  ]
}
```

**User content**: `원문 제목 + 원문 본문 + 추출된 사건 제목/요약`

## 6. 에러 처리

기존 코드 전체를 관통하는 fail-safe 패턴을 그대로 따른다 (`localizeAndSave`와 동일):
- 검증 API 호출/파싱 실패 → 해당 이벤트는 `EventEntity` 없이, `confidence`는 기존 기본값(`UNVERIFIED`) 그대로 진행
- 이벤트 생성 자체나 재작성 단계에는 영향 없음

## 7. 기존 방식 대비 무엇이 더 나은가

| 관점 | 기존 (`AiEventExtractor`만) | 신규 (검증 pass 추가) |
|---|---|---|
| **검증 주체** | 추출한 모델이 같은 호출 안에서 자기 주장에 스스로 확신도(`evidence` 1문장)를 붙임 (자기 신고) | 별도 호출로 원문과 대조하는 **독립된 감사(audit)** 단계. 생성과 검증의 역할이 분리됨 |
| **판정 단위** | 사건 전체에 근거 문장 1개 | 사건을 2~5개 개별 사실로 쪼개 **항목별로 참/거짓 판정** — 일부만 틀렸을 때도 구분 가능 |
| **결과의 쓰임새** | `Event.evidence` 문자열 하나, 프론트 API에 노출 안 됨 | `EventEntity`로 저장돼 `/api/events/{id}`의 `facts`/`sources`가 **실제 데이터를 반환**하게 됨 (기존엔 항상 빈 배열이던 버그성 미구현을 고침) |
| **신뢰도 표현** | `Confidence.VERIFIED`/`DISPUTED`가 코드상 죽은 값 — 항상 `UNVERIFIED` | 검증 결과에 따라 `VERIFIED`/`DISPUTED`가 실제로 갈림 — 프론트에서 신뢰도 뱃지로 바로 활용 가능 |
| **저신뢰(PENDING_REVIEW) 소스 처리** | 에디터가 승인/반려 판단할 근거가 원문 통짜밖에 없음 | AI가 미리 항목별로 걸러준 팩트 목록을 참고해 승인 판단 가능 |
| **실패 시 안전성** | — | 검증 실패해도 이벤트 생성/재작성에 영향 없음 (기존 fail-safe 철학 그대로 유지) |

한 줄 요약: **"내가 맞다고 나 스스로 한 줄 적어놓기"에서 "별도 단계가 원문과 항목별로 대조해서 감사하기"로 바뀌는 것**이며, 그 결과가 지금까지 죽어있던 `facts`/`sources` API와 `confidence` 뱃지를 실제로 살린다.

## 8. 기존 코드 영향

`EventExtractionService` 생성자 시그니처 변경으로 `EventExtractionServiceTest`의 기존 테스트 10개가 생성자 호출부를 전부 고쳐야 컴파일된다. 이번 구현 범위에 포함한다.

## 9. 테스트 계획

- `AiFactVerifierTest`: `AiEventExtractorTest`와 동일한 패턴으로 JSON 파싱(`parseVerification`)/`extractContent` 단위 테스트, 필수 필드 누락·비정상 JSON 예외 케이스 포함
- `EventExtractionServiceTest`:
  - 기존 10개 테스트 생성자 인자 보정
  - 신규: 모든 fact가 `verified=true`면 `Event.confidence`가 `VERIFIED`로 바뀌는지
  - 신규: 하나라도 `verified=false`면 `DISPUTED`로 바뀌는지
  - 신규: 검증 호출이 예외를 던져도 이벤트 생성 자체(`created` 카운트)는 성공하는지

## 10. 명시적으로 하지 않는 것

- 지오코딩(위경도 정확도) 개선 — 별도 논의 대상, 이번 스코프에서 항상 제외
- 검증 결과로 `EventStatus`나 `reviewReason`을 바꾸는 것 — 이미 공개된 신뢰 소스 이벤트를 검증 결과만으로 숨기는 건 더 큰 정책 결정이 필요해 제외. `confidence` 값 노출까지만 한다
- 이벤트 클러스터링/중복 제거(여러 소스를 하나로 병합) — 다음 개선 후보로 남겨둠
