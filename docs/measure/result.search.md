# 검색 SQL 실행 시간 결과

## 1. 머리말

| 항목 | 값 |
|---|---|
| 측정 실행 일시 | 미측정 |
| 측정 기준 커밋 | 미측정 |
| 프로파일 | measure |
| DB 접속 | `localhost:1522/XEPDB1` |
| Fixture DB | `MEASURE` / `measure_local` |
| Perf DB | `MEASURE_PERF` / `measure_local` |
| Fixture 건수 | 환경 확인 시점 사용자 5, 상품 20, 리뷰 40 |
| Perf 건수 | 환경 확인 시점 사용자 20, 상품 200, 리뷰 2,000 |
| 환경 확인 일시 | 2026-11-08 22:40:09 |
| 환경 확인 JDK | 21.0.8 |
| Oracle | 21c Express Edition Release 21.0.0.0.0 |
| 파일 작성 시점 HEAD | `f5dd54a6` |
| 파일 작성 시점 작업 트리 | `AGENTS.md`, `docs/measure_v2/Plan/plan.md` 수정 |

원본 로그: 검색 SQL 측정 로그 없음.

## 2. 측정 상태

미측정. `HeaderSearchMeasureTest`는 이번 재측정에서 실행하지 않았다.

## 3. 성능 측정 조건

10회 측정은 아래 네 조건만 대상이다. `sort`, `filter_category`, `filter_brand`는 테스트 코드에서 null로 전달한다. 이번 실행에서 바인딩된 SQL은 아직 확인하지 않았다.

| 구분 | Statement | keyword | sort | filter_category | filter_brand |
|---|---|---|---|---|---|
| Fixture 상품 | `searchProducts` | `측정` | null | null | null |
| Fixture 리뷰 | `searchReviews` | `리뷰검색어` | null | null | null |
| Perf 상품 | `searchProducts` | `성능` | null | null | null |
| Perf 리뷰 | `searchReviews` | `성능` | null | null | null |

상품명 검색과 브랜드 검색의 기능 케이스는 같은 `searchProducts`를 쓴다. 브랜드 필터를 넣은 호출은 이 10회 측정에 없다.

JDBC 구간은 `readCount` 직전부터 반환 직후다. 워밍업 2회는 표본에 넣지 않는다.

명령: `./mvnw -Dsurefire.excludedGroups= -Dtest=HeaderSearchMeasureTest test`

## 4. 기능 검증

Fixture DB 매퍼 직접 호출이다. 성능 측정이 아니고, 서비스 검색 전체의 정상 동작 근거가 아니다.

| 케이스 | 입력 | 기대 결과 ID | 실제 결과 ID | 단언 |
|---|---|---|---|---|
| 상품명 정확 | keyword `측정토너` | `[1]` | 미측정 | 미측정 |
| 부분 일치 | keyword `측정` | `[1, 2]` | 미측정 | 미측정 |
| 최신순 | keyword `측정`, sort `latest` | `[1, 2]` | 미측정 | 미측정 |
| 저가순 | keyword `측정`, sort `low_price` | `[2, 1]` | 미측정 | 미측정 |
| 브랜드 | keyword `측정`, brand `MeasureBrand` | `[1]` | 미측정 | 미측정 |
| 브랜드를 카테고리 인자로 전달 | keyword `측정`, category `MeasureBrand` | `[]` | 미측정 | 미측정 |
| 1글자 | keyword `측` | `[1, 2]` | 미측정 | 미측정 |
| 리뷰 키워드 | keyword `리뷰검색어` | review_id `3`, 1건 | 미측정 | 미측정 |

## 5. 실행 시간

### 5.1 Fixture DB

| Statement | rows | 평균 | 최댓값 | 10회 표본 |
|---|---|---|---|---|
| `searchProducts` | 미측정 | 미측정 | 미측정 | 미측정 |
| `searchReviews` | 미측정 | 미측정 | 미측정 | 미측정 |

### 5.2 Perf DB

| Statement | rows | 평균 | 최댓값 | 10회 표본 |
|---|---|---|---|---|
| `searchProducts` | 미측정 | 미측정 | 미측정 | 미측정 |
| `searchReviews` | 미측정 | 미측정 | 미측정 | 미측정 |

## 6. 예상 실행 계획

미수집. 상품 검색 SQL과 리뷰 검색 SQL의 계획 원문이 없다.

## 7. 해석

측정값이 없다. 검색 성능과 기능 케이스 통과 여부를 판단하지 않는다.
