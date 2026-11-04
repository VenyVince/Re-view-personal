# v0.1.0 측정 기록

계획의 기준 커밋은 `c95dd83d`다. 이 작업 트리의 HEAD는 `c84fbc3a`다. `src`, `infra`, `View`, 루트 `Dockerfile`은 `c95dd83d`와 같다. `.github/workflows/deploy.yml`은 빈 줄만 다르다. 단계, `self-hosted`, Secrets, `compose down`, `build --no-cache`, `up -d`는 같다.

반영 판정은 v0.9.0에서 채운다. 여기의 결과는 파일에서 읽은 사실과, 측정 프로파일로 확인한 적재 건수다.

## E0 결과 표

| 이력서 문장 | 파일 경로 | 커밋 해시 | 조건 | 결과 | 한계 | 반영 판정 |
| --- | --- | --- | --- | --- | --- | --- |
| 바우만 4기준 일치, 리뷰 수·별점 가중치, 부분 일치, `ROW_NUMBER`로 상품당 대표 리뷰 1개, MyBatis SQL | `src/main/resources/mapper/recommendations/RecommendationsMapper.xml`의 `findRecommendProducts`, `findRecommendReviews`. `src/main/java/com/review/shop/service/recommendations/RecommendationsService.java` | 현재 코드 `c95dd83d`. 추천 SQL 이력 `e7c2fa82`, `9b4be372`, `54d9aa3b` | 정답 픽스처의 4축 일치, 2축 일치, 상품당 리뷰 2개, `is_checked = 1`의 1.2배. 리뷰 2,000건에서 워밍업 2회 뒤 10회. `EXPLAIN PLAN` 1회 | 미측정 | 상품 쿼리에는 `ROW_NUMBER`가 없다. 과거 SQL 비교는 `e7c2fa82`를 30분 안에 실행할 때만 | 미판정 |
| 주문 상품에 연결해 실제 구매자만 리뷰를 작성. 주문 시점의 상품명·가격 저장. 포인트 잔액과 분리된 이력 | `ProductReviewService.canCreate`. `src/main/resources/mapper/order/OrderItemIdMapper.xml`. `OrdersMapper.xml`의 `ORDER_ITEM.PRODUCT_NAME`, `PRODUCT_PRICE`. `src/main/resources/mapper/userinfo/other/point/PointMapper.xml` | 현재 코드 `c95dd83d` | 주문상품 없음, 타인 `order_item_id`, 이미 리뷰가 있는 `order_item_id`, 본인 주문상품 각 1건. 주문 후 상품명·가격 변경. `usePoint` 1건. 재고 부족 예외 뒤 주문 행과 포인트 이력 | 미측정 | 거절 메시지는 하나로 묶인다. 단일 트랜잭션 문장은 이력서에 없다 | 미판정 |
| 사용자 행을 잠그고 같은 트랜잭션에서 계산·갱신 | `src/main/resources/mapper/userinfo/other/point/PointMapper.xml`의 `selectForUpdate`. `src/main/java/com/review/shop/service/userinfo/other/PointService.java`의 `updateUserPoint`, `addReviewPoint`, `usePoint`, `addBestReviewPoint` | 락이 들어온 커밋 `1d954cca`. 현재 코드 `c95dd83d` | 사용자 1명, 시작 잔액 10,000, 적립 100과 차감 100을 섞어 20개 동시, 5번 반복. 같은 사용자·같은 리뷰에 `addBestReviewPoint` 10스레드 1회 | 미측정 | `updateUserPoint`는 잠금 안에서 `Security_Util.getCurrentUserRole()`을 본다. `addBestReviewPoint`는 `checkBestReviewPoint`가 `selectForUpdate`보다 앞이다. `point_history`에 중복 방지 유니크 제약이 없다 | 미판정 |
| 리뷰 수정 시 `http` URL은 무시하고, 새 object key가 있을 때만 교체 | `src/main/java/com/review/shop/service/review/ProductReviewService.java`의 `filterObjectKeys`, `updateReview` | 필터 커밋 `63521f66`. 현재 코드 `c95dd83d` | `images`가 비어 있음, `http` URL만, `reviews/...` key, 본문이 비어 예외. 교체 1건의 MinIO 객체 | 미측정 | 수정 API에 이미지 제거 분기가 없다 | 미판정 |
| `Client → Nginx → Spring Boot → Oracle / MinIO`를 EC2 Compose로 운영. GitHub Actions self-hosted runner와 Secrets. 이미지에 `.env` 없음 | `infra/docker-compose.yml`. `View/nginx/default.conf`. `View/Dockerfile`. `.github/workflows/deploy.yml`. 루트 `Dockerfile` | 현재 코드 `c95dd83d`. workflow 파일은 `c84fbc3a`에서 빈 줄만 다름 | 서비스 4개, `/api` 프록시, `self-hosted`, Secrets, 루트 Dockerfile의 `.env` COPY 유무. Actions는 최근 실행의 상태와 run id. EC2 재배포 없음 | 아래 C2. 최근 실행은 `cancelled`, run id `25278122824` | 운영 배포 성공률은 이 파일 대조로 말하지 않는다 | 미판정 |
| 소개 문장의 검색·추천·이미지·포인트·배포 | 검색 `src/main/java/com/review/shop/service/search/HeaderSearchService.java`, `src/main/resources/mapper/search/header/HeaderSearchProductMapper.xml`, `HeaderSearchReviewMapper.xml`. 추천·이미지·포인트·배포는 위 행의 경로 | 현재 코드 `c95dd83d` | 상품명 키워드, 브랜드 필터, 삭제 상품 제외, 기본 정렬, 정렬 하나. 리뷰 검색 키워드 1건과 10회 시간 | 미측정 | 포트폴리오의 “2글자 이상”은 이 표의 조건에 넣지 않았다. A3에서 `HeaderSearchService`를 다시 본다 | 미판정 |

