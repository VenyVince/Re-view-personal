# Re:View 정량 근거 보완 측정 로드맵 (v3)

## 0. 문서 개요

본 문서는 [`measure.resume.v2.md`](../../measure.resume.v2.md)의 측정 계획을 기반으로, **실제 테스트 코드와 측정 환경을 근거로** 재측정을 수행하기 위한 상세 실행 로드맵이다.

v2 계획서가 "무엇을 측정할지"를 선언적으로 정의했다면, 본 v3 로드맵은 **"어느 테스트 클래스를, 어떤 DB/프로파일로, 어떤 명령으로 실행해, 어떤 콘솔 출력에서 어떤 값을 읽어 어느 표에 채울지"** 를 구체적으로 지정한다.

- 기준 커밋: `c95dd83d` (환경 변경 시 별도 기록)
- 측정 대상 테스트 패키지: `src/test/java/org/spring/project/re_view/measure`
- 측정 프로파일: `measure` (`src/test/resources/application-measure.properties`)
- 측정 결과 기록 위치: 본 디렉토리(`docs/measure`) 하위 결과 파일

---

## 1. 측정 환경 사전 확인

### 1.1 이중 데이터소스 구조

측정 테스트는 **두 개의 Oracle 스키마**를 동시에 사용한다. 이 점이 v2 계획서에는 명시되지 않았으나 실제 테스트 코드의 핵심 구조이므로 반드시 선행 이해가 필요하다.

| 역할 | 프로퍼티 | 스키마 | 용도 | 데이터 규모 |
|---|---|---|---|---|
| fixture DB | `spring.datasource.*` | `MEASURE` | 기능 정합성 검증(결과 ID, 점수 등) | 상품 20 / 리뷰 40 |
| perf DB | `measure.perf.*` | `MEASURE_PERF` | 성능·실행계획 측정 | 상품 200 / 리뷰 2,000 / 사용자 20 |

```
jdbc:oracle:thin:@//localhost:1522/XEPDB1
MEASURE      / measure_local   (fixture)
MEASURE_PERF / measure_local   (perf)
```

즉, **정합성 단언(assert)은 fixture DB(20/40)** 에서, **평균·최대 실행 시간과 EXPLAIN PLAN은 perf DB(200/2000)** 에서 수집된다. 결과 기록 시 두 DB의 수치를 혼동하지 않는다.

### 1.2 사전 점검 체크리스트

- [ ] Oracle XE가 `localhost:1522 / XEPDB1`로 기동되어 있는가
- [ ] `MEASURE`, `MEASURE_PERF` 두 스키마가 모두 접속 가능한가
- [ ] perf DB의 건수가 사용자 20 / 상품 200 / 리뷰 2,000과 정확히 일치하는가 (테스트가 `assertEquals`로 강제함)
- [ ] fixture DB의 건수가 상품 20 / 리뷰 40인가
- [ ] JDK 21 / Maven 래퍼(`./mvnw`) 동작 확인
- [ ] MinIO 등 외부 의존성은 측정 대상이 아니므로 기동 여부만 확인

> perf DB 건수가 틀어지면 `RecommendSqlMeasureTest`, `HeaderSearchMeasureTest`가 단언 단계에서 실패한다. 측정 전 건수 고정이 가장 중요한 선행 작업이다.

### 1.3 Maven 실행 명령

`pom.xml`의 `surefire.excludedGroups`는 `measure`다. Surefire 3.5.4는 이 값을 `excludedGroups`로 쓴다. 측정 테스트 클래스에는 `@Tag("measure")`가 있다.

`-Dtest=클래스명`만 주면 클래스는 지정되지만 태그가 제외된다. 2026-11-08에 `./mvnw -Dtest=PointConcurrencyMeasureTest test`를 실행한 결과는 `Tests run: 0`이고 빌드는 성공했다.

측정 시에는 제외 그룹을 비운다.

```bash
./mvnw -Dsurefire.excludedGroups= -Dtest=PointConcurrencyMeasureTest test
./mvnw -Dsurefire.excludedGroups= -Dtest=RecommendSqlMeasureTest test
./mvnw -Dsurefire.excludedGroups= -Dtest=HeaderSearchMeasureTest test
```

같은 날 세 클래스를 이 형태로 실행한 결과는 `Tests run: 3, Errors: 3`이다. 각 테스트 메서드까지 진입한 뒤 `localhost:1522` 리스너가 없어 `ORA-12541`로 끊겼다. 이 실행은 명령이 테스트를 선택하는지만 확인한 것이고, 측정값은 나오지 않았다. 그 프로세스 로그의 JDK는 21.0.8이다.

