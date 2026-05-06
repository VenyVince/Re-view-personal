# Endpoint List

작성일: 2026-05-06

## 목적

Day 3 작업 기준으로 백엔드 컨트롤러 endpoint와 프론트 호출 API를 확인하여 공개 API, 로그인 필요 API, 관리자 API로 분류한다.

기준:

- 인증 방식은 세션 인증을 유지한다.
- JWT/Bearer 토큰 방식은 도입하지 않는다.
- 쓰기 작업은 기본적으로 로그인 필요로 분류한다.
- `/api/admin/**`는 관리자 권한 필요로 분류한다.
- `{id}` 형태의 path variable은 실제 코드의 `product_id`, `review_id` 등을 단순화해서 적는다.



## 프론트 호출과 백엔드 불일치 후보

`/api/survey/baumann`

- 프론트: `View/src/api/survey/surveyApi.js`
- 백엔드 컨트롤러 검색 결과에는 endpoint가 보이지 않음.
- 조치: 미사용 코드인지, 누락된 컨트롤러인지 확인.

`/api/users/me/baumann`

- 프론트: `View/src/api/survey/surveyApi.js`
- 백엔드 실제 endpoint: `PATCH /api/users/me/baumann`
- 조치: 프론트는 GET으로 호출하므로 실제 사용 여부와 API 계약 확인.

`/api/products/recommendations/baumann`

- 프론트: `View/src/api/survey/surveyApi.js`
- 백엔드 컨트롤러 검색 결과에는 endpoint가 보이지 않음.
- 조치: `/api/recommendations/all`로 대체된 코드인지 확인.

`/api/images/products`

- 프론트: `View/src/api/admin/adminProductApi.js`
- 백엔드 `ImageController` 검색 결과에는 `POST /api/images/products`가 보이지 않음.
- 조치: 관리자 상품 이미지 업로드 endpoint 누락 여부 확인.

`/api/admin/products/{productId}/images`

- 프론트: `View/src/api/admin/adminProductApi.js`
- 백엔드 컨트롤러 검색 결과에는 endpoint가 보이지 않음.
- 조치: 이미지 수정 API 누락 여부 확인.

`api/auth/logout`

- 프론트: `View/src/context/AuthContext.js`
- 백엔드: `/api/auth/logout`
- 조치: 프론트 경로 앞에 `/`가 빠져 있음. baseURL 결합 결과 확인 필요.


## 공개 API

### Auth

`/api/auth/register`

- POST: 회원가입
- 분류: 공개
- 메모: 가입 요청이므로 비로그인 허용.

`/api/auth/login`

- POST: 로그인
- 분류: 공개
- 메모: 세션 생성 API. CSRF 예외가 현재 설정되어 있음.

`/api/auth/check-id`

- POST: 아이디 중복 확인
- 분류: 공개

`/api/auth/find-id`

- POST: 아이디 찾기
- 분류: 공개

`/api/auth/send-temp-password`

- POST: 임시 비밀번호 발급
- 분류: 공개
- 메모: 남용 방지를 위해 rate limit 또는 추가 검증은 별도 검토.

### Product

`/api/products`

- GET: 상품 목록 조회
- 분류: 공개
- 메모: 카테고리/정렬 조건 조회. 추후 페이징 검토 필요.

`/api/products/{product_id}`

- GET: 상품 상세 조회
- 분류: 공개

### Review

`/api/reviews`

- GET: 리뷰 목록 조회
- 분류: 공개
- 메모: 프론트 리뷰 페이지에서 호출.

`/api/reviews/{review_id}`

- GET: 리뷰 상세 조회
- 분류: 공개

`/api/reviews/{product_id}/reviews`

- GET: 상품 상세의 리뷰 목록 조회
- 분류: 공개
- 메모: naming이 다소 중복적이라 추후 정리 검토.

`/api/products/{product_id}/reviews/search`

- GET: 상품 리뷰 검색/정렬
- 분류: 공개

### Search

`/api/search`

- GET: 헤더/통합 검색
- 분류: 공개

### QnA 조회

`/api/qna/list/{product_id}`

- GET: 상품 QnA 목록 조회
- 분류: 공개

`/api/qna/{qna_id}`

