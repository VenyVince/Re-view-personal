# Day 8 - Scheduler and Review Reward Flow Check

## 목표

베스트 리뷰 갱신용 스케줄러를 실제로 활성화할지 결정하고, 리뷰 보상 흐름이 코드와 README 설명에 맞는지 확인한다.

## 작업 체크리스트

- [ ] `ReviewScheduler`를 확인한다.
- [ ] 애플리케이션 시작 클래스에 `@EnableScheduling`이 필요한지 확인한다.
- [ ] 스케줄러를 자동 활성화할지, 수동/후속 개선으로 둘지 결정한다.
- [ ] 리뷰 작성 보상 흐름을 확인한다.
- [ ] 리뷰 삭제 시 포인트 회수 흐름을 확인한다.
- [ ] 베스트 리뷰 보상 흐름을 확인한다.
- [ ] 관리자 선정 리뷰 보상 흐름을 확인한다.
- [ ] 자동 스케줄 활성화 시 로컬/Docker 환경에서 의도치 않은 DB 변경이 생기지 않는지 검토한다.
- [ ] README에 적을 리뷰 보상 설명 초안을 작성한다.

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

## 완료 기준

- 스케줄러가 의도적으로 활성화되었거나, 수동 실행/추후 개선 대상으로 명확히 기록되어 있다.
- 리뷰 보상 설명이 실제 코드와 일치한다.
