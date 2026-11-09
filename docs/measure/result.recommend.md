# 추천 SQL 실행 시간 결과

## 1. 머리말

| 항목 | 값 |
|---|---|
| 측정 실행 일시 | 2026-11-09 16:48:44 |
| 측정 기준 커밋 | `dd876b2d` |
| 로드맵 참고 커밋 | `c95dd83d` |
| 측정 시점 작업 트리 | 없음 |
| 프로파일 | measure |
| JDK | 21.0.8 |
| DB 접속 | `localhost:1522/XEPDB1` |
| Fixture DB | `MEASURE` / `measure_local` |
| Perf DB | `MEASURE_PERF` / `measure_local` |
| 이번 실행 Fixture 건수 | 상품 20, 리뷰 40 |
| 이번 실행 Perf 건수 | 사용자 20, 상품 200, 리뷰 2,000 |
| Oracle | 21c Express Edition Release 21.0.0.0.0. 2026-11-08 조회. 이번 실행에서 `v$version`은 다시 조회하지 않음 |

원본 로그: `target/surefire-reports/TEST-org.spring.project.re_view.measure.RecommendSqlMeasureTest.xml`의 `system-out`.

명령:

```bash
./mvnw -Dsurefire.excludedGroups= -Dtest=RecommendSqlMeasureTest test
```

## 2. 측정 상태

실행 완료. `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`. `BUILD SUCCESS`. 경과 8.584초.

## 3. 실행 조건

| 항목 | 조건 |
|---|---|
| 클래스 | `RecommendSqlMeasureTest` |
| 메서드 | `recommendQueriesOnFixtureAndTwoThousandReviews` |
| Statement | `findRecommendProducts`, `findRecommendReviews` |
| 사용자 축 | `D, S, P, W` |
| 워밍업 | 2회. 시간 표본에 포함하지 않음 |
| 측정 | 10회 |
| JDBC 구간 | `readCount` 직전부터 반환 직후. `PreparedStatement` 생성, 바인딩, `executeQuery()`, 마지막 행까지 읽기, Statement 닫기 |

이력서에 쓸 실행 시간은 Perf DB 값으로 구분한다. 레거시 SQL은 measure 커넥션에서 실행됐다. 같은 조건의 개선 전후가 아니므로 개선율은 계산하지 않는다.

콘솔의 SQL 문자열 확인: `productSqlHasRowNumber=false`, `reviewSqlHasRowNumber=true`.

## 4. Fixture DB

### 4.1 상품 추천 SQL

| 항목 | 값 |
|---|---|
| rows | 16, 16, 16, 16, 16, 16, 16, 16, 16, 16 |
| 평균 | 2.591ms |
| 최댓값 | 2.911ms |
| 10회 표본 | 2.669, 2.884, 2.404, 2.661, 2.623, 2.911, 2.380, 2.427, 2.320, 2.630 |

### 4.2 리뷰 추천 SQL

| 항목 | 값 |
|---|---|
| rows | 16, 16, 16, 16, 16, 16, 16, 16, 16, 16 |
| 평균 | 6.678ms |
| 최댓값 | 7.890ms |
| 10회 표본 | 6.565, 7.890, 7.194, 6.964, 6.381, 6.225, 7.510, 6.566, 5.642, 5.844 |

## 5. Perf DB

### 5.1 상품 추천 SQL

| 항목 | 값 |
|---|---|
| rows | 16, 16, 16, 16, 16, 16, 16, 16, 16, 16 |
| 평균 | 2.501ms |
| 최댓값 | 2.688ms |
| 10회 표본 | 2.406, 2.531, 2.558, 2.586, 2.688, 2.380, 2.330, 2.302, 2.687, 2.545 |

### 5.2 리뷰 추천 SQL

| 항목 | 값 |
|---|---|
| rows | 16, 16, 16, 16, 16, 16, 16, 16, 16, 16 |
| 평균 | 12.465ms |
| 최댓값 | 13.556ms |
| 10회 표본 | 12.021, 12.805, 13.274, 12.144, 13.556, 12.590, 11.485, 11.790, 12.679, 12.305 |

