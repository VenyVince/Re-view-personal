
# TODO

## Must

- [x] 백엔드 실행 확인
- [x] 프론트 실행 확인
- [x] Docker Compose 실행 확인
- [x] `Dockerfile`에서 `.env` 복사 제거
- [x] `.env.example` 추가
- [x] `SecurityConfig` 인증/인가 정책 정리
- [x] 401/403 응답 상태 코드 정리
- [x] 테스트 프로파일 수정
- [x] `mvn test` 실행 가능 상태 만들기
- [x] 깨진 한글 문구 정리
- [x] README 취업용으로 재작성

## Should

- [x] `ErrorResponse` DTO 추가
- [x] 전역 예외 응답 표준화
- [x] 회원가입/로그인/비밀번호 변경 DTO validation 적용
- [x] DTO 검증 실패 응답 공통 포맷 적용
- [x] Swagger 설명 정리
- [x] MinIO 설정 키 통일
- [x] `@EnableScheduling` 추가 여부 결정
- [ ] DB 초기화 방법 문서화
- [ ] 서비스 아키텍처 다이어그램 추가
- [ ] ERD 추가

## Optional

- [ ] Redis Session 적용 검토
- [ ] 추천 결과 Redis Cache 적용 검토
- [ ] GitHub Actions CI 추가
- [ ] MinIO 리버스 프록시/도메인 정리
- [ ] MinIO 내부 URL과 공개 URL 분리
- [ ] public endpoint 기준 presigned URL 발급
- [ ] MinIO bucket 존재 확인
- [ ] MinIO 업로드 파일 검증
- [ ] MinIO 이미지 리사이징 검토
- [ ] MinIO 바이러스 검사 검토
- [ ] 주문/포인트 테스트 추가
- [ ] 추천 서비스 테스트 추가
