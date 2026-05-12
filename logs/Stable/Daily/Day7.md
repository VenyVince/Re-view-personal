# Day 7 - Maven Test Cleanup

## 목표

백엔드 테스트를 실행 가능하게 유지하고, 컨텍스트 로드 1개에 머무르지 않도록 다음 기능 테스트 대상을 정한다.

## 작업 체크리스트

- [x] Day 6에서 정리한 테스트 프로파일로 `mvn test`를 실행한다.
- [x] 실패가 있다면 설정 문제를 우선 수정한다.
- [x] DB 스키마나 샘플 데이터가 없어 실패하면 우회하지 않고 원인을 기록한다.
- [x] 필요한 최소 mock 또는 test property만 추가한다.
- [x] 인증/인가 테스트 후보를 정리한다.
- [x] 예외 응답 테스트 후보를 정리한다.
- [x] 주문/리뷰/마이페이지 중 핵심 서비스 테스트 후보를 정리한다.
- [x] Mockito 동적 agent 경고처럼 당장 실패는 아니지만 추후 문제가 될 경고를 기록한다.

## 우선 테스트 후보

- 인증 실패 시 401 반환
- 권한 부족 시 403 반환
- 잘못된 요청 값에 대한 400 응답
- 사용자 조회 실패 시 404 응답
- 주문 생성 또는 리뷰 작성의 서비스 레벨 검증

## 주의사항

- 넓은 가짜 인프라를 만들지 않는다.
- 테스트 통과만을 위해 실제 문제를 숨기지 않는다.
- Oracle 전용 SQL 때문에 H2에서 어려운 테스트는 이유를 기록하고 별도 통합 테스트 후보로 둔다.

## 참고사항

### 공개 API 테스트 시나리오 예시

`anyRequest().denyAll()` 전환 전에는 비로그인 상태에서 공개 API가 막히지 않는지 먼저 확인한다. 응답 본문 데이터 유무보다 Security 레이어에서 401/403으로 막히지 않는지를 우선 검증한다.

| 시나리오 | 요청 | 기대 |
| --- | --- | --- |
| Swagger API 문서 접근 | `GET /v3/api-docs` | 비로그인 접근 가능 |
| Swagger UI 접근 | `GET /swagger-ui/index.html` | 비로그인 접근 가능 |
| 상품 목록 조회 | `GET /api/products` | 비로그인 접근 가능 |
| 상품 상세 조회 | `GET /api/products/{product_id}` | 비로그인 접근 가능 |
| 리뷰 목록 조회 | `GET /api/reviews` | 비로그인 접근 가능 |
| 리뷰 상세 조회 | `GET /api/reviews/{review_id}` | 비로그인 접근 가능 |
| 상품별 리뷰 조회 | `GET /api/reviews/{product_id}/reviews` | 비로그인 접근 가능 |
| 상품 리뷰 검색 | `GET /api/products/{product_id}/reviews/search` | 비로그인 접근 가능 |
| 헤더 검색 | `GET /api/search?keyword=크림` | 비로그인 접근 가능 |
| 메인 배너 조회 | `GET /api/images/banners` | 비로그인 접근 가능 |
| 관리자 추천 리뷰 조회 | `GET /api/recommendations/admin-pick` | 비로그인 접근 가능 |
| 상품 QnA 목록 조회 | `GET /api/qna/list/{product_id}` | 비로그인 접근 가능 |

### 프론트 흐름 대조 메모

- 상품 목록, 상품 상세, 리뷰 목록, 리뷰 상세, 검색, 메인 배너, 관리자 추천 리뷰, 상품 QnA 목록은 현재 `SecurityConfig`의 공개 API 목록과 대체로 일치한다.
- 프론트에 `/api/survey/baumann`, `/api/products/recommendations/baumann` 호출 흔적이 있지만 현재 백엔드 컨트롤러 매핑에서는 확인되지 않았다. 실제 사용 중이면 API 정리 또는 프론트 호출 제거가 필요하다.
- `/api/auth/me`는 여러 화면에서 로그인 여부 확인용으로 호출되지만 인증 필요 API가 맞다. 비로그인일 때 401 `ErrorResponseDTO`가 내려가는지 테스트 대상으로 둔다.

## 결과물

- `mvn test` 통과 또는 남은 실패 원인 기록
- 백엔드 테스트 추가 후보 목록

## 완료 기준

- 남은 테스트 실패가 명백한 프로파일 설정 오류 때문은 아니다.
- 다음에 추가할 기능 테스트의 우선순위가 정리되어 있다.

## 작업 결과

### Maven 테스트

- 실행 명령: `.\mvnw.cmd test`
- 실행 시각: 2026-05-10 19:36 KST
- 결과: 성공
- 테스트 수: 1개 실행, 실패 0개, 에러 0개, 스킵 0개
- 현재 테스트 범위: `ReViewApplicationTests.contextLoads()`로 Spring context load만 검증한다.

### 실패/설정 이슈