## 6. 레거시 상품 추천 SQL

대상은 `e7c2fa82`의 `findRecommencementsWithAll`이다. 실행 커넥션은 measure 커넥션이다. 리뷰 추천 레거시 SQL은 그 커밋에 없다고 콘솔에 있다.

| 항목 | 값 |
|---|---|
| rowsPerRun | 19, 19, 19, 19, 19, 19, 19, 19, 19, 19 |
| 평균 | 5.052ms |
| 최댓값 | 6.412ms |
| 10회 표본 | 4.538, 6.412, 5.153, 4.816, 5.634, 5.130, 4.362, 4.245, 5.539, 4.690 |
| distinctIds | 19 |
| onlyCurrent | `[]` |
| onlyLegacy | `[3, 17, 2]` |

## 7. 예상 실행 계획

Perf DB에서 `EXPLAIN PLAN FOR`와 `DBMS_XPLAN.DISPLAY()`로 수집했다. 실제 실행 통계가 아니다. 검색 SQL 계획은 이 테스트에 없다.

### 7.1 상품 추천 SQL

Plan hash value `2939117215`. 예상 Cost 28.

계획에 보이는 연산: `PRODUCT` full table scan, `BAUMANN` index full scan(`SYS_C008307`), merge join, `PRODUCT_IMAGE` full table scan, window sort pushed rank, `ROW_NUMBER()`, sort order by.

SQL 문자열 확인은 `ROW_NUMBER` 없음이다. 예상 계획의 `ROW_NUMBER`는 위 계획 원문에 있다.

```text
Plan hash value: 2939117215

-----------------------------------------------------------------------------------------------------
| Id  | Operation                         | Name            | Rows  | Bytes | Cost (%CPU)| Time     |
-----------------------------------------------------------------------------------------------------
|   0 | SELECT STATEMENT                  |                 |    16 |  8160 |    28  (11)| 00:00:01 |
|   1 |  SORT ORDER BY                    |                 |    16 |  8160 |    28  (11)| 00:00:01 |
|*  2 |   VIEW                            |                 |    16 |  8160 |    27   (8)| 00:00:01 |
|*  3 |    WINDOW SORT PUSHED RANK        |                 |    10 |  3060 |    27   (8)| 00:00:01 |
|   4 |     MERGE JOIN OUTER              |                 |    10 |  3060 |    26   (4)| 00:00:01 |
|   5 |      MERGE JOIN                   |                 |    10 |   540 |     6  (17)| 00:00:01 |
|   6 |       TABLE ACCESS BY INDEX ROWID | BAUMANN         |     1 |    11 |     2   (0)| 00:00:01 |
|   7 |        INDEX FULL SCAN            | SYS_C008307     |     1 |       |     1   (0)| 00:00:01 |
|*  8 |       FILTER                      |                 |       |       |            |          |
|*  9 |        SORT JOIN                  |                 |   200 |  8600 |     4  (25)| 00:00:01 |
|  10 |         TABLE ACCESS FULL         | PRODUCT         |   200 |  8600 |     3   (0)| 00:00:01 |
|  11 |      BUFFER SORT                  |                 |     1 |   252 |    24   (9)| 00:00:01 |
|  12 |       VIEW                        | VW_LAT_4AA3951B |     1 |   252 |     2   (0)| 00:00:01 |
|  13 |        VIEW                       | VW_LAT_1BBF5C63 |     1 |   252 |     2   (0)| 00:00:01 |
|* 14 |         VIEW                      |                 |     1 |   265 |     2   (0)| 00:00:01 |
|* 15 |          WINDOW BUFFER PUSHED RANK|                 |     1 |   267 |     2   (0)| 00:00:01 |
|* 16 |           TABLE ACCESS FULL       | PRODUCT_IMAGE   |     1 |   267 |     2   (0)| 00:00:01 |
-----------------------------------------------------------------------------------------------------
```