- GET: QnA 상세 조회
- 분류: 공개 또는 로그인 필요 검토
- 메모: 비밀글/본인글 정책이 있으면 로그인 필요 또는 소유자/관리자만 허용으로 변경.

### Images

`/api/images/banners`

- GET: 배너 이미지 목록 조회
- 분류: 공개

### Recommendations

`/api/recommendations/admin-pick`

- GET: 관리자 선정 리뷰/추천 영역 조회
- 분류: 공개

## 로그인 필요 API

### Auth / Current User

`/api/auth/me`

- GET: 현재 로그인 사용자 조회
- 분류: 로그인 필요

`/api/auth/my-baumann-type`

- GET: 현재 로그인 사용자의 바우만 타입 조회
- 분류: 로그인 필요

`/api/auth/reset-password`

- POST: 비밀번호 변경
- 분류: 로그인 필요
- 메모: 현재 프론트 마이페이지에서 호출하므로 공개 API로 두면 안 됨.

`/api/auth/logout`

- POST: 로그아웃
- 분류: 로그인 필요
- 메모: Spring Security logoutUrl로 처리됨.

### User Profile

`/api/users/me`

- GET: 내 회원 정보 조회
- PATCH: 내 회원 정보 수정
- DELETE: 회원 탈퇴
- 분류: 로그인 필요

`/api/users/me/baumann`

- PATCH: 내 바우만 타입 수정
- 분류: 로그인 필요

### Address

`/api/addresses`

- GET: 사용자의 배송지 목록 조회
- POST: 배송지 추가
- 분류: 로그인 필요

`/api/addresses/{address_id}`

- PATCH: 배송지 수정
- DELETE: 배송지 삭제
- 분류: 로그인 필요
- 메모: 로그인 외에 현재 사용자 소유 배송지인지 서비스 계층에서 검증 필요.

### Cart

`/api/cart`

- GET: 장바구니 조회
- POST: 장바구니 상품 추가
- PATCH: 장바구니 수량 수정
- DELETE: 장바구니 상품 삭제
- 분류: 로그인 필요

### Wishlist

`/api/wishlist`

- GET: 위시리스트 조회
- POST: 위시리스트 추가
- DELETE: 위시리스트 삭제
- 분류: 로그인 필요

### Orders

`/api/orders/checkout`

- POST: 주문 전 상품/포인트/결제 정보 확인
- 분류: 로그인 필요

`/api/orders`

- GET: 내 주문 목록 조회
- POST: 주문 생성
- 분류: 로그인 필요

`/api/orders/{order_id}`

- GET: 내 주문 상세 조회
- 분류: 로그인 필요
- 메모: 현재 사용자 소유 주문인지 검증 필요.

### Payments

`/api/users/me/payments`

- GET: 내 결제수단 목록 조회
- POST: 결제수단 추가
- 분류: 로그인 필요

`/api/users/me/payments/{payment_id}`

- DELETE: 결제수단 삭제
- 분류: 로그인 필요
- 메모: 현재 사용자 소유 결제수단인지 검증 필요.

### Points

`/api/users/me/points`

- GET: 내 총 포인트 조회
- 분류: 로그인 필요

`/api/users/me/points/history`

- GET: 내 포인트 이력 조회
- 분류: 로그인 필요

### Review Write / Actions

`/api/reviews/exists/create`

- GET: 특정 주문 상품의 리뷰 작성 가능 여부 확인
- 분류: 로그인 필요

`/api/reviews/exists/update`

- GET: 리뷰 수정 가능 여부 확인
- 분류: 로그인 필요

`/api/reviews/{product_id}`

- POST: 상품 리뷰 작성
- 분류: 로그인 필요

`/api/reviews/{review_id}`

- PATCH: 리뷰 수정
- 분류: 로그인 필요
- 메모: GET 상세 조회와 같은 path지만 method 기준으로 보호 필요.

`/api/reviews/{product_id}/{review_id}`

- DELETE: 리뷰 삭제
- 분류: 로그인 필요
- 메모: 작성자 본인 또는 관리자만 허용해야 함.

`/api/reviews/{review_id}/comments`

- POST: 리뷰 댓글 작성
- 분류: 로그인 필요

`/api/reviews/comments/{comment_id}`

