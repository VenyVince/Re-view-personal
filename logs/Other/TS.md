# Backend Troubleshooting Topics

작성 기준: Git 커밋 작성자가 `VenyVince` 또는 `Veny`인 커밋 중, 백엔드 개발자 이력서에 문제 해결 경험으로 정리할 수 있는 항목만 선별했다.

참고 커밋:

- `b2900958` Docs: Day 3 API Authentication/Authorization Policy Classification Summary
- `49be85a5` chore: MinIO endpoint separation is deferred because there are no internal processing tasks (such as virus scanning or image resizing) in the current setup.
- `458f821e` chore: Delete 'copy env file' on Docker Images. Add .env.example.
- `86db4fcd` merge&fix:Update MinIO credentials after MinIO upgrade
- `788998e3` chore: transactional 처리(동시성 해결 및 row Lock)
- `36536c3d` fix: admin 및 security_util exception관련 처리
- `cb67fe36` QnA -> Qna error resolve
- `c9e022ff` feat/chore: ProductPage Review Search 구현, SecurityConfig 임시 허용 기록

## 1. 포인트 적립/사용 동시성 정합성 문제

### 트러블슈팅 명

포인트 적립/차감 로직의 동시성 문제 해결 및 Row Lock 적용

### 트러블슈팅 원인

기존 포인트 갱신은 포인트 이력 저장과 사용자 총 포인트 갱신이 분리되어 있었고, 동시에 여러 요청이 들어올 경우 같은 사용자 포인트를 기준으로 중복 계산될 가능성이 있었다. 또한 포인트 차감 시 잔액 부족 검증이 트랜잭션 경계 안에서 명확히 보장되지 않았다.

### 트러블슈팅 이유

포인트는 주문, 리뷰 작성, 리뷰 삭제, 운영자 선정 등 여러 기능에서 함께 사용되는 금액성 데이터다. 동시 요청 상황에서 포인트가 음수가 되거나 누락 갱신이 발생하면 사용자 신뢰와 주문 정합성에 직접 영향을 줄 수 있어 서버 단에서 일관성을 보장해야 했다.

### 트러블슈팅 해결 과정

- `PointService`의 포인트 변경 메서드에 `@Transactional`을 적용했다.
- MyBatis Mapper에 `SELECT ... FOR UPDATE` 조회를 추가해 사용자 포인트 행을 잠근 뒤 계산하도록 변경했다.
- 기존 `point = point + amount` 방식 대신 잠금 상태에서 현재 포인트를 조회하고, 적립/사용 타입에 따라 새 포인트를 계산한 뒤 명시적으로 업데이트했다.
- 차감 후 포인트가 음수가 되는 경우 `WrongRequestException`을 발생시켜 잘못된 요청으로 차단했다.

### 트러블슈팅 해결된 결과

동일 사용자에 대한 포인트 변경 요청이 트랜잭션 안에서 순차 처리되도록 정리되었고, 포인트 부족 상태의 사용 요청을 서버에서 방어할 수 있게 되었다. 리뷰 보상, 운영자 선정 보상, 구매 시 포인트 사용 같은 흐름에서 데이터 정합성을 설명할 수 있는 개선 포인트가 생겼다.

## 2. MinIO Presigned URL 내부/공개 주소 분리 문제

### 트러블슈팅 명

Docker 환경에서 MinIO Presigned URL의 내부 주소와 브라우저 공개 주소 분리

### 트러블슈팅 원인

Docker Compose 내부에서 백엔드는 `minio:9000` 같은 서비스명으로 MinIO에 접근할 수 있지만, 브라우저는 Docker 내부 DNS 이름을 해석할 수 없다. 반대로 `localhost:9000`을 기준으로만 설정하면 컨테이너 내부 백엔드가 MinIO에 접근하지 못할 수 있다. Presigned URL은 host를 포함해 서명되므로 발급 후 문자열 치환으로 해결할 수도 없다.

### 트러블슈팅 이유