Oracle이 떠 있는 상태의 측정 성공은 이번 문서 보완에서 확인하지 않았다.

---

## 2. 측정 대상 테스트 매핑

v2 계획서의 각 "테스트 N"이 실제 어떤 테스트 클래스/메서드에 대응하는지 명시한다.

| v2 테스트 | 테스트 클래스 | 메서드 | 핵심 측정값 |
|---|---|---|---|
| 테스트 1. 동시성 정합성 | `PointConcurrencyMeasureTest` | `concurrentEarnAndUseThenBestAward` | 잔액·이력 정합성, best 중복 적립 |
| 테스트 2. 추천 SQL 성능 | `RecommendSqlMeasureTest` | `recommendQueriesOnFixtureAndTwoThousandReviews` | 추천 상품/리뷰 SQL avg·max |
| 테스트 3. 검색 SQL 성능 | `HeaderSearchMeasureTest` | `headerSearchCasesAndCurrentSqlTime` | 상품명/브랜드/리뷰 검색 avg·max |
| 테스트 4. 실행 계획 분석 | `RecommendSqlMeasureTest` + `HeaderSearchMeasureTest` | `explain(...)` 출력 | perf DB EXPLAIN PLAN |

공통 측정 파라미터 (코드 상수):

| 상수 | 값 | 위치 |
|---|---|---|
| `WARMUP` | 2 | Recommend / HeaderSearch |
| `RUNS` | 10 | Recommend / HeaderSearch |
| `ROUNDS` | 5 | PointConcurrency |
| `OPERATIONS` | 20 (적립 10 + 차감 10) | PointConcurrency |
| `BEST_THREADS` | 10 | PointConcurrency |
| `START_BALANCE` | 10,000 | PointConcurrency |
| `AMOUNT` | 100 | PointConcurrency |
| 셔플 시드 | `new Random(20261104)` | PointConcurrency |

실행 시간 포맷은 모든 성능 테스트가 동일하다:
`avg=<평균>ms max=<최대>ms samples=<10개 개별값>`

### 2.1 JDBC 측정 구간

`RecommendSqlMeasureTest.timeQuery`와 `HeaderSearchMeasureTest.time`의 10회 시간은 같은 구간이다.

- 시작: `readCount` 호출 직전의 `System.nanoTime()`
- 끝: `readCount`가 반환된 직후의 `System.nanoTime()`
- 구간에 포함: `PreparedStatement` 생성, 파라미터 바인딩, `executeQuery()`, `ResultSet.next()`로 마지막 행까지 읽기, Statement와 ResultSet 닫기
- 구간에 없음: 워밍업 2회, MyBatis DTO 매핑, 그 앞에서 호출하는 매퍼 기능 검증, `EXPLAIN PLAN`

이 값은 JDBC 호출과 결과 행을 모두 읽는 시간이다. DB 내부 실행 시간만으로 해석하지 않는다.

---

## 3. 테스트 1 · 일반 포인트 동시성 정합성

### 3.1 실행 방법

```bash
./mvnw -Dsurefire.excludedGroups= -Dtest=PointConcurrencyMeasureTest test
```

### 3.2 코드 기준 동작 흐름

1. `resetBalance()` : `point_history` 삭제 → `user_table.point = 10000` → 시드 잔액 10,000 EARN 1건 삽입
2. 단일 사용자(`USER_ID=1`, `measure_user`)에 대해 `SecurityContext`를 주입하고(`runAsUser`) 포인트 서비스 호출
3. 적립 10건 + 차감 10건(각 100원)을 시드 `20261104`로 셔플 → `ExecutorService` + `CountDownLatch`로 동시에 출발
4. 라운드마다 `expected = 10000 + earnSuccess*100 - useSuccess*100`을 계산해 출력한다. `point()`와 `historyNet()`도 출력한다
5. 5라운드 반복. `point != history`인 라운드만 `balanceHistoryMismatches`에 더한다. `expected`와 `point`의 차이는 이 카운터에 넣지 않는다
6. 사용자 1, 리뷰 2, 금액 700, 타입 `EARN` 이력을 삭제한 뒤 `addBestReviewPoint`를 10스레드로 호출하고, 그 조건의 이력 행 수(`bestRows`)와 예외 목록(`bestFailures`)을 출력한다

### 3.3 결과 판독 포인트

콘솔 `report` 출력에서 다음을 읽는다.

