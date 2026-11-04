# Re:View 이력서 문장안

`resume.md`의 Re:View 항목을 바꿀 때 쓰기 위한 초안입니다. `resume.md`는 수정하지 않았습니다.  
백엔드 개발자 지원 기준으로, 프론트 연동·QnA/신고 화면 연결은 뺐습니다.

기간은 팀 개발만 쓰면 `25.10 ~ 25.12`, 종료 후 인가·예외 정리까지 넣으면 `25.10 ~ 25.12 (안정화 26.05)`로 쓰면 됩니다.

---

## 0. 현재 `resume.md`에 없는 것 — 추가 권장만

지금 이력서에는 이미 구현 4줄(이미지 계약, 포인트 락, 추천 SQL, Actions)이 있습니다.  
아래는 **아직 없거나 한 줄만 보강하면 신입 백엔드로 더 잘 읽히는 문장**입니다. 전부 넣지 말고, 칸이 남는 곳만 고르면 됩니다.

### 넣으면 좋은 것 (우선순)

**핵심 역량 1번에 한 줄** — 이미지 유실은 자소서에만 있고, 이력서 상단에는 없음. 서류 스캔용으로 가장 이득입니다.

```
●	리뷰 수정 시 Presigned URL이 object key로 저장되며 이미지가 유실되자, 저장값과 응답값의 역할을 나눠 재발을 막음
```

항목이 3줄이 되면, 기존 MinIO 도입 줄과 역할이 겹칩니다. 그때는 MinIO 줄을 2번(실행 환경)에만 두고 1번에서는 빼도 됩니다.

**담당 역할 — 검색·관리자 한 줄을 이렇게 바꾸기**

지금: `검색·정렬·필터 API, 관리자 API를 사용자 API와 분리`  
화면마다 API를 나눈 것이 안 보입니다.

```
●	검색·정렬·필터 API를 헤더 / 상품상세 / 마이페이지 / 관리자로 분리. 관리자 API를 사용자 API와 계층·경로에서 분리
```

**담당 역할 — 마이페이지에 포인트 명시**

지금 역할에는 장바구니·결제수단·위시·프로필만 있고, 포인트는 구현 칸에만 있습니다.

```
●	마이페이지 REST API: 프로필 조회/수정, 장바구니, 위시리스트, 결제수단, 포인트
```

**구현 4번째 줄 보강 (둘 중 하나)**

지금: `GitHub Actions Self-hosted Runner로 main 푸시 시 Compose 재배포`

배포를 조금 더 백엔드답게 쓰려면:

```
●	GitHub Actions Self-hosted Runner로 main 푸시 시 Compose 재배포. 이미지에 .env를 넣지 않고 Secrets·Compose로 주입
```

종료 후 작업을 기간에 넣을 수 있으면:

```
●	GitHub Actions Self-hosted Runner로 main 푸시 시 Compose 재배포. API를 공개 / 로그인 / 관리자로 분류해 인가를 적용하고, 전역 예외와 공통 에러 응답을 정리
```

두 문장을 한 줄에 다 넣으면 길어집니다. **Secrets 주입**과 **인가 정리** 중 하나만 붙이는 편이 낫습니다.

**성과 3번째를 이 문장으로 교체 (선택)**

지금 3번은 팀장 합의라 역할·자소서 협업과 겹칩니다. CRUD와 가르려면:

```
●	동작하는 구현 이후에도 동시 요청·저장 계약·인가 경계를 다시 나누는 쪽이 백엔드 일이라고 배움
```

기존 팀장 문장은 자소서 협업에 이미 있으므로 이력서에서 빼도 됩니다.

**Skill Set Spring Boot 두 번째 줄**

지금: `세션 기반 로그인과 BCrypt 비밀번호 저장이 적용된 프로젝트에서 API 개발`  
로그인 구현은 이재빈 담당에 가깝습니다. 본인 작업에 맞게:

```
●	마이페이지·검색·관리자 REST API를 도메인별로 분리. 세션 인증이 적용된 프로젝트에서 API 개발
```

**기간·환경 (26.05를 쓸 때만)**

```
작업 기간	25.10 ~ 25.12 (안정화 26.05)
환경	Java, Spring Boot, Spring Security, Oracle SQL, MyBatis, Docker Compose, GitHub Actions, MinIO, AWS EC2
```

인가·예외를 구현 칸에 안 넣으면 기간도 `25.10 ~ 25.12`로 두는 것이 맞습니다.

### 자소서만 손보면 되는 것

이력서 프로젝트 칸이 아니라, 자기소개서 문장만 구체화하면 되는 부분입니다.

**전문성을 키운 경험 — 마지막 줄**

