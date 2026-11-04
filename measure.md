# 측정 기록

계획의 기준 커밋은 `c95dd83d`다. 이 작업 트리의 HEAD는 `c84fbc3a`다. `src`, `infra`, `View`, 루트 `Dockerfile`은 `c95dd83d`와 같다. `.github/workflows/deploy.yml`은 빈 줄만 다르다. 단계, `self-hosted`, Secrets, `compose down`, `build --no-cache`, `up -d`는 같다.

반영 판정은 v0.9.0에서 채운다. 여기의 결과는 파일에서 읽은 사실, 측정 프로파일로 확인한 적재 건수, v0.2.0 포인트 동시성, v0.3.0 추천 SQL, v0.4.0 헤더 검색, v0.5.0 주문·포인트, v0.6.0 이미지·MinIO, v0.7.0 스케줄러, v0.8.0 보안이다.

## E0 결과 표

| 이력서 문장 | 파일 경로 | 커밋 해시 | 조건 | 결과 | 한계 | 반영 판정 |
| --- | --- | --- | --- | --- | --- | --- |
| 바우만 4기준 일치, 리뷰 수·별점 가중치, 부분 일치, `ROW_NUMBER`로 상품당 대표 리뷰 1개, MyBatis SQL | `src/main/resources/mapper/recommendations/RecommendationsMapper.xml`의 `findRecommendProducts`, `findRecommendReviews`. `src/main/java/com/review/shop/service/recommendations/RecommendationsService.java` | 현재 코드 `c95dd83d`. 추천 SQL 이력 `e7c2fa82`, `9b4be372`, `54d9aa3b` | 정답 픽스처의 4축 일치, 2축 일치, 상품당 리뷰 2개, `is_checked = 1`의 1.2배. 리뷰 2,000건에서 워밍업 2회 뒤 10회. `EXPLAIN PLAN` 1회 | 아래 v0.3.0. 리뷰 16행, 상품 ID 중복 0건. 상품 1의 대표는 리뷰 2, 점수 72. 상품 쿼리 16행에 상품 1(120) 포함, 상품 2(60) 제외. 리뷰 2,000건에서 상품 쿼리 평균 2.357ms 최댓값 2.807ms, 리뷰 쿼리 평균 11.481ms 최댓값 13.299ms | 상품 쿼리 원문에는 `ROW_NUMBER`가 없다. 실행 계획의 `ROW_NUMBER`는 `FETCH FIRST 16`이다. `deleted_at` 조건은 없다. `e7c2fa82`에는 리뷰 추천 SQL이 없다. 개선율은 계산하지 않았다 | 미판정 |
| 주문 상품에 연결해 실제 구매자만 리뷰를 작성. 주문 시점의 상품명·가격 저장. 포인트 잔액과 분리된 이력 | `ProductReviewService.canCreate`. `src/main/resources/mapper/order/OrderItemIdMapper.xml`. `OrdersMapper.xml`의 `ORDER_ITEM.PRODUCT_NAME`, `PRODUCT_PRICE`. `src/main/resources/mapper/userinfo/other/point/PointMapper.xml` | 현재 코드 `c95dd83d` | 주문상품 없음, 타인 `order_item_id`, 이미 리뷰가 있는 `order_item_id`, 본인 주문상품 각 1건. 주문 후 상품명·가격 변경. `usePoint` 1건. 재고 부족 예외 뒤 주문 행과 포인트 이력 | 아래 v0.5.0. 거절 3건은 모두 `canCreate` false와 `잘못된 접근입니다.` 본인 미작성 주문상품 1은 true. 상품명을 바꾼 뒤 기존 주문상품은 `측정토너` 12,000, 그 뒤 만든 주문상품은 `변경토너` 999. `usePoint` 100은 잔액 9,900, USE 1행. 재고 0 상품 주문은 예외 뒤 주문 수와 잔액이 그대로 | 거절 이유는 메시지로 구분되지 않는다. 확인한 롤백은 `processOrder` 한 번의 재고 예외다. 단일 트랜잭션 문장은 이력서에 없다 | 미판정 |
| 사용자 행을 잠그고 같은 트랜잭션에서 계산·갱신 | `src/main/resources/mapper/userinfo/other/point/PointMapper.xml`의 `selectForUpdate`. `src/main/java/com/review/shop/service/userinfo/other/PointService.java`의 `updateUserPoint`, `addReviewPoint`, `usePoint`, `addBestReviewPoint` | 락이 들어온 커밋 `1d954cca`. 확인 순서 변경은 이번 측정에서 `PointService`에 반영 | 사용자 1명, 시작 잔액 10,000, 적립 100과 차감 100을 섞어 20개 동시, 5번 반복. 같은 사용자·같은 리뷰에 `addBestReviewPoint` 10스레드 1회 | 일반 잔액·이력 불일치 0건. 수정 전 베스트 700원 EARN 9행. 잠금 뒤 확인으로 바꾼 재측정은 1행. 아래 v0.2.0 | `getCurrentUserRole()`은 잠금을 잡은 뒤 로그인 아이디로 `USER_TABLE.role`을 다시 조회한다. `point_history`에는 중복 방지 유니크 제약이 없다 | 미판정 |
| 리뷰 수정 시 `http` URL은 무시하고, 새 object key가 있을 때만 교체 | `src/main/java/com/review/shop/service/review/ProductReviewService.java`의 `filterObjectKeys`, `updateReview` | 필터 커밋 `63521f66`. 현재 코드 `c95dd83d` | 리뷰 1. `images`가 비어 있음, `http` URL만, `reviews/...` key, 본문이 비어 예외. 교체 1건의 MinIO 객체 | 아래 v0.6.0. 빈 목록과 `http` URL은 key `reviews/fixture/review-1.jpg` 유지. 본문 없음은 `리뷰 내용이 필수입니다`이고 key 유지. 새 key 1건은 그 key로 교체되고 객체 크기 13 | 수정 API에 이미지 제거가 없다. `selectReviewById`는 `REVIEW_IMAGES`를 읽지 않아 유지 경로의 응답 `images`는 빈 목록이다 | 미판정 |
| `Client → Nginx → Spring Boot → Oracle / MinIO`를 EC2 Compose로 운영. GitHub Actions self-hosted runner와 Secrets. 이미지에 `.env` 없음 | `infra/docker-compose.yml`. `View/nginx/default.conf`. `View/Dockerfile`. `.github/workflows/deploy.yml`. 루트 `Dockerfile` | 현재 코드 `c95dd83d`. workflow 파일은 `c84fbc3a`에서 빈 줄만 다름 | 서비스 4개, `/api` 프록시, `self-hosted`, Secrets, 루트 Dockerfile의 `.env` COPY 유무. Actions는 최근 실행의 상태와 run id. EC2 재배포 없음 | 아래 C2. 최근 실행은 `cancelled`, run id `25278122824` | 운영 배포 성공률은 이 파일 대조로 말하지 않는다 | 미판정 |
| 소개 문장의 검색·추천·이미지·포인트·배포 | 검색 `src/main/java/com/review/shop/service/search/HeaderSearchService.java`, `src/main/resources/mapper/search/header/HeaderSearchProductMapper.xml`, `HeaderSearchReviewMapper.xml`. 추천·이미지·포인트·배포는 위 행의 경로 | 현재 코드 `c95dd83d`. 정렬 키 변경 `2c4f24ab` | 상품명 키워드, 브랜드 필터, 삭제 상품 제외, 기본 정렬, 정렬 하나. 리뷰 검색 키워드 1건과 10회 시간 | 아래 v0.4.0. 키워드 `측정토너`는 상품 1, 삭제 상품 3은 제외. `filter_brand=MeasureBrand`는 상품 1. 기본 정렬과 `latest`는 1, 2 순. `low_price`는 2, 1 순. 리뷰 키워드 `리뷰검색어`는 리뷰 3. 리뷰 2,000건에서 상품 평균 6.396ms 최댓값 7.968ms, 리뷰 평균 15.181ms 최댓값 17.447ms | 2글자 제한은 `HeaderSearchController`에 있고 SQL에는 없다. 서비스는 브랜드를 `filter_category` 자리에 넘긴다. `2c4f24ab`는 술어가 같고 정렬만 달라 개선율을 계산하지 않았다 | 미판정 |

