김석현
희망 직무  백엔드 개발자
Email  redguy0814@gmail.com
Mobile  010-5926-8629	 
핵심 역량 / Strength
1. 문제 해결의 구조화와 기술 선택의 근거
●	추천 조회처럼 SQL 제어가 필요한 구간을 고려해, 팀 논의 끝에 JPA 대신 MyBatis를 사용
●	리뷰 수정 시 Presigned URL이 object key로 저장되며 이미지가 유실되자, 저장값과 응답값의 역할을 나눠 재발을 막음
2. 실행 환경 표준화와 배포
●	Compose에서 localhost가 컨테이너 자신을 가리켜 DB 연결이 실패하자, 서비스명(oracle-db)으로 주소를 분리
●	Dockerfile이 .env를 이미지에 넣어 시크릿이 남을 수 있어 COPY를 제거하고 Secrets·Compose로 주입
3. 필요한 기술을 찾아 프로젝트에 적용
●	멀티캠퍼스 현대 이지웰 Java 풀스택 부트캠프 최우수 수료생 선정
●	커리큘럼에 없던 Docker와 MinIO를 학습해 프로젝트에 적용
학력 사항 / Educational Qualifications
기간	2020.02 – 2026.02
학교	원광대학교
학과	컴퓨터소프트웨어공학과
교육 사항 / Education Experience
Multicampus — 현대이지웰 Java 풀스택 과정 (25.06 ~ 25.12)
●	Frontend: HTML, CSS, JavaScript, React 기초, Redux를 활용한 상태 관리
●	Backend: Java, Spring MVC, Spring Boot 기반 WAS 및 REST API
●	Database: Oracle 기반 RDBMS 설계 및 SQL
●	Persistence: JPA, MyBatis를 사용한 CRUD
●	Tools: Git, GitHub
●	Project: Spring Boot와 React 기반 쇼핑몰 프로젝트
●	전체 수료생 50명 중 5명에게 주어지는 최우수 수료생 선정
Comento — IT 8개 직무 체험 (25.01 ~ 25.02)
●	AWS에서 EC2, Apache, Lambda, API Gateway, CloudWatch를 활용한 웹 서비스 배포 실습
●	VPC 등 네트워크 구성을 포함한 클라우드 아키텍처 체험
주요 SW 역량 / Skill Set
Java	●	컬렉션, 예외 처리, I/O를 활용한 애플리케이션 개발
●	팀 프로젝트에서 비즈니스 로직 구현 및 리팩토링
Spring Boot	●	REST API 설계. 마이페이지·검색·관리자 API를 도메인별로 분리
●	세션 인증 프로젝트에서 전역 예외·DTO 검증으로 에러 응답을 맞춤
Oracle SQL	●	JOIN, Subquery, Window Function(ROW_NUMBER 등) 작성
●	포인트 동시 갱신에 SELECT FOR UPDATE. 추천 조회에서 상품당 중복을 ROW_NUMBER로 제거
MyBatis	●	XML 매핑 및 동적 쿼리 작성
●	추천 SQL을 점수 항목별로 나누고 상품당 1건만 남기도록 재구성
Docker / Docker Compose	●	Oracle, MinIO, Nginx, Spring Boot를 Compose로 묶음. localhost와 서비스명 주소를 구분해 DB 연결 실패를 해소
MinIO	●	Presigned PUT/GET. DB에는 object key만 저장
●	URL이 key로 저장되며 유실되던 문제를 저장 계약과 조회 계약 분리로 수정
Nginx	●	React 정적 파일 서빙 및 /api 프록시 설정
GitHub Actions	●	Self-hosted Runner에서 docker compose로 EC2 재배포. 이미지 COPY .env를 제거하고 Secrets로 주입 (테스트 CI는 없음)
AWS	●	Comento: EC2, Lambda, API Gateway 실습
●	Re:View: EC2에 Compose 스택 배포
●	Amazon Linux에서 패키지·권한 등 기본 운영
React	●	컴포넌트 구성 및 Axios를 이용한 REST API 연동
HTML / CSS / JavaScript	●	웹페이지 구성 및 이벤트 처리
Node.js (Croffle)	●	Electron Main Process에서 검색 API 및 OS 네이티브 기능 연동
NestJS / TypeScript (Cosider)	●	REST Auth API. Guard·Strategy·Filter 기반 로컬·OAuth 인증 모듈 구현
JWT / Passport	●	Access·Refresh Token, Google/GitHub OAuth
●	CSRF state 검증에 timing-safe 비교 적용
대표 프로젝트 / Key Project
1. Re:View
작업 기간	25.10 ~ 25.12
개요	리뷰와 Baumann 피부 타입을 활용한 스킨케어 추천 커머스
인원	6명 (팀장 / 백엔드·인프라)
환경	Java, Spring Boot, React, Oracle SQL, MyBatis, Docker Compose, GitHub Actions, MinIO, AWS EC2

