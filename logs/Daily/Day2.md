# Day 2 - Docker Environment Variable Cleanup Draft

## 목표

Day 2의 목표는 Docker 환경 변수 구조를 정리하고, MinIO 내부 접속 URL과 클라이언트 공개 URL을 분리하는 것이다.

- `.env`가 Docker 이미지에 포함되지 않도록 정리한다.
- Docker Compose 런타임 환경 변수 주입 구조를 유지한다.
- MinIO 내부 접속 URL과 공개 URL을 분리한다.
- presigned URL host 문자열 치환 방식은 사용하지 않는다.

## 배경

Day 1에서 확인한 결과:

- 백엔드 로컬 실행은 가능했다.
- 프론트엔드 로컬 실행도 가능했다.
- Docker Compose 컨테이너 기동은 가능했다.
- Docker Compose 환경에서 DB 조회 API는 정상 동작했다.
- MinIO는 브라우저에서 `http://localhost:9000`으로 접근 가능했지만, presigned URL은 `http://minio:9000/...` 형태로 생성되어 브라우저 접근이 막혔다.
- presigned URL의 host를 `localhost:9000`으로 바꿔도 서명 불일치로 거절되므로, 단순 문자열 치환으로는 해결할 수 없다.

## Day 2 작업 범위

### 1. Dockerfile 정리

- 백엔드 `Dockerfile`에서 `COPY infra/.env .env`가 있으면 제거한다.
- Docker 이미지가 `.env` 파일 복사에 의존하지 않는지 확인한다.

### 2. `.env.example` 정리

- `infra/.env.example`을 추가하거나 갱신한다.
- 민감 정보는 제외하고 필요한 키만 명시한다.
- 로컬 Docker와 배포용 값의 차이를 주석으로 구분한다.

### 3. MinIO 내부/공개 URL 분리

- `MINIO_URL`은 백엔드가 MinIO에 접속하는 내부 endpoint로 사용한다.
  - 예시: `http://minio:9000`
- `MINIO_PUBLIC_URL`은 브라우저가 접근할 공개 endpoint로 사용한다.
  - 예시: `http://localhost:9000`
- MinIO Client를 역할별로 나눈다.
  - 내부 작업용 client: 업로드, 삭제, bucket 확인
  - 공개 URL 발급용 client: presigned URL 발급
- 발급된 presigned URL은 host 치환하지 않는다.

### 4. Compose 환경 재확인

- Docker Compose에서 `be`, `fe`, `oracle-db`, `minio`가 함께 동작하는지 다시 확인한다.
- 백엔드가 `oracle-db`와 `minio`를 내부 네트워크로 접근하는지 확인한다.
- 브라우저가 `MINIO_PUBLIC_URL` 기준 이미지 URL에 접근하는지 확인한다.

## 기대 결과

- Docker 이미지가 `.env` 복사에 의존하지 않는다.
- Docker Compose 환경에서 백엔드가 `oracle-db`와 `minio`를 올바르게 참조한다.
- 브라우저는 `MINIO_PUBLIC_URL` 기준 presigned URL로 이미지를 열 수 있다.
- MinIO URL 관련 문제는 환경 변수 설계 문제로 정리되고, Day 2에서는 구조만 바로잡는다.

## 남은 확인 포인트

- 로컬 Maven/IntelliJ 실행과 Docker Compose 실행의 환경 변수 값을 분리해서 문서화해야 한다.
- `REACT_APP_API_BASE_URL`은 로컬 Docker 검증용 값과 배포용 값을 따로 정리할 필요가 있다.
- Day 2가 끝나면 `Todo.md`에서 Docker 관련 항목을 갱신한다.