- `mvn test`가 통과해서 Day7 범위에서 수정할 테스트 프로파일 설정 오류는 없었다.
- DB 스키마 또는 샘플 데이터 부재로 인한 실패도 이번 실행에서는 발생하지 않았다.
- 따라서 테스트 통과만을 위한 mock이나 추가 test property는 넣지 않았다.
- 참고: `application-test.properties`는 H2 URL(`jdbc:h2:mem:testdb`)을 사용하지만 드라이버는 `oracle.jdbc.OracleDriver`로 지정되어 있다. 현재 context load에서는 실패하지 않지만, 실제 DB 접근 테스트를 추가할 때 우선 확인해야 할 설정 후보이다.

### 기록할 경고

- Mockito inline mock maker가 현재 JDK에서 동적 self-attach 방식으로 agent를 붙이고 있다.
- 출력 경고 요지:
  - 향후 JDK에서는 Mockito self-attaching 방식이 동작하지 않을 수 있다.
  - `byte-buddy-agent-1.17.7.jar`가 동적으로 로드되었다.
  - 향후 동적 agent 로딩은 기본 비허용될 예정이다.
- 후속 조치 후보: Maven Surefire 설정에 Mockito Java agent를 명시하거나, Mockito/JDK 조합을 정리할 때 공식 권장 방식으로 전환한다.

## 다음 기능 테스트 우선순위

### 1. Security 레이어 테스트

- `GET /api/auth/me` 비로그인 요청은 401과 `ErrorResponseDTO` 형태의 JSON을 반환해야 한다.
- `GET /api/admin/users` 또는 실제 존재하는 `/api/admin/**` 엔드포인트에 일반 사용자 권한으로 접근하면 403과 `ErrorResponseDTO`를 반환해야 한다.
- 공개 API는 비로그인 상태에서 Security 레이어가 막지 않아야 한다.
  - 우선 후보: `GET /v3/api-docs`, `GET /swagger-ui/index.html`, `GET /api/products`, `GET /api/reviews`, `GET /api/search?keyword=크림`, `GET /api/qna/list/{product_id}`
- 현재 `SecurityConfig`의 마지막 규칙이 `anyRequest().permitAll()`이므로, 보호 API 목록 누락 여부를 확인하는 테스트가 필요하다.

### 2. 예외 응답 테스트

- `ResourceNotFoundException`은 404, error 값 `Not Found`, 요청 path를 포함해야 한다.
- `WrongRequestException`은 400, error 값 `Bad_Request`, 요청 path를 포함해야 한다.
- `MethodArgumentNotValidException`은 400, error 값 `Validation Failed`, 필드별 메시지를 합쳐 반환해야 한다.
- `BadCredentialsException`은 401, error 값 `Unauthorized`를 반환해야 한다.
- `BannedUserException`은 401, error 값 `Banned User`를 반환해야 한다.
- `DatabaseException`과 `FileProcessingException`은 500 응답을 유지해야 한다.

### 3. 주문 서비스 테스트

- `OrderService.processOrder()` 호출 순서와 실패 처리를 검증한다.
  - 포인트 차감 실패 시 재고 차감/주문 저장이 호출되지 않아야 한다.
  - `deductStock()` 결과가 0이면 `WrongRequestException`을 던져야 한다.
  - 상품 정보가 없으면 `WrongRequestException`을 던져야 한다.
- Oracle 전용 SQL이나 실제 DB 스키마가 필요한 mapper 통합 검증은 H2 단위 테스트로 우회하지 않고 별도 통합 테스트 후보로 둔다.

### 4. 리뷰 서비스 테스트

- `ReviewService.getReviewDetail()`에서 mapper가 null을 반환하면 `ResourceNotFoundException`을 던져야 한다.
- 비로그인 사용자(`user_id <= 0`)의 리뷰 상세 조회는 좋아요/싫어요 상태를 false로 내려야 한다.
- 이미지 key가 있을 때만 `ImageService.presignedUrlGet()`을 호출하는지 검증한다.
- `getReviewList()`에서 mapper의 `DataAccessException`은 `DatabaseException`으로 변환되어야 한다.

### 5. 마이페이지/사용자 테스트

- `/api/auth/me`는 인증 성공 시 id, role, nickname을 반환해야 한다.
- `UserController.login()`은 밴 사용자이면 `BannedUserException`, 비밀번호 불일치이면 `ResourceNotFoundException` 흐름으로 떨어져야 한다.
- 사용자 조회 실패 404는 `UserService`가 던지는 `ResourceNotFoundException`을 기준으로 컨트롤러 또는 예외 핸들러 테스트를 구성한다.

## Day7 결론

- 현재 백엔드 테스트는 통과한다.
- 남은 실패는 없고, 프로파일 설정 오류로 인한 차단도 확인되지 않았다.
- 다음 단계는 context load 1개에서 벗어나 `SecurityConfig` 401/403 테스트와 `ExceptionHandlers` 응답 shape 테스트를 먼저 추가하는 것이다.