측정 항목 중 이력서 문장 행에 결과가 나뉘어 들어가는 경로는 다음과 같다.

| 항목 | 파일 경로 | 커밋 해시 | 조건 | 결과 | 한계 | 반영 판정 |
| --- | --- | --- | --- | --- | --- | --- |
| C1 MinIO | `src/main/java/com/review/shop/image/ImageService.java`. `src/main/java/com/review/shop/image/minio/MinioConfig.java`. `ImageUploadController` | `c95dd83d` | 파일 1건. object key, PUT URL, DB의 key, GET URL, 코드의 만료 10분·5분, 객체 존재 | 아래 v0.6.0. 응답 키는 `objectKey`, `presignedUrl`. PUT 200, DB는 URL이 아닌 key, GET은 다른 URL. `X-Amz-Expires`는 PUT 600, GET 300. 객체 크기 13 | `main`의 MinIO 클라이언트는 `MINIO_URL` 하나다. 만료 시각까지 기다리지 않았다 | 미판정 |
| C3 스케줄러 | `src/main/java/org/spring/project/re_view/ReViewApplication.java`의 `@EnableScheduling`. `src/main/java/com/review/shop/util/ReviewScheduler.java`. `ProductreviewMapper.xml`의 `selectBestReviewIds` | `c95dd83d` | cron `0 0 0 1 * *`, zone `Asia/Seoul`, 좋아요 20 초과, 상품 리뷰 수 30 초과, 상위 10%. 로그인 없이 `runUpdateBestReviewsNow()` 2회 | 아래 v0.7.0. 후보 리뷰는 4, 5, 6, 7. 두 번 모두 `로그인이 필요합니다`로 롤백. `is_checked`와 700원 EARN 0행, user 4 잔액 0은 그대로 | 운영 로그가 없어 정기 실행이라고 쓰지 않는다. 이번 2회는 이력 행이 생기기 전에 롤백되어 중복 방지까지 가지 않았다. 동시 중복은 v0.2.0을 인용한다 | 미판정 |
| B3 Security | `src/main/java/com/review/shop/config/SecurityConfig.java` 112행 `anyRequest().permitAll()` | `c95dd83d` | 공개 GET `/api/products`, 비로그인 POST `/api/qna`, `ROLE_USER`의 GET `/api/admin/reviews`, matcher 밖 GET `/api/not-in-matchers`, 빈 로그인 JSON | 아래 v0.8.0. 200, 403, 403, 500, 400. 401은 이 다섯 요청에 없었다. matcher 밖은 보안을 통과한 뒤 500 | 전체 보호율은 계산하지 않는다. 403 JSON은 CSRF 거부와 권한 부족이 같은 문구다 | 미판정 |

