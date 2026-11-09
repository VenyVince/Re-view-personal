# 정량 근거 보완 재측정

기준: `docs/measure_v2/measure.roadmap.v2.md`

이 로드맵은 버전 표를 두지 않는다. 아래는 그 로드맵의 측정 작업 전체를 한 계획으로 적는다.

## 1. 목표

기존 측정 테스트를 클래스별로 다시 실행하고, fixture DB와 perf DB의 결과를 구분한 기록과 그 기록에 맞는 이력서 R 초안을 남긴다.

## 2. 구현 내용

- 측정 환경의 사전 확인
- 일반 포인트 동시성 정합성의 재측정
- 추천 SQL 실행 시간의 재측정
- 검색 SQL 실행 시간의 재측정
- 추천·검색 SQL 실행 계획의 기록
- 측정 결과 파일의 작성
- 이력서 R 문장과 측정 근거의 대조

## 3. 상세 구현 내용

### 3.1 측정 환경의 사전 확인

**구현**

- `src/test/resources/application-measure.properties` 유지
- Oracle XE `localhost:1522/XEPDB1` 기동 확인
- fixture 스키마 `MEASURE` / `measure_local` 접속 확인
- perf 스키마 `MEASURE_PERF` / `measure_local` 접속 확인
- fixture 건수 확인: 상품 20, 리뷰 40
- perf 건수 확인: 사용자 20, 상품 200, 리뷰 2,000
- JDK 21과 `./mvnw` 동작 확인
- 운영 코드와 측정 테스트 코드는 이 항목에서 수정하지 않음

**완료**

- [x] Oracle XE가 `localhost:1522/XEPDB1`로 기동되어 있다
- [x] `MEASURE`와 `MEASURE_PERF`에 접속된다
- [x] fixture 건수가 상품 20, 리뷰 40이다
- [x] perf 건수가 사용자 20, 상품 200, 리뷰 2,000이다
- [x] JDK 21에서 `./mvnw`가 동작한다

### 3.2 일반 포인트 동시성 정합성의 재측정

**구현**

- `src/test/java/org/spring/project/re_view/measure/PointConcurrencyMeasureTest.java` 유지
- 대상 메서드 `concurrentEarnAndUseThenBestAward` 유지
- 측정 조건 유지: 사용자 1, 초기 잔액 10,000, 적립 100원 10건, 차감 100원 10건, 셔플 시드 `20261104`, 5라운드
- best 리뷰 포인트 유지: 700원, 10스레드, 이력 행 수 집계
- 실행 명령 `./mvnw -Dtest=PointConcurrencyMeasureTest test`
- 콘솔에서 라운드별 `earnSuccess`, `useSuccess`, `failures`, `expected`, `point`, `history` 확인
- 콘솔에서 `balanceHistoryMismatches`, `bestRows`, `bestFailures` 확인

**완료**

- [x] `PointConcurrencyMeasureTest`가 단독으로 실행된다
- [x] 5라운드의 성공 건수, 실패 건수, `expected`, `point`, `history`가 콘솔에 있다
- [x] `balanceHistoryMismatches`가 콘솔에 있다
- [x] `bestRows`와 `bestFailures`가 콘솔에 있다

### 3.3 추천 SQL 실행 시간의 재측정

**구현**

- `src/test/java/org/spring/project/re_view/measure/RecommendSqlMeasureTest.java` 유지
- 대상 메서드 `recommendQueriesOnFixtureAndTwoThousandReviews` 유지
- 대상 Statement 유지: `findRecommendProducts`, `findRecommendReviews`
- 측정 방식 유지: JDBC, 워밍업 2회, 측정 10회, 평균과 최댓값
- fixture DB와 perf DB를 각각 측정
- `e7c2fa82` 레거시 상품 추천 SQL의 실행 시간과 결과 ID 차이(`onlyCurrent`, `onlyLegacy`) 확인
- 실행 명령 `./mvnw -Dtest=RecommendSqlMeasureTest test`
- 이력서에 쓸 실행 시간은 perf DB 값으로 구분

**완료**

- [x] 상품 추천 SQL의 fixture 평균, 최댓값, 10회 표본이 콘솔에 있다
- [x] 리뷰 추천 SQL의 fixture 평균, 최댓값, 10회 표본이 콘솔에 있다
- [x] 두 SQL의 perf 평균, 최댓값, 10회 표본이 콘솔에 있다
- [x] 레거시 SQL의 `rowsPerRun`, 평균, 최댓값, `onlyCurrent`, `onlyLegacy`가 콘솔에 있다

