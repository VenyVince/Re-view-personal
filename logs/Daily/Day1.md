# Day 1 - Current Run Status Check

확인 일시: 2026-05-04

## 목표

Day 1의 목표는 코드를 수정하기보다 현재 실행 상태를 확인하고, 막히는 지점을 기록하는 것이다.

- 백엔드 로컬 실행 상태 확인
- 프론트엔드 로컬 실행 상태 확인
- Docker Compose 실행 상태 확인
- 환경 변수, DB, MinIO, CORS, 포트 문제를 코드 문제와 분리해서 기록

## 요약

| 항목 | 결과 | 메모 |
| --- | --- | --- |
| 백엔드 로컬 실행 | 가능 | 환경 변수 주입과 `JAVA_HOME` 설정 필요 |
| Oracle XE 연결 | 통과 | 로컬 백엔드에서 DB 조회 API 응답 확인 |
| MinIO 연결 | 통과 | 로컬 백엔드에서 presigned 이미지 URL 생성 확인 |
| 프론트엔드 로컬 실행 | 가능 | React dev server 실행 및 기본 화면 확인 |
| FE -> BE 연동 | 가능 | CORS origin 설정 보정 후 로그인/상품 조회 흐름 확인 |
| Docker Compose 실행 | 부분 통과 | 컨테이너 기동과 DB 조회 API는 통과, MinIO presigned URL 공개 주소 이슈 남음 |

## 1. 백엔드 로컬 실행 확인

### 1.1 Maven으로 Spring Boot 실행

실행 명령:

```powershell
.\mvnw.cmd spring-boot:run
```

처음 확인된 실패 원인:

- 시작 시 필요한 환경 변수가 주입되지 않음
- `JAVA_HOME`이 잘못된 경로를 가리키고 있었음
  - 잘못된 값: `C:\Program Files\Java\java21`
  - 실제 JDK 21 경로: `C:\Program Files\Java\jdk-21`

`JAVA_HOME`을 현재 PowerShell 프로세스에서 보정한 뒤 다시 실행:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21'
.\mvnw.cmd spring-boot:run
```

이후 확인된 실패 원인:

- `MINIO_URL`이 주입되지 않아 `${MINIO_URL}` 문자열이 그대로 MinIO endpoint로 들어감
- 주요 오류: `invalid hostname ${MINIO_URL}`

환경 변수를 주입한 뒤 실행 결과:

- Spring Boot 기동 성공
- `http://localhost:8080/swagger-ui/index.html` -> 200 OK
- `http://localhost:8080/v3/api-docs` -> 200 OK
- `http://localhost:8080/api/products` -> 200 OK
- `http://localhost:8080/api/reviews` -> 200 OK

### 1.2 백엔드 로컬 실행 환경 변수

로컬 Maven/IntelliJ 실행 기준:

```properties
spring.application.name=Re_View
spring.datasource.driver-class-name=oracle.jdbc.OracleDriver
spring.datasource.url=jdbc:oracle:thin:@//localhost:1521/XEPDB1
spring.datasource.username=REVIEW
spring.datasource.password=<local-password>

CORS_ALLOWED_ORIGINS=http://localhost:3000
cors.allowed-origins=${CORS_ALLOWED_ORIGINS:http://localhost:3000}

minio.url=http://localhost:9000
minio.root-user=review
minio.root-password=<local-password>
minio.bucket=review
```

주의:

- `infra/.env`는 Docker Compose의 `env_file`로 사용된다.
- Maven/IntelliJ 로컬 실행은 `infra/.env`를 자동으로 읽지 않는다.
- IntelliJ 실행 시 Run Configuration의 Environment variables에 직접 넣거나 별도 env file 로딩 설정이 필요하다.

### 1.3 Oracle XE, MinIO 연결 확인

결과:

- Oracle XE 연결: 통과
- MinIO 연결: 통과
- 상품 목록 API에서 DB 조회와 MinIO presigned URL 생성 확인
- 리뷰 목록 API에서 DB 조회 확인

