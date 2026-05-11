# Day 8 - Scheduler and Review Reward Flow Check

## 목표

베스트 리뷰 갱신용 스케줄러를 실제로 활성화할지 결정하고, 리뷰 보상 흐름이 코드와 README 설명에 맞는지 확인한다.

## 작업 체크리스트

- [x] `ReviewScheduler`를 확인한다.
- [x] 애플리케이션 시작 클래스에 `@EnableScheduling`이 필요한지 확인한다.
- [x] 스케줄러를 자동 활성화할지, 수동/후속 개선으로 둘지 결정한다.
- [x] 리뷰 작성 보상 흐름을 확인한다.
- [x] 리뷰 삭제 시 포인트 회수 흐름을 확인한다.
- [x] 베스트 리뷰 보상 흐름을 확인한다.
- [x] 관리자 선정 리뷰 보상 흐름을 확인한다.
- [x] 자동 스케줄 활성화 시 로컬/Docker 환경에서 의도치 않은 DB 변경이 생기지 않는지 검토한다.
- [x] README에 적을 리뷰 보상 설명 초안을 작성한다.

## 확인할 파일

- `src/main/java/com/review/shop/util/ReviewScheduler.java`
- `src/main/java/org/spring/project/re_view/ReViewApplication.java`
- `src/main/java/com/review/shop/service/review/**/*.java`
- `src/main/java/com/review/shop/service/admin/**/*.java`
- `src/main/resources/mapper/review/**/*.xml`

## 결정 기준

- 스케줄러가 실제 서비스 동작에 필수이면 명시적으로 활성화한다.
- 포트폴리오 안정화 기간에 자동 DB 변경이 위험하면 후속 개선으로 둔다.
- README에는 실제 활성화 상태만 적는다.

## 결과물

- 스케줄러 처리 결정 메모
- README 수정을 위한 리뷰 보상 흐름 메모

## 작업 결과 요약

- `ReviewScheduler`는 매월 1일 00:00(Asia/Seoul)에 `ProductReviewService.updateBestReviews()`를 실행하도록 작성되어 있었다.
- 애플리케이션 시작 클래스에 `@EnableScheduling`이 없어 자동 스케줄은 등록되지 않는 상태였다.
- 베스트 리뷰 갱신은 실제 서비스 동작에 필요한 기능으로 판단해 `ReViewApplication`에 `@EnableScheduling`을 추가했다.
- 리뷰 작성 시 `createReviewWithReward()` 흐름에서 리뷰 생성 후 100포인트를 지급한다.
- 리뷰 삭제 시 soft delete 후 100포인트를 회수한다.
- 베스트 리뷰 갱신 시 기존 `is_checked`를 초기화하고, 선정된 리뷰의 `is_checked`를 1로 갱신한 뒤 700포인트를 지급한다.
- 베스트 리뷰 포인트는 `point_history`의 `user_id`, `review_id`, `amount`, `type` 기준으로 중복 지급을 방지한다.
- 관리자 선정 리뷰는 `is_selected` 변경 후 500포인트를 지급한다.
- README의 리뷰 보상 시스템 설명을 실제 코드 기준으로 보강했다.

## 스케줄러 처리 결정 메모

- 자동 활성화 상태: 활성화 완료
- 적용 파일: `src/main/java/org/spring/project/re_view/ReViewApplication.java`
- 실행 주기: 매월 1일 00:00, `Asia/Seoul`
- 수동 실행 경로: `ReviewScheduler.runUpdateBestReviewsNow()`

## DB 변경 영향 검토

- 로컬 환경에서는 애플리케이션이 실행 중이고 실제 Oracle DB 환경 변수가 설정되어 있으면 매월 1일 00:00에 리뷰와 포인트 테이블이 변경될 수 있다.
- Docker 환경에서는 `review-be` 컨테이너가 같은 jar를 실행하므로 `infra/.env`에 연결된 Oracle DB에 동일한 변경이 발생할 수 있다.
- 자동 실행 시 `review.is_checked` 초기화/재설정은 매번 발생한다.
- 베스트 리뷰 보상 포인트는 같은 리뷰에 중복 지급되지 않도록 방어되어 있다.

## 리뷰 보상 흐름 메모

- 리뷰 작성 보상: 리뷰 작성 성공 후 100포인트 지급, 이력 설명은 `리뷰 작성 보상`.
- 리뷰 삭제 회수: 리뷰 soft delete 후 100포인트 회수, 이력 설명은 `리뷰 삭제로 인한 포인트 회수`.
- 베스트 리뷰 보상: 월간 갱신 대상 리뷰에 700포인트 지급, 이력 설명은 `Best 리뷰 선정 보상`.
- 관리자 선정 리뷰 보상: 관리자 선정 처리 후 500포인트 지급, 이력 설명은 `운영자 리뷰 채택 보상`.

## 후속 메모

- 관리자 선정 리뷰는 `is_selected`를 0으로 변경해도 현재 코드상 보상 지급 로직이 실행된다.
- 관리자 선정 리뷰 보상에는 베스트 리뷰처럼 중복 지급 방지가 없다.
- 베스트 리뷰 자동 갱신은 DB write를 동반하므로 로컬 개발 DB와 Docker DB가 운영성 데이터인지 확인하고 실행해야 한다.

## 완료 기준

- [x] 스케줄러가 의도적으로 활성화되었거나, 수동 실행/추후 개선 대상으로 명확히 기록되어 있다.
- [x] 리뷰 보상 설명이 실제 코드와 일치한다.
