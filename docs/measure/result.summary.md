# 정량 측정 통합 요약

세부 결과는 아래 파일만 사용한다.

- `docs/measure/result.concurrency.md`
- `docs/measure/result.recommend.md`
- `docs/measure/result.search.md`

이 파일에 그 세 파일에 없는 수치는 없다.

## 1. 머리말

| 항목 | 값 |
|---|---|
| 측정 실행 일시 | 동시성 2026-11-08 23:26:47. 추천 SQL 2026-11-09 16:48:44. 검색 SQL 2026-11-09 16:50:09 |
| 측정 기준 커밋 | 동시성 `f5dd54a6`. 추천 SQL과 검색 SQL `dd876b2d` |
| 프로파일 | measure |
| DB 접속 | `localhost:1522/XEPDB1` |
| Fixture 건수 | 환경 확인과 추천 SQL 실행 모두 상품 20, 리뷰 40. 사용자 5는 환경 확인 값 |
| Perf 건수 | 환경 확인, 추천 SQL, 검색 SQL 실행 모두 사용자 20, 상품 200, 리뷰 2,000 |
| 환경 확인 일시 | 2026-11-08 22:40:09 |
| 파일 작성 시점 HEAD | `f5dd54a6` |

## 2. 측정 항목

| 항목 | 상태 | 핵심 결과 |
|---|---|---|
| 포인트 동시성 | 실행 완료 | 5라운드 모두 expected=point=history=10000, failures=0, balanceHistoryMismatches=0 |
| Best 리뷰 포인트 | 실행 완료 | bestRows=1, bestFailures=0 |
| 추천 SQL Fixture | 실행 완료 | 상품 평균 2.591ms, 최댓값 2.911ms. 리뷰 평균 6.678ms, 최댓값 7.890ms |
| 추천 SQL Perf | 실행 완료 | 상품 평균 2.501ms, 최댓값 2.688ms. 리뷰 평균 12.465ms, 최댓값 13.556ms |
| 레거시 상품 추천 SQL | 실행 완료 | measure 커넥션 평균 5.052ms, 최댓값 6.412ms. onlyCurrent=[], onlyLegacy=[3, 17, 2] |
| 검색 SQL Fixture | 실행 완료 | 상품 평균 1.957ms, 최댓값 2.933ms. 리뷰 평균 3.440ms, 최댓값 5.468ms |
| 검색 SQL Perf | 실행 완료 | 상품 평균 6.403ms, 최댓값 8.289ms. 리뷰 평균 15.950ms, 최댓값 18.577ms |
| 예상 실행 계획 | 추천 2개, 검색 2개 수집 | 상품 추천 예상 Cost 28. 리뷰 추천 예상 Cost 4281. 상품 검색 예상 Cost 9. 리뷰 검색 예상 Cost 423 |

실행 계획 수집과 시간 측정은 따로 본다. 비교 표는 아래 4절이다. 원문은 `result.recommend.md`와 `result.search.md`에 있다.

## 3. 예상 실행 계획 비교

Perf DB의 `EXPLAIN PLAN FOR`와 `DBMS_XPLAN.DISPLAY()` 결과만 적는다. 실제 실행 통계가 아니다.

| 분석 항목 | 상품 추천 SQL | 리뷰 추천 SQL | 상품 검색 SQL | 리뷰 검색 SQL |
|---|---|---|---|---|
| Plan hash | 2939117215 | 381092898 | 3623065135 | 4282550867 |
| 테이블 접근 | `PRODUCT` full, `PRODUCT_IMAGE` full, `BAUMANN` index rowid | `REVIEW` full, `PRODUCT` full, `USER_TABLE` full, `BAUMANN` full, `PRODUCT_IMAGE` full, `REVIEW_IMAGES` full | `PRODUCT` full, `PRODUCT_IMAGE` full | `REVIEW` full, `PRODUCT` full, `USER_TABLE` full, `PRODUCT_IMAGE` full, `REVIEW_IMAGES` full |
| 인덱스 | `SYS_C008307` index full scan | `SYS_C008307` index full scan, `SYS_C008373` index full scan | `SYS_C008307` index full scan | `SYS_C008307` index full scan, `SYS_C008373` index unique scan |
| 조인 | merge join, merge join outer | hash join, merge join, merge join outer | hash join right outer, merge join | hash join, merge join, merge join outer |
| 정렬 | sort order by, sort join, window sort pushed rank | window sort pushed rank, sort join | sort order by, sort join | sort order by, sort join, sort aggregate |
| 윈도우 함수 | 계획에 `ROW_NUMBER`. SQL 문자열 확인은 없음 | 계획에 `ROW_NUMBER` | 계획에 없음 | 계획에 `ROW_NUMBER` |
| 예상 Cost | 28 | 4281 | 9 | 423 |
| 계획에 보이는 특이사항 | 상위 16행 제한. 점수 조건 filter | TempSpc 1240K. 상품별 `RN=1` | `DELETED_AT IS NULL`. 이름·브랜드·성분·바우만 `LIKE` | `DELETED_AT IS NULL`. content·nickname `LIKE` |

## 4. 제한

추천 레거시 SQL은 measure 커넥션 결과라서 Perf 시간과 개선율로 바꾸지 않았다. 검색의 브랜드를 카테고리 인자에 넣은 0건은 매퍼 직접 호출이다. 예외 없이 반환한 Best 호출 수는 상세 결과에 없으므로 여기에도 없다.
