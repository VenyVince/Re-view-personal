# Re:View 트러블슈팅

git 로그(`VenyVince`)와 안정화 일지(`logs/Daily`)를 기준으로 정리했습니다.  
머지 충돌, 오타, 프론트 표시 버그는 빼고, **원인 분석이 남는 백엔드 이슈**만 넣었습니다.

면접에서는 1~3번을 깊게, 4~6번은 한 사례만 골라 말하면 됩니다.

---

## 1. 리뷰 수정 시 이미지 유실 — Presigned URL과 object key 혼동

**시기:** 2025.12.18  
**근거 커밋**

- 팀원: `fix: 리뷰 수정 시 이미지 url path 넘어가지 않는 오류 -> 리뷰 수정 시 이미지 재등록으로 에러 우회`
- 본인: `refactor: 리뷰 수정시 기존 이미지 데이터 presignedurl로 보낼 시 무시` (`63521f66`)

### 증상

리뷰 본문만 수정해도 기존 이미지가 사라지거나, 저장 후 이미지가 깨졌다.

### 원인

이미지 조회 API는 DB의 object key를 Presigned GET URL로 바꿔 내려준다.  
수정 API는 클라이언트가 보낸 문자열을 그대로 object key로 저장했다.

프론트는 수정 화면에 **이미 변환된 URL**을 다시 실어 보냈다. URL이 key로 저장되면 MinIO에서 객체를 찾지 못한다. 새 파일의 key와 기존 URL이 한 배열에 섞이기도 했다.

같은 날 프론트에서는 “수정 시 이미지를 다시 올리라”는 우회로 증상을 피했다.

### 대응

`ProductReviewService.filterObjectKeys`에서 `http`로 시작하는 값은 저장하지 않는다.

- object key가 하나도 없으면 → 이미지 변경 없음. 기존 key 유지
- object key가 있으면 → 그 목록으로 교체

저장 계약(DB = object key)과 응답 계약(클라이언트 = Presigned URL)을 서버에서 나눴다.

### 면접에서 말할 점

화면 우회가 아니라 **저장값과 조회값을 나눈 수정**이다.  
“왜 프론트에서 URL을 안 보내게 하지 않았나”에는, 수정 화면에 기존 이미지를 보여주려면 URL이 필요하고, 서버가 저장 직전에 걸러야 계약이 안정된다고 답하면 된다.

---

## 2. Docker에서 이미지가 안 보임 — MinIO 내부 주소와 서명

**시기:** 배포 직후 2025.12, 원인 정리 2026.05.04~05  
**근거**

- 커밋: `chore: MinIO endpoint separation is deferred...` (`49be85a5`)
- 일지: `logs/Daily/Day1.md` 3.4절, 5절

### 증상

Compose로 올리면 API는 되는데, 브라우저에서 이미지가 안 뜬다.  
로컬에서 Spring만 띄우면 이미지는 보인다.

### 원인

`MINIO_URL` 하나가 두 역할을 겸했다.

| 값 | 백엔드 → MinIO | 브라우저 → 이미지 |
| --- | --- | --- |
| `http://minio:9000` | 성공 (Compose DNS) | 실패. 브라우저는 `minio`를 모름 |
| `http://localhost:9000` | 컨테이너 안에서 실패. `localhost`는 BE 자신 | 성공 |

Presigned URL은 host까지 포함해 서명한다. 발급 후 `minio:9000`을 `localhost:9000`으로 바꾸면 **서명 불일치로 MinIO가 거절**한다.

같은 날 확인한 DB 이슈도 같은 종류의 주소 문제였다. BE 컨테이너의 `SPRING_DATASOURCE_URL`이 `localhost:1521`이면 Oracle에 못 붙는다. `oracle-db:1521`로 바꾼 뒤에 조회가 됐다. (`Day1.md` 3.3절)

### 대응

- DB: Compose 서비스명 `oracle-db`를 쓰도록 env를 나눔
- MinIO: `MINIO_URL`(내부)과 `MINIO_PUBLIC_URL`(브라우저)을 나누는 설계를 일지에 남김
- 다만 당시 리사이즈·백신 같은 **내부 후처리가 없어** 클라이언트 분리를 보류함 (`49be85a5`)

