# 검색 SQL 실행 시간 결과

## 1. 머리말

| 항목 | 값 |
|---|---|
| 측정 실행 일시 | 2026-11-09 16:50:09 |
| 측정 기준 커밋 | `dd876b2d` |
| 로드맵 참고 커밋 | `c95dd83d` |
| 측정 시점 작업 트리 | `docs/measure/result.recommend.md`, `docs/measure/result.summary.md`, `docs/measure_v2/Plan/plan.md` 수정 |
| 프로파일 | measure |
| JDK | 21.0.8 |
| DB 접속 | `localhost:1522/XEPDB1` |
| Fixture DB | `MEASURE` / `measure_local` |
| Perf DB | `MEASURE_PERF` / `measure_local` |
| 이번 실행 Perf 건수 | 사용자 20, 상품 200, 리뷰 2,000 |
| Oracle | 21c Express Edition Release 21.0.0.0.0. 2026-11-08 조회. 이번 실행에서 `v$version`은 다시 조회하지 않음 |

원본 로그: `target/surefire-reports/TEST-org.spring.project.re_view.measure.HeaderSearchMeasureTest.xml`의 `system-out`.

명령:

```bash
./mvnw -Dsurefire.excludedGroups= -Dtest=HeaderSearchMeasureTest test
```

## 2. 측정 상태

실행 완료. `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`. `BUILD SUCCESS`. 경과 6.435초.

## 3. 성능 측정 조건

10회 측정은 아래 네 조건이다. `sort`, `filter_category`, `filter_brand`는 null이다. 상품명 검색과 브랜드 검색의 기능 케이스는 같은 `searchProducts`를 쓴다. 브랜드 필터를 넣은 호출은 이 10회 측정에 없다.

| 구분 | Statement | keyword | sort | filter_category | filter_brand |
|---|---|---|---|---|---|
| Fixture 상품 | `searchProducts` | `측정` | null | null | null |
| Fixture 리뷰 | `searchReviews` | `리뷰검색어` | null | null | null |
| Perf 상품 | `searchProducts` | `성능` | null | null | null |
| Perf 리뷰 | `searchReviews` | `성능` | null | null | null |

JDBC 구간은 `readCount` 직전부터 반환 직후다. 워밍업 2회는 표본에 넣지 않는다.

콘솔의 SQL 문자열 확인: `productSqlHasLike=true`, `productSqlDeletes=true`, `productSqlHasLength=false`, `reviewSqlHasLike=true`.

## 4. 기능 검증

Fixture DB 매퍼 직접 호출이다. 성능 측정이 아니고, 서비스 검색 전체의 정상 동작 근거가 아니다. 테스트 단언을 통과했다.

| 케이스 | 입력 | 기대 결과 ID | 실제 결과 ID | 단언 |
|---|---|---|---|---|
| 상품명 정확 | keyword `측정토너` | `[1]` | `[1]` | 통과 |
| 부분 일치 | keyword `측정` | `[1, 2]` | `[1, 2]` | 통과 |
| 최신순 | keyword `측정`, sort `latest` | `[1, 2]` | `[1, 2]` | 통과 |
| 저가순 | keyword `측정`, sort `low_price` | `[2, 1]` | `[2, 1]` | 통과 |
| 브랜드 | keyword `측정`, brand `MeasureBrand` | `[1]` | `[1]` | 통과 |
| 브랜드를 카테고리 인자로 전달 | keyword `측정`, category `MeasureBrand` | `[]` | `[]` | 통과. 0건 |
| 1글자 | keyword `측` | `[1, 2]` | `[1, 2]` | 통과 |
| 리뷰 키워드 | keyword `리뷰검색어` | review_id `3`, 1건 | `3:리뷰검색어 부분일치` | 통과 |

## 5. 실행 시간

### 5.1 Fixture DB

| Statement | rows | 평균 | 최댓값 | 10회 표본 |
|---|---|---|---|---|
| `searchProducts` | 2, 2, 2, 2, 2, 2, 2, 2, 2, 2 | 1.957ms | 2.933ms | 1.390, 1.920, 1.987, 2.087, 1.677, 1.425, 1.635, 2.593, 1.924, 2.933 |
| `searchReviews` | 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 | 3.440ms | 5.468ms | 3.311, 2.806, 2.500, 2.983, 5.468, 3.474, 3.177, 4.249, 3.530, 2.902 |

### 5.2 Perf DB

| Statement | rows | 평균 | 최댓값 | 10회 표본 |
|---|---|---|---|---|
| `searchProducts` | 200, 200, 200, 200, 200, 200, 200, 200, 200, 200 | 6.403ms | 8.289ms | 7.611, 6.750, 5.703, 7.288, 7.623, 8.289, 5.505, 5.762, 4.832, 4.671 |
| `searchReviews` | 2000, 2000, 2000, 2000, 2000, 2000, 2000, 2000, 2000, 2000 | 15.950ms | 18.577ms | 18.577, 17.484, 17.875, 14.029, 14.924, 14.926, 15.633, 14.621, 17.086, 14.344 |

## 6. 예상 실행 계획

Perf DB에서 keyword `성능`, 나머지 인자 null로 `EXPLAIN PLAN FOR`와 `DBMS_XPLAN.DISPLAY()`를 실행했다. 실제 실행 통계가 아니다.

### 6.1 상품 검색 SQL

Plan hash value `3623065135`. 예상 Cost 9.