지금: `프로젝트 종료 후에도 Re:View의 추천 SQL, 이미지 처리, Compose·배포 설정을 손보며 구조를 정리하고 있습니다.`

```
프로젝트 종료 후에는 기능을 더하지 않고, API를 공개 / 로그인 / 관리자로 나눠 인가를 맞추고 전역 예외 응답을 정리했습니다. Compose 이미지에서 .env 복사를 제거해 시크릿이 빌드에 들어가지 않게 했습니다.
```

**강점 문단**은 이미 이미지 유실·추천 SQL·포인트 락이 들어 있습니다. 여기에 MinIO 주소/서명 이슈를 또 넣으면 이미지 이야기가 두 번입니다. 그 사례는 면접용으로 두는 편이 낫습니다.

### 넣지 말 것

이력서에 새로 넣으면 약해지거나, 이미 있는 내용과 겹칩니다.

- CORS origin을 환경 변수로 뺀 것, MinIO 내부/공개 URL 분리(보류), 월간 스케줄러
- “추천 알고리즘”, QnA·신고 연동, 커밋 수, 검색 프론트 버그
- 핵심 역량에 포인트 락을 또 넣는 것 (Skill·구현에 이미 있음)
- React/HTML 줄을 늘리는 것

### 복붙용 — 최소 추가만 할 때

공간 없으면 아래 세 곳만 바꾸면 됩니다.

1. 핵심 역량 1번에 이미지 계약 한 줄 추가  
2. 담당 역할의 검색 줄을 `헤더 / 상품상세 / 마이페이지 / 관리자`로 교체  
3. 구현 배포 줄 끝에 `이미지에 .env를 넣지 않고 Secrets·Compose로 주입` 추가


---

## 1. 프로젝트 헤더 (그대로 붙여넣기)

```
1. Re:View
작업 기간	25.10 ~ 25.12 (안정화 26.05)
개요	리뷰와 Baumann 피부 타입을 활용한 스킨케어 추천 커머스
인원	6명 (팀장 / 백엔드·인프라)
환경	Java, Spring Boot, Spring Security, Oracle SQL, MyBatis, Docker Compose, GitHub Actions, MinIO, AWS EC2
```

팀 기간만 쓸 때는 `(안정화 26.05)`와 환경의 `Spring Security`를 빼도 됩니다.

---

## 2. 담당 역할

이력서에 넣을 문장 (4~5줄 권장):

- 팀장으로서 ORM·스토리지 선택 시 장단점을 정리해 팀 합의를 맞춤. 기술 문서 작성
- 마이페이지 REST API: 프로필 조회/수정, 장바구니, 위시리스트, 결제수단, 포인트
- 검색·정렬·필터 API를 헤더 / 상품상세 / 마이페이지 / 관리자로 분리. 관리자 API를 사용자 API와 계층·경로에서 분리
- Docker Compose로 Oracle·MinIO·Nginx·Spring Boot 실행 환경을 맞추고, GitHub Actions Self-hosted Runner로 EC2 재배포 자동화
- MinIO Presigned URL 이미지 구조 도입. 팀원이 만든 추천 조회 SQL을 점수화·중복 제거 구조로 재구성

역할이 길어지면 아래처럼 줄여도 됩니다.

- 팀장 / 백엔드: 마이페이지·검색 API, 관리자 계층 분리, 포인트 트랜잭션, MinIO, Compose·EC2 배포
- 추천 조회는 최초 구현이 팀원, 후반 점수화·중복 제거 SQL은 본인

---

## 3. 주요 기능 및 구현

이력서 본문에 넣을 문장입니다. 4줄이 가장 읽기 좋습니다. 공간 있으면 5번째까지.

- Presigned URL 이미지 업로드. WAS는 파일을 받지 않고 PUT/GET URL만 발급하고, DB에는 object key를 저장. 리뷰 수정 시 클라이언트가 Presigned URL과 object key를 섞어 보내 이미지가 유실되던 문제를, 저장값(object key)과 응답값(URL)을 나눠 수정
- 포인트 적립/차감에 `@Transactional`과 `SELECT FOR UPDATE`를 적용해, 동일 사용자 동시 요청 시 잔액 경쟁을 트랜잭션 안에서 방어
- 기존 추천 조회에 작성자 바우만 적합, 상품 타입 적합, 평점 유사, 검증 가중을 나누고 `ROW_NUMBER() OVER (PARTITION BY product_id)`로 상품당 리뷰 1건만 남기도록 SQL을 재구성 (최초 추천 기능은 팀원 구현)
- GitHub Actions Self-hosted Runner로 `main` 푸시 시 Compose 스택을 EC2에 재배포. 이미지에 `.env`를 넣지 않고 Secrets·Compose로 주입
- (선택, 26.05) API를 공개 / 로그인 / 관리자로 분류해 Spring Security 인가를 적용하고, 인증 DTO 검증과 전역 예외·공통 에러 응답을 정리