```
round 1 earnSuccess=.. useSuccess=.. failures=.. expected=.. point=.. history=..
...
balanceHistoryMismatches=.. bestRows=.. bestFailures=..
```

- `point == history` 여부. 테스트가 세는 불일치는 이 비교뿐이다
- `earnSuccess + useSuccess == 20` 여부. 테스트가 `assertEquals(OPERATIONS, ...)`로 강제한다. 한 건이라도 예외가 나면 이 단언이 실패한다
- `expected`는 출력만 한다. `point` 또는 `history`와 같다고 단언하지 않는다
- `bestRows`는 행 수다. 테스트는 `bestRows >= 0`만 단언하고, 1행을 성공 조건으로 단언하지 않는다

### 3.4 결과 기록 양식

| 회차 | 초기 잔액 | 적립 성공 | 차감 성공 | 실패 | expected | 최종 잔액(point) | 이력 합(history) | point!=history |
|---|---:|---:|---:|---:|---:|---:|---:|---|
| 1 | 10000 | | | | | | | |
| 2 | 10000 | | | | | | | |
| 3 | 10000 | | | | | | | |
| 4 | 10000 | | | | | | | |
| 5 | 10000 | | | | | | | |

**집계**

- `balanceHistoryMismatches`:
- best 중복 적립 행 수(`bestRows`):
- `bestFailures`:
- 전체 예외 발생:

### 3.5 계산 기준

코드에서 확인한 기준만 적는다.

| 항목 | 코드 기준 |
|---|---|
| 초기 잔액 | `resetBalance()`가 `user_table.point`를 10,000으로 갱신한다 |
| 초기 이력 | 같은 메서드가 해당 사용자 `point_history`를 지운 뒤, 금액 10,000, 타입 `EARN`, 설명 `시드 잔액` 1건을 넣는다. 초기 잔액을 이력에 더하는 별도 분기는 없다 |
| `earnSuccess` | `addReviewPoint`가 예외 없이 반환된 횟수. 금액은 호출당 100원(`PointConstants.CreateREVIEW`) |
| `useSuccess` | `usePoint`가 예외 없이 반환된 횟수. 금액은 호출당 100원 |
| `failures` | 워커에서 잡은 예외의 클래스명과 메시지. 예외 없이 반환한 호출은 들어가지 않는다 |
| `expected` | `10000 + earnSuccess * 100 - useSuccess * 100`. 예외로 끝나 성공 횟수에 없는 요청은 빠진다 |
| `point` | `SELECT point FROM user_table WHERE user_id = 1` |
| `history` | 해당 사용자의 `point_history` 합. `EARN`은 더하고 `USE`는 뺀다. 시드 10,000 EARN이 포함된다. 이 합에 `START_BALANCE`를 다시 더하지 않는다 |
| `balanceHistoryMismatches` | `point != history`인 라운드 수. 금액 차이가 아니고, `point`와 `expected`의 비교도 아니다 |

`usePoint`는 차감 후 잔액이 0 미만이면 `WrongRequestException("포인트가 부족합니다.")`를 던진다. 이 예외는 `updateUserPoint`에서 나므로 그 요청의 `point_history` INSERT에는 도달하지 않는다. 그 요청은 `useSuccess`에 없고 `failures`에 있다. 동시에 `earnSuccess + useSuccess == 20` 단언이 실패하므로, 차감 실패가 있는 실행은 측정 완료로 기록되지 않는다.

### 3.6 best 집계 기준

`addBestReviewPoint`는 `checkBestReviewPoint`가 기존 700원 `EARN`을 찾으면 예외 없이 `return`한다.

- `bestRows`: 실행 후 `user_id = 1`, `review_id = 2`, `amount = 700`, `type = 'EARN'` 행 수
- `bestFailures`: 그 10개 호출 중 예외가 난 호출의 예외 문구. 위 `return`은 예외가 아니므로 여기 들어가지 않는다
- 조용히 반환한 호출 수는 테스트가 출력하지 않는다
- 테스트는 `bestRows == 1`을 단언하지 않는다. `bestFailures`가 0이어도 `bestRows`는 1이 아닐 수 있다

best 결과는 행 수와 예외 수의 정합성 기록이다. 실행 시간 근거로 쓰지 않는다.

### 3.7 해석 주의

- `expected`와 `history`를 같은 식으로 부르지 않는다. `history`는 시드 EARN을 포함한 이력 합이다
- best 결과를 성능 수치로 쓰지 않는다

---

## 4. 테스트 2 · 추천 SQL 성능

