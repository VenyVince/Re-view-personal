# Backend Optimization Issue Notes

이 문서는 백엔드 성능 개선 계획을 세우기 위한 문제 제기 목록이다.

위험도 기준:

- 위험도 3: 데이터 증가 시 성능 저하가 거의 확실하거나, 사용자 주요 API에 직접 영향이 큰 항목
- 위험도 2: 성능 저하 가능성이 높지만 사용 빈도, 데이터 규모, 동시성 수준에 따라 영향이 달라지는 항목
- 위험도 1: 영향 범위가 제한적이거나, 운영 빈도가 낮아 우선순위가 낮은 항목

## 위험도 3 - 고위험군

### 1. 상품 목록 조회에 페이징이 없음

- 근거: `src/main/java/com/review/shop/service/product/ProductService.java:29`, `src/main/resources/mapper/product/ProductMapper.xml:7`
- `selectProductList`가 조건에 맞는 상품 전체를 반환한다.
- 정렬 기준이 `rating`, `price`, `review_count`, `created_at`, `product_id` 등으로 다양해 데이터가 늘어나면 정렬 비용이 커질 수 있다.
- 서비스에서 조회된 모든 상품의 썸네일을 `presignedUrlGet`으로 변환하므로 상품 수가 늘수록 DB 조회 이후 애플리케이션 처리 시간도 같이 증가한다.
- 성능 지표 후보: 상품 수 증가에 따른 목록 API 응답 시간, DB 실행 시간, 응답 payload 크기.

### 2. 검색 API가 부분 일치 검색과 전체 결과 반환 구조임

- 근거: `src/main/resources/mapper/search/header/HeaderSearchProductMapper.xml:16`, `src/main/resources/mapper/search/header/HeaderSearchReviewMapper.xml:36`
- 상품 검색은 `prd_name`, `prd_brand`, `ingredient`, `baumann.type`에 `LIKE '%' || keyword || '%'`를 사용한다.
- 리뷰 검색도 `review.content`, `user.nickname`에 동일한 부분 일치 검색을 사용한다.
- 일반 B-Tree 인덱스를 타기 어려운 조건이며, 검색 결과에 페이징 제한도 없다.
- `HeaderSearchService.search()`는 상품 검색과 리뷰 검색을 한 요청에서 모두 수행한다.
- 검색 결과 전체에 대해 이미지 URL 변환까지 반복하므로 키워드가 넓을수록 DB, 애플리케이션, 응답 크기 병목이 동시에 발생할 수 있다.

### 3. 상품 상세 리뷰 조회에 페이징이 없음

- 근거: `src/main/java/com/review/shop/service/review/ProductReviewService.java:72`, `src/main/resources/mapper/review/ProductreviewMapper.xml:24`
- 특정 상품의 모든 리뷰를 한 번에 조회한다.
- `review_images`를 직접 조인하기 때문에 이미지가 여러 개인 리뷰는 row가 늘어난다.
- 서비스에서 다시 `review_id` 기준으로 그룹핑하고, 이미지마다 Presigned URL을 생성한다.
- 리뷰 수와 이미지 수가 늘어날수록 DB row 수, Java 메모리 사용량, URL 생성 횟수가 함께 증가한다.
- 상품 상세 페이지는 사용자 진입 빈도가 높은 영역이므로 성능 영향이 클 수 있다.

### 4. 이미지 Presigned URL 생성이 목록 row 수에 비례해 반복됨

- 근거: `src/main/java/com/review/shop/image/ImageService.java:164`
- 사용 위치: 상품 목록, 리뷰 목록, 상품 상세 리뷰, 검색 결과, 추천 결과, 장바구니, 위시리스트, 관리자 상품 목록 등.
- `presignedUrlGet`은 이미지 object key마다 MinIO SDK를 통해 URL을 생성한다.
- 이미지가 포함된 목록 API에서 DB 조회가 빨라도 URL 생성 반복이 응답 시간의 병목이 될 수 있다.
- 같은 이미지 key가 여러 API 또는 같은 응답 안에서 반복될 경우에도 매번 새 URL을 생성한다.

### 5. 리뷰 목록 조회가 OFFSET 기반 페이지네이션과 무거운 조인을 함께 사용함

