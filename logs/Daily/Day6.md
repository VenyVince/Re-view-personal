# Day 6 - Test Profile Stabilization

## 목표

테스트 프로파일 설정의 모순을 제거하고, `mvn test` 실패 원인을 해석 가능한 상태로 만든다.

## 작업 체크리스트

- [ ] `src/test/resources/application-test.properties`를 확인한다.
- [ ] H2 URL과 Oracle driver가 함께 쓰이는 문제를 정리한다.
- [ ] H2 기준으로 테스트할지, Oracle 테스트 환경을 명시할지 결정한다.
- [ ] MinIO 테스트 프로퍼티 키를 운영 코드와 맞춘다.
- [ ] 테스트 프로파일의 깨진 한글 주석을 제거하거나 다시 쓴다.
- [ ] 메일 설정이 테스트 실행을 막지 않도록 필요한 test property 또는 mock 범위를 검토한다.
- [ ] `mvn test`를 실행한다.
- [ ] 실패한다면 설정 문제인지, DB 스키마/샘플 데이터 문제인지 분리해 기록한다.

## MinIO 테스트 키 기준

```properties
minio.url=
minio.root-user=
minio.root-password=
minio.bucket=
```

## 확인할 파일

- `src/test/resources/application-test.properties`
- `src/main/java/com/review/shop/image/minio/MinioProperties.java`
- `src/main/java/com/review/shop/image/minio/MinioConfig.java`
- `pom.xml`

## 결과물

- 일관된 테스트 프로파일
- 첫 `mvn test` 실행 결과
- 남은 테스트 실패 원인 기록

## 완료 기준

- 테스트 설정에서 H2 URL과 Oracle driver가 더 이상 섞이지 않는다.
- MinIO 프로퍼티 키가 `MinioProperties`와 일치한다.
- 남은 실패가 있다면 원인이 구체적으로 기록되어 있다.