### 4.1 실행 방법

```bash
./mvnw -Dsurefire.excludedGroups= -Dtest=RecommendSqlMeasureTest test
```

### 4.2 측정 구조

- 대상 Statement: `RecommendationsMapper.findRecommendProducts`, `findRecommendReviews`
- MyBatis `BoundSql`로 실제 바인딩 SQL을 추출한 뒤 JDBC로 `WARMUP 2 + RUNS 10` 실행
- 10회 시간의 구간은 §2.1이다
- **fixture DB(20/40)와 perf DB(200/2000) 양쪽**에서 각각 시간 측정
- 추가로 `e7c2fa82` 레거시 상품 추천 SQL(`LEGACY_PRODUCT_SQL`, 스칼라 서브쿼리 다중 중첩 버전)을 perf 아닌 measure 커넥션에서 실행해 현재 SQL과 결과 ID 집합 차이를 비교

### 4.3 결과 판독 포인트

콘솔 출력 키:

```
fixtureJdbc product rows=[..] avg=..ms max=..ms samples=..
fixtureJdbc review  rows=[..] avg=..ms max=..ms samples=..
perf users=20 products=200 reviews=2000
perfJdbc product rows=[..] avg=..ms max=..ms samples=..
perfJdbc review  rows=[..] avg=..ms max=..ms samples=..
legacy e7c2fa82 ... rowsPerRun=[..] avg=..ms max=..ms ... onlyCurrent=.. onlyLegacy=..
```

- 이력서 R 근거로는 **perf DB(200/2000) 수치**를 사용한다(실데이터 규모에 가장 근접).
- `productSqlHasRowNumber=false`, `reviewSqlHasRowNumber=true` → 상품/리뷰 추천 SQL의 구조 차이(리뷰 추천만 윈도우 함수 사용)를 설명하는 근거.

### 4.4 결과 기록 양식 (perf DB 기준)

**상품 추천 SQL (`findRecommendProducts`)**

| 구분 | rows | avg(ms) | max(ms) |
|---|---:|---:|---:|
| fixture(20/40) | | | |
| perf(200/2000) | | | |

**리뷰 추천 SQL (`findRecommendReviews`)**

| 구분 | rows | avg(ms) | max(ms) |
|---|---:|---:|---:|
| fixture(20/40) | | | |
| perf(200/2000) | | | |

**레거시 비교(`e7c2fa82`, measure 커넥션의 상품 추천)**

| 항목 | 값 |
|---|---|
| rowsPerRun | |
| avg / max | |
| onlyCurrent(현재에만 존재) | |
| onlyLegacy(레거시에만 존재) | |

개별 10회 samples 값은 결과 로그 원문에 보관한다.

### 4.5 해석 주의

- 개선 전후 비교는 레거시 SQL과의 **결과 집합/실행 시간 차이**로만 서술하며, 수치가 역전될 수 있으므로 "개선율 N%" 식 단정은 금지한다.
- 10회 측정값으로 p95 등 백분위는 주장하지 않는다(avg/max만).

---

## 5. 테스트 3 · 검색 SQL 성능

### 5.1 실행 방법

```bash
./mvnw -Dsurefire.excludedGroups= -Dtest=HeaderSearchMeasureTest test
```

### 5.2 측정 구조

- 대상 Statement: `HeaderSearchProductMapper.searchProducts`, `HeaderSearchReviewMapper.searchReviews`
- fixture DB 매퍼 호출로 기능 케이스를 확인한다. 이 호출은 10회 시간 측정이 아니다
- 10회 시간 측정은 §2.1 구간이며, 아래 파라미터만 사용한다
- 예상 실행 계획은 perf DB에서 수집한다

**10회 시간 측정의 파라미터**

| 구분 | Statement | keyword | sort | filter_category | filter_brand |
|---|---|---|---|---|---|
| fixture 상품 | `searchProducts` | `측정` | null | null | null |
| fixture 리뷰 | `searchReviews` | `리뷰검색어` | null | null | null |
| perf 상품 | `searchProducts` | `성능` | null | null | null |
| perf 리뷰 | `searchReviews` | `성능` | null | null | null |

`HeaderSearchProductMapper.xml`에서 `filter_brand`, `filter_category`가 null이면 그 조건은 SQL에 붙지 않는다. `sort`가 null이면 `ORDER BY p.created_at DESC`다. 상품 키워드는 `prd_name`, `prd_brand`, `ingredient`, 바우만 `type`의 `LIKE`다.