## C2 Compose·Actions

파일을 읽어 확인한 내용이다. EC2에는 다시 배포하지 않았고, BE·FE 이미지는 빌드하지 않았다.

| 확인 | 파일 | 읽은 내용 |
| --- | --- | --- |
| 서비스 4개 | `infra/docker-compose.yml` | `minio`(`quay.io/minio/minio`), `oracle-db`(`gvenzl/oracle-xe:latest`), `be`(루트 `Dockerfile`), `fe`(`View/Dockerfile`). 네 서비스 모두 `env_file: .env`는 `minio`, `oracle-db`, `be`에 있다. `fe`는 build arg `REACT_APP_API_BASE_URL` |
| `/api` 프록시 | `View/nginx/default.conf` | `location /api/`가 `proxy_pass http://be:8080/api/` |
| nginx 이미지 | `View/Dockerfile` | 빌드 스테이지는 `COPY . .` 이후 `npm run build`. 최종 스테이지는 `/app/build`를 `/usr/share/nginx/html`로 복사하고 `nginx/default.conf`를 넣는다 |
| self-hosted | `.github/workflows/deploy.yml` | `on.push.branches: main`. `runs-on: self-hosted` |
| Secrets | `.github/workflows/deploy.yml` | `secrets.ENV_FILE`을 `infra/.env`로 쓰고, `secrets.REACT_APP_API_BASE_URL`을 `View/.env.production`에 쓴다 |
| workflow 단계 | `.github/workflows/deploy.yml` | checkout(`clean: false`), env 파일 생성, `docker compose down`, `docker compose build --no-cache`, `docker compose up -d` |
| 루트 이미지의 `.env` | `Dockerfile` | `COPY pom.xml`, `COPY src ./src`, `COPY --from=build /app/target/*.jar app.jar`. `.env` COPY는 없다 |
| Actions 실행 | GitHub `VenyVince/Re_View` workflow `Deploy Review Project` | 최근 실행은 `cancelled`. run id `25278122824` |

Oracle을 띄운 뒤 `docker compose ps`로 측정 컨테이너가 `gvenzl/oracle-xe:latest`인지 확인한다. 그 결과는 E1에 적는다.

## E1 스키마·시드·프로파일

측정 Oracle은 `measure/docker-compose.yml`의 `oracle-db`다. 이미지는 `infra/docker-compose.yml`과 같은 `gvenzl/oracle-xe:latest`다. v0.6.0에서 같은 파일에 `minio`(`quay.io/minio/minio`, 컨테이너 `measure-minio`, 호스트 9000)를 추가했다. 계정은 측정 프로파일과 같은 `measure` / `measure_local`이고 볼륨은 `measure-minio-data`다. `infra/.env`와 `~/volumes/minio_data`는 쓰지 않는다. BE·FE는 올리지 않는다. `infra/.env`가 없고, 제품 compose의 Oracle 볼륨은 `~/volumes/oracle-data`라서 제품 compose 파일은 수정하지 않고 측정 compose를 따로 둔다.

접속은 `src/test/resources/application-measure.properties`의 프로파일 `measure`다. `application-test.properties`와 `ReViewApplicationTests`는 바꾸지 않았다. 기본 `mvn test`는 surefire `excludedGroups=measure`라 이 프로파일 테스트를 돌리지 않는다.

계정은 로컬 측정 전용이다.

| 스키마 | 용도 | 사용자 | JDBC |
| --- | --- | --- | --- |
| 정답 픽스처 | A1·A2·A3·B1·B2·C3 | `MEASURE` / `measure_local` | `jdbc:oracle:thin:@//localhost:1522/XEPDB1` |
| 성능 | A2·A3 시간 | `MEASURE_PERF` / `measure_local` | 같은 URL |

호스트 1521은 이미 기동된 `review-db`(`gvenzl/oracle-xe:latest`)가 쓴다. 측정 컨테이너는 호스트 1522를 컨테이너 1521에 연결한다. `review-db`의 볼륨은 변경하지 않는다.

SYS 비밀번호 `measure_oracle`는 컨테이너 기동용이다.

정답 픽스처의 고정 id는 아래다. 이후 측정은 이 id를 쓴다.

