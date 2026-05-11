# Day 6 - Swagger, Text Cleanup, and Security Hardening Prep

## 목표

Day 5에서 정리한 공통 예외 응답 포맷을 문서와 화면 문구에 반영하고, 이후 `anyRequest().denyAll()` 전환을 검토할 수 있도록 남은 노출 범위와 테스트 필요 지점을 정리한다.

## Day 5에서 넘어온 상태

- 인증 실패는 401, 권한 부족은 403으로 분리했다.
- `ErrorResponseDTO`를 추가하고 전역 예외 응답을 공통 JSON 포맷으로 통일했다.
- DTO 검증 실패도 `ErrorResponseDTO` 포맷으로 내려가도록 정리했다.
- 회원가입, 로그인, 비밀번호 변경, 임시 비밀번호 발송 DTO에 `jakarta.validation`을 적용했다.
- `mvn test`는 통과하는 상태다.
- `anyRequest().permitAll()`은 유지 중이며, 테스트 보강 후 `denyAll()` 전환을 검토한다.

## 작업 체크리스트

- [x] Swagger의 400/401/403/404/500 응답 설명을 `ErrorResponseDTO` 기준으로 정리한다.
- [x] 인증 관련 API 문서에서 문자열 오류 응답으로 남아 있는 설명을 공통 JSON 포맷으로 바꾼다.
- [x] 깨진 한글 문구를 검색하고 우선 수정 범위를 정한다.
- [x] README를 취업용 프로젝트 설명에 맞게 재작성할 목차를 정한다.
- [x] 공개 API / 인증 필요 API / 관리자 API 기준이 문서와 실제 `SecurityConfig`에서 어긋나지 않는지 다시 확인한다.
- [x] `anyRequest().denyAll()` 전환 전에 필요한 테스트 시나리오를 정리한다.
- [x] MinIO 설정 키 통일 여부를 확인하고, Day 6 범위에 포함할지 결정한다.

## 확인할 파일

- `src/main/java/com/review/shop/config/SecurityConfig.java`
- `src/main/java/com/review/shop/config/ExceptionHandlers.java`
- `src/main/java/com/review/shop/dto/ErrorResponseDTO.java`
- `src/main/java/com/review/shop/controller/user/UserController.java`
- `src/main/java/com/review/shop/controller/user/UserUtilController.java`
- `README.md`
- `logs/Todo.md`

## 유의사항

- Day 5에서 응답 본문 구조가 바뀌었으므로 Swagger 설명이 문자열 응답으로 남아 있으면 실제 API와 문서가 달라질 수 있다.
- `anyRequest().denyAll()`은 바로 적용하면 누락된 공개 API가 막힐 수 있으므로 테스트 시나리오를 먼저 정리한다.
- 깨진 한글 문구 정리는 기능 변경과 섞지 말고, 화면/문서/로그 중 어디를 먼저 정리할지 범위를 좁힌다.
- README는 단순 사용법보다 프로젝트 구조, 인증 방식, 예외 응답 정책, 실행 방법, 검증 결과가 보이도록 정리한다.

## 결과물

- 공통 예외 응답 기준과 맞는 Swagger 정리 방향
- 깨진 한글 문구 수정 범위
- README 재작성 초안 또는 목차
- `denyAll()` 전환 전 필요한 테스트 시나리오 목록

## 오늘 작업 결과

- `UserController`, `UserUtilController`의 인증 관련 Swagger 오류 응답 설명을 `ErrorResponseDTO` 기준으로 정리했다.
- 밴 처리된 사용자 로그인 응답이 공통 예외 응답 형식과 다르던 문제를 정리했다.
  - `BannedUserException`을 추가했다.
  - `ExceptionHandlers`에 `BannedUserException` 핸들러를 추가했다.
  - `UserController.login()`에서 직접 오류 응답을 만들지 않고 예외를 던지도록 수정했다.
- `README.md`를 실행 방법, API 문서, 예외 응답 정책, 보안 정책, 검증 결과, 취업용 보강 항목이 보이도록 재작성했다.
- `src/test/resources/application-test.properties`의 깨진 한글 주석을 수정했다.
- 테스트 프로파일의 MinIO 설정 키를 실제 `MinioProperties`와 맞췄다.
  - 기존: `minio.access-key`, `minio.secret-key`
  - 변경: `minio.root-user`, `minio.root-password`
- 공개 API / 인증 필요 API / 관리자 API 기준을 `SecurityConfig`와 프론트 API 호출 흐름 기준으로 대조했다.
- 공개 API 테스트 시나리오 예시를 `logs/Daily/Day7.md` 참고사항에 정리했다.

## 확인 결과

- `mvnw.cmd test` 통과.
- `npm --prefix View run build` 성공.
- 프론트 빌드에는 기존 ESLint 경고가 남아 있다.

## 남은 확인 포인트

- `/api/survey/baumann`, `/api/products/recommendations/baumann`은 프론트 호출 흔적이 있지만 현재 백엔드 컨트롤러 매핑에서는 확인되지 않았다. 실제 사용 중이면 API 추가 또는 프론트 호출 정리가 필요하다.
- `anyRequest().denyAll()` 전환은 Day 7 이후 공개 API, 인증 API, 관리자 API 테스트를 먼저 만든 뒤 진행한다.

## 완료 기준

- Swagger 문서가 Day 5의 `ErrorResponseDTO` 응답 형식과 충돌하지 않는다.
- 다음 보안 강화 작업에서 어떤 endpoint를 테스트해야 하는지 명확하다.
- README와 문구 정리의 우선순위가 정해져 있다.