`HeaderSearchReviewMapper.xml`도 두 필터가 null이면 빠지고, `sort`가 null이면 `ORDER BY r.created_at DESC`다. 리뷰 키워드는 `content`, `nickname`의 `LIKE`다.

브랜드 필터, 최신순, 저가순, 1글자 검색은 이 10회 측정에 없다.

### 5.3 기능 케이스(fixture DB 매퍼 호출, 시간 측정 아님)

| 케이스 | 입력 | 기대 결과 ID |
|---|---|---|
| 상품명 정확 | `측정토너` | `[1]` |
| 부분 일치 | `측정` | `[1, 2]` |
| 최신순 | `측정`, latest | `[1, 2]` |
| 저가순 | `측정`, low_price | `[2, 1]` |
| 브랜드 | `측정`, brand=`MeasureBrand` | `[1]` |
| 브랜드를 카테고리 슬롯에 전달 | `측정`, category=`MeasureBrand` | `[]` (버그 재현) |
| 1글자 | `측` | `[1, 2]` |
| 리뷰 키워드 | `리뷰검색어` | review_id `3`, 1건 |

> "브랜드를 카테고리 슬롯에 전달 → 결과 0건"은 v2 §5.8에서 언급한 **서비스-매퍼 인자 전달 문제**의 재현 지점이다. 매퍼 직접 호출 결과를 서비스 정상 동작 근거로 확대 해석하지 않는다.

### 5.4 결과 판독 포인트

```
fixture product rows=[..] avg=..ms max=..ms
fixture review  rows=[..] avg=..ms max=..ms
perf product    rows=[..] avg=..ms max=..ms
perf review     rows=[..] avg=..ms max=..ms
productSqlHasLike=.. productSqlDeletes=.. productSqlHasLength=false ..
```

### 5.5 결과 기록 양식 (perf DB 기준)

| 검색 유형 | Statement | rows | avg(ms) | max(ms) |
|---|---|---:|---:|---:|
| 상품 검색 | `searchProducts` | | | |
| 리뷰 검색 | `searchReviews` | | | |

> 기능 케이스의 브랜드 검색과 상품명 검색은 같은 `searchProducts`다. 차이는 파라미터다. 브랜드 필터를 넣은 호출은 10회 시간 측정에 포함되지 않는다. 결과 파일에는 §5.2의 측정 파라미터와 §5.3의 기능 케이스를 따로 적는다.

---

## 6. 테스트 4 · 추천/검색 SQL 예상 실행 계획

### 6.1 수집 방법

실행 계획은 `RecommendSqlMeasureTest`와 `HeaderSearchMeasureTest`가 **perf DB 커넥션에서** 자동으로 수집한다.

```sql
EXPLAIN PLAN FOR
-- 대상 SQL

SELECT PLAN_TABLE_OUTPUT FROM TABLE(DBMS_XPLAN.DISPLAY());
```

코드는 `DBMS_XPLAN.DISPLAY_CURSOR`나 `GATHER_PLAN_STATISTICS`를 쓰지 않는다. 콘솔의 `explain product`, `explain review`는 옵티마이저의 예상 실행 계획이다. 실제 실행 통계와 구분하고, 결과 파일에는 예상 실행 계획(Explain Plan)으로 적는다.

대상은 네 SQL이다.

1. 상품 추천 SQL
2. 리뷰 추천 SQL
3. 상품 검색 SQL. perf 파라미터는 keyword `성능`, 나머지 null
4. 리뷰 검색 SQL. perf 파라미터는 keyword `성능`, 나머지 null

### 6.2 분석 항목 기록 양식

| 분석 항목 | 추천 상품 SQL | 추천 리뷰 SQL | 검색 상품 SQL | 검색 리뷰 SQL |
|---|---|---|---|---|
| 테이블 접근 방식 | | | | |
| 인덱스 접근 방식 | | | | |
| 조인 방식 | | | | |
| 정렬 연산 | | | | |
| 윈도우 함수 | | | | |
| 예상 Cost | | | | |
| 계획에 보이는 특이사항 | | | | |

실행 계획 원문(`PLAN_TABLE_OUTPUT`)은 결과 로그 파일에 그대로 보관한다.

### 6.3 해석 주의

- 예상 실행 계획을 확인했다는 사실만으로 최적화 성과를 주장하지 않는다.
- 예상 Cost는 옵티마이저 추정치다. 실제 실행 통계로 적지 않는다.
- 계획에 없는 연산은 비워 둔다.

---

## 7. 전체 실행 순서