| id | 내용 |
| --- | --- |
| user 1 | `measure_user`, 축 D/S/P/W, 잔액 10,000, 이력 EARN 10,000 1행. 동시성 시작 잔액 |
| user 2 | `other_user`. 주문상품 3의 구매자 |
| user 3 | `empty_user`. 주문상품 없음 |
| user 4 | `author_user`. 스케줄·필러 리뷰 작성자 |
| user 5 | `admin_user`, role `ADMIN` |
| product 1 | `측정토너`, 브랜드 `MeasureBrand`, 4축 일치, 평점 4.5, 가격 12,000, 재고 20, `created_at` 2026-03-01 |
| product 2 | `측정로션`, 2축(D,S)만 일치 |
| product 3 | `측정토너-단종`, 같은 키워드·브랜드, `deleted_at` 있음 |
| product 4 | `스케줄세럼`. 컬럼 `review_count` 40, 리뷰 행은 7개. 좋아요 30, 28, 26, 24, 22, 10, 5가 review 4부터 10 |
| product 5 | `저가고평점`. 가격 5,000, 평점 4.9, `created_at` 2026-01-01 |
| product 16 | 4축 모두 불일치, `review_count` 0 |
| product 17 | `THIRD`가 null |
| product 18 | 재고 0 |
| order item 1 | user 1, 리뷰 없음 |
| order item 2 | user 1, review 1이 연결. 스냅샷 이름 `측정토너`, 가격 12,000 |
| order item 3 | user 2 |
| order item 4 | user 1, review 2가 연결 |
| review 1 | product 1, 평점 4.5, `is_checked` 0, 좋아요 5, 이미지 `reviews/fixture/review-1.jpg` |
| review 2 | product 1, 평점 2.0, `is_checked` 1, 좋아요 1 |
| review 3 | product 2, 본문에 `리뷰검색어` |

`measure-oracle`는 `gvenzl/oracle-xe:latest`로 떠 있다. 같은 이미지의 `review-db`도 호스트 1521에서 떠 있고, `infra/docker-compose.yml`의 `oracle-db` 이미지와 같다. `review-minio`는 Exited다. BE·FE 이미지는 이번에 빌드하지 않았다.

프로파일 `measure`로 `MeasureConnectionTest` 2건이 통과했다. 정답 스키마는 사용자 5, 상품 20, 리뷰 40, 주문상품 4, user 1 잔액 10,000이다. 성능 스키마는 사용자 20, 상품 200, 리뷰 2,000이다.

## v0.2.0 포인트 동시성

`PointConcurrencyMeasureTest`가 스프링의 `PointService`를 호출했다. 스레드마다 로그인 아이디 `measure_user`의 `SecurityContext`를 넣었다. `getCurrentUserRole()`은 권한 객체가 아니라 그 아이디로 DB의 role을 읽고, 이 조회는 `selectForUpdate` 다음에 있다. 첫 측정에서는 락 순서를 바꾸지 않았다. `8856d129`는 부팅하지 않았다.

일반 잔액은 user 1, 시작 10,000, 적립 100 열 건과 차감 100 열 건을 동시에 실행하고 그 묶음을 5번 반복했다. 회차마다 잔액과 이력을 시작 상태로 되돌린 뒤 측정했다. 기댓값은 시작 잔액 + 성공 적립 − 성공 차감이다.

| 회차 | 성공 적립 | 성공 차감 | 예외 | 기댓값 | `USER_TABLE.point` | EARN 합 − USE 합 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | 10 | 10 | 0 | 10,000 | 10,000 | 10,000 |
| 2 | 10 | 10 | 0 | 10,000 | 10,000 | 10,000 |
| 3 | 10 | 10 | 0 | 10,000 | 10,000 | 10,000 |
| 4 | 10 | 10 | 0 | 10,000 | 10,000 | 10,000 |
| 5 | 10 | 10 | 0 | 10,000 | 10,000 | 10,000 |

잔액과 이력의 불일치 건수는 0이다.

베스트 지급은 같은 user 1, review 2에 `addBestReviewPoint` 10스레드를 한 번 호출했다. 예외는 0이고, 700원 EARN 행은 9행이다. 1행이 아니므로 실패다. 당시에는 `checkBestReviewPoint`가 `selectForUpdate`보다 앞이었다.

이 결과를 보고 `addBestReviewPoint`에서 사용자 행 `selectForUpdate`를 중복 확인보다 앞으로 옮겼다. `point_history` 유니크 제약은 추가하지 않았다. 같은 테스트로 다시 측정했다. 일반 잔액 5회는 다시 불일치 0건이었다. 베스트 10스레드는 예외 0, 700원 EARN 1행이었다.

| 구분 | 성공 적립 | 성공 차감 | 예외 | 잔액 | 이력 합 | 베스트 700원 EARN |
| --- | --- | --- | --- | --- | --- | --- |
| 수정 전 | 10 | 10 | 0 | 10,000 | 10,000 | 9행 |
| 잠금 뒤 확인 | 10 | 10 | 0 | 10,000 | 10,000 | 1행 |

재측정이 끝난 뒤 user 1은 시드 상태로 되돌렸다. 잔액 10,000, EARN 10,000 1행이다.

## v0.3.0 추천 SQL

`RecommendSqlMeasureTest`가 사용자 1의 바우만 축 D/S/P/W로 `RecommendationsMapper`의 두 SELECT를 따로 호출했다. `RecommendationsService`와 `ImageService`는 호출하지 않았다. 시간과 실행 계획은 같은 SQL을 JDBC `PreparedStatement`로 실행했다. 워밍업 2회 뒤 10회다. 평균과 최댓값만 적는다. 인덱스는 추가하지 않았다.

정답 픽스처는 상품 20, 리뷰 40이다. 성능 스키마는 사용자 20, 상품 200, 리뷰 2,000이다.

### 대표 케이스

