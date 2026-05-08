# Day 5 - 401/403 and Error Response Cleanup

## 목표

인증 실패와 권한 부족의 HTTP 상태 코드를 정리하고, 프론트가 처리하기 쉬운 공통 예외 응답 형식을 만든다.

## Day 4에서 넘어온 상태

- `SecurityConfig` matcher 정리는 완료했다.
- Swagger, 인증 진입 API, 상품/리뷰/QnA 공개 조회 API는 공개로 유지한다.
- `/api/admin/**`는 `hasRole("ADMIN")`로 제한한다.
- 사용자 개인 데이터 API와 리뷰/QnA 쓰기성 API는 인증 필요로 제한했다.
- `GET /api/qna/{qna_id}`는 공개 API로 유지한다.
- `anyRequest().permitAll()`은 일단 유지하고, 테스트 코드 작성 후 `denyAll()` 전환을 검토한다.
- CORS와 CSRF는 현재 로그인 흐름과 충돌하지 않는 것으로 확인했다.
- 프론트 공통 API 클라이언트의 Bearer token 전송 코드는 제거했다.
- 남은 핵심 문제는 인증 실패와 권한 부족이 모두 400으로 내려가는 점이다.

## 작업 체크리스트

- [x] `SecurityConfig`의 `authenticationEntryPoint`를 401 기준으로 수정한다.
- [x] `SecurityConfig`의 `accessDeniedHandler`를 403 기준으로 수정한다.
- [x] 기존 로그아웃 응답이 깨지지 않는지 확인한다.
- [x] `ErrorResponse` DTO 추가 여부와 필드를 결정한다.
- [x] 전역 예외 응답을 문자열에서 공통 JSON 포맷으로 바꾼다.
- [x] DB 예외 처리에서 `printStackTrace()`/`System.err` 사용을 로거 기반으로 바꿀지 검토한다.
- [x] 회원가입, 로그인, 비밀번호 변경 DTO부터 `jakarta.validation` 적용 범위를 정한다.
- [x] DTO 검증 실패 응답을 공통 포맷에 맞출지 결정한다.
- [ ] `anyRequest().denyAll()` 전환은 Day 5 범위에 포함할지, 테스트 작성 이후로 유지할지 다시 확인한다.

## 권장 응답 기준

| 상황 | 상태 코드 |
| --- | --- |
| 미로그인 보호 API 접근 | 401 |
| 로그인했지만 권한 부족 | 403 |
| 요청 값 오류 | 400 |
| 리소스 없음 | 404 |
| 서버 내부 오류 | 500 |

## 검증 시나리오

- [x] 미로그인 상태로 보호 API 호출 시 401이 반환된다.
- [x] 일반 사용자로 관리자 API 호출 시 403이 반환된다.
- [x] 잘못된 요청 값은 400으로 반환된다.
- [x] 없는 리소스는 404로 반환된다.
- [x] 프론트에서 상태 코드와 JSON 본문을 기준으로 오류 원인을 구분할 수 있다.

## 결과물

- 인증 실패와 권한 부족에 맞는 상태 코드
- 공통 예외 응답 포맷 초안 또는 적용 결과
- DTO 검증 적용 범위 메모

## 주의사항

- JWT는 도입하지 않고 세션 인증을 유지한다.
- Day 4에서 확인한 공개 탐색 API 흐름은 깨지지 않게 유지한다.
- 응답 포맷 개편은 작게 진행하고, 프론트가 바로 구분해야 하는 401/403을 우선 처리한다.
- `anyRequest().denyAll()` 전환은 테스트 코드가 준비된 뒤 적용하는 방향을 기본값으로 둔다.
- `SecurityConfig`에서 `ObjectMapper`를 직접 생성했었는데, 이렇게 하면 `LocalDateTime` 직렬화 설정이 빠져 런타임 오류가 날 수 있어서 Spring Bean으로 주입받도록 재변경했다.
- DTO 검증 실패가 기본 Spring 오류 응답으로 내려갈 수 있어서 `MethodArgumentNotValidException`도 `ErrorResponseDTO` 포맷으로 재변경했다.

## 완료 기준

- 401/403이 의미에 맞게 분리되어 있다.
- 기존 로그인/로그아웃 동작은 유지된다.
- 프론트 오류 처리 기준이 명확해진다.