분류:

- 현재 로컬 백엔드 실행 문제는 코드 문제가 아니라 환경 변수 주입과 로컬 JDK 설정 문제로 분류한다.

## 2. 프론트엔드 로컬 실행 확인

### 2.1 의존성 설치 여부

위치:

```powershell
cd View
```

결과:

- 의존성 설치 여부 확인: 통과

### 2.2 React dev server 실행

실행 명령:

```powershell
npm start
```

결과:

- React dev server 실행: 통과
- 화면 표시: 통과

### 2.3 백엔드 API 주소 설정

프론트 공통 axios 설정:

```javascript
baseURL: process.env.REACT_APP_API_BASE_URL || "http://localhost:8080"
```

확인된 이슈:

- 백엔드 CORS 허용 origin 설정이 비어 있거나 잘못되어 FE -> BE 요청이 막힐 수 있었음
- `CORS_ALLOWED_ORIGINS=http://localhost:3000`으로 보정 후 기본 흐름 확인

추가 확인 필요:

- `REACT_APP_API_BASE_URL=http://221.143.110.221:8080`은 Docker 프론트 빌드 인자로 사용되는 값이다.
- 로컬 Docker 검증에서는 브라우저 기준 주소인 `http://localhost:8080`이 더 적절하다.
- 배포 환경에서는 공인 IP 또는 도메인 기준 URL로 별도 관리해야 한다.

### 2.4 기본 흐름 확인

결과:

- 화면 표시: 정상
- 로그인 흐름: 정상 동작 확인
- 상품 조회 흐름: 정상 동작 확인

## 3. Docker Compose 실행 확인

### 3.1 Compose 파일 확인

파일:

```text
infra/docker-compose.yml
```

확인 결과:

- Oracle XE 서비스 있음: `oracle-db`
- MinIO 서비스 있음: `minio`
- 백엔드 서비스 있음: `be`
- 프론트엔드 서비스 있음: `fe`
- 프론트 컨테이너 내부에서 nginx 사용

### 3.2 Docker Compose 실행

확인 결과:

- `docker compose up` 실행 가능
- Docker Desktop 기준 컨테이너 기동 확인
- `.env` 파일 누락 없음
- 포트 충돌 없음

확인된 컨테이너:

| 컨테이너 | 상태 |
| --- | --- |
| `review-db` | 기동 |
| `review-minio` | 기동 |
| `review-be` | 기동 |
| `review-fe` | 기동 |

### 3.3 Docker 환경 DB 조회 API 확인

초기 증상:

```text
Error querying database
Cause: org.springframework.jdbc.CannotGetJdbcConnectionException: Failed to obtain JDBC Connection
The error may exist in class path resource [mapper/user/UserMapper.xml]
The error may involve com.review.shop.repository.user.UserMapper.findUserById
```

해석:

- `UserMapper.xml` 자체 문제가 아님.
- 쿼리 실행 시점에 백엔드 컨테이너가 JDBC Connection을 얻지 못한 상태.
- Spring Boot 애플리케이션 기동 로그가 정상이어도, 실제 DB 커넥션 풀은 첫 DB API 호출 시점에 생성될 수 있음.

확인된 원인:

```text
review-be 컨테이너 내부 SPRING_DATASOURCE_URL=jdbc:oracle:thin:@//localhost:1521/XEPDB1
```

Docker 컨테이너 내부에서 `localhost`는 호스트 PC가 아니라 `review-be` 컨테이너 자기 자신이다.

따라서 Docker Compose 환경에서는 백엔드 컨테이너가 Oracle XE에 접근할 때 compose 서비스명을 사용해야 한다.

Docker Compose 기준 DB URL:

```env
SPRING_DATASOURCE_URL=jdbc:oracle:thin:@//oracle-db:1521/XEPDB1
```

재확인 결과:

- Docker Compose 환경에서 DB 조회 API 정상 동작 확인
- Docker 환경의 DB 연결 문제는 `localhost` 대신 compose 서비스명 `oracle-db`를 사용해야 하는 환경 변수/네트워크 문제로 정리

분류:

- 코드 문제 아님
- DB 스키마 문제로 단정하지 않음
- Docker 환경 변수와 컨테이너 네트워크 주소 문제

### 3.4 Docker 환경 MinIO 이미지 접근 실패

확인 결과:

- 브라우저에서 `http://localhost:9000`으로 MinIO 접근은 정상
- 백엔드가 반환하는 presigned URL은 `http://minio:9000/...` 형태로 생성됨
- 브라우저는 Docker 내부 DNS 이름인 `minio`를 해석할 수 없으므로 이미지 다운로드/표시 실패
- presigned URL의 host를 수동으로 `minio:9000`에서 `localhost:9000`으로 바꾸면 MinIO가 서명 불일치로 거절함

해석:

- 백엔드 컨테이너가 MinIO에 접속하려면 내부 주소 `http://minio:9000`이 필요하다.
- 클라이언트 브라우저가 이미지에 접근하려면 공개 주소 `http://localhost:9000` 또는 배포용 공인 주소가 필요하다.
- 현재 `minio.url` 하나가 내부 접속 endpoint와 공개 이미지 URL 역할을 동시에 하면서 충돌이 발생한다.
- presigned URL은 host를 포함한 요청 정보 기준으로 서명되므로, 발급 후 URL 문자열의 host만 바꾸는 방식으로는 해결할 수 없다.
- 공개 접근 주소로 동작하는 presigned URL이 필요하면 처음부터 공개 endpoint 기준으로 서명 URL을 발급해야 한다.

분류:

- 코드 문제라기보다 Docker 환경 변수 설계 문제
- MinIO 내부 접속 URL과 클라이언트 공개 URL을 분리해야 하는 개선 안건

## 4. 환경 변수 정리

### 4.1 로컬 Maven/IntelliJ 실행 기준

```env
SPRING_DATASOURCE_URL=jdbc:oracle:thin:@//localhost:1521/XEPDB1
SPRING_DATASOURCE_USERNAME=REVIEW
SPRING_DATASOURCE_PASSWORD=<local-password>

MINIO_URL=http://localhost:9000
MINIO_ROOT_USER=review
MINIO_ROOT_PASSWORD=<local-password>
MINIO_BUCKET=review

CORS_ALLOWED_ORIGINS=http://localhost:3000
REACT_APP_API_BASE_URL=http://localhost:8080
```

### 4.2 Docker Compose 실행 기준

```env
SPRING_DATASOURCE_URL=jdbc:oracle:thin:@//oracle-db:1521/XEPDB1
SPRING_DATASOURCE_USERNAME=REVIEW
SPRING_DATASOURCE_PASSWORD=<docker-password>

MINIO_URL=http://minio:9000
MINIO_ROOT_USER=review
MINIO_ROOT_PASSWORD=<docker-password>
MINIO_BUCKET=review

CORS_ALLOWED_ORIGINS=http://localhost:3000
REACT_APP_API_BASE_URL=http://localhost:8080
```

Caution:

- `SPRING_DATASOURCE_URL`과 `MINIO_URL`은 백엔드 컨테이너 기준의 주소.
- `REACT_APP_API_BASE_URL`은 브라우저 기준의 주소.
- 로컬 Docker 확인에서는 브라우저가 호스트에서 백엔드에 접근하므로 `http://localhost:8080`을 사용.

## 5. 추가 안건 - MinIO 내부 URL과 공개 URL 분리

### 5.1 현재 문제

현재 백엔드는 `minio.url` 하나를 두 용도로 사용한다.

1. 백엔드가 MinIO 서버에 접속하는 endpoint
2. 클라이언트에게 반환하는 presigned/image URL의 host

Docker Compose 환경에서는 이 두 주소가 다르다.

