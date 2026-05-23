# Re_View

리뷰 기반 개인 맞춤 스킨케어 추천 커머스 프로젝트

Re_View는 사용자 리뷰와 Baumann 피부 타입 데이터를 활용해 스킨케어 상품과 리뷰를 추천하는 풀스택 팀 프로젝트입니다. 단순 상품 조회 중심의 쇼핑몰이 아니라, 사용자 피부 타입, 상품 타깃 피부 타입, 리뷰 작성자의 피부 타입, 리뷰 반응 데이터를 함께 반영해 구매 판단에 도움이 되는 추천 흐름을 구현하는 데 초점을 두었습니다.

| 구분 | 링크 |
| --- | --- |
| 프로젝트 문서 | https://github.com/VenyVince/5.Re_View_Doc |
| 시연 영상 | https://www.youtube.com/watch?v=kP2HrcrGmvU |

## 목차

- [주요 기능](#주요-기능)
- [기술 스택](#기술-스택)
- [시스템 구조](#시스템-구조)
- [핵심 구현 내용](#핵심-구현-내용)
- [본인 기여도](#본인-기여도)
- [트러블슈팅](#트러블슈팅)
- [검증 결과](#검증-결과)
- [팀 구성 및 역할](#팀-구성-및-역할)

## 주요 기능

### 사용자

- 회원가입, 로그인, 로그아웃
- 상품 목록 및 상세 조회
- 리뷰 작성, 조회, 수정, 삭제
- 리뷰 좋아요, 싫어요, 댓글, 신고
- 장바구니, 위시리스트, 주문 처리
- 포인트 적립 및 사용
- 주소 및 결제수단 관리
- QnA 작성 및 조회
- Baumann 피부 타입 등록 및 수정
- 피부 타입 기반 상품 및 리뷰 추천

### 관리자

- 상품 등록, 수정, 삭제
- 주문 상태 관리
- 사용자 관리 및 밴 처리
- 사용자 포인트 관리
- 리뷰 관리 및 관리자 추천 리뷰 선정
- 리뷰 신고 처리
- QnA 답변 관리

## 기술 스택

### Backend

- Java 21
- Spring Boot 3.5
- Spring Security
- MyBatis
- Oracle XE
- Spring Mail
- springdoc OpenAPI / Swagger
- MinIO SDK
- Lombok

### Frontend

- React
- React Router
- Axios
- Styled Components
- React Icons

### Infra

- Docker
- Docker Compose
- Nginx
- Oracle XE
- MinIO

## 시스템 구조

<이미지 삽입 예정 - AWS EC2배포시 이미지 삽입 예정>

### 백엔드 패키지 구조

```text
src/main/java/com/review/shop
├── config          # Security, CORS, 예외 처리 설정
├── controller      # REST API Controller
├── service         # 비즈니스 로직
├── repository      # MyBatis Mapper Interface
├── dto             # 요청 / 응답 DTO
├── image           # MinIO 이미지 처리
├── exception       # 커스텀 예외
└── util            # 인증 유틸, 스케줄러
```

## 핵심 구현 내용

### Baumann 피부 타입 기반 추천

Baumann 피부 타입의 4가지 요소를 기준으로 사용자와 상품, 리뷰의 적합도를 계산했습니다. 상품 추천은 상품 피부 타입 적합도와 리뷰 수 기반 점수를 합산한 `total_score`를 사용하고, 리뷰 추천은 리뷰 작성자의 피부 타입 일치도와 리뷰 반응 데이터를 반영한 `total_score`를 기준으로 정렬했습니다.

### MinIO Presigned URL 기반 이미지 처리

백엔드가 파일 바이너리를 직접 저장하지 않고 MinIO Presigned URL을 발급하는 구조로 구현했습니다. 프론트엔드는 발급받은 URL로 이미지를 업로드하고, 백엔드는 object key를 DB에 저장해 조회 URL 생성 책임을 분리했습니다.

### 주문 / 포인트 / 재고 트랜잭션 처리

주문 처리 과정에서 사용자 포인트 차감, 상품 재고 차감, 주문 정보 저장, 주문 상세 정보 저장을 하나의 트랜잭션으로 처리했습니다. 클라이언트가 전달한 가격을 그대로 신뢰하지 않고 서버에서 상품 가격을 다시 조회해 최종 금액을 계산하도록 했습니다.

### 리뷰 보상 시스템

구매 상품 리뷰 작성 시 포인트를 지급하고, 리뷰 삭제 시 지급된 포인트를 회수하도록 구현했습니다. 베스트 리뷰는 매월 1일 00:00(서울 기준)에 자동 갱신되며, 동일 리뷰에 보상이 중복 지급되지 않도록 포인트 이력을 기준으로 확인합니다.


## 본인 기여도

| 영역 | 기여 내용 |
| --- | --- |
| 백엔드/인프라 안정화 | 로컬 실행 환경을 점검하고 DB, MinIO, CORS, 환경 변수 문제를 코드 문제와 분리해 정리했습니다. |
| 보안 정책 정리 | 세션 인증 방식을 유지하면서 공개 API, 인증 필요 API, 관리자 API를 분류하고 `SecurityConfig` matcher에 반영했습니다. |
| 예외 응답 표준화 | 인증 실패 401, 권한 부족 403을 분리하고 `ErrorResponseDTO` 기반 공통 JSON 응답으로 정리했습니다. |
| 이미지 처리 | MinIO presigned URL 기반 업로드/조회 흐름을 유지하면서 object key와 조회용 URL의 책임을 분리했습니다. |
| 추천/리뷰 보상 검증 | Baumann 기반 추천 점수와 리뷰 작성/삭제/베스트/관리자 선정 보상 흐름을 코드 기준으로 확인했습니다. |

## 트러블슈팅

| 문제 | 원인 | 해결 |
| --- | --- | --- |
| MinIO presigned URL이 브라우저에서 열리지 않음 | 백엔드는 `minio:9000`으로 접근하지만 브라우저는 Docker 내부 DNS 이름을 해석하지 못함 | 내부 접속용 URL과 브라우저 접근용 URL을 분리 |
| 보호 API 인증 실패와 권한 부족이 구분되지 않음 | Spring Security entry point와 access denied handler 응답 기준이 명확하지 않음 | 미로그인 접근은 401, 권한 부족은 403과 `ErrorResponseDTO`로 반환하도록 정리 |
| 리뷰 수정 시 기존 이미지가 사라질 수 있음 | 기존 이미지는 조회 URL, 새 이미지는 object key로 전달되는데 모두 새 저장 값처럼 처리됨 | `http`로 시작하는 조회용 URL은 저장 대상에서 제외하고 새 object key가 있을 때만 이미지 매핑을 교체 |
| 헤더 검색 리뷰가 이미지 수만큼 중복될 수 있음 | `review_images` 다중 조인으로 리뷰 row가 이미지 개수만큼 늘어남 | 검색 카드에는 대표 이미지 1장만 필요하므로 단일 이미지 조회로 응답 구조를 단순화 |

## 검증 결과

- 백엔드 테스트: `mvnw.cmd test` 통과
- 프론트 빌드: `npm --prefix View run build` 성공
- 남은 경고: Mockito 동적 Java agent 로딩 경고, SpringDoc 운영 환경 비활성화 권장 경고, React Hook dependency와 미사용 변수/import 중심의 ESLint 경고

## 주요 ERD

<이미지 삽입 예정>

## 팀 구성 및 역할

| 이름 | 역할 | 담당 영역 |
| --- | --- | --- |
| 김석현 | 팀장 / 백엔드 / 인프라 | DB 설계, 회원, 포인트, 결제수단, 장바구니, 이미지 처리, 추천 알고리즘, MinIO |
| 이재빈 | 백엔드 | 로그인, 결제, 구매, 추천 알고리즘 |
| 정윤성 | 백엔드 | 상품, 리뷰, QnA, 커뮤니티 |
| 오승환 | 프론트엔드 | 마이페이지 전체 UI |
| 김시연 | 프론트엔드 | 관리자 페이지, 설문조사 UI |
| 박진성 | 프론트엔드 | 검색, 상품, 리뷰 페이지 UI |

## 참고 문서

- [작업 계획](logs/Stable/Plan.md)
- [ERD 메모](logs/Other/ERD.md)
- [트러블슈팅 메모](logs/Other/TS.md)