담당 역할
●	팀장으로서 ORM·스토리지 선택 시 장단점을 정리해 팀 합의를 맞춤. 기술 문서 작성
●	마이페이지·검색·관리자 REST API, MinIO 이미지, Compose·EC2 배포
문제 해결 및 구현
●	리뷰 수정 시 클라이언트가 Presigned URL과 object key를 섞어 보내 기존 이미지가 유실됨. 화면에서 재업로드를 강제하는 우회 대신, filterObjectKeys로 저장값(object key)과 응답값(URL)을 나눠 재발을 막음
●	리뷰 적립과 주문 차감이 같은 잔액을 동시에 갱신하면 덮어쓸 수 있어, @Transactional과 SELECT FOR UPDATE로 읽기·계산·쓰기를 한 트랜잭션에 묶음
●	추천 조회에서 한 상품의 리뷰가 상위권을 채워 다양성이 떨어짐. 점수 항목을 나눈 뒤 ROW_NUMBER() OVER (PARTITION BY product_id)로 상품당 1건만 남김 (최초 추천 기능은 팀원 구현)
●	Compose에서 BE가 localhost로 Oracle에 붙어 커넥션이 실패하고, CORS origin이 localhost:3000에 고정되어 EC2에서 API가 막힘. DB는 서비스명(oracle-db)으로, CORS는 환경 변수로 분리. Dockerfile의 .env COPY를 제거하고 Secrets·Compose로 주입
●	마이페이지(프로필·장바구니·위시·결제수단·포인트)와 검색·정렬·필터 API를 헤더 / 상품상세 / 마이페이지 / 관리자로 분리. GitHub Actions Self-hosted Runner로 main 푸시 시 Compose 재배포
성과 및 느낀 점
●	우회가 아니라 저장 계약·트랜잭션 경계·조회 중복처럼 같은 문제가 다시 생기지 않게 경계를 나누는 쪽이 백엔드 일이라고 배움
●	MyBatis와 MinIO 선택은 쿼리 제어, 비용, 로컬 환경을 기준으로 팀과 맞춰 본 경험
●	Compose의 localhost는 컨테이너 자신이라, 로컬에서 되던 연결이 배포에서 깨지는 원인을 주소와 서명 단위로 보게 됨
링크
●	https://github.com/VenyVince/Re_View.git
●	https://github.com/VenyVince/5.Re_View_Doc
 
2. Cosider
작업 기간	26.05 ~ 진행 중
개요	기획부터 배포·유지보수까지 프로젝트 전 과정을 추적하는 관리·협업 툴 (Croffle Dev)
인원	팀 협업 (본인: 인증 도메인)
환경	NestJS, TypeScript, Passport, JWT, Drizzle, PostgreSQL, Nuxt 4, Yarn workspaces

담당 역할
●	NestJS 인증 모듈: 회원가입·이메일 인증, 로그인·로그아웃, Access/Refresh Token, Google·GitHub OAuth
●	Auth API Swagger 문서화, DTO·인터페이스 정리
문제 해결 및 구현
●	회원가입과 인증 메일 발급이 따로 실패하면 계정만 남거나 메일만 나가는 상태가 되어, 두 작업을 DB 트랜잭션으로 묶음
●	Access와 Refresh를 나눈 뒤에도 로그아웃 후 토큰이 남으면 재사용되므로, Refresh는 쿠키로 다루고 로그인·로그아웃 시 revoke. Refresh 기준으로 멀티 디바이스 로그인을 제한
●	OAuth state를 일반 문자열 비교하면 타이밍 공격에 노출될 수 있어 timingSafeEqual로 검증. 인증 실패 시 예외 필터와 리다이렉트 경로를 맞춰 클라이언트가 받을 응답을 고정
●	(보조) Nuxt 회원가입·로그인·이메일 인증 페이지 연동
성과 및 느낀 점
●	OAuth는 연동만으로 끝나지 않고, CSRF·예외·토큰 폐기까지 맞춰야 로그인 흐름이 안정된다는 것을 체감
●	Spring Security가 없는 NestJS에서도 Guard·Strategy·Filter로 인증 경계를 직접 나눔
●	monorepo에서 shared DTO로 FE·BE 계약을 맞추며, 타입을 문서처럼 쓰는 습관을 키움
링크
●	https://github.com/team-croffle/cosider
3. Croffle
작업 기간	프로젝트 전체 26.01 ~ 26.03 / 본인 참여 26.02 ~ 26.03
개요	Electron 기반 데스크톱 생산성 앱. 일정·할 일과 플러그인 구조를 제공
인원	7명 (중간 합류)
환경	Electron, Vue3, TypeScript, Vite, Node.js, SQLite, TypeORM

