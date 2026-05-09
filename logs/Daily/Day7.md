# Day 7 - Maven Test Cleanup

## 목표

백엔드 테스트를 실행 가능하게 유지하고, 컨텍스트 로드 1개에 머무르지 않도록 다음 기능 테스트 대상을 정한다.

## 작업 체크리스트

- [ ] Day 6에서 정리한 테스트 프로파일로 `mvn test`를 실행한다.
- [ ] 실패가 있다면 설정 문제를 우선 수정한다.
- [ ] DB 스키마나 샘플 데이터가 없어 실패하면 우회하지 않고 원인을 기록한다.
- [ ] 필요한 최소 mock 또는 test property만 추가한다.
- [ ] 인증/인가 테스트 후보를 정리한다.
- [ ] 예외 응답 테스트 후보를 정리한다.
- [ ] 주문/리뷰/마이페이지 중 핵심 서비스 테스트 후보를 정리한다.
- [ ] Mockito 동적 agent 경고처럼 당장 실패는 아니지만 추후 문제가 될 경고를 기록한다.

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
