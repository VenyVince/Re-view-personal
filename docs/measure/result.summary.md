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

실행 계획 수집과 시간 측정은 따로 본다. 네 SQL의 계획 원문은 각 상세 파일에 있다. 네 SQL을 한 표로 비교한 분석은 아직 없다.

## 3. 제한

추천 레거시 SQL은 measure 커넥션 결과라서 Perf 시간과 개선율로 바꾸지 않았다. 검색의 브랜드를 카테고리 인자에 넣은 0건은 매퍼 직접 호출이다. 예외 없이 반환한 Best 호출 수는 상세 결과에 없으므로 여기에도 없다.