담당 역할
●	중간 합류. Electron Main Process에서 검색 API와 OS 네이티브 기능 연동
문제 해결 및 구현
●	Renderer가 OS API에 직접 접근하면 보안 모델이 깨지므로, 검색·알림은 Main에서 처리하고 Renderer는 IPC로만 요청·결과를 주고받게 맞춤
●	익숙하지 않은 코드베이스를 처음부터 다 읽지 않고, 맡은 구간의 데이터 흐름부터 따라가 기능을 붙임
성과 및 느낀 점
●	중간 합류 시 전체 코드를 순서대로 읽기보다, 핵심 경로의 데이터 흐름부터 파악하는 쪽이 빨랐음
●	Main/Renderer 분리와 IPC 제약(무엇을 Renderer에서 하면 안 되는지)을 실제 구현으로 익힘
링크
●	https://github.com/team-croffled/croffle
 
자기소개서
직무 성장과정
고등학교 2학년 때, 친구가 C언어로 콘솔에 테트리스를 구현하는 것을 보며 프로그래밍에 흥미를 갖게 되었습니다. 픽셀 단위로 동작하는 프로그램이 어떻게 만들어지는지가 궁금했고, 그 궁금증이 컴퓨터공학 진학으로 이어졌습니다.
진학 후에는 수학·물리 등 비프로그래밍 과목 비중이 커 학습 방향을 스스로 잡았습니다. Python, Java, JavaScript, Flutter를 유튜브와 개인 프로젝트로 익히며 여러 언어를 써 보았습니다. 당시에는 특정 직무보다 “만들 수 있다”는 것 자체가 재미였습니다.
군 전역 후 Flutter와 Firebase 팀 프로젝트로 감각을 다시 잡던 중, Comento IT 직무 체험에서 백엔드로 방향을 정했습니다. AWS Lambda로 서버 로직을 만들고, API Gateway로 REST API를 붙이고, EC2와 VPC로 네트워크를 구성해 보았습니다. 멘토가 이것이 백엔드 일이라고 했을 때, 다른 직무보다 이 일이 가장 재미있다고 느꼈습니다.
이후 부트캠프와 팀 프로젝트에서는 동작하는 구현을 넘어, 왜 이 기술을 쓰는지부터 맞춰 보려 했습니다. Re:View에서 MyBatis와 MinIO를 고른 이유가 그 습관의 결과입니다.
Re:View에서는 팀장을 맡아 일정, 문서, 기술 선택 합의를 맡았습니다. 팀원 사이에 정보와 이해가 어긋나는 것이 협업을 가장 많이 흔든다는 것을 그때 체감했습니다. 기능만큼이나 같은 이유를 공유하는 일이 중요하다는 것이 팀장으로서의 배움입니다.
Croffle에는 개발이 진행 중인 팀에 중간 합류해, 처음 보는 Electron 구조에서 Main Process를 맡았습니다. 익숙하지 않은 코드는 핵심 기능의 데이터 흐름부터 따라가며 적응했습니다. 특정 스택에 묶이지 않고 구조를 읽는 쪽이 실무에서 더 자주 필요하다는 것을 그때 느꼈습니다.
이후 Cosider에서는 NestJS로 인증 도메인을 맡았습니다. Access·Refresh Token과 Google·GitHub OAuth, CSRF state 검증까지 직접 맞추며, Spring 밖에서도 인증 경계를 설계하는 경험을 쌓고 있습니다.
기술은 바뀌지만, 문제를 나누고 근거를 남기는 방식은 잘 바뀌지 않는다고 생각합니다. 그 방식으로 꾸준히 성장하는 백엔드 개발자가 되고 싶습니다.
직무상 강점
구조적 사고와 기술 선택의 근거
기능을 붙이기 전에 서비스 목적에 맞는 구조인지를 먼저 맞춰 보려 합니다. Re:View에서 ORM을 논의할 때 JPA의 생산성과 MyBatis의 SQL 제어를 비교했고, 추천 조회처럼 쿼리를 직접 다루는 구간이 있어 MyBatis를 쓰기로 했습니다. 이미지 스토리지는 비용과 로컬 환경을 이유로 S3 대신 MinIO를 썼습니다.
같은 습관으로 버그도 고쳤습니다. 리뷰 수정 API에서 presigned URL과 object key가 섞여 이미지가 사라지자, 저장값과 응답값의 역할을 나눠 재발을 막았습니다. 팀원이 만든 추천 조회는 단순 매칭에 그치지 않도록 적합도·평점 유사·검증 가중을 나누고, ROW_NUMBER() PARTITION BY로 상품당 한 건만 남기게 SQL을 정리했습니다. 포인트는 동시 요청 시 잔액이 어긋날 수 있어 SELECT FOR UPDATE와 트랜잭션 경계로 막았습니다. 동작하는 것에서 멈추지 않고, 같은 문제가 다시 생기지 않게 경계를 두는 쪽이 제 개발 방식입니다.
협업
Re:View에서 팀장을 하며 일정과 기술 문서, 선택지 비교를 맡았습니다. 설계가 갈릴 때는 장단점을 문서로 공유하고 근거로 합의를 맞췄습니다. 기술적 결정에는 논리와 팀 신뢰가 같이 필요하다는 것을 그때 배웠고, 팀장 입장에서 팀원에게 무엇이 필요한지도 같이 보게 되었습니다.
직무 전문성을 키우기 위해 노력한 경험
커리큘럼 밖의 기술도, 서비스에 필요하다고 판단하면 찾아 적용했습니다. Re:View의 Docker와 MinIO가 그랬습니다. 도입할 때는 쓰는 것 자체보다 목적에 맞는지를 먼저 봤습니다.
AWS는 Comento에서 Lambda·API Gateway·EC2·VPC를 구성해 보며 네트워크 단위를 익혔고, Re:View에서는 EC2에 Compose 스택을 올려 배포까지 이었습니다. Spring Boot API는 Controller·Service·Mapper와 DTO로 계층을 나눠, 데이터 접근과 전달 경로가 엉키지 않게 가져가려 했습니다.
Croffle에서는 낯선 Electron 코드베이스를 처음부터 다 읽지 않고, 맡은 기능의 데이터 흐름부터 따라갔습니다. 새 스택을 만날 때 언어 숙련도만큼 구조를 읽는 일이 중요하다는 것을 그 경험으로 확인했습니다.
Cosider에서는 NestJS·Passport 환경에서 JWT와 OAuth CSRF를 직접 다루며, 익숙한 Spring이 아니어도 인증·세션 정책을 서버에서 맞출 수 있는지 확인하려 했습니다.
프로젝트 종료 후에는 기능을 더하지 않고, API를 공개 / 로그인 / 관리자로 나눠 인가를 맞추고 전역 예외 응답을 정리했습니다. Compose 이미지에서 .env 복사를 제거해 시크릿이 빌드에 들어가지 않게 했습니다.
지원동기 및 포부
[회사명]은 [회사 특성 한 줄 요약]을 통해 실질적인 문제를 해결하고 있습니다. [회사가 인상 깊었던 이유 한두 문장]. 구현에서 그치지 않고, 실제 환경의 문제를 분석하고 개선하는 데 기술을 쓴다는 점이 제 방향과 맞다고 생각해 지원했습니다.
백엔드를 고른 이유는 데이터 흐름과 서버 구조를 맞추는 일이 재미있었기 때문입니다. Comento에서 Lambda와 EC2·VPC를 다뤄 본 뒤 방향을 정했고, 부트캠프와 Re:View에서 API, 트랜잭션, 배포까지 범위를 넓혔습니다. Cosider에서는 NestJS로 인증·토큰·OAuth 경계를 직접 설계하며 스택을 확장하고 있습니다.
입사 후에는 맡은 도메인의 API와 데이터 정합성을 안정적으로 지키는 일부터 쌓고 싶습니다. 서비스가 커질수록 구조적 한계는 반드시 드러난다고 생각합니다. 그때 기능만 더하지 않고, 경계를 나눠 더 나은 구조를 제안할 수 있는 개발자로 성장하고 싶습니다.
