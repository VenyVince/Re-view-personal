# Day 3 - Security Policy Classification

## 목표

Day 3의 목표는 코드를 바로 막는 것이 아니라, 실제 API 사용 흐름을 기준으로 공개 API, 로그인 필요 API, 관리자 API를 분류하는 것이다.

- Spring Security 세션 인증 방식을 유지한다.
- JWT는 도입하지 않는다.
- 프론트에서 실제 호출하는 API를 함께 확인한다.
- `anyRequest().permitAll()`로 노출된 API 중 보호가 필요한 대상을 찾는다.

## 작업 체크리스트

- [ ] 전체 컨트롤러의 endpoint 목록을 수집한다.
- [ ] `View/src/api`와 주요 페이지에서 호출하는 API 목록을 확인한다.
- [ ] 공개 API를 분류한다.
- [ ] 로그인 필요 API를 분류한다.
- [ ] 관리자 API를 분류한다.
- [ ] `SecurityConfig`의 현재 matcher와 실제 API 분류를 비교한다.
- [ ] `View/src/api/axiosClient.js`의 `Authorization: Bearer` 전송 코드가 세션 인증 정책과 충돌하는지 기록한다.
- [ ] Day 4에서 적용할 matcher 초안을 작성한다.

## 분류 기준

### 공개 API 후보

- 회원가입, 로그인, 아이디 찾기, 임시 비밀번호 발급
- 상품 목록/상세 조회
- 리뷰 목록/상세 조회
- 검색
- Swagger, OpenAPI 문서

주의:

- 공개 API라도 쓰기 작업이면 별도로 검토한다.
- 인증 관련 API 중 비밀번호 변경처럼 로그인 상태가 필요한 API는 공개로 두지 않는다.

### 로그인 필요 API 후보

- 장바구니
- 위시리스트
- 주문/주문 상세
- 포인트
- 주소
- 결제수단
- 마이페이지
- 리뷰 작성/수정/삭제
- 리뷰 추천/비추천/신고 같은 사용자 반응 API
- QnA 작성/수정/삭제 중 사용자 식별이 필요한 API

### 관리자 API 후보

- `/api/admin/**`
- 관리자 상품 등록/수정/삭제
- 관리자 주문 상태 변경
- 관리자 유저 관리
- 관리자 리뷰/신고/QnA 처리

## 확인할 파일

- `src/main/java/com/review/shop/config/SecurityConfig.java`
- `src/main/java/com/review/shop/controller/**/*.java`
- `View/src/api/**/*.js`
- `View/src/pages/**/*.jsx`
- `View/src/components/**/*.jsx`

## 결과물

- API 인증/인가 분류 메모
- `SecurityConfig` matcher 정책 초안
- 프론트 인증 요청 방식 정리 메모

## 완료 기준

- 공개 API, 로그인 필요 API, 관리자 API가 구분되어 있다.
- Day 4에서 바로 적용 가능한 matcher 초안이 있다.
- 세션 인증 유지 기준에서 프론트 `Bearer token` 코드의 처리 방향이 정리되어 있다.
