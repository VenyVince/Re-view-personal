# 정량 측정 통합 요약

세부 결과는 아래 파일만 사용한다.

- `docs/measure/result.concurrency.md`
- `docs/measure/result.recommend.md`
- `docs/measure/result.search.md`

이 파일에 그 세 파일에 없는 수치는 없다.

## 1. 머리말

| 항목 | 값 |
|---|---|
| 측정 실행 일시 | 동시성 2026-11-08 23:26:47. 추천 SQL 2026-11-09 16:48:44. 검색은 미측정 |
| 측정 기준 커밋 | 동시성 `f5dd54a6`. 추천 SQL `dd876b2d`. 검색은 미측정 |
| 프로파일 | measure |
| DB 접속 | `localhost:1522/XEPDB1` |
| Fixture 건수 | 환경 확인과 추천 SQL 실행 모두 상품 20, 리뷰 40. 사용자 5는 환경 확인 값 |
| Perf 건수 | 환경 확인과 추천 SQL 실행 모두 사용자 20, 상품 200, 리뷰 2,000 |
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
| 검색 SQL Fixture | 미측정 | 없음 |
| 검색 SQL Perf | 미측정 | 없음 |
| 예상 실행 계획 | 추천 2개 수집. 검색 미수집 | 상품 추천 예상 Cost 28. 리뷰 추천 예상 Cost 4281 |

실행 계획 수집과 시간 측정은 따로 본다. 검색 SQL은 둘 다 아직 없다.

## 3. 제한

검색 SQL은 미측정이다. 추천 레거시 SQL은 measure 커넥션 결과라서 Perf 시간과 개선율로 바꾸지 않았다. 예외 없이 반환한 Best 호출 수는 상세 결과에 없으므로 여기에도 없다.
