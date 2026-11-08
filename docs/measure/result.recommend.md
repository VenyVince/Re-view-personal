# 추천 SQL 실행 시간 결과

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

원본 로그: 추천 SQL 측정 로그 없음.

## 2. 측정 상태

미측정. `RecommendSqlMeasureTest`는 이번 재측정에서 실행하지 않았다.

## 3. 실행 조건

| 항목 | 조건 |
|---|---|
| 클래스 | `RecommendSqlMeasureTest` |
| 메서드 | `recommendQueriesOnFixtureAndTwoThousandReviews` |
| Statement | `findRecommendProducts`, `findRecommendReviews` |
| 워밍업 | 2회. 시간 표본에 포함하지 않음 |
| 측정 | 10회 |
| JDBC 구간 | `readCount` 직전부터 반환 직후. `PreparedStatement` 생성, 바인딩, `executeQuery()`, 마지막 행까지 읽기, Statement 닫기 |
| 명령 | `./mvnw -Dsurefire.excludedGroups= -Dtest=RecommendSqlMeasureTest test` |

이력서에 쓸 실행 시간은 Perf DB 값으로 구분한다. 같은 조건의 개선 전후가 없으면 개선율을 적지 않는다.

## 4. Fixture DB

### 4.1 상품 추천 SQL

| 항목 | 값 |
|---|---|
| rows | 미측정 |
| 평균 | 미측정 |
| 최댓값 | 미측정 |
| 10회 표본 | 미측정 |

### 4.2 리뷰 추천 SQL

| 항목 | 값 |
|---|---|
| rows | 미측정 |
| 평균 | 미측정 |
| 최댓값 | 미측정 |
| 10회 표본 | 미측정 |

## 5. Perf DB

### 5.1 상품 추천 SQL

| 항목 | 값 |
|---|---|
| rows | 미측정 |
| 평균 | 미측정 |
| 최댓값 | 미측정 |
| 10회 표본 | 미측정 |

### 5.2 리뷰 추천 SQL

| 항목 | 값 |
|---|---|
| rows | 미측정 |
| 평균 | 미측정 |
| 최댓값 | 미측정 |
| 10회 표본 | 미측정 |

## 6. 레거시 상품 추천 SQL

대상은 `e7c2fa82` SQL이다. 실행 커넥션은 measure 커넥션이다. Perf DB가 아니다.

| 항목 | 값 |
|---|---|
| rowsPerRun | 미측정 |
| 평균 | 미측정 |
| 최댓값 | 미측정 |
| onlyCurrent | 미측정 |
| onlyLegacy | 미측정 |

## 7. 예상 실행 계획

미수집. `findRecommendProducts`, `findRecommendReviews`의 계획 원문이 없다.

## 8. 해석

측정값이 없다. 실행 시간과 개선 여부를 판단하지 않는다.