- 근거: `src/main/resources/mapper/review/ReviewMapper.xml:8`, `src/main/resources/mapper/review/ReviewMapper.xml:66`
- 리뷰 목록은 `review`, `user_table`, `product`, `review_images`를 조인하고 상품 썸네일 서브쿼리도 수행한다.
- `OFFSET #{offset} ROWS FETCH NEXT #{size} ROWS ONLY` 구조라 뒤 페이지로 갈수록 앞 row를 건너뛰는 비용이 커질 수 있다.
- 정렬 기준이 `created_at`, `rating`, `like_count`, `dislike_count` 등으로 다양해 인덱스 설계 없이 데이터가 증가하면 정렬 비용이 커질 수 있다.

### 6. MinIO 이미지 처리에 업로드 검증과 장기 운영 대비가 없음

- 근거: `src/main/java/com/review/shop/image/ImageUploadController.java:22`, `src/main/java/com/review/shop/image/ImageService.java:60`, `src/main/java/com/review/shop/image/ImageService.java:99`
- 업로드 API는 파일 본문이 아니라 `file_name`만 받아 presigned PUT URL을 발급한다.
- 서버가 MIME 타입, 파일 크기, 실제 이미지 여부를 검증하지 않는다.
- 내부용/외부용 URL을 분리하는 설정이 없고, nginx도 MinIO를 직접 다루지 않는다.
- 이미지 목록 조회는 요청마다 object key를 presigned GET URL로 바꾸는 방식이라 이미지 수가 많아질수록 조회 비용도 같이 커질 수 있다.
- 이미지 보관 기간, 미사용 object 정리, 썸네일 재생성, 캐시 계층 같은 장기 운영 전략이 보이지 않는다.
- 이미지가 리뷰와 상품의 핵심 시각 요소라 기능 중요도는 높고, 누적 데이터가 늘어날수록 위험이 커진다.

## 위험도 2 - 중위험군

### 1. 추천 쿼리가 요청 시점마다 점수 계산과 정렬을 수행함

- 근거: `src/main/resources/mapper/recommendations/RecommendationsMapper.xml:44`, `src/main/resources/mapper/recommendations/RecommendationsMapper.xml:85`
- 상품 추천은 전체 상품 후보에 대해 Baumann 타입 점수와 리뷰 수 점수를 계산한 뒤 정렬한다.
- 리뷰 추천은 리뷰, 상품, 사용자, Baumann, 이미지 테이블을 조인하고 `ROW_NUMBER() OVER (PARTITION BY p.product_id ...)`로 상품별 대표 리뷰를 계산한다.
- 사용자마다 계산 값이 달라질 수 있어 요청량이 늘면 DB CPU 사용량이 증가할 가능성이 있다.
- `RecommendationsService`에서 추천 결과 이미지 URL도 개별 변환한다.

### 2. 관리자/마이페이지 목록성 API에 페이징이 부족함

- 근거:
  - `src/main/resources/mapper/admin/AdminProductMapper.xml:34`
  - `src/main/resources/mapper/admin/AdminReviewMapper.xml:24`
  - `src/main/resources/mapper/report/reportMapper.xml:7`
  - `src/main/resources/mapper/order/OrderListMapper.xml:7`
- 관리자 상품, 관리자 리뷰, 신고 목록, 사용자 주문 목록이 전체 조회 중심이다.
- 테스트 데이터가 많아지면 관리자 페이지나 마이페이지 최초 진입 응답 시간이 늘어날 수 있다.
- 주문 목록은 row마다 대표 상품명과 item count를 서브쿼리로 계산한다.

### 3. 주문 검색이 DB에서 넓게 조회한 뒤 Java에서 추가 필터링함

- 근거: `src/main/java/com/review/shop/service/search/HeaderSearchService.java:118`, `src/main/resources/mapper/order/OrdersMapper.xml:96`
- 주문번호와 배송번호를 `LIKE '%' || keyword || '%'`로 검색한다.
- 상태 필터와 정렬은 `HeaderSearchService.searchOrders()`에서 Java stream으로 처리한다.
- 검색 결과가 많아지면 DB에서 불필요한 row를 가져오고 애플리케이션에서 다시 필터링하는 비용이 발생한다.