- DELETE: 리뷰 댓글 삭제
- 분류: 로그인 필요
- 메모: 작성자 본인 또는 관리자만 허용해야 함.

`/api/reviews/{review_id}/reaction`

- POST: 리뷰 추천/비추천 반응
- 분류: 로그인 필요

`/api/reviews/{review_id}/report`

- POST: 리뷰 신고
- 분류: 로그인 필요

`/api/users/reviews/search`

- GET: 마이페이지 내 리뷰 검색
- 분류: 로그인 필요

### QnA Write / My QnA

`/api/qna/my`

- GET: 내 QnA 목록 조회
- 분류: 로그인 필요

`/api/qna`

- POST: QnA 작성
- PUT: QnA 수정
- 분류: 로그인 필요

`/api/qna/{qna_id}`

- DELETE: QnA 삭제
- 분류: 로그인 필요
- 메모: 작성자 본인 또는 관리자만 허용해야 함.

### Images

`/api/images/products/convert-data`

- POST: 단일 이미지 변환/업로드 보조
- 분류: 로그인 필요
- 메모: 리뷰 작성/관리자 상품 등록에서 호출됨. 실제 목적에 따라 관리자 전용 분리 검토.

`/api/images/products/convert-datas`

- POST: 다중 이미지 변환/업로드 보조
- 분류: 로그인 필요
- 메모: 리뷰 이미지 업로드에서 호출됨.

### Recommendations

`/api/recommendations/all`

- POST: 로그인 사용자 기준 전체 추천 조회
- 분류: 로그인 필요

## 관리자 API

### Admin Product

`/api/admin/allproducts`

- GET: 관리자 상품 목록 조회
- 분류: 관리자

`/api/admin/products`

- POST: 관리자 상품 등록
- 분류: 관리자

`/api/admin/products/{product_id}`

- PATCH: 관리자 상품 수정
- DELETE: 관리자 상품 삭제
- 분류: 관리자

`/api/admin/products/find/{product_id}`

- GET: 관리자 상품 상세 조회
- 분류: 관리자

### Admin Order

`/api/admin/orders/{order_id}/status`

- PATCH: 주문 상태 변경
- 분류: 관리자

`/api/admin/run`

- GET: 관리자 주문 관련 실행 API
- 분류: 관리자
- 메모: 목적이 불명확하므로 Day 4에서 유지 여부 확인 필요.

### Admin QnA

`/api/admin/qna`

- GET: 관리자 QnA 목록 조회
- 분류: 관리자

`/api/admin/qna/{qna_id}`

- GET: 관리자 QnA 상세 조회
- 분류: 관리자

`/api/admin/qna/{qna_id}/answer`

- PATCH: 관리자 QnA 답변 처리
- 분류: 관리자

### Admin Review

`/api/admin/reviews`

- GET: 관리자 리뷰 목록 조회
- 분류: 관리자

`/api/admin/reviews/{review_id}`

- DELETE: 관리자 리뷰 삭제
- 분류: 관리자

`/api/admin/reviews/{review_id}/select`

- POST: 베스트/관리자 선정 리뷰 지정
- 분류: 관리자

### Admin Reports

`/api/admin/reports`

- GET: 신고 목록 조회
- 분류: 관리자

`/api/admin/reports/{report_id}`

- PATCH: 신고 처리 상태 변경
- 분류: 관리자

### Admin Search

`/api/admin/search/reviews`

- GET: 관리자 리뷰 검색
- 분류: 관리자

`/api/admin/search/products`

- GET: 관리자 상품 검색
- 분류: 관리자

`/api/admin/search/orders`

- GET: 관리자 주문 검색
- 분류: 관리자

### Admin User

`/api/admin/users`

- GET: 관리자 유저 목록 조회
- 분류: 관리자

`/api/admin/users/{user_id}/points`

- GET: 특정 유저 포인트 조회
- PATCH: 특정 유저 포인트 수정
- 분류: 관리자

`/api/admin/users/{user_id}/ban`

- POST: 유저 제재/정지 처리
- 분류: 관리자

## 현재 SecurityConfig 비교

현재 설정:

```text
.requestMatchers(
    "/v3/api-docs/**",
    "/swagger-ui/**",
    "/swagger-ui.html"
).permitAll()
.requestMatchers("/api/admin/**").hasRole("ADMIN")
.requestMatchers("/api/mypage/**").hasRole("USER")
.anyRequest().permitAll()
```

문제:

- `/api/admin/**`는 관리자 보호됨.
- `/api/mypage/**`는 실제 주요 API prefix와 맞지 않음.
- 장바구니, 주문, 주소, 위시리스트, 포인트, 결제수단, 마이페이지, 리뷰 작성/수정/삭제, QnA 작성/수정/삭제가 `anyRequest().permitAll()`에 의해 기본 공개될 수 있음.
- 인증 실패/인가 실패가 모두 400으로 내려가므로 프론트가 로그인 필요와 권한 부족을 구분하기 어려움.

## 프론트 인증 요청 방식 메모

`View/src/api/axiosClient.js`는 `withCredentials: true`로 세션 쿠키를 보내는 동시에 `localStorage.token`이 있으면 `Authorization: Bearer {token}`도 전송한다.

Day 3 기준 결정:

- 세션 인증을 유지한다.
- JWT는 도입하지 않는다.
- 따라서 Bearer 토큰 전송 코드는 제거하거나 사용하지 않는 legacy 코드로 명시해야 한다.
- CSRF 토큰 전송은 세션 인증 정책과 함께 유지 검토한다.

## Day 4 SecurityConfig Matcher 초안

메서드 단위 matcher를 쓰는 방향이 안전하다. 같은 path라도 GET은 공개, POST/PATCH/DELETE는 로그인 필요인 경우가 있기 때문이다.

초안:

```text
.authorizeHttpRequests(auth -> auth
    .requestMatchers(
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html"
    ).permitAll()

    .requestMatchers("/api/admin/**").hasRole("ADMIN")

    .requestMatchers(HttpMethod.POST,
        "/api/auth/register",
        "/api/auth/login",
        "/api/auth/check-id",
        "/api/auth/find-id",
        "/api/auth/send-temp-password"
    ).permitAll()

    .requestMatchers(HttpMethod.GET,
        "/api/products",
        "/api/products/*",
        "/api/reviews",
        "/api/reviews/*",
        "/api/reviews/*/reviews",
        "/api/products/*/reviews/search",
        "/api/search",
        "/api/qna/list/*",
        "/api/images/banners",
        "/api/recommendations/admin-pick"
    ).permitAll()

    .requestMatchers(
        "/api/auth/me",
        "/api/auth/my-baumann-type",
        "/api/auth/reset-password",
        "/api/users/me/**",
        "/api/addresses/**",
        "/api/cart/**",
        "/api/wishlist/**",
        "/api/orders/**",
        "/api/reviews/exists/**",
        "/api/reviews/*/comments",
        "/api/reviews/comments/**",
        "/api/reviews/*/reaction",
        "/api/reviews/*/report",
        "/api/users/reviews/search",
        "/api/qna/my",
        "/api/images/products/convert-data",
        "/api/images/products/convert-datas",
        "/api/recommendations/all"
    ).authenticated()

    .requestMatchers(HttpMethod.POST, "/api/reviews/*").authenticated()
    .requestMatchers(HttpMethod.PATCH, "/api/reviews/*").authenticated()
    .requestMatchers(HttpMethod.DELETE, "/api/reviews/*/*").authenticated()
    .requestMatchers(HttpMethod.POST, "/api/qna").authenticated()
    .requestMatchers(HttpMethod.PUT, "/api/qna").authenticated()
    .requestMatchers(HttpMethod.DELETE, "/api/qna/*").authenticated()

    .anyRequest().denyAll()
)
```

Day 4 적용 전 확인:

- `HttpMethod` import 필요.
- Spring Security matcher의 `*`/`**` 매칭 방식 확인 후 테스트 필요.
- `GET /api/qna/{qna_id}`를 공개로 둘지 로그인 필요로 둘지 비밀글 정책 확인 필요.
- `/api/images/products/**`는 리뷰 사용자용과 관리자 상품용을 분리하는 편이 좋음.
- 컨트롤러/서비스 단에서 리소스 소유자 검증이 빠진 곳이 있는지 별도 확인 필요.
