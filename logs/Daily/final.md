# Final - Day 1~10 Summary

## 전체 요약

2주 안정화 작업은 실행 환경 확인, Docker 환경 변수 정리, Spring Security 정책 분류와 적용, 공통 예외 응답 표준화, Swagger/README 정리, 테스트/빌드 검증까지 진행했다. 최종 상태는 백엔드 테스트와 프론트 빌드가 성공하지만, 프론트 ESLint 경고와 일부 후속 리팩토링 항목은 남아 있다.

## Day별 수행 내용

| Day | 핵심 내용 | 결과 |
| --- | --- | --- |
| Day 1 | 백엔드/프론트/Docker 실행 상태 점검 | 로컬 실행 가능, Docker DB 주소와 MinIO 공개 URL 이슈 분리 |
| Day 2 | Docker 환경 변수 정리 | Dockerfile의 `.env` 복사 제거, `infra/.env.example` 추가 |
| Day 3 | API 인증/인가 정책 분류 | 공개 API, 인증 필요 API, 관리자 API 분류와 matcher 초안 작성 |
| Day 4 | `SecurityConfig` 적용 | 관리자 API 권한 제한, 개인 API 인증 제한, Bearer token 전송 제거 |
| Day 5 | 401/403 및 예외 응답 정리 | `ErrorResponseDTO` 도입, 인증 실패 401과 권한 부족 403 분리 |
| Day 6 | Swagger/README/문구 정리 | 인증 API Swagger 설명, 테스트 프로파일, README 초안 정리 |
| Day 7 | Maven 테스트와 테스트 후보 정리 | `mvn test` 성공, 보안/예외/서비스 테스트 후보 정리 |
| Day 8 | 스케줄러와 리뷰 보상 흐름 확인 | `@EnableScheduling` 추가, 리뷰 보상 설명을 실제 코드 기준으로 정리 |
| Day 9 | UTF-8, Swagger, 프론트 경고 정리 | 프론트 빌드 성공, 남은 ESLint 경고와 후속 처리 기준 기록 |
| Day 10 | 최종 검증과 README 준비 | `mvn test`, `npm --prefix View run build` 성공, README 최신화 |

## 최종 검증

- 백엔드 테스트: 2026-05-12 14:18 KST, `.\mvnw.cmd test` 성공
- 프론트 빌드: 2026-05-12 14:18 KST, `npm --prefix View run build` 성공
- 남은 경고:
  - Mockito 동적 Java agent 로딩 경고
  - SpringDoc 운영 환경 비활성화 권장 경고
  - React Hook dependency와 미사용 변수/import 중심의 ESLint 경고

## 남은 작업

- 공개 API, 인증 필요 API, 관리자 API 테스트 보강 후 `anyRequest().denyAll()` 전환 검토
- DB 초기화 SQL과 샘플 데이터 문서화
- ERD 추가
- 주문/포인트/추천 서비스 테스트 추가
- 프론트 ESLint 경고 cleanup
- MinIO public endpoint, bucket 검증, 업로드 파일 검증은 별도 브랜치에서 처리

## 커밋 메시지 정리

```text
docs: summarize day9 swagger and frontend warning cleanup
docs: summarize day10 final verification and readme updates
```