### 4. 주문 처리 트랜잭션 안에서 여러 작업이 순차 실행됨

- 근거: `src/main/java/com/review/shop/service/order/OrderService.java:151`
- `processOrder()` 안에서 포인트 차감, 재고 차감, 주문 저장, 주문 상세 저장이 순차 실행된다.
- 재고 차감은 상품별로 `deductStock`을 반복 호출한다.
- 주문 상품 수가 늘어나면 트랜잭션 유지 시간이 길어질 수 있다.
- 인기 상품 주문이 몰리면 재고 row 업데이트 경합이 발생할 수 있다.

### 5. 포인트 처리가 사용자 row lock에 의존함

- 근거: `src/main/java/com/review/shop/service/userinfo/other/PointService.java:31`, `src/main/resources/mapper/userinfo/other/point/PointMapper.xml:23`
- 포인트 변경 시 `SELECT point ... FOR UPDATE`로 사용자 row를 잠근 뒤 업데이트한다.
- 주문, 리뷰 작성, 리뷰 삭제, 관리자 지급, 베스트 리뷰 지급이 모두 같은 포인트 처리 경로를 사용한다.
- 동일 사용자의 주문/리뷰/포인트 요청이 겹치면 대기 시간이 생길 수 있다.
- 기능 안정성 측면에서는 명확하지만, 동시 요청 성능 측정 시 락 대기 시간이 주요 지표가 될 수 있다.

### 6. 마이페이지 리뷰 검색도 이미지 조인과 부분 일치 검색을 사용함

- 근거: `src/main/java/com/review/shop/service/search/MyPageReviewService.java:24`, `src/main/resources/mapper/review/MyPageReviewMapper.xml:23`
- 사용자별 리뷰 검색이지만 `review_images`를 조인하고 `p.prd_name`, `r.content`에 부분 일치 검색을 사용한다.
- 사용자당 리뷰 수가 적을 때는 체감이 작지만, 리뷰와 이미지가 누적되면 검색 결과 row와 URL 변환 비용이 커진다.
- 마이페이지는 내역 조회가 반복되는 화면이라 누적 부하가 생기기 쉽다.

### 7. 상품 상세 조회가 이미지 수만큼 URL 변환을 반복함

- 근거: `src/main/java/com/review/shop/service/product/ProductDetailService.java:23`
- 상품 상세는 상품 본문 조회 후 이미지 목록을 다시 조회하고, 각 이미지 key마다 presigned URL을 생성한다.
- 상품당 이미지 개수가 많아질수록 응답 생성 비용이 선형으로 증가한다.
- 상세 페이지는 방문 빈도가 높아 요청량이 몰릴 수 있어 누적 부하가 커질 수 있다.

### 8. 주문 목록 조회가 행마다 서브쿼리를 반복함

- 근거: `src/main/resources/mapper/order/OrderListMapper.xml:7`
- 주문 목록은 각 주문 row마다 대표 상품명과 주문 상품 수를 서브쿼리로 구한다.
- 주문 이력이 길어질수록 목록 진입 시 반복 계산 비용이 늘어난다.
- 마이페이지에서 반복 조회되는 화면이라 규모가 커지면 응답 지연이 보일 수 있다.

### 9. 월간 베스트 리뷰 갱신 작업이 전체 후보를 다시 계산함

- 근거: `src/main/java/com/review/shop/util/ReviewScheduler.java:10`, `src/main/java/com/review/shop/service/review/ProductReviewService.java:268`
- 매달 1일 실행되는 배치가 베스트 리뷰 후보를 다시 조회하고, 기존 플래그를 초기화한 뒤 다시 업데이트한다.
- 댓글 수, 좋아요 수, 리뷰 수가 누적될수록 배치 실행 시간이 길어질 수 있다.
- 현재는 월 1회지만 데이터가 커지면 배치 자체가 늦어져 운영 시간에 영향을 줄 수 있다.

## 위험도 1 - 저위험군

### 1. `ORDER BY DBMS_RANDOM.VALUE` 사용

- 근거: `src/main/resources/mapper/recommendations/RecommendationsMapper.xml:152`
- 관리자 픽 추천 1건을 뽑기 위해 조건에 맞는 row를 랜덤 정렬한다.
- 선택된 리뷰 수가 많지 않으면 영향은 제한적이다.
- 데이터가 많아지면 매 요청마다 정렬 비용이 생길 수 있다.