### 면접에서 말할 점

고치지 않고 보류한 것도 로그에 남는다.  
“문자열로 host만 바꾸면 된다”가 왜 안 되는지, Presigned URL 서명이 host에 묶여 있다는 점을 말하면 된다.  
완료된 수정으로 포장하지 말 것.

---

## 3. 포인트 잔액 경쟁 — `@Transactional`과 `SELECT FOR UPDATE`

**시기:** 2025.11.27  
**근거 커밋:** `chore: transactional 처리(동시성 해결 및 row Lock)` (`788998e3`)

### 증상 (예상)

리뷰 적립과 주문 차감이 같은 사용자 행을 동시에 갱신하면, 둘 다 같은 잔액을 읽고 나중에 쓴 쪽이 앞선 갱신을 덮을 수 있다.

운영에서 장애 티켓이 난 기록은 없다. 구현 직후 동시 요청을 전제로 넣은 방어 코드다.

### 원인

기존 쿼리는 `SET point = point + amount` 형태였고, 트랜잭션 경계가 메서드마다 따로였다.  
히스토리 insert와 잔액 update가 한 잠금 안에서 읽기→계산→쓰기로 묶이지 않았다.

### 대응

- `SELECT point FROM user_table WHERE user_id = ? FOR UPDATE`
- `@Transactional` 안에서 잠금 → 가감 → `SET point = #{newPoint}`
- 적립/차감/리뷰 보상 경로가 모두 `updateUserPoint`를 타게 통일
- 잔액이 0 미만이면 예외

### 면접에서 말할 점

부하 테스트로 재현한 건 아니다. 로컬·코드 리뷰 수준이다.  
락 범위를 사용자 한 행으로 좁힌 이유, 주문(재고+포인트)과 리뷰 적립의 트랜잭션을 어떻게 나눴는지는 코드를 보고 답할 준비를 하면 된다.

---

## 4. 추천 목록에 같은 상품 리뷰가 반복

**시기:** 2025.12.13 ~ 12.17  
**근거 커밋**

- `rec, enh: Recommend Service`
- `refactor: Refactoring FE, BE recommend`
- `refactor: Recommend 리팩트링`

### 증상

피부 타입 추천 리뷰에 한 상품의 리뷰가 여러 건 나와, 상품 다양성이 떨어졌다.

### 원인

최초 추천은 팀원 구현이었다. 리뷰 단위로 점수를 매기면 인기 상품의 리뷰가 상위권을 채운다.  
JOIN으로 이미지가 붙으면 행이 늘어 같은 리뷰가 중복될 여지도 있었다.

### 대응

점수를 항목별로 나눈 뒤, 상품당 한 건만 남겼다.

```sql
ROW_NUMBER() OVER (
  PARTITION BY p.product_id
  ORDER BY total_score DESC, r.like_count DESC
) AS rn
-- WHERE rn = 1
```

리뷰 이미지가 없으면 상품 썸네일로 `COALESCE` / `LATERAL` 처리.

### 면접에서 말할 점

“추천 알고리즘을 만들었다”보다 **조회 품질을 SQL에서 정리했다**가 로그와 맞다.  
최초 구현은 팀원, 점수화·중복 제거는 본인으로 역할을 분명히 할 것.

---

## 5. EC2 배포 후 API가 브라우저에서 막힘 — CORS origin 고정

**시기:** 2025.12.10 ~ 12.11  
**근거 커밋**

- `chore: Corsconfig test`
- `deploy: Edit corsconfig` (`fe8234a1`)

### 증상

로컬(`localhost:3000`)에서는 로그인·조회가 되고, Compose를 EC2에 올린 뒤에는 브라우저가 API를 막았다.

### 원인

`CorsConfig`가 `http://localhost:3000`만 허용했다.  
배포 프론트 origin은 서버 IP 또는 다른 포트다. 세션 쿠키를 쓰므로 `allowCredentials(true)`와 origin 화이트리스트가 맞아야 한다.

### 대응

origin을 코드 상수가 아니라 환경 변수에서 읽게 바꿨다.

```text
cors.allowed-origins (기본값 localhost:3000, 콤마로 복수)
```

배포 `.env`의 `CORS_ALLOWED_ORIGINS`로 맞춤.