상품 쿼리 원문에는 `ROW_NUMBER`가 없다. 리뷰 쿼리 원문에는 `PARTITION BY p.product_id`인 `ROW_NUMBER`가 있다.

| 케이스 | 쿼리 | 이번 실행 |
| --- | --- | --- |
| 4축 일치 | 상품 | 상품 1, 점수 120, 16행 안에 있음 |
| 2축 일치 | 상품 | 상품 2, 점수 60. `total_score >= 40`은 넘지만 `FETCH FIRST 16` 밖 |
| 리뷰 수 가중 | 상품 | 상품 4, `review_count` 40, 점수 140, 1위 |
| 4축 불일치 | 상품 | 상품 16, 점수 0, 16행 밖 |
| 삭제 상품 | 상품 | `deleted_at` 조건이 없다. 상품 3, 18, 20은 점수 120, `review_count` 0으로 같다. 이번 16행에는 3과 18이 들어갔고 20은 빠졌다 |
| 한 상품에 리뷰 2개 | 리뷰 | 16행, 상품 ID 중복 0건. 상품 1은 리뷰 2만 남음 |
| `is_checked = 1`의 1.2배 | 리뷰 | 리뷰 1 기본 점수 70, `is_checked` 0. 리뷰 2 기본 점수 60, `is_checked` 1, 1.2배 후 72. 대표는 리뷰 2 |
| 2축 리뷰 | 리뷰 | 상품 2의 리뷰 3이 이번 16행의 마지막, 점수 60. 상품 5의 리뷰 11·12도 점수 60, 좋아요 0이다. `ORDER BY`는 둘 중 누구를 16번째로 둘지 정하지 않는다 |

상품 쿼리가 이번 실행에서 반환한 상품 ID는 4, 1, 5, 19, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 3, 18이다. 같은 점수와 같은 `review_count` 사이의 순서는 고정되어 있지 않다.

### 시간

단위는 ms다. 10회 표본의 평균과 최댓값이다.

| 쿼리 | 데이터 | 반환 행 | 평균 | 최댓값 |
| --- | --- | --- | --- | --- |
| `findRecommendProducts` | 정답 픽스처 | 16 | 2.710 | 3.244 |
| `findRecommendReviews` | 정답 픽스처 | 16 | 6.956 | 8.622 |
| `findRecommendProducts` | 성능 스키마, 리뷰 2,000 | 16 | 2.357 | 2.807 |
| `findRecommendReviews` | 성능 스키마, 리뷰 2,000 | 16 | 11.481 | 13.299 |

성능 스키마의 `EXPLAIN PLAN`은 쿼리당 1회다. 상품 쿼리 plan hash `1000561082`는 `BAUMANN`, `PRODUCT`, `PRODUCT_IMAGE`를 `TABLE ACCESS FULL`로 읽는다. 계획에 보이는 `ROW_NUMBER`는 `FETCH FIRST 16`을 정렬 제한으로 바꾼 것이고 `PARTITION BY product_id`가 아니다. 리뷰 쿼리 plan hash `120112575`는 `REVIEW` 2,000행을 포함한 조인 테이블을 풀 스캔하고, `PARTITION BY product_id` 창 정렬에 임시 공간 3,208KB를 적었다. 옵티마이저 `Time` 열은 측정 시간이 아니다.

### `e7c2fa82`

그 커밋의 `findRecommencementsWithAll`을 현재 `MEASURE` 스키마에서 실행했다. 컬럼은 맞았다. 10회 모두 19행, 평균 3.630ms, 최댓값 4.381ms다. `ORDER BY`에 `DBMS_RANDOM.VALUE`가 있어 순서는 비교하지 않았다. 10회를 합친 상품 ID는 19개고, 현재 상품 쿼리의 16개 ID는 그 안에 모두 있다. 과거에만 있는 ID는 2, 17, 20이다. 상품 16은 양쪽 다 없다.

같은 픽스처의 현재 상품 쿼리는 16행, 평균 2.710ms다. 점수식과 행 제한이 다르므로 개선율로 계산하지 않았다. 그 커밋의 나머지 네 개 축별 SELECT는 실행하지 않았다. 리뷰 추천 SQL은 그 커밋에 없다. `9b4be372`는 비교하지 않았다.

## v0.4.0 헤더 검색

`HeaderSearchMeasureTest`가 `HeaderSearchProductMapper.searchProducts`와 `HeaderSearchReviewMapper.searchReviews`를 호출했다. `HeaderSearchService`와 `ImageService`는 호출하지 않았다. 시간과 실행 계획은 같은 SQL을 JDBC로 워밍업 2회 뒤 10회 실행한 값이다. 인덱스는 추가하지 않았다.

매퍼 인자 순서는 `(keyword, sort, filter_category, filter_brand)`다. `HeaderSearchService`는 세 번째에 `filter_brand`, 네 번째에 `filter_category`를 넘긴다. 아래 브랜드 통과는 XML 이름에 맞춘 호출이다. 브랜드 문자열을 세 번째 자리에 넣은 호출은 비었다.

### 대표 조건

