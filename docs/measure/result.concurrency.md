# 일반 포인트 동시성 정합성 결과

## 1. 머리말

| 항목 | 값 |
|---|---|
| 측정 실행 일시 | 2026-11-08 23:26:47 |
| 측정 기준 커밋 | `f5dd54a6` |
| 로드맵 참고 커밋 | `c95dd83d` |
| 측정 시점 작업 트리 | `AGENTS.md`, `docs/measure_v2/Plan/plan.md` 수정. `docs/measure/` 미추적 |
| 프로파일 | measure |
| JDK | 21.0.8 |
| DB 접속 | `localhost:1522/XEPDB1` |
| 사용 스키마 | Fixture `MEASURE` / `measure_local` |
| Perf DB | 이 측정은 사용하지 않음 |
| Fixture 건수 | 2026-11-08 22:40:09 환경 확인 값. 상품 20, 리뷰 40, 사용자 5, 주문상품 4, user 1 잔액 10,000. 이 테스트가 다시 세지는 않음 |
| Oracle | 21c Express Edition Release 21.0.0.0.0. 환경 확인 때 조회 |

원본 로그: `target/surefire-reports/TEST-org.spring.project.re_view.measure.PointConcurrencyMeasureTest.xml`의 `system-out`. 요약 파일 `target/surefire-reports/org.spring.project.re_view.measure.PointConcurrencyMeasureTest.txt`에는 `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 8.270 s`만 있다.

명령:

```bash
./mvnw -Dsurefire.excludedGroups= -Dtest=PointConcurrencyMeasureTest test
```

## 2. 측정 상태

실행 완료. `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`. `BUILD SUCCESS`.

## 3. 실행 조건

| 항목 | 조건 |
|---|---|
| 클래스 | `PointConcurrencyMeasureTest` |
| 메서드 | `concurrentEarnAndUseThenBestAward` |
| 사용자 | `user_id` 1, `measure_user` |
| 초기 잔액 | 10,000 |
| 초기 이력 | 금액 10,000, 타입 `EARN`, 설명 `시드 잔액` 1건 |
| 적립 | 100원 10건, `addReviewPoint` |
| 차감 | 100원 10건, `usePoint` |
| 라운드 | 5 |
| 셔플 시드 | `20261104` |
| Best | 700원, 10스레드, 리뷰 2 |

기록 기준:

- `expected` = 10,000 + `earnSuccess` × 100 - `useSuccess` × 100
- `history`는 시드 `EARN`을 포함한 이력 합이다. 초기 잔액을 다시 더하지 않는다.
- `balanceHistoryMismatches`는 `point`와 `history`가 다른 라운드 수다.
- `bestRows`는 사용자 1, 리뷰 2, 700원 `EARN` 행 수다.
- `bestFailures`는 예외 발생 건수다. 예외 없이 반환한 호출 수는 출력되지 않았다.

## 4. 라운드 결과

| 회차 | 적립 성공 | 차감 성공 | 실패 | expected | point | history | point!=history |
|---|---:|---:|---:|---:|---:|---:|---|
| 1 | 10 | 10 | 0 | 10000 | 10000 | 10000 | 없음 |
| 2 | 10 | 10 | 0 | 10000 | 10000 | 10000 | 없음 |
| 3 | 10 | 10 | 0 | 10000 | 10000 | 10000 | 없음 |
| 4 | 10 | 10 | 0 | 10000 | 10000 | 10000 | 없음 |
| 5 | 10 | 10 | 0 | 10000 | 10000 | 10000 | 없음 |

## 5. 집계

| 항목 | 값 |
|---|---|
| `balanceHistoryMismatches` | 0 |
| `bestRows` | 1 |
| `bestFailures` | 0 |

## 6. 해석

5라운드 모두 출력된 `expected`, `point`, `history`가 10,000으로 같았다. `balanceHistoryMismatches`는 0이다. 테스트는 `earnSuccess + useSuccess == 20` 단언을 통과했다.

Best 호출 10회 뒤 해당 700원 `EARN` 행은 1건이고, 예외 목록은 0건이다. 예외 없이 반환한 호출 수는 로그에 없으므로 그 횟수는 적지 않는다.