공간을 더 줄여야 할 때:

- MinIO Presigned URL. 리뷰 수정 시 URL과 object key가 섞여 이미지가 유실되던 문제를, 저장 계약(key)과 조회 계약(URL)을 나눠 수정
- 포인트 적립/차감에 `@Transactional` + `SELECT FOR UPDATE`로 동일 사용자 잔액 경쟁을 방어
- 추천 SQL을 점수 항목별로 나누고 `ROW_NUMBER() PARTITION BY product_id`로 상품당 1건만 조회 (최초 구현은 팀원)
- Compose + Self-hosted Runner로 EC2 재배포. 종료 후 공개/로그인/관리자 인가와 전역 예외를 정리

넣지 않는 편이 나은 구현:

- 헤더 검색의 프론트 카테고리 초기화 버그, axiosClient, 닉네임 표시, 날짜 포맷
- 신고·QnA 화면 연동 (도메인 담당이 팀원)
- 월간 베스트 리뷰 스케줄러 (부가 기능, 면접에서만)

---

## 4. 성과 및 느낀 점

- 추천을 별도 알고리즘이 아니라 규칙 기반 SQL 점수화로 다루며, 조회 품질과 상품 중복을 쿼리에서 정리함
- MyBatis·MinIO 선택은 쿼리 제어, 비용, 로컬 환경을 기준으로 팀과 맞춰 본 경험
- 설계가 갈릴 때 장단점을 문서로 공유해야 합의가 남는다는 것을 팀장으로서 체감
- (선택) 기능 구현 이후에도 동시 요청·저장 계약·인가 경계를 다시 나누는 쪽이 백엔드 일이라고 배움

3줄만 쓸 때: 위 1~3번.  
“동작하는 CRUD”와 차별하려면 4번을 넣고 2번을 빼도 됩니다.

---

## 5. 핵심 역량에 보강할 문장

지금 `resume.md` 핵심 역량과 겹치지 않게, 바꾸거나 한 줄 더 넣을 때만 사용합니다.

**1. 문제 해결의 구조화와 기술 선택의 근거**

- (유지) 추천 조회처럼 SQL 제어가 필요한 구간을 고려해, 팀 논의 끝에 JPA 대신 MyBatis를 사용
- (유지) 비용과 로컬 개발 환경을 고려해 객체 스토리지로 AWS S3 대신 MinIO를 도입
- (추가) 리뷰 수정 시 Presigned URL이 object key로 저장되며 이미지가 유실되자, 저장값과 응답값의 역할을 나눠 재발을 막음

**2. 실행 환경 표준화와 배포**

- (유지) Docker Compose로 Oracle, MinIO, Nginx(React), Spring Boot 실행 환경을 맞춤
- (유지) Presigned URL로 이미지 업로드를 객체 스토리지에 위임해 WAS가 파일을 직접 받지 않도록 구성
- (추가) 포인트 적립/차감에 `SELECT FOR UPDATE`를 적용해 동일 사용자 동시 요청 시 잔액 정합성을 트랜잭션 안에서 방어
- (추가, 26.05를 쓸 때) API를 공개 / 로그인 / 관리자로 나눠 Spring Security 인가 정책을 적용

핵심 역량은 항목당 2줄이 읽기 좋습니다. 3번 항목을 넣으면 2번의 Presigned URL 한 줄을 빼는 식으로 맞추면 됩니다.

---

## 6. Skill Set — Re:View 관련만 다듬은 문장

기존 표를 바꿀 때 쓸 백엔드 문장입니다.

| 기술 | 문장 |
| --- | --- |
| Spring Boot | REST API 설계. 마이페이지·검색·관리자 API를 도메인별로 분리. 세션 인증이 적용된 프로젝트에서 API 개발 |
| Spring Security | (26.05를 쓸 때) API를 공개 / 로그인 / 관리자로 분류해 인가. 인증 DTO 검증 및 전역 예외 처리 |
| Oracle SQL | JOIN, Subquery, Window Function(`ROW_NUMBER`) 작성. 포인트 처리에 `SELECT FOR UPDATE` 적용 |
| MyBatis | XML 매핑 및 동적 쿼리. 추천 조회 SQL을 점수 항목별로 나누고 상품당 중복을 제거하도록 재구성 |
| MinIO | Presigned PUT/GET. DB에는 object key만 저장하고, 조회 시에만 URL을 발급 |
| Docker Compose | Oracle, MinIO, Nginx, Spring Boot를 묶어 로컬과 EC2 실행 환경을 맞춤 |
| GitHub Actions | Self-hosted Runner에서 `docker compose`로 EC2 재배포 (테스트 CI는 없음) |

