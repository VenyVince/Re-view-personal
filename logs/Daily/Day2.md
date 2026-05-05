# Day 2 - Docker Environment Variable Cleanup

확인 일시: 2026-05-05

## 목표

Day 2의 우선 목표는 Docker 이미지에 `.env`가 포함되지 않도록 정리하고, Docker Compose 실행에 필요한 환경 변수 예시 파일을 준비하는 것이다.

- 백엔드 Dockerfile에서 `.env` 복사 구문을 제거한다.
- Docker Compose는 런타임 환경 변수 주입 구조를 유지한다.
- `infra/.env.example`에 필요한 환경 변수 키를 민감 정보 없이 정리한다.
- 로컬 실행용 값과 Docker Compose 실행용 값을 구분해서 기록한다.

## 작업 체크리스트

- [x] 백엔드 `Dockerfile`에서 `.env` 복사 구문을 확인했다.
- [x] 백엔드 `Dockerfile`에서 `COPY infra/.env .env`를 제거했다.
- [x] Docker 이미지가 `.env` 복사에 의존하지 않는 구조인지 확인했다.
- [x] `infra/.env.example` 파일을 추가했다.
- [x] `infra/.env.example`에 실제 비밀번호 없이 placeholder 값을 기록했다.
- [x] Docker Compose 실행에 필요한 DB 환경 변수 키를 정리했다.
- [x] Docker Compose 실행에 필요한 MinIO 환경 변수 키를 정리했다.
- [x] CORS와 프론트 API base URL 환경 변수 키를 정리했다.
- [x] 로컬 Maven/IntelliJ 실행용 환경 변수 예시를 Docker Compose 값과 분리했다.
- [x] Docker Compose의 `env_file` 런타임 주입 구조를 유지했다.
- [x] MinIO 내부 URL과 공개 URL 분리 작업을 후속 작업으로 분류했다.
- [x] 이번 범위에 포함할 작업과 제외할 작업을 기록했다.

## 요약

| 항목 | 결과 | 메모 |
| --- | --- | --- |
| 백엔드 Dockerfile `.env` 복사 제거 | 완료 | `COPY infra/.env .env` 제거 확인 |
| `infra/.env.example` 추가 | 완료 | 실제 비밀번호 없이 placeholder 값 사용 |
| 로컬/Docker 환경 변수 구분 | 반영 | Docker Compose 기준 값과 로컬 개발용 예시를 분리 |
| Docker Compose 런타임 env 주입 | 유지 | `infra/docker-compose.yml`의 `env_file: .env` 구조 유지 |
| MinIO URL 분리 | 후속 작업 | 2주 이후 계획으로 이동 |

## 1. Dockerfile 정리

### 1.1 확인 대상

파일:

```text
Dockerfile
```

### 1.2 변경 내용

기존 백엔드 Dockerfile에 있던 `.env` 복사 구문을 제거했다.

```dockerfile
COPY infra/.env .env
```

현재 백엔드 Dockerfile은 빌드된 jar만 런타임 이미지에 복사한다.

### 1.3 결과

- Docker 이미지가 `infra/.env` 파일 복사에 의존하지 않는다.
- 민감 정보가 이미지 레이어에 포함될 가능성을 줄였다.
- 런타임 환경 변수는 Docker Compose의 `env_file`을 통해 외부에서 주입하는 구조를 유지한다.

## 2. 환경 변수 예시 파일 정리

### 2.1 확인 대상

파일:

```text
infra/.env.example
```

### 2.2 변경 내용

Docker Compose 실행에 필요한 환경 변수 예시를 추가했다.

주요 키:

```env
SPRING_DATASOURCE_URL=jdbc:oracle:thin:@//{db_service_name}:1521/XEPDB1
SPRING_DATASOURCE_USERNAME=oracle_username
SPRING_DATASOURCE_PASSWORD=oracle_password

MINIO_URL=http://{minio_service_name}:9000
MINIO_PUBLIC_URL=http://localhost:9000

MINIO_ROOT_USER=minio_admin
MINIO_ROOT_PASSWORD=minio_password
MINIO_BUCKET=minio_bucket

CORS_ALLOWED_ORIGINS=http://localhost:3000
REACT_APP_API_BASE_URL=http://localhost:8080
```

실제 비밀번호나 운영 키는 넣지 않고 placeholder만 기록했다.

### 2.3 로컬 실행용 예시

로컬 Maven/IntelliJ 실행은 Docker Compose 서비스명을 사용할 수 없으므로, 로컬 실행용 값은 주석으로 분리했다.

```env
# SPRING_DATASOURCE_URL=jdbc:oracle:thin:@//localhost:1521/XEPDB1
# MINIO_URL=http://localhost:9000
# MINIO_PUBLIC_URL=http://localhost:9000
```

### 2.4 결과와 주의사항

- Docker Compose 실행 시 `{db_service_name}`은 실제 Compose DB 서비스명으로 바꿔야 한다.
- Docker Compose 실행 시 `{minio_service_name}`은 실제 Compose MinIO 서비스명으로 바꿔야 한다.
- 배포 전에는 `CORS_ALLOWED_ORIGINS`, `REACT_APP_API_BASE_URL`, `MINIO_PUBLIC_URL`을 공개 주소 기준으로 바꿔야 한다.
- `infra/.env.example`은 예시 파일이며, 실제 실행 값은 `infra/.env`에 둔다.
- 실제 비밀번호, MinIO root password, 운영 주소는 `.env.example`에 기록하지 않는다.

## 3. MinIO 관련 결정

MinIO 내부 URL과 공개 URL을 코드에서 분리하는 작업은 이번 커밋 범위에서 제외한다.

이유:

- 현재 프로젝트의 실제 이미지 흐름은 백엔드 내부 MinIO 작업보다 presigned URL 발급 중심이다.
- 내부용 MinIO client와 공개 URL 발급용 client 분리는 별도 브랜치에서 검토하는 편이 변경 범위가 명확하다.
- Docker Compose 환경에서 `MINIO_PUBLIC_URL`은 브라우저뿐 아니라 백엔드 컨테이너도 접근 가능한 주소여야 하므로 추가 검증이 필요하다.

추후 검토할 내용:

- MinIO bucket 존재 확인 같은 작은 내부 검증 작업 추가 여부
- 업로드 파일 검증, 이미지 리사이징, 바이러스 검사 같은 내부 보강 작업 추가 여부
- `MINIO_URL` / `MINIO_PUBLIC_URL` 분리 코드 유지 여부
- Docker Compose 환경에서 presigned URL host 검증

## 4. 검증

### 4.1 파일 확인

확인 결과:

- 백엔드 Dockerfile에서 `.env` 복사 구문이 제거되어 있다.
- `infra/.env.example`이 존재한다.
- `infra/docker-compose.yml`은 계속 `env_file: .env` 방식으로 런타임 환경 변수를 주입한다.

### 4.2 이번 범위에서 제외한 검증

Docker Compose 전체 재기동과 presigned URL host 검증은 이번 커밋 범위에서 제외한다.

이유:

- MinIO 공개 URL 분리와 presigned URL 발급 구조는 2주 이후 계획으로 이동했다.
- 현재 커밋의 목적은 Docker 이미지에 `.env`를 포함하지 않고, 필요한 환경 변수 예시를 정리하는 것이다.

## 5. 남은 확인 항목

- Docker Compose에서 실제 `.env` 값을 기준으로 백엔드, 프론트엔드, Oracle XE, MinIO 기동 확인
- `infra/.env.example`의 placeholder를 실제 로컬 Docker 값으로 복사해 사용할 때 오류가 없는지 확인
- MinIO URL 분리 관련 코드는 별도 브랜치에서 재검토