| 조건 | 호출 | 이번 실행 |
| --- | --- | --- |
| 상품명 키워드 | `측정토너`, 정렬 없음 | 상품 1. 이름에 같은 글자가 있는 삭제 상품 3은 없음 |
| 삭제 상품 제외 | 위와 같음 | `deleted_at IS NULL`. 상품 3은 결과에 없음 |
| 기본 정렬 | `측정`, 정렬 없음 | 상품 1, 2. `created_at DESC` |
| `latest` | `측정`, `latest` | 상품 1, 2. 기본 정렬과 같은 순서 |
| `low_price` | `측정`, `low_price` | 상품 2, 1. 가격 8,000이 12,000보다 앞 |
| 브랜드 필터 | `측정`, `filter_brand=MeasureBrand` | 상품 1. 브랜드가 다른 상품 2는 없음 |
| 서비스와 같은 인자 순서 | `측정`, 세 번째 인자 `MeasureBrand` | 빈 목록. 그 자리는 `filter_category` |
| 한 글자 | `측` | 상품 1, 2. SQL에는 글자 수 조건이 없음 |
| 리뷰 키워드 | `리뷰검색어` | 리뷰 3, 본문 `리뷰검색어 부분일치` |

2글자 제한은 `HeaderSearchController` 46행 `keyword.length() < 2`다. `HeaderSearchService`와 두 매퍼 SQL에는 없다. 픽스처에 삭제된 리뷰 행은 없어서 리뷰의 `deleted_at` 제외는 행으로 확인하지 못했다. SQL 문장에는 `r.deleted_at IS NULL`이 있다.

### 시간

키워드 `성능`은 성능 스키마의 상품 200건과 리뷰 2,000건에 모두 맞는다. 단위는 ms다.

| 쿼리 | 데이터 | 반환 행 | 평균 | 최댓값 |
| --- | --- | --- | --- | --- |
| 상품 검색 | 정답 픽스처, 키워드 `측정` | 2 | 3.904 | 4.870 |
| 리뷰 검색 | 정답 픽스처, 키워드 `리뷰검색어` | 1 | 3.880 | 5.229 |
| 상품 검색 | 성능 스키마, 키워드 `성능` | 200 | 6.396 | 7.968 |
| 리뷰 검색 | 성능 스키마, 키워드 `성능` | 2,000 | 15.181 | 17.447 |

상품 계획 hash `563358819`는 `PRODUCT`, `BAUMANN`, `PRODUCT_IMAGE`를 `TABLE ACCESS FULL`로 읽는다. 리뷰 계획 hash `2630596270`은 `REVIEW`, `PRODUCT`, `USER_TABLE`, `BAUMANN`을 풀 스캔하고, 리뷰 이미지의 최소 id는 `REVIEW_IMAGES` 기본키 `INDEX UNIQUE SCAN`이다. 정렬에 임시 공간 2,296KB를 적었다. 옵티마이저 `Time` 열은 측정 시간이 아니다.

### `2c4f24ab`

diff의 WHERE 술어는 현재와 같다. 바뀐 것은 정렬 키 이름과 `low_price`, `high_price`, `reviews`, `popular` 분기다. 과거 SQL은 실행하지 않았고 개선율은 계산하지 않았다.

## v0.5.0 주문·포인트

`OrderPointMeasureTest`가 `ProductReviewService`, `PointService.usePoint`, `OrderService.processOrder`를 호출했다. 로그인 아이디는 `measure_user`다. 측정이 끝난 뒤 user 1은 잔액 10,000과 EARN 10,000 한 줄로 되돌렸고, 이번 호출로 생긴 주문은 지웠다. 상품 1은 이름 `측정토너`, 가격 12,000, 재고 20으로 되돌렸다.

### 구매자 제한

`canCreate`는 두 조건을 따로 본다. `existsOrderItem`은 그 `order_item_id`의 주문자와 user id가 같은지 세고, `checkReviewByOrderItemId`는 그 주문상품에 `deleted_at`이 없는 리뷰가 있는지 센다. 둘 중 하나라도 통과하지 못하면 false다. `createReviewWithReward`는 그 false를 모두 `잘못된 접근입니다.`로 던진다.

| 경우 | DB 조건 | `canCreate` | 예외 메시지 |
| --- | --- | --- | --- |
| user 3, 주문상품 1 | 주문자가 user 1 | false | 잘못된 접근입니다. |
| user 1, 주문상품 3 | 주문자가 user 2 | false | 잘못된 접근입니다. |
| user 1, 주문상품 2 | 리뷰 1이 연결됨 | false | 잘못된 접근입니다. |
| user 1, 주문상품 1 | 본인 주문이고 리뷰 없음 | true | 호출하지 않음 |

### 주문 스냅샷

주문상품 1은 `측정토너`, 12,000이었다. 상품 1을 `변경토너`, 999로 바꾼 뒤에도 주문상품 1은 그대로였다. 그 상태에서 상품 1을 1개, `using_point` 0으로 `processOrder`했다. 새 주문상품은 `변경토너`, 999였다. 상품명을 `두번째변경`으로 다시 바꿔도 그 주문상품은 `변경토너`, 999였다. 주문 수는 4에서 5가 됐다.

### 포인트 1건

`usePoint(1, 100)` 앞의 잔액은 10,000, USE 100원 행은 0이었다. 뒤에는 잔액 9,900, USE 100원 행 1이었다. 설명은 `물품 구매로 인한 차감`이다.

### 재고 부족

