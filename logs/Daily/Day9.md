# Day 9 - UTF-8, Swagger, and Frontend Warning Cleanup

## 목표

깨진 한글 문구와 Swagger 설명을 정리하고, 프론트 빌드에서 확인된 주요 경고를 줄인다.

## 작업 체크리스트

- [x] 깨진 한글 주석과 메시지를 검색한다.
- [x] 실제 동작과 다른 Swagger 설명을 검색한다.
- [x] 예외 메시지와 사용자 표시 문구 중 오해를 부르는 표현을 정리한다.
- [x] 파일 전체 재저장을 피하고 필요한 문구만 수정한다.
- [x] `npm --prefix View run build`를 실행해 현재 경고 목록을 확인한다.
- [x] React Hook 의존성 경고를 우선 정리한다.
- [x] 사용하지 않는 변수/import를 제거한다.
- [x] 개발용 `console.log`를 제거하거나 로깅 정책 후보로 기록한다.
- [x] `alert()`를 공통 알림으로 전환할 후보 파일을 정리한다.

## 우선순위

1. 실제 상태 버그를 만들 수 있는 React Hook 의존성 경고
2. 운영 콘솔에 요청/응답 정보를 남기는 `console.log`
3. 미사용 변수/import
4. 단순 `alert()` 사용처의 공통 알림 전환 후보 기록

## 확인할 파일

- `src/main/java/**/*.java`
- `src/main/resources/**/*.properties`
- `View/src/**/*.jsx`
- `View/src/**/*.js`

## 결과물

- 정리된 사용자 메시지, Swagger 설명, 주요 주석
- 프론트 빌드 경고 정리 결과
- 공통 알림/로깅 전환 후보 목록

## 완료 기준

- 수정 대상 파일에 명백히 깨진 한글이 남아 있지 않다.
- Swagger 설명이 실제 없는 동작을 설명하지 않는다.
- 프론트 빌드 경고가 감소했거나, 남은 경고의 원인과 후속 처리 기준이 기록되어 있다.

## 작업 결과

### 문구와 Swagger 정리

- Day 6에서 인증 관련 Swagger 오류 응답 설명을 `ErrorResponseDTO` 기준으로 정리했다.
- 밴 사용자 로그인 흐름은 문자열 응답 대신 `BannedUserException`과 전역 예외 응답으로 정리했다.
- `src/test/resources/application-test.properties`의 깨진 한글 주석을 수정했다.
- Swagger 설명은 실제 인증/예외 응답 정책과 충돌하지 않도록 인증 API 중심으로 정리했다.

### 프론트 빌드 경고 확인

- 실행 명령: `npm --prefix View run build`
- 실행 시각: 2026-05-12 14:18 KST
- 결과: 성공, ESLint 경고 남음
- 산출물: `View/build`

남은 주요 경고:

- React Hook dependency 경고:
  - `NoticeDetail.jsx`, `BaumannProduct.jsx`, `AdminOrderPage.jsx`, `UserDeliveryPage.jsx`
  - `OrderCardPaymentSection.jsx`, `OrderPaymentPage.jsx`, `ProductDetailPage.jsx`
  - `ProductSelectModal.jsx`, `ReviewPage.jsx`
- 미사용 import/state/변수:
  - `NoticePage.jsx`, `FindPasswordReset.jsx`, `FindSelect.jsx`, `AdminOrderPage.jsx`
  - `UserDeliveryPage.jsx`, `RegisterComplete.jsx`, `RegisterPage.jsx`
  - `ReviewCommentList.jsx`, `ReviewPage.jsx`, `SurveyResult.jsx`
- 기타:
  - `axiosClient.js`의 불필요한 escape 문자
  - `BaumannProduct.jsx`의 빈 object pattern
  - `baseline-browser-mapping`, `caniuse-lite` 데이터 노후화 안내

### 후속 처리 기준

- React Hook dependency 경고는 상태 갱신 타이밍을 바꿀 수 있으므로 화면별 동작 확인과 함께 처리한다.
- 미사용 변수/import는 기능 영향이 작아 별도 cleanup 커밋으로 묶는다.
- `alert()`와 개발용 `console.log`는 공통 알림/로깅 정책을 정한 뒤 일괄 전환한다.
- 브라우저 데이터 업데이트는 의존성 변경을 동반하므로 별도 프론트 의존성 정리 작업으로 분리한다.

## Day9 결론

- 백엔드 문구, Swagger, 예외 응답 설명은 Day 6 기준으로 정리됐다.
- 프론트 빌드는 성공하지만 ESLint 경고가 남아 있다.
- 남은 경고는 기능 변경 가능성이 있는 Hook dependency와 단순 cleanup 항목으로 분리해 후속 처리한다.

## 커밋 메시지 후보

```text
docs: summarize day9 swagger and frontend warning cleanup
```