| 순서 | 작업 | 명령 / 산출 | 완료 기준 |
|---|---|---|---|
| 1 | 환경 점검 | §1.2 체크리스트 | perf 20/200/2000, fixture 20/40 확인 |
| 2 | 동시성 측정 | `-Dsurefire.excludedGroups= -Dtest=PointConcurrencyMeasureTest` | 5라운드 + best 결과 기록 |
| 3 | 추천 SQL 측정 | `-Dsurefire.excludedGroups= -Dtest=RecommendSqlMeasureTest` | perf avg/max + 레거시 비교 |
| 4 | 검색 SQL 측정 | `-Dsurefire.excludedGroups= -Dtest=HeaderSearchMeasureTest` | §5.2의 10회 시간 + §5.3 기능 케이스 |
| 5 | 예상 실행 계획 정리 | 3·4 콘솔 explain 블록 | 4개 SQL의 예상 계획 표 작성 |
| 6 | 결과 종합 | 결과 파일 작성 | 모든 표/원문 로그 보관 |
| 7 | 이력서 R 검토 | STAR-R 초안 | 측정값과 문장 일치 확인 |

전체 일괄 실행(선택):

```bash
./mvnw -Dsurefire.excludedGroups= -Dgroups=measure test
```

> 단, `@SpringBootTest` 컨텍스트와 동시성/데이터 상태 간섭을 피하기 위해 **클래스별 개별 실행**을 권장한다(v2 §2.3 원칙 유지).

---

## 8. 산출물 및 저장 규칙

| 산출물 | 저장 위치 | 내용 |
|---|---|---|
| 동시성 결과 | `docs/measure/result.concurrency.md` | §3.4 표 + 콘솔 원문 |
| 추천 SQL 결과 | `docs/measure/result.recommend.md` | §4.4 표 + samples + 예상 실행 계획 |
| 검색 SQL 결과 | `docs/measure/result.search.md` | §5.2 측정 파라미터 + §5.5 표 + §5.3 기능 케이스 + 예상 실행 계획 |
| 통합 요약 | `docs/measure/result.summary.md` | 핵심 수치 한 장 요약 |
| 이력서 R 초안 | `docs/measure/resume.R.md` | 검증된 값만 반영 |

각 결과 파일 머리말에는 측정 시점에 확인한 값만 적는다.

- 실행 일시
- 기준 커밋. `git rev-parse HEAD` 결과. 이 문서의 참고 커밋 `c95dd83d`와 다르면 둘 다 적는다
- Git 작업 트리 상태. `git status --porcelain`에 출력이 있으면 그 사실을 적는다
- JDK 버전. 측정 프로세스의 Java 버전
- Oracle XE 버전. 측정 시점의 `v$version`. `measure/docker-compose.yml`의 이미지는 `gvenzl/oracle-xe:latest`라서 파일만으로 버전을 정하지 않는다
- DB 접속 대상과 스키마. `localhost:1522/XEPDB1`의 `MEASURE` 또는 `MEASURE_PERF`
- DB 데이터 건수
- 프로파일 `measure`
- JDBC 측정 구간. §2.1. 동시성 결과 파일에는 해당 없음

없는 값은 비워 두고 미확인으로 적는다. 예상값으로 채우지 않는다.

---

## 9. 최종 완료 기준

- [ ] 환경 점검(perf 20/200/2000, fixture 20/40) 통과
- [ ] 동시성 5라운드 결과 및 `balanceHistoryMismatches` 기록
- [ ] `bestRows`와 `bestFailures`를 §3.6의 집계 기준대로 기록
- [ ] 추천 상품/리뷰 SQL perf avg·max 확보
- [ ] 레거시 SQL 결과 집합/시간 비교 기록
- [ ] §5.2의 검색 SQL 10회 평균과 최댓값 확보
- [ ] 검색 기능 케이스(브랜드를 카테고리 인자에 넣은 호출 포함) 재현 확인
- [ ] 추천·검색 4개 SQL의 예상 실행 계획 표와 원문 보관
- [ ] 모든 결과 머리말에 §8의 재현성 항목을 기록. 확인하지 못한 항목은 미확인
- [ ] 이력서 문장과 측정 근거 일치 확인

**최종 목표:** fixture DB(정합성)와 perf DB(성능)를 구분하여, 동시성·추천 SQL·검색 SQL·실행 계획에 대해 면접에서 "어떤 조건에서 어떤 수치가 나왔는지"를 코드와 함께 설명할 수 있는 상태를 만든다.
