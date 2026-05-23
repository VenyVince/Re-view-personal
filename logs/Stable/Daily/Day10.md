# Day 10 - Final Verification and README Preparation

## 목표

2주 안정화 작업 결과를 다시 검증하고, README를 실제 구현 기준으로 수정할 수 있게 체크리스트를 확정한다.

## 작업 체크리스트

- [x] `mvn test`를 다시 실행한다.
- [x] `npm --prefix View run build`를 다시 실행한다.
- [x] 환경이 가능하면 백엔드 로컬 실행을 확인한다.
- [x] 환경이 가능하면 프론트 로컬 실행을 확인한다.
- [x] 환경이 가능하면 Docker Compose 실행을 확인한다.
- [x] 수정된 `SecurityConfig` 기준으로 보안 섹션 설명을 정리한다.
- [x] Docker `.env` 런타임 주입 구조를 README에 반영할 문장으로 정리한다.
- [x] 추천 점수 설명이 실제 코드와 맞는지 확인한다.
- [x] 이미지 API 표에 누락된 endpoint가 있는지 확인한다.
- [x] 루트 `package.json`과 `View/package.json`의 스크립트/의존성 중복 정리 필요 여부를 기록한다.
- [x] 프론트 API 모듈 중복 정리 필요 여부를 기록한다.
- [x] 패키지 구조와 Mapper 네이밍 정리를 2주 내 필수인지 후속 리팩토링인지 분류한다.

## README 후속 수정 대상

- 추천 설명
- 이미지 API 표
- 보안 섹션
- Docker 섹션
- 실행 명령어
- 환경 변수
- 깨진 Markdown 표
- 시스템 구조 코드블록

## 후속 리팩토링 분류 기준

- README 수정 전 필수: 실제 실행/검증 결과와 문서가 충돌하는 항목
- 2주 이후 개선: 패키지 구조, Mapper 네이밍, 프론트 API 모듈 중복처럼 장기 유지보수 비용을 줄이는 항목
- 별도 브랜치 권장: MinIO public URL 분리, 이미지 리사이징, 바이러스 검사처럼 변경 범위가 큰 항목

## 결과물

- 최종 검증 메모
- README 수정 체크리스트
- 후속 리팩토링 목록

## 완료 기준

- README는 추측이 아니라 검증된 구현 기준으로 수정할 수 있다.
- 남은 리팩토링 항목이 README 수정 전 필수인지, 2주 이후 개선인지 분류되어 있다.

## 작업 결과

### 최종 검증 메모

- 실행 명령: `.\mvnw.cmd test`
- 실행 시각: 2026-05-12 14:18 KST
- 결과: 성공
- 테스트 수: 1개 실행, 실패 0개, 에러 0개, 스킵 0개
- 남은 경고: Mockito inline mock maker의 동적 Java agent 로딩 경고, SpringDoc 운영 비활성화 권장 경고

- 실행 명령: `npm --prefix View run build`
- 실행 시각: 2026-05-12 14:18 KST
- 결과: 성공, ESLint 경고 남음
- 남은 경고 유형: React Hook dependency, 미사용 변수/import, 불필요 escape, 브라우저 데이터 노후화 안내

백엔드 로컬 실행, 프론트 로컬 실행, Docker Compose 실행은 Day 1에서 확인한 결과를 최종 문서에 반영했다. Day 10에는 장시간 실행 프로세스를 다시 띄우지 않고 테스트/빌드 재검증으로 마무리했다.

### README 반영 기준

- 보안 섹션은 현재 `SecurityConfig` 기준으로 공개 API, 인증 필요 API, 관리자 API를 분리해서 설명한다.
- `anyRequest().permitAll()`은 유지 중이므로 `denyAll()` 전환 전 endpoint 테스트 보강이 필요하다고 명시한다.
- Docker 섹션은 `.env`를 이미지에 복사하지 않고 `infra/.env`를 런타임에 주입하는 구조로 설명한다.
- 추천 설명은 실제 SQL 기준으로 상품 추천과 리뷰 추천이 각각 `total_score`를 계산하고 정렬한다는 수준으로만 적는다.
- 이미지 API 표에는 `POST /api/images/reviews`, `POST /api/images/products`, `POST /api/images/products/convert-data`, `POST /api/images/products/convert-datas`, `GET /api/images/banners`를 포함한다.

### 후속 리팩토링 분류

README 수정 전 필수:

- 검증 결과의 날짜와 실제 명령 결과 반영
- 이미지 API 표 누락 endpoint 보완
- 보안 정책과 Docker 환경 변수 설명 최신화

2주 이후 개선:

- 루트 `package.json`과 `View/package.json`의 스크립트/의존성 중복 정리
- 프론트 API 모듈 중복 정리
- 패키지 구조 정리
- Mapper 네이밍 일관화
- 주문/포인트/추천 서비스 테스트 보강

별도 브랜치 권장:

- MinIO 내부 URL과 공개 URL 분리 재검증
- public endpoint 기준 presigned URL 발급 구조 정리
- MinIO bucket 존재 확인, 업로드 파일 검증, 이미지 리사이징, 바이러스 검사

## Day10 결론

- 백엔드 테스트와 프론트 빌드는 모두 성공했다.
- README는 현재 구현과 검증 결과를 기준으로 수정할 수 있는 상태다.
- 남은 항목은 README 전 필수 수정, 2주 이후 리팩토링, 별도 브랜치 작업으로 분류했다.

## 커밋 메시지 후보

```text
docs: summarize day10 final verification and readme updates
```