계획에 보이는 연산: `PRODUCT` full table scan, `DELETED_AT IS NULL`, `LIKE` 네 조건, `BAUMANN` index full scan(`SYS_C008307`), merge join, `PRODUCT_IMAGE` full table scan, hash join right outer, sort order by.

```text
Plan hash value: 3623065135

------------------------------------------------------------------------------------------------
| Id  | Operation                      | Name          | Rows  | Bytes | Cost (%CPU)| Time     |
------------------------------------------------------------------------------------------------
|   0 | SELECT STATEMENT               |               |   200 | 71800 |     9  (23)| 00:00:01 |
|   1 |  SORT ORDER BY                 |               |   200 | 71800 |     9  (23)| 00:00:01 |
|*  2 |   HASH JOIN RIGHT OUTER        |               |   200 | 71800 |     8  (13)| 00:00:01 |
|*  3 |    TABLE ACCESS FULL           | PRODUCT_IMAGE |     1 |   267 |     2   (0)| 00:00:01 |
|   4 |    MERGE JOIN                  |               |   200 | 18400 |     6  (17)| 00:00:01 |
|   5 |     TABLE ACCESS BY INDEX ROWID| BAUMANN       |     1 |     8 |     2   (0)| 00:00:01 |
|   6 |      INDEX FULL SCAN           | SYS_C008307   |     1 |       |     1   (0)| 00:00:01 |
|*  7 |     FILTER                     |               |       |       |            |          |
|*  8 |      SORT JOIN                 |               |   200 | 16800 |     4  (25)| 00:00:01 |
|*  9 |       TABLE ACCESS FULL        | PRODUCT       |   200 | 16800 |     3   (0)| 00:00:01 |
------------------------------------------------------------------------------------------------
```

### 6.2 리뷰 검색 SQL

Plan hash value `4282550867`. 예상 Cost 423.

계획에 보이는 연산: `REVIEW` full table scan, `DELETED_AT IS NULL`, content·nickname `LIKE`, `USER_TABLE` full table scan, `BAUMANN` index full scan, hash join, merge join, `REVIEW_IMAGES` index unique scan(`SYS_C008373`), `PRODUCT_IMAGE` full table scan, `ROW_NUMBER()`, sort order by.

```text
Plan hash value: 4282550867

----------------------------------------------------------------------------------------------------
| Id  | Operation                        | Name            | Rows  | Bytes | Cost (%CPU)| Time     |
----------------------------------------------------------------------------------------------------
|   0 | SELECT STATEMENT                 |                 |   290 |   104K|   423   (1)| 00:00:01 |
|*  1 |  VIEW                            |                 |     1 |   265 |     2   (0)| 00:00:01 |
|*  2 |   WINDOW BUFFER PUSHED RANK      |                 |     1 |   267 |     2   (0)| 00:00:01 |
|*  3 |    TABLE ACCESS FULL             | PRODUCT_IMAGE   |     1 |   267 |     2   (0)| 00:00:01 |
|   4 |  SORT ORDER BY                   |                 |   290 |   104K|   423   (1)| 00:00:01 |
|*  5 |   HASH JOIN                      |                 |   290 |   104K|    16   (7)| 00:00:01 |
|   6 |    TABLE ACCESS FULL             | PRODUCT         |   200 |  7800 |     3   (0)| 00:00:01 |
|   7 |    MERGE JOIN OUTER              |                 |   290 | 95410 |    13   (8)| 00:00:01 |
|*  8 |     HASH JOIN                    |                 |   290 | 22330 |    13   (8)| 00:00:01 |
|   9 |      MERGE JOIN                  |                 |    20 |   600 |     6  (17)| 00:00:01 |
|  10 |       TABLE ACCESS BY INDEX ROWID| BAUMANN         |     1 |     8 |     2   (0)| 00:00:01 |
|  11 |        INDEX FULL SCAN           | SYS_C008307     |     1 |       |     1   (0)| 00:00:01 |
|* 12 |       SORT JOIN                  |                 |    20 |   440 |     4  (25)| 00:00:01 |
|  13 |        TABLE ACCESS FULL         | USER_TABLE      |    20 |   440 |     3   (0)| 00:00:01 |
|* 14 |      TABLE ACCESS FULL           | REVIEW          |  2000 | 94000 |     7   (0)| 00:00:01 |
|  15 |     BUFFER SORT                  |                 |     1 |   252 |     6  (17)| 00:00:01 |
|  16 |      VIEW                        | VW_LAT_1D7E31DF |     1 |   252 |     0   (0)| 00:00:01 |
|* 17 |       TABLE ACCESS BY INDEX ROWID| REVIEW_IMAGES   |     1 |   278 |     0   (0)| 00:00:01 |
|* 18 |        INDEX UNIQUE SCAN         | SYS_C008373     |     1 |       |     0   (0)| 00:00:01 |
|  19 |         SORT AGGREGATE           |                 |     1 |    26 |            |          |
|* 20 |          TABLE ACCESS FULL       | REVIEW_IMAGES   |     1 |    26 |     2   (0)| 00:00:01 |
----------------------------------------------------------------------------------------------------
```

## 7. 해석

Perf DB 상품 검색 SQL 10회 평균은 6.403ms, 최댓값은 8.289ms이고 매 회 200행이다. Perf DB 리뷰 검색 SQL 10회 평균은 15.950ms, 최댓값은 18.577ms이고 매 회 2,000행이다.

Fixture 기능 케이스 8개는 단언을 통과했다. 브랜드 값을 카테고리 인자에 넣은 결과는 0건이다. 이 결과는 매퍼 직접 호출이다.

개선율과 p95는 계산하지 않는다.