### 7.2 리뷰 추천 SQL

Plan hash value `381092898`. 예상 Cost 4281.

계획에 보이는 연산: `REVIEW`, `PRODUCT`, `USER_TABLE` full table scan, `BAUMANN` index full scan, hash join, merge join, window sort pushed rank, `ROW_NUMBER()`, `REVIEW_IMAGES` index rowid, `PRODUCT_IMAGE` full table scan. TempSpc 1240K가 계획에 있다.

```text
Plan hash value: 381092898

------------------------------------------------------------------------------------------------------------------
| Id  | Operation                              | Name            | Rows  | Bytes |TempSpc| Cost (%CPU)| Time     |
------------------------------------------------------------------------------------------------------------------
|   0 | SELECT STATEMENT                       |                 |    16 | 16208 |       |  4281   (1)| 00:00:01 |
|*  1 |  VIEW                                  |                 |    16 | 16208 |       |  4281   (1)| 00:00:01 |
|*  2 |   WINDOW SORT PUSHED RANK              |                 |   200 |   192K|       |  4281   (1)| 00:00:01 |
|*  3 |    VIEW                                |                 |   200 |   192K|       |  4280   (1)| 00:00:01 |
|*  4 |     WINDOW SORT PUSHED RANK            |                 |  2000 |  1191K|  1240K|  4280   (1)| 00:00:01 |
|   5 |      MERGE JOIN OUTER                  |                 |  2000 |  1191K|       |  4019   (1)| 00:00:01 |
|   6 |       MERGE JOIN OUTER                 |                 |  2000 |   699K|       |    19   (6)| 00:00:01 |
|*  7 |        HASH JOIN                       |                 |  2000 |   207K|       |    19   (6)| 00:00:01 |
|   8 |         TABLE ACCESS FULL              | BAUMANN         |     1 |    11 |       |     3   (0)| 00:00:01 |
|*  9 |         HASH JOIN                      |                 |  2000 |   185K|       |    16   (7)| 00:00:01 |
|  10 |          TABLE ACCESS FULL             | PRODUCT         |   200 |  5200 |       |     3   (0)| 00:00:01 |
|* 11 |          HASH JOIN                     |                 |  2000 |   134K|       |    13   (8)| 00:00:01 |
|  12 |           MERGE JOIN                   |                 |    20 |   660 |       |     6  (17)| 00:00:01 |
|  13 |            TABLE ACCESS BY INDEX ROWID | BAUMANN         |     1 |    11 |       |     2   (0)| 00:00:01 |
|  14 |             INDEX FULL SCAN            | SYS_C008307     |     1 |       |       |     1   (0)| 00:00:01 |
|* 15 |            SORT JOIN                   |                 |    20 |   440 |       |     4  (25)| 00:00:01 |
|  16 |             TABLE ACCESS FULL          | USER_TABLE      |    20 |   440 |       |     3   (0)| 00:00:01 |
|  17 |           TABLE ACCESS FULL            | REVIEW          |  2000 | 72000 |       |     7   (0)| 00:00:01 |
------------------------------------------------------------------------------------------------------------------
```

아래쪽 Id 18–30은 리뷰 이미지와 상품 이미지 lateral view다. 원문 전체는 위 XML `system-out`에 있다.

## 8. 해석

Perf DB 상품 추천 SQL 10회 평균은 2.501ms, 최댓값은 2.688ms다. Perf DB 리뷰 추천 SQL 10회 평균은 12.465ms, 최댓값은 13.556ms다. 각 회차 반환 행 수는 16이다.

레거시 상품 SQL은 measure 커넥션에서 회차마다 19행이다. 현재 Fixture 결과에는 없고 레거시 결과에만 있는 상품 ID는 3, 17, 2다. 현재 결과에만 있는 ID는 없다. 레거시 리뷰 SQL은 실행하지 않았다.

개선율은 계산하지 않는다. p95는 계산하지 않는다.