이미지 업로드/조회가 MinIO Presigned URL 방식으로 동작하기 때문에, URL 서명 기준과 실제 클라이언트 접근 주소가 맞지 않으면 이미지 업로드 또는 표시가 실패한다. 백엔드 API는 정상이어도 사용자 화면에서는 이미지가 깨지는 문제가 발생할 수 있었다.

### 트러블슈팅 해결 과정

- `MinioProperties`에 `minio.url`과 별도로 `minio.public-url` 설정을 추가했다.
- `MinioConfig`에서 내부 접근용 `MinioClient`와 공개 URL 발급용 `publicMinioClient`를 분리했다.
- `ImageService`의 Presigned PUT/GET URL 발급 로직이 공개 endpoint 기준 client를 사용하도록 변경했다.
- `application.properties`에서 `MINIO_PUBLIC_URL`이 없으면 `MINIO_URL`을 fallback으로 쓰도록 구성했다.
- `infra/.env.example`에 `MINIO_URL`은 백엔드 내부 접근 주소, `MINIO_PUBLIC_URL`은 브라우저 접근 주소로 구분해 문서화했다.

### 트러블슈팅 해결된 결과

Docker 내부 네트워크 접근과 브라우저 공개 접근 주소를 분리해 Presigned URL 서명 불일치 문제를 피할 수 있게 되었다. 로컬/Docker/배포 환경별 MinIO 주소를 환경 변수로 분리할 수 있어 이미지 업로드와 조회 흐름의 환경 의존성이 줄었다.

## 3. Docker 이미지에 민감 환경 파일이 포함되는 문제

### 트러블슈팅 명

Dockerfile의 `.env` 이미지 복사 제거 및 런타임 환경 변수 주입 구조 정리

### 트러블슈팅 원인

백엔드 Dockerfile에 `COPY infra/.env .env` 구문이 있어 실제 환경 변수 파일이 이미지 레이어에 포함될 수 있는 구조였다. 이 방식은 DB 계정, MinIO 계정 같은 민감 정보가 이미지에 남을 위험이 있고, 환경별 설정 변경도 이미지 재빌드에 묶일 수 있다.

### 트러블슈팅 이유

백엔드 서버는 DB, MinIO, CORS 등 환경에 따라 달라지는 설정이 많다. 이 값을 이미지 안에 넣으면 보안 리스크뿐 아니라 로컬, Docker Compose, 배포 환경 전환 시 운영성이 떨어진다.

### 트러블슈팅 해결 과정

- 백엔드 `Dockerfile`에서 `.env` 복사 구문을 제거했다.
- 실제 값 대신 placeholder만 포함한 `infra/.env.example`을 추가했다.
- Docker Compose는 `env_file` 또는 environment 기반 런타임 주입 구조를 유지하도록 정리했다.
- DB, MinIO 내부 주소, MinIO 공개 주소, CORS, 프론트 API base URL을 예시 파일에 구분해 기록했다.

### 트러블슈팅 해결된 결과

Docker 이미지가 민감 환경 파일에 의존하지 않게 되었고, 환경별 설정은 컨테이너 실행 시 외부에서 주입하는 구조로 분리되었다. 이력서에서는 “컨테이너 이미지 보안과 환경 변수 운영 방식 개선” 경험으로 정리할 수 있다.

## 4. MinIO 인증 키 설정 변경으로 인한 연결 실패

### 트러블슈팅 명

MinIO 업그레이드 이후 인증 프로퍼티 불일치 해결

### 트러블슈팅 원인

MinIO 설정이 기존 `access-key`, `secret-key` 형태와 `root-user`, `root-password` 형태로 혼재되어 있었다. Spring Boot `ConfigurationProperties`, `application.properties`, Docker Compose 환경 변수명이 서로 맞지 않으면 MinIO Client 생성 시 인증 정보가 비어 있거나 잘못 바인딩될 수 있었다.

### 트러블슈팅 이유

이미지 업로드와 Presigned URL 발급은 MinIO Client 생성에 의존한다. 인증 키 바인딩이 깨지면 서버 실행 후 이미지 기능 전체가 실패할 수 있으므로 설정 키를 한 기준으로 통일해야 했다.