React·HTML 줄은 백엔드 이력서에서는 한 줄로 줄이거나 빼도 됩니다.

- React: 검색·마이페이지 API 연동 확인 수준. 메인 담당은 프론트 팀원

---

## 7. 복사용 완성본 (Re:View 블록)

아래를 `resume.md`의 Re:View 구간과 바꿔 넣으면 됩니다.

```
1. Re:View
작업 기간	25.10 ~ 25.12 (안정화 26.05)
개요	리뷰와 Baumann 피부 타입을 활용한 스킨케어 추천 커머스
인원	6명 (팀장 / 백엔드·인프라)
환경	Java, Spring Boot, Spring Security, Oracle SQL, MyBatis, Docker Compose, GitHub Actions, MinIO, AWS EC2

담당 역할
●	팀장으로서 ORM·스토리지 선택 시 장단점을 정리해 팀 합의를 맞춤. 기술 문서 작성
●	마이페이지 REST API: 프로필 조회/수정, 장바구니, 위시리스트, 결제수단, 포인트
●	검색·정렬·필터 API를 헤더 / 상품상세 / 마이페이지 / 관리자로 분리. 관리자 API를 사용자 API와 계층·경로에서 분리
●	Docker Compose 통합 환경 구성, GitHub Actions Self-hosted Runner로 EC2 재배포 자동화
●	MinIO Presigned URL 이미지 구조 도입. 팀원이 만든 추천 조회 SQL을 점수화·중복 제거 구조로 재구성

주요 기능 및 구현
●	Presigned URL 이미지 업로드. WAS는 파일을 받지 않고 URL만 발급하고, DB에는 object key를 저장. 리뷰 수정 시 URL과 object key가 섞여 이미지가 유실되던 문제를, 저장값(object key)과 응답값(URL)을 나눠 수정
●	포인트 적립/차감에 @Transactional과 SELECT FOR UPDATE를 적용해 동일 사용자 동시 요청 시 잔액 경쟁을 트랜잭션 안에서 방어
●	기존 추천 조회에 작성자 바우만 적합, 상품 타입 적합, 평점 유사, 검증 가중을 나누고 ROW_NUMBER() OVER (PARTITION BY product_id)로 상품당 리뷰 1건만 남기도록 SQL을 재구성 (최초 추천 기능은 팀원 구현)
●	GitHub Actions Self-hosted Runner로 main 푸시 시 Compose 재배포. API를 공개 / 로그인 / 관리자로 분류해 인가를 적용하고, 전역 예외와 공통 에러 응답을 정리

성과 및 느낀 점
●	추천을 규칙 기반 SQL 점수화로 다루며, 조회 품질과 상품 중복을 쿼리에서 정리함
●	MyBatis와 MinIO 선택은 쿼리 제어, 비용, 로컬 환경을 기준으로 팀과 맞춰 본 경험
●	동작하는 구현 이후에도 동시 요청·저장 계약·인가 경계를 다시 나누는 쪽이 백엔드 일이라고 배움
```

26.05 작업을 이력서 기간에 넣기 부담되면, 구현 4번째 줄에서 `API를 공개 / 로그인 / 관리자로...` 문장만 빼고 배포 문장만 남기면 됩니다.

```
●	GitHub Actions Self-hosted Runner로 main 푸시 시 Compose 재배포
```

---

## 8. 자소서와 겹치지 않게 쓰는 법

이력서 구현 칸은 **무엇을 했는지**만 남기고, 왜/어떻게 고쳤는지는 자소서 강점에 두는 편이 낫습니다.

| 이력서에 남길 것 | 자소서에 남길 것 |
| --- | --- |
| Presigned URL, 저장값/응답값 분리 | 이미지가 왜 유실됐고, 재업로드 우회 대신 계약을 나눈 이유 |
| `@Transactional` + `SELECT FOR UPDATE` | 트랜잭션만으로는 잔액이 어긋날 수 있다는 점, 부하 테스트는 없었다는 점 |
| 추천 SQL 점수화·`ROW_NUMBER` | JPA 대신 MyBatis를 고른 팀 논의 |
| Compose + Actions | 커리큘럼 밖 Docker를 실행 환경 문제로 찾아 적용한 과정 |
| 공개/로그인/관리자 인가 | 기능을 더하지 않고 권한 경계를 나중에 맞춘 판단 |