상품 18의 재고는 0이다. `processOrder`는 `usePoint`를 먼저 호출하고, 이어서 같은 클래스의 `checkAndDeductStock`을 호출한다. `deductStock`은 `stock >= quantity`일 때만 재고를 줄인다. 수량이 1이면 반영 행이 0이고 `WrongRequestException: 재고가 부족합니다. 상품ID: 18`이 난다. `processOrder`에 `@Transactional`이 있고 `usePoint`는 다른 빈의 기본 전파라 그 트랜잭션에 들어온다. 예외 뒤 주문 수는 5 그대로, 잔액은 9,900 그대로, USE 50원 행은 0 그대로였다. 이 한 번의 호출에서 포인트 차감과 주문 저장이 같이 롤백된 것은 확인했다. 다른 진입점까지 한 트랜잭션이라고 쓰지 않는다.

`checkAndDeductStock`과 `saveOrderToDatabase`에는 `@Transactional`이 없다. 재고 차감과 주문 저장은 `processOrder` 안의 같은 클래스 호출이다.

## v0.6.0 이미지·MinIO

`ReviewImageMeasureTest`가 리뷰 1에 `ProductReviewService.updateReview`를 호출하고, `ImageService.uploadReviewImages`로 URL을 받은 뒤 그 URL에 바이트를 직접 PUT했다. 로그인 아이디는 `measure_user`다. MinIO는 `measure-minio`다. 앱은 버킷을 만들지 않아 테스트가 버킷 `measure`가 없으면 만들었다. 끝난 뒤 리뷰 1의 본문은 `풀매치 기본`, 평점은 4.5, 이미지 key는 `reviews/fixture/review-1.jpg`로 되돌렸다. `0884094a`는 부팅하지 않았다.

### 이미지 보존

대상은 리뷰 1, 작성자 user 1, 좋아요 5, `is_checked` 0이다. 시작 key는 `reviews/fixture/review-1.jpg` 한 줄이다. `filterObjectKeys`는 null과 `http`로 시작하는 문자열을 버린다.

| 경우 | 요청 | `REVIEW_IMAGES` | 그 외 |
| --- | --- | --- | --- |
| 본문 없음 | content 빈 문자열, images 빈 목록 | `reviews/fixture/review-1.jpg` | `WrongRequestException: 리뷰 내용이 필수입니다`. 본문은 `풀매치 기본` |
| 이미지 없음 | content `측정수정`, images 빈 목록 | 같은 key | 행을 지우지 않음 |
| URL만 | `http://example.com/old.jpg` | 같은 key | `http`로 시작하는 값은 테이블에 없음 |
| 새 key | 아래 발급 key 1개 | 그 key 한 줄 | 객체 크기 13바이트 |

`updateReview`는 필터 결과가 비면 `deleteReviewImagesByReviewId`를 호출하지 않는다. 이미지 제거 분기는 없고, 제거 테스트는 만들지 않았다.

유지 경로의 응답은 `selectReviewById`를 다시 읽은 뒤 `getImages()`를 서명한다. 그 조회는 `REVIEW_IMAGES`를 가져오지 않는다. DTO의 `images` 기본값은 빈 목록이라, DB key는 유지되지만 응답의 `images`는 비어 있고 기존 key에 대한 GET URL은 만들어지지 않는다.

### 파일 1건

`ImageUploadController`의 `POST /api/images/reviews`는 JSON `ImageRequestDTO.file_name`만 받는다. 테스트는 그 컨트롤러가 그대로 반환하는 `uploadReviewImages("measure.jpg")`를 호출했다. 역할은 `USER`라 관리자 거절에는 들어가지 않았다.

| 확인 | 결과 |
| --- | --- |
| 응답 키 | `objectKey`, `presignedUrl`. 값에 파일 바이트 없음 |
| object key | `reviews/932e1d22-1bb5-40eb-992e-902f43d41ef1.jpg` |
| PUT | 그 URL에 `measure-image` 13바이트. HTTP 200 |
| DB | `REVIEW_IMAGES.image_url`이 그 key. URL 문자열이 아님 |
| GET | `presignedUrlGet` 결과는 PUT URL과 다름 |
| 만료 쿼리 | PUT `X-Amz-Expires=600`, GET `X-Amz-Expires=300`. 코드는 `60 * 10`, `60 * 5` |
| 객체 | `statObject` 크기 13 |

클라이언트는 `MinioConfig`의 `minio.url` 하나다. 측정 프로파일 값은 `http://127.0.0.1:9000`이다. 만료 시각이 지날 때까지 기다리지 않았고, CPU·메모리 감소율은 계산하지 않았다.

## v0.7.0 스케줄러

`SchedulerMeasureTest`가 어노테이션과 선정 쿼리를 읽고, `SecurityContext`를 비운 뒤 `ReviewScheduler.runUpdateBestReviewsNow()`를 두 번 호출했다. 매달 1일을 기다리지 않았고 운영 로그는 보지 않았다. 제품 코드는 바꾸지 않았다.

`ReViewApplication`에 `@EnableScheduling`이 있다. `updateBestReviewMonthly`의 cron은 `0 0 0 1 * *`, zone은 `Asia/Seoul`이다. 이 메서드와 `runUpdateBestReviewsNow`는 둘 다 `ProductReviewService.updateBestReviews`를 호출한다. `GET /api/admin/run`도 같은 메서드를 호출하지만, 이번 측정은 HTTP로 그 경로를 치지 않았다.