### 트러블슈팅 해결 과정

- `MinioProperties` 필드를 `rootUser`, `rootPassword` 기준으로 변경했다.
- `MinioConfig`에서 MinIO Client credentials를 `getRootUser()`, `getRootPassword()`로 주입하도록 수정했다.
- `application.properties`를 `minio.root-user`, `minio.root-password` 기준으로 변경했다.
- Docker Compose의 MinIO 관련 환경 변수도 `MINIO_ROOT_USER`, `MINIO_ROOT_PASSWORD` 기준으로 맞췄다.

### 트러블슈팅 해결된 결과

MinIO 설정 키가 코드, properties, Docker Compose 사이에서 같은 기준으로 정리되었다. 이미지 업로드/조회 기능이 환경 변수명 불일치 때문에 실패할 가능성을 줄였다.

## 5. 인증/인가 정책과 실제 API 경로 불일치 문제

### 트러블슈팅 명

Spring Security matcher와 실제 API 권한 정책 불일치 분석

### 트러블슈팅 원인

기존 SecurityConfig는 `/api/admin/**`와 `/api/mypage/**` 중심으로 보호하고 나머지는 `anyRequest().permitAll()`에 가까운 구조였다. 하지만 실제 로그인 필요 API는 장바구니, 주문, 주소, 위시리스트, 포인트, 리뷰 작성/수정/삭제, QnA 작성/수정/삭제처럼 `/api/mypage/**` 외부에 넓게 분포해 있었다.

### 트러블슈팅 이유

보호되어야 할 쓰기 API가 공개되면 인증 없이 데이터 변경 요청이 가능해질 수 있다. 백엔드 개발자 관점에서는 컨트롤러 실제 경로, 프론트 호출 경로, SecurityConfig matcher를 함께 맞춰야 보안 정책이 동작한다.

### 트러블슈팅 해결 과정

- 백엔드 컨트롤러 endpoint와 프론트 호출 API를 수집했다.
- API를 공개 API, 로그인 필요 API, 관리자 API로 분류했다.
- 같은 path라도 method에 따라 공개/보호가 달라지는 리뷰 API 등을 따로 구분했다.
- 세션 인증 유지 기준에서 프론트의 Bearer token 전송 코드가 정책과 충돌할 수 있음을 기록했다.
- Day 4 적용을 위한 method/path 기반 SecurityConfig matcher 초안을 작성했다.

### 트러블슈팅 해결된 결과

보호 대상 API를 실제 구현 기준으로 재분류했고, SecurityConfig를 구체적으로 개선할 수 있는 기준표가 만들어졌다. 이력서에서는 “컨트롤러-프론트 호출-Spring Security 정책을 대조해 인증/인가 누락 위험을 식별”한 경험으로 정리할 수 있다.

## 6. 관리자/공통 예외 처리의 상태 코드와 예외 타입 혼선

### 트러블슈팅 명

관리자 API와 Security Util의 예외 타입 정리 및 전역 예외 처리 개선

### 트러블슈팅 원인

인증 정보가 없거나 사용자 조회 결과가 없을 때 일반 `RuntimeException` 또는 잘못된 예외 타입이 사용되고 있었다. 또한 리소스 없음, 잘못된 요청, DB 오류가 구분되지 않거나 DB 오류가 400 응답으로 내려가는 등 API 오류 의미가 섞여 있었다.

### 트러블슈팅 이유

프론트와 API 클라이언트는 상태 코드와 응답 메시지를 기준으로 로그인 필요, 잘못된 입력, 데이터 없음, 서버 오류를 구분한다. 예외 의미가 섞이면 사용자 안내와 디버깅이 어려워지고, 운영 중 장애 원인도 파악하기 힘들다.

### 트러블슈팅 해결 과정

- `Security_Util`에서 인증 정보 없음은 `WrongRequestException`, 사용자 없음은 `ResourceNotFoundException`으로 분리했다.
- 오타가 있던 `ResourceNotFountException`을 `ResourceNotFoundException`으로 정리했다.
- `ExceptionHandlers`에서 리소스 없음은 404, 잘못된 요청은 400, DB 오류는 500으로 응답하도록 구분했다.
- 관리자 서비스에서 상품, 주문, QnA, 리뷰 작업의 입력값 누락과 대상 없음 상황을 각각 다른 예외로 처리했다.

