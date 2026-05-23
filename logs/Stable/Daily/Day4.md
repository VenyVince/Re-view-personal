# Day 4 - SecurityConfig Authorization Apply

## 목표

Day 3에서 정리한 API 분류를 `SecurityConfig`에 반영한다. 변경 범위는 작게 유지하고, 상품/리뷰 공개 탐색 흐름은 깨지지 않게 검증한다.

## 작업 체크리스트

- [x] Day 3의 API 분류 메모를 다시 확인한다.
- [x] `SecurityConfig`의 request matcher 순서를 정리한다.
- [x] Swagger/OpenAPI 문서 endpoint는 공개로 유지한다.
- [x] 로그인/회원가입/아이디 찾기/임시 비밀번호 발급은 공개 여부를 확인한다.
- [x] `/api/admin/**`는 `hasRole("ADMIN")`로 제한한다.
- [x] 사용자 개인 데이터 API는 인증 필요로 제한한다.
- [x] 상품/리뷰 공개 조회 API는 `permitAll()`로 유지한다.
- [x] `anyRequest().permitAll()`을 유지할지, 더 보수적인 기본 정책으로 바꿀지 결정한다.
- [x] CORS 설정이 프론트 origin을 허용하는지 확인한다.
- [x] CSRF 설정이 세션 로그인 흐름과 충돌하지 않는지 확인한다.
- [x] `View/src/api/axiosClient.js`에서 세션 인증과 맞지 않는 `Bearer token` 전송 코드를 제거하거나 후속 작업으로 명확히 기록한다.

## 검증 시나리오

- [x] 공개 상품 목록 조회가 동작한다.
- [x] 공개 리뷰 목록 조회가 동작한다.
- [x] 미로그인 상태에서 보호 API 접근 시 거부된다.
- [x] 일반 사용자로 관리자 API 접근 시 거부된다.
- [x] 로그인 후 마이페이지 또는 장바구니 API 접근이 가능하다.
- [x] 로그아웃 후 보호 API 접근이 다시 거부된다.

## 주의사항

- JWT 필터를 새로 추가하지 않는다.
- 세션 인증 유지가 기본 방향이다.
- 프론트 사용 흐름을 확인하지 않은 API를 무리하게 막지 않는다.
- matcher 순서는 구체적인 규칙을 먼저 두고, 넓은 규칙은 뒤에 둔다.

## 결과물

- 수정된 `SecurityConfig`
- 공개 API와 로그인 필요 API 구분 메모
- 프론트 인증 요청 방식 정리 결과

## 작업 결과 요약

- Day 3의 endpoint 분류를 기준으로 `SecurityConfig` matcher를 정리했다.
- Swagger, 인증 진입 API, 상품/리뷰/QnA 공개 조회 API는 공개로 유지했다.
- `/api/admin/**`는 관리자 권한이 필요하도록 유지했다.
- 장바구니, 주문, 주소, 위시리스트, 포인트, 결제수단, 마이페이지, 리뷰 작성/수정/삭제, 댓글, 반응, 신고 API는 인증 필요 대상으로 제한했다.
- `GET /api/qna/{qna_id}`는 공개 API로 유지하기로 결정했다.
- `anyRequest().permitAll()`은 일단 유지하고, 테스트 코드 작성 후 `denyAll()` 전환을 검토하기로 했다.
- CORS는 프론트 origin과 credentials 허용 상태를 확인했다.
- CSRF는 로그인 예외, `XSRF-TOKEN` 쿠키, `X-XSRF-TOKEN` 헤더 전송, POST 요청 정상 동작을 확인했다.
- 프론트 공통 API 클라이언트에서 세션 인증과 맞지 않는 Bearer token 전송 코드를 제거했다.
- 공개 API, 보호 API, 관리자 API, 로그인 후 개인 API 접근 흐름을 검증했다.

## 후속 메모

- 테스트 코드 작성 후 `anyRequest().denyAll()` 전환 여부를 다시 판단한다.
- 인증 실패와 권한 부족 응답이 아직 400으로 내려가는 부분은 Day 5에서 401/403으로 정리한다.

## 완료 기준

- 관리자 API는 관리자 권한이 필요하다.
- 사용자 개인 API는 대부분 `permitAll()` 상태가 아니다.
- 상품/리뷰 공개 탐색 기능은 유지된다.
- 프론트 인증 요청 방식과 백엔드 세션 인증 방식이 충돌하지 않는다.