### 선정 조건

`selectBestReviewIds`는 `like_count > 20`이고 `product.review_count > 30`인 리뷰를 상품별로 줄 세운다. 정렬은 좋아요 내림차순, 싫어요 오름차순, 댓글 수 내림차순이다. 남기는 수는 `review_number <= FLOOR(review_count * 0.1)`이다. 10%의 기준은 리뷰 행 수가 아니라 상품 컬럼 `review_count`다. SQL에 `deleted_at` 조건은 없다.

상품 4 `스케줄세럼`의 컬럼 `review_count`는 40이고 리뷰 행은 7개다. `FLOOR(40 * 0.1)`은 4다.

| 리뷰 | 좋아요 | 결과 |
| --- | --- | --- |
| 4 | 30 | 후보. user 4 |
| 5 | 28 | 후보. user 4 |
| 6 | 26 | 후보. user 4 |
| 7 | 24 | 후보. user 4 |
| 8 | 22 | 5번째라 탈락 |
| 9 | 10 | `like_count > 20`에서 탈락 |
| 10 | 5 | `like_count > 20`에서 탈락 |

쿼리 결과는 리뷰 4, 5, 6, 7이고 작성자는 user 4 한 명이다. 좋아요가 서로 달라 댓글 수 정렬은 순위를 바꾸지 않았다. 상품 4에 삭제된 리뷰 행은 없다.

### 로컬 2회

`updateBestReviews`는 `@Transactional`이다. 기존 `is_checked`를 0으로 지우고, 후보에 1을 찍은 뒤 `addBestReviewPoint`로 700원을 넣는다. 포인트 갱신은 `getCurrentUserRole()`을 호출하고, 인증이 없으면 `WrongRequestException: 로그인이 필요합니다`를 던진다. 스케줄 메서드는 `SecurityContext`를 채우지 않는다.

두 호출 모두 예외 사슬은 `DatabaseException: Best 리뷰 포인트 적립 중 DB 오류가 발생했습니다.` ← `DatabaseException: 포인트 처리 중 DB 오류가 발생했습니다.` ← `WrongRequestException: 로그인이 필요합니다.`였다.

예외 뒤 리뷰 2의 `is_checked`는 1, 리뷰 4부터 10은 0으로 호출 전과 같았다. user 4의 700원 EARN은 0행, 잔액은 0이었다. 1회차에서 이력 행이 생기기 전에 트랜잭션이 롤백되어, 2회차가 중복 방지를 지나가는지는 보지 못했다. 같은 사용자·같은 리뷰의 동시 10스레드는 v0.2.0에서 잠금 뒤 확인으로 1행이었다. 그 결과를 이번 2회 실행의 중복 방지로 다시 세지 않는다.

## v0.8.0 보안

`SecurityMeasureTest`가 MockMvc로 요청 다섯 개를 보냈다. 전체 경로를 세지 않았고 보호율은 계산하지 않았다. CSRF와 `testcode` 브랜치 대조는 하지 않았다. 제품 코드는 바꾸지 않았다.

`SecurityConfig`는 form login과 HTTP Basic을 끄고, CSRF는 `/api/auth/login`만 제외한다. 인증 실패 진입점은 401 `로그인이 필요합니다`를 쓰고, 권한 거부는 403 `접근 권한이 부족합니다.`를 쓴다. 둘 다 `ErrorResponseDTO`의 `status`, `code`, `message`, `path`, `timestamp`다. 마지막 규칙은 112행 `anyRequest().permitAll()`이다.

| 요청 | 조건 | 상태 | 본문 |
| --- | --- | --- | --- |
| GET `/api/products` | 비로그인. GET 공개 matcher | 200 | JSON 배열. `ProductController`가 호출됨 |
| POST `/api/qna` | 비로그인. 본문 `{}`. CSRF 토큰 없음. POST `/api/qna`는 authenticated | 403 | `Access Denied`, `접근 권한이 부족합니다.`, path `/api/qna`. 컨트롤러는 호출되지 않음 |
| GET `/api/admin/reviews` | 이름 `measure_user`, 권한 `ROLE_USER`. `/api/admin/**`는 `hasRole("ADMIN")` | 403 | 같은 403 JSON. path `/api/admin/reviews`. 컨트롤러는 호출되지 않음 |
| GET `/api/not-in-matchers` | 위 matcher에 없는 경로 | 500 | `Other Exception!`, `No static resource api/not-in-matchers.` 보안 401·403이 아님 |
| POST `/api/auth/login` | 본문 `{}`. 이 경로는 permitAll이고 CSRF 제외 | 400 | `Validation Failed`. `id: 아이디를 입력해주세요.`와 `password: 비밀번호를 입력해주세요.` |

인증 진입점의 401은 이 다섯 요청에 없었다. 비로그인 POST는 컨트롤러 앞에서 403 JSON으로 끝났다. 그 문구는 일반 사용자의 관리자 GET과 같다. matcher 밖 경로는 `permitAll`을 지나 `NoResourceFoundException`이 되었고, 전역 `Exception` 핸들러가 500으로 바꿨다. 검증 실패도 같은 다섯 필드의 JSON이다. 필드 문구의 순서는 고정하지 않는다.
