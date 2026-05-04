
# TODO

## Must

- [x] 백엔드 실행 확인
- [x] 프론트 실행 확인
- [x] Docker Compose 실행 확인
- [ ] `Dockerfile`에서 `.env` 복사 제거
- [ ] `.env.example` 추가
- [ ] MinIO 내부 URL과 공개 URL 분리
- [ ] public endpoint 기준 presigned URL 발급
- [ ] `SecurityConfig` 인증/인가 정책 정리
- [ ] 401/403 응답 상태 코드 정리
- [ ] 테스트 프로파일 수정
- [ ] `mvn test` 실행 가능 상태 만들기
- [ ] 깨진 한글 문구 정리
- [ ] README 취업용으로 재작성

## Should

- [ ] `ErrorResponse` DTO 추가
- [ ] 전역 예외 응답 표준화
- [ ] Swagger 설명 정리
- [ ] MinIO 설정 키 통일
- [ ] `@EnableScheduling` 추가 여부 결정
- [ ] DB 초기화 방법 문서화
- [ ] 서비스 아키텍처 다이어그램 추가
- [ ] ERD 추가

## Optional

- [ ] Redis Session 적용 검토
- [ ] 추천 결과 Redis Cache 적용 검토
- [ ] GitHub Actions CI 추가
- [ ] MinIO 리버스 프록시/도메인 정리
- [ ] 주문/포인트 테스트 추가
- [ ] 추천 서비스 테스트 추가