### 트러블슈팅 해결된 결과

관리자 API와 공통 인증 유틸의 예외 의미가 더 명확해졌고, 클라이언트가 오류 원인을 상태 코드 기준으로 구분할 수 있는 기반이 마련되었다. 서버 로그와 API 응답의 해석 가능성도 좋아졌다.

## 7. DTO/Mapper 클래스명 대소문자 불일치 문제

### 트러블슈팅 명

QnA DTO 명명 불일치로 인한 MyBatis/Spring 참조 오류 해결

### 트러블슈팅 원인

`QnADTO`와 `QnaDTO`처럼 약어 대소문자가 섞여 컨트롤러, 서비스, Mapper 인터페이스, Mapper XML의 resultType 참조가 서로 달라질 수 있는 상태였다. 특히 파일 시스템이나 빌드 환경에 따라 대소문자 불일치는 컴파일 또는 런타임 매핑 오류로 이어질 수 있다.

### 트러블슈팅 이유

MyBatis XML의 `resultType`은 전체 클래스 경로에 의존한다. DTO 파일명과 클래스명, XML 참조가 맞지 않으면 관리자 QnA 상세 조회 같은 API가 실행 시점에 실패할 수 있어 명명 규칙을 통일해야 했다.

### 트러블슈팅 해결 과정

- DTO 파일과 클래스명을 `QnaDTO`로 통일했다.
- 관리자 컨트롤러 Swagger schema 참조를 `QnaDTO.class`로 변경했다.
- 관리자 서비스와 Mapper 인터페이스의 반환 타입을 `QnaDTO`로 맞췄다.
- MyBatis XML의 `resultType`을 `com.review.shop.dto.qna.QnaDTO`로 수정했다.

### 트러블슈팅 해결된 결과

QnA 상세 조회 흐름에서 DTO 클래스 참조가 일관되게 정리되었다. MyBatis result mapping과 Java 타입 참조가 같은 이름을 바라보게 되어 대소문자/명명 불일치로 인한 오류 가능성이 줄었다.

## 8. 상품 리뷰 검색 구현 중 임시 보안 허용 상태 식별

### 트러블슈팅 명

상품 상세 리뷰 검색 기능 개발 중 SecurityConfig 임시 허용 상태 기록

### 트러블슈팅 원인

상품 상세 페이지의 리뷰 검색, 정렬, 필터링 기능을 구현하는 과정에서 비즈니스 로직과 API 확인을 위해 SecurityConfig가 임시로 모든 요청을 허용하는 상태가 커밋 메시지에 남아 있었다.

### 트러블슈팅 이유

개발 중 임시 보안 완화는 기능 확인에는 도움이 되지만, 그대로 유지되면 보호 API가 공개되는 문제가 생긴다. 특히 검색 API와 마이페이지/리뷰 관련 API가 섞여 있을 때 공개 가능 API와 로그인 필요 API를 명확히 나누어야 한다.

### 트러블슈팅 해결 과정

- 상품 리뷰 검색 컨트롤러, 서비스, Mapper, DTO를 추가하면서 API 동작 확인을 먼저 진행했다.
- 커밋 메시지에 SecurityConfig 임시 허용 상태와 추후 변경 필요성을 명시했다.
- 이후 API 인증/인가 정책 분류 문서에서 공개 API와 로그인 필요 API를 구분해 matcher 정리 대상으로 연결했다.

### 트러블슈팅 해결된 결과

검색 기능 구현 과정에서 발생한 임시 보안 완화 상태가 추적 가능한 기술 부채로 기록되었다. 후속 인증/인가 정책 정리 작업의 근거가 되었고, 이력서에서는 “기능 개발 중 생긴 임시 보안 설정을 식별하고 권한 정책 재정리 대상으로 관리”한 사례로 활용할 수 있다.
