# 정량 측정 통합 요약

세부 결과는 아래 파일만 사용한다.

- `docs/measure/result.concurrency.md`
- `docs/measure/result.recommend.md`
- `docs/measure/result.search.md`

이 파일에 그 세 파일에 없는 수치는 없다.

## 1. 머리말

| 항목 | 값 |
|---|---|
| 측정 실행 일시 | 항목별. 동시성만 2026-11-08 23:26:47 |
| 측정 기준 커밋 | 동시성 실행 시점 `f5dd54a6`. 추천·검색은 미측정 |
| 프로파일 | measure |
| DB 접속 | `localhost:1522/XEPDB1` |
| Fixture 건수 | 환경 확인 시점 사용자 5, 상품 20, 리뷰 40 |
| Perf 건수 | 환경 확인 시점 사용자 20, 상품 200, 리뷰 2,000 |
| 환경 확인 일시 | 2026-11-08 22:40:09 |
| 파일 작성 시점 HEAD | `f5dd54a6` |

## 2. 측정 항목

| 항목 | 상태 | 핵심 결과 |
|---|---|---|
| 포인트 동시성 | 실행 완료 | 5라운드 모두 expected=point=history=10000, failures=0, balanceHistoryMismatches=0 |
| Best 리뷰 포인트 | 실행 완료 | bestRows=1, bestFailures=0 |
| 추천 SQL Fixture | 미측정 | 없음 |
| 추천 SQL Perf | 미측정 | 없음 |
| 레거시 상품 추천 SQL | 미측정 | 없음 |
| 검색 SQL Fixture | 미측정 | 없음 |
| 검색 SQL Perf | 미측정 | 없음 |
| 예상 실행 계획 | 미수집 | 없음 |

실행 계획 수집 여부는 측정 성공 여부와 별도다. 둘 다 아직 없다.

## 3. 제한

동시성 테스트만 실행했다. 추천 SQL, 검색 SQL, 예상 실행 계획은 미측정이다. 예외 없이 반환한 Best 호출 수는 상세 결과에 없으므로 여기에도 없다.