### 3.4 검색 SQL 실행 시간의 재측정

**구현**

- `src/test/java/org/spring/project/re_view/measure/HeaderSearchMeasureTest.java` 유지
- 대상 메서드 `headerSearchCasesAndCurrentSqlTime` 유지
- 대상 Statement 유지: `HeaderSearchProductMapper.searchProducts`, `HeaderSearchReviewMapper.searchReviews`
- fixture DB에서 상품명, 부분 일치, 최신순, 저가순, 브랜드, 1글자, 리뷰 키워드 결과 ID 확인
- 브랜드 값을 카테고리 인자 자리에 넣은 호출의 결과 건수 확인
- fixture DB와 perf DB에서 워밍업 2회, 측정 10회
- 실행 명령 `./mvnw -Dtest=HeaderSearchMeasureTest test`
- 상품명 검색과 브랜드 검색이 같은 `searchProducts`라는 점을 결과에 구분

**완료**

- [ ] fixture의 검색 케이스별 결과 ID가 테스트 단언과 일치한다
- [ ] 브랜드 값을 카테고리 인자 자리에 넣은 결과가 0건이다
- [ ] 상품 검색 SQL의 perf 평균, 최댓값, 10회 표본이 콘솔에 있다
- [ ] 리뷰 검색 SQL의 perf 평균, 최댓값, 10회 표본이 콘솔에 있다

### 3.5 추천·검색 SQL 실행 계획의 기록

**구현**

- 실행 계획 수집 코드 유지: `RecommendSqlMeasureTest`, `HeaderSearchMeasureTest`의 perf DB `EXPLAIN PLAN FOR`와 `DBMS_XPLAN.DISPLAY`
- 별도 측정 클래스 추가 없음
- 3.3과 3.4 콘솔의 `explain product`, `explain review` 원문 보관
- 기록 칸: 테이블 접근, 사용 인덱스, 조인 방식, 정렬 연산, 윈도우 함수, Cost, 확인된 특이사항
- 계획에 없는 연산은 추정으로 채우지 않음

**완료**

- [ ] 추천 상품 SQL의 실행 계획 원문이 있다
- [ ] 추천 리뷰 SQL의 실행 계획 원문이 있다
- [ ] 검색 상품 SQL의 실행 계획 원문이 있다
- [ ] 검색 리뷰 SQL의 실행 계획 원문이 있다
- [ ] 네 SQL의 분석 표에 계획에서 확인된 값만 적혀 있다

### 3.6 측정 결과 파일의 작성

**구현**

- 결과 파일 추가: `docs/measure/result.concurrency.md`
- 결과 파일 추가: `docs/measure/result.recommend.md`
- 결과 파일 추가: `docs/measure/result.search.md`
- 결과 파일 추가: `docs/measure/result.summary.md`
- 각 파일 머리말에 실행 일시, 기준 커밋, DB 건수, 프로파일 `measure` 기록
- 3.2의 동시성 수치, 3.3의 추천 시간, 3.4의 검색 시간, 3.5의 실행 계획을 해당 파일에만 옮김
- 콘솔에 없는 수치는 비워 둠

**완료**

- [ ] 동시성 결과 파일에 5라운드 표와 `balanceHistoryMismatches`, `bestRows`, `bestFailures`가 있다
- [ ] 추천 결과 파일에 fixture·perf 평균, 최댓값, 10회 표본, 레거시 비교가 있다
- [ ] 검색 결과 파일에 기능 케이스와 perf 평균, 최댓값이 있다
- [ ] 통합 요약 파일에 위 세 결과의 핵심 수치만 있다
- [ ] 네 파일의 머리말에 실행 일시, 기준 커밋, DB 건수, 프로파일이 있다

### 3.7 이력서 R 문장과 측정 근거의 대조

**구현**

- 초안 파일 추가: `docs/measure/resume.R.md`
- 3.6에 기록된 값만 문장에 사용
- 개선 전후를 같은 조건에서 비교하지 않은 항목은 개선율로 쓰지 않음
- 10회 측정값으로 p95를 쓰지 않음
- 매퍼 직접 호출 결과를 서비스 검색의 정상 동작 근거로 쓰지 않음
- best 적립 결과는 정합성 근거로만 사용

**완료**

- [ ] `docs/measure/resume.R.md`의 각 문장에 대응 측정값이 있다
- [ ] 측정 파일에 없는 수치가 문장에 없다
- [ ] 개선율과 p95가 문장에 없다