```text
백엔드 컨테이너 내부 접속 주소: http://minio:9000
브라우저 공개 접근 주소: http://localhost:9000 또는 배포 서버 주소
```

단일 `MINIO_URL`만 쓰면 둘 중 하나가 깨진다.

```text
MINIO_URL=http://localhost:9000
-> 브라우저 이미지 접근은 가능
-> review-be 컨테이너가 MinIO 접속 실패

MINIO_URL=http://minio:9000
-> review-be 컨테이너가 MinIO 접속 가능
-> 브라우저가 http://minio:9000 이미지 URL 접근 불가
```

### 5.2 개선 방향

환경 변수를 두 개로 분리한다.

```env
MINIO_URL=http://minio:9000
MINIO_PUBLIC_URL=http://localhost:9000
```

로컬 Maven/IntelliJ 실행:

```env
MINIO_URL=http://localhost:9000
MINIO_PUBLIC_URL=http://localhost:9000
```

로컬 Docker Compose 실행:

```env
MINIO_URL=http://minio:9000
MINIO_PUBLIC_URL=http://localhost:9000
```

배포 환경 예시:

```env
MINIO_URL=http://minio:9000
MINIO_PUBLIC_URL=http://221.143.110.221:9000
```

도메인/HTTPS 적용 시:

```env
MINIO_URL=http://minio:9000
MINIO_PUBLIC_URL=https://image.example.com
```

### 5.3 코드 수정 방향

`MinioProperties`에 공개 URL 필드를 추가한다.

```java
private String url;
private String publicUrl;
private String rootUser;
private String rootPassword;
private String bucket;
```

`application.properties` 예시:

```properties
minio.url=${MINIO_URL}
minio.public-url=${MINIO_PUBLIC_URL:${MINIO_URL}}
minio.root-user=${MINIO_ROOT_USER}
minio.root-password=${MINIO_ROOT_PASSWORD}
minio.bucket=${MINIO_BUCKET}
```

역할 분리:

```text
MinioClient 생성: minio.url 사용
클라이언트에게 반환하는 이미지 URL/presigned URL: minio.public-url 기준 사용
```

Caution:

- Presigned URL은 MinIO SDK가 endpoint 기준으로 서명 URL을 발급한다.
- 발급된 presigned URL에서 `minio:9000`을 `localhost:9000`으로 단순 치환하면 서명 불일치로 거절된다.
- 따라서 단순 문자열 replace는 해결책이 아니다.
- 해결하려면 public endpoint 기준으로 presigned URL을 발급하거나, 클라이언트가 접근 가능한 reverse proxy/도메인을 MinIO endpoint로 사용해야 한다.
- Day1에서는 수정하지 않고 안건으로 기록한다.

## 6. 문제 구분

| 문제 | 분류 | 내용 |
| --- | --- | --- |
| `JAVA_HOME` 오류 | 로컬 환경 문제 | Maven Wrapper가 잘못된 JDK 경로를 봄 |
| Maven/IntelliJ 실행 시 env 미주입 | 환경 변수 문제 | `infra/.env`는 자동 로드되지 않음 |
| CORS origin 오류 | 환경 변수 문제 | FE origin과 BE 허용 origin 불일치 |
| Docker DB 조회 API 초기 실패 | Docker 환경 변수/네트워크 문제 | BE 컨테이너 내부에서 `localhost`로 Oracle 접근 시도, `oracle-db` 사용 후 정상 |
| MinIO URL 단일화 | 환경 변수 설계 문제 | 내부 접속 URL과 공개 URL 역할 충돌 |

## 7. Day 1 남은 확인 항목

- Docker Compose 환경의 MinIO presigned URL 실패를 Day2 이후 개선 항목으로 넘길지 확정
- `MINIO_PUBLIC_URL` 분리 작업을 Day2 환경 변수 정리 또는 별도 MinIO 개선 작업으로 배치
- `REACT_APP_API_BASE_URL`의 로컬 Docker 값과 배포 값을 분리해서 기록