측정 항목 중 이력서 문장 행에 결과가 나뉘어 들어가는 경로는 다음과 같다.

| 항목 | 파일 경로 | 커밋 해시 | 조건 | 결과 | 한계 | 반영 판정 |
| --- | --- | --- | --- | --- | --- | --- |
| C1 MinIO | `src/main/java/com/review/shop/image/ImageService.java`. `src/main/java/com/review/shop/image/minio/MinioConfig.java` | `c95dd83d` | 파일 1건. object key, PUT URL, DB의 key, GET URL, 코드의 만료 10분·5분, 객체 존재 | 미측정 | `main`의 MinIO 클라이언트는 `MINIO_URL` 하나다 | 미판정 |
| C3 스케줄러 | `src/main/java/org/spring/project/re_view/ReViewApplication.java`의 `@EnableScheduling`. `src/main/java/com/review/shop/util/ReviewScheduler.java`. `ProductreviewMapper.xml`의 `selectBestReviewIds` | `c95dd83d` | cron `0 0 0 1 * *`, zone `Asia/Seoul`, 좋아요 20 초과, 상품 리뷰 수 30 초과, 상위 10%. `runUpdateBestReviewsNow()` 2회 | 미측정 | 운영 로그가 없으면 정기 실행이라고 쓰지 않는다. 동시 중복은 A1 결과를 인용한다 | 미판정 |
| B3 Security | `src/main/java/com/review/shop/config/SecurityConfig.java` 112행 `anyRequest().permitAll()` | `c95dd83d` | 공개 GET 1개, 인증 POST 1개의 401, `/api/admin/**` 1개의 403, matcher 밖 1개, Validation 1개 | 미측정 | 전체 보호율은 계산하지 않는다 | 미판정 |

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

측정 Oracle은 `measure/docker-compose.yml`의 `oracle-db` 하나다. 이미지는 `infra/docker-compose.yml`과 같은 `gvenzl/oracle-xe:latest`다. BE·FE는 올리지 않는다. `infra/.env`가 없고, 그 compose의 데이터 볼륨은 `~/volumes/oracle-data`라서 제품 compose 파일은 수정하지 않고 측정 compose를 따로 둔다.

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