### 2. 베스트 리뷰 선정 쿼리가 댓글 수 서브쿼리를 반복함

- 근거: `src/main/resources/mapper/review/ProductreviewMapper.xml:130`
- 베스트 리뷰 후보 조회에서 `REVIEW_COMMENT` count 서브쿼리가 select와 order by에 반복된다.
- 스케줄러성 작업이라 사용자 요청 API보다 우선순위는 낮다.
- 데이터가 늘면 배치 실행 시간이 길어질 수 있다.

### 3. 목록 응답 DTO에 큰 텍스트 컬럼이 포함될 수 있음

- 근거:
  - `src/main/resources/mapper/search/header/HeaderSearchProductMapper.xml:9`
  - `src/main/resources/mapper/admin/AdminProductMapper.xml:35`
- 상품 검색 결과에 `description`, 관리자 상품 목록에 `ingredient`, `description` 등 큰 텍스트성 컬럼이 포함될 수 있다.
- 목록 화면에서 전체 본문이 필요하지 않다면 응답 크기와 DB I/O가 불필요하게 커질 수 있다.

## 문제 발생 가능성 존재

### 1. MyBatis cache 설정의 실제 효과가 불명확함

- 근거: `src/main/resources/application.properties:16`
- `mybatis.configuration.cache-enabled=true`가 설정되어 있지만 mapper XML에 명시적인 `<cache>` 설정은 보이지 않는다.
- 현재 구조에서 읽기 빈도가 높은 기준 데이터 또는 반복 조회 데이터에 캐시 효과가 있는지 확실하지 않다.
- 성능 문제가 이미 발생했다고 보기는 어렵고, 측정 후 판단할 항목이다.

### 2. 추천 API의 실제 병목 여부는 데이터 규모와 호출 빈도에 따라 달라짐

- 근거: `src/main/resources/mapper/recommendations/RecommendationsMapper.xml:44`, `src/main/resources/mapper/recommendations/RecommendationsMapper.xml:85`
- 쿼리 구조는 무겁지만 현재 데이터가 작거나 호출 빈도가 낮으면 실제 병목으로 드러나지 않을 수 있다.
- 성능 개선 대상으로 삼기 전 상품/리뷰 데이터 규모를 늘린 상태에서 실행 시간 측정이 필요하다.

### 3. 포인트 row lock은 문제이면서 동시에 정합성 보호 장치임

- 근거: `src/main/resources/mapper/userinfo/other/point/PointMapper.xml:23`
- `FOR UPDATE`는 동시성 성능에는 불리할 수 있지만 포인트 정합성을 보호한다.
- 단순히 제거할 수 있는 문제가 아니며, 실제 락 대기 시간이 측정될 때 개선 대상으로 보는 것이 적절하다.

## 성능 개선 주제로 잡기 좋은 API 후보

1. 상품 목록 API
   - 이유: 페이징 없음, 정렬 다양, 이미지 URL 변환 반복.
   - 측정 포인트: 상품 100/1,000/10,000건 기준 평균 응답 시간, p95, 응답 크기.

2. 통합 검색 API
   - 이유: 부분 일치 검색, 상품+리뷰 동시 조회, 페이징 없음.
   - 측정 포인트: 키워드별 응답 시간, DB 실행 계획, 검색 결과 수 증가에 따른 처리 시간.

3. 상품 상세 리뷰 API
   - 이유: 특정 상품 리뷰 전체 조회, 이미지 조인 row 증폭, Java 그룹핑.
   - 측정 포인트: 리뷰 수와 이미지 수 증가에 따른 응답 시간, 반환 row 수, URL 생성 횟수.

4. 리뷰 목록 API
   - 이유: 조인/서브쿼리/OFFSET 페이지네이션 조합.
   - 측정 포인트: page 번호 증가에 따른 응답 시간, 정렬 기준별 DB 비용.

5. 추천 API
   - 이유: 요청 시점 점수 계산, window function, lateral image lookup.
   - 측정 포인트: 상품/리뷰 데이터 증가에 따른 DB CPU 시간과 응답 시간.