### 면접에서 말할 점

CORS를 “전부 허용”으로 풀지 않고, 환경별로 origin을 주입한 이유를 말하면 된다.

---

## 6. 이미지에 비밀번호가 들어감 — Dockerfile이 `.env`를 COPY

**시기:** 발견·수정 2026.05.05  
**근거 커밋:** `chore: Delete 'copy env file' on Docker Images. Add .env.example` (`458f821e`)

### 증상

백엔드 이미지가 `infra/.env` 없이 빌드되지 않았고, DB·MinIO 비밀번호가 이미지 레이어에 남을 수 있었다.

### 원인

```dockerfile
COPY infra/.env .env
```

빌드 시점 파일이 이미지에 박힌다. Compose `env_file`과 역할이 겹친다.

### 대응

- Dockerfile에서 COPY 제거. jar만 넣음
- 런타임은 Compose `env_file: .env`
- 비밀번호 없는 `infra/.env.example` 추가
- Actions는 GitHub Secrets로 `.env`를 만들어 주입 (`cicd: use git secret`)

### 면접에서 말할 점

배포를 붙인 뒤에야 보인 설정 실수다. “처음부터 잘 나눴다”보다 **이미지와 런타임 주입을 나중에 분리했다**고 말하는 편이 정직하다.

---

## 그 밖에 로그에 남는 이슈 (짧게)

면접 본사례로 쓰기엔 얇고, “또 어떤 걸 고쳤나”에 대비하면 된다.

| 이슈 | 커밋 | 한 줄 |
| --- | --- | --- |
| MinIO 이미지 업그레이드 후 인증 키 이름 불일치 (`ACCESS_KEY`/`SECRET_KEY` vs `ROOT_USER`/`ROOT_PASSWORD`) | `merge&fix: Update MinIO credentials after MinIO upgrade` (2025.12.10) | 공식 이미지 env 이름에 맞춰 properties·Compose를 바꿈 |
| 헤더 검색에서 리뷰×이미지 JOIN으로 행이 늘어남 | `refactor: return single image URL for header search reviews` (12.14) | collection 매핑 대신 대표 이미지 1건. 이후 `COALESCE`로 리뷰 이미지 없으면 상품 썸네일 (12.18) |
| 업로드 API가 Form 파라미터인데 클라이언트는 JSON | `temp: Image Json 통신 형태로 변환` (12.06) | `@RequestParam` → `@RequestBody`. 파일은 MinIO로 직접 PUT |
| Windows에서 `Dto`/`DTO` 파일명 충돌 | `Fix casing conflict... PasswordUpdateDTO` (11.14) | Git이 대소문자를 구분하지 않아 클래스가 둘로 보임. 파일명 통일 |
| 개발 중 Security가 전부 허용 | `feat/chore: ... SecurityConfig ... 임시로 모든 요청 허용` → `fix: apply SecurityConfig authorization policy` (2026.05.08) | 공개 / 로그인 / 관리자로 인가를 나중에 분류 |

프론트 `fix: 브랜드 선택 후 Sort하고 다른 카테고리 변경하면 초기화되는 버그 해결`은 상태 초기화 이슈라 백엔드 트러블슈팅으로는 약하다.

---

## 자소서·면접에서 쓰는 법

| 우선 | 사례 | 한 줄 메시지 |
| --- | --- | --- |
| 1 | 이미지 유실 | 저장 계약(key)과 응답 계약(URL)을 나눔 |
| 2 | MinIO 주소 | 컨테이너 DNS와 브라우저 주소가 다르고, Presigned 서명은 host를 포함함 |
| 3 | 포인트 락 | 트랜잭션만으로는 잔액 덮어쓰기를 못 막음 |
| 보조 | CORS / `.env` COPY | 로컬 상수가 배포에서 깨짐. 시크릿은 이미지에 넣지 않음 |

1번과 2번은 둘 다 이미지라서, 자소서에는 **1번만** 쓰고 2번은 면접 인프라 질문용으로 남겨 두는 편이 낫다.  
3번은 장애 재현이 없다고 먼저 말하는 것이 안전하다.

rebase 충돌을 14번 고친 로그는 트러블슈팅으로 쓰지 말 것.
