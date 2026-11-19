# CODE AUDIT REPORT — Re:View (`Re_View`)

**Audit date:** 2026-10-26 (Asia/Seoul)  
**Repository HEAD inspected:** `c95dd83d` (`main`, working tree clean at audit start)  
**Auditor role:** Evidence-based static/code-structure audit (read-only; no application code changes)

**Evidence classes used throughout:**
- **Verified fact** — directly observed in source, config, docs, or command output
- **Evidence-based interpretation** — reasonable conclusion from verified facts
- **Hypothesis** — plausible but unconfirmed
- **Unknown** — cannot be established from available materials

---

## 1. Executive Summary

This repository is a team-built Baumann-skin-type cosmetics review/commerce application. The backend is a Spring Boot 3.5.6 / Java 21 / MyBatis / Oracle / Spring Security (session + CSRF) service with MinIO presigned image URLs, Docker Compose packaging, and a GitHub Actions self-hosted deploy workflow. A React frontend lives under `View/`.

**What the codebase demonstrably shows**

- A conventional layered backend (controller → service → MyBatis mapper) covering auth, products, reviews, orders, search, recommendations, points, cart/wishlist/payments, admin APIs, and image URL issuance.
- Non-trivial SQL for Baumann-weighted recommendations and monthly “best review” selection.
- Explicit concurrency-oriented patterns for points (`SELECT … FOR UPDATE`) and stock (`UPDATE … AND stock >= :qty`).
- Session authentication, role matchers for admin routes, cookie CSRF, BCrypt passwords, and a global `@RestControllerAdvice` exception layer.
- Portfolio-oriented documentation (`README.md`, `logs/`) that largely matches the owned-scope code paths it cites, and that also candidly lists known limitations (`anyRequest().permitAll()`, best-point check ordering, thin tests).

**Most important confirmed or high-confidence issues**

| ID | Summary | Severity | Confidence |
| --- | --- | --- | --- |
| SEC-001 | Default-permit fallback: `.anyRequest().permitAll()` | High | High |
| SEC-002 | Registration persists client-supplied `role` into `USER_TABLE` | High | High |
| SEC-003 | `/minio/test` remains publicly reachable under default-permit | Medium | High |
| CON-001 | Best-review point duplicate check runs before row lock | Medium | High |
| CON-002 | Admin “selected review” award has no duplicate guard and awards even on deselect path | Medium–High | High |
| TEST-001 | Main-branch automated tests are effectively a single empty `contextLoads` | High (quality risk) | High |
| DOC-001 | Internal troubleshooting notes still describe a dual MinIO client removed from `main` | Informational | High |

**Tests:** Not executed in this audit (see §10 and §2). Existence of test code was inspected; pass/fail is **Not Verified**.

**Resume file:** No resume/CV document was found in the repository. Claim checks in §15 use `README.md` and `logs/` as documentation sources only.

---

## 2. Audit Scope and Limitations

### Inspected (directly opened or searched)

| Area | Paths / commands |
| --- | --- |
| Root / build | `pom.xml`, `package.json`, `Dockerfile`, `README.md`, `.gitignore` |
| Git | `git status`, `git branch`, `git log`, `git shortlog`, path histories for key files |
| App entry | `ReViewApplication.java`, `application.properties` |
| Security / CORS / errors | `SecurityConfig.java`, `CorsConfig.java`, `ExceptionHandlers.java`, `Security_Util.java` |
| Auth / users | `UserController.java`, `UserUtilController.java`, `UserService.java`, `UserMapper.xml`, `UserInfoDTO.java` |
| Points / orders / stock | `PointService.java`, `PointMapper.xml`, `OrderService.java`, `OrdersMapper.xml`, `OrderController.java` |
| Recommendations | `RecommendationsService.java`, `RecommendationsMapper.xml`, `RecommendationsController.java` |
| Reviews / scheduler | `ProductReviewService.java`, `ProductreviewMapper.xml`, `ReviewScheduler.java`, `AdminReviewService.java` |
| Search | `HeaderSearchController.java`, `AdminSearchController.java`, `HeaderSearchService.java`, `HeaderSearchProductMapper.xml` |
| Images / MinIO | `ImageService.java`, `ImageController.java`, `ImageUploadController.java`, `MinioConfig.java`, `MinioProperties.java`, `MinioTestController.java` |
| Shopping APIs | `CartController.java`, `CartService.java`, `PointController.java`, `Payment_MethodController.java`, `Payment_MethodService.java`, `Payment_MethodMapper.xml` |
| Infra / CI | `infra/docker-compose.yml`, `infra/.env.example`, `.github/workflows/deploy.yml`, `View/nginx/default.conf` |
| Tests | `ReViewApplicationTests.java`, `application-test.properties` |
| Docs / notes | `README.md`, `logs/Other/ERD.md`, `logs/Other/endpointList.md`, `logs/Other/TS.md` (partial), `docs/images/*` (presence) |
| Structure scan | Full `src/main/java` and `src/main/resources/mapper` file listing (~175 Java sources, 28 mapper XMLs) |

### Not inspected or only sampled

- Most of `View/` React UI implementation (only nginx config and README-related claims).
- Every admin/service/mapper file line-by-line (admin product/user/QnA/report flows sampled via grep and selected files).
- Full contents of all `logs/Stable` / `logs/Optimize` notes.
- Binary ERD/architecture PNGs (presence noted; pixel content not OCR’d).
- External Notion portfolio, YouTube demo, or `Re_View_Doc` repository.
- Live Oracle schema / DDL (no `.sql` DDL found in this repo).
- Runtime behavior against a running stack (Compose/DB/MinIO not started for this audit).
- Production EC2, secrets contents, or GitHub Actions run logs.

### Explicit execution constraints followed

- No application source modifications other than creating this report.
- No dependency installs / Maven builds / test runs (would download artifacts and may alter local caches; audit procedure forbids package changes without approval).
- No destructive Git operations during analysis; branch creation for report delivery is separate from analysis.

**Limitation statement:** This audit is **not exhaustive**. Conclusions about “the project” refer to inspected paths. Absence of a finding outside the inspected set is **not** proof of absence.

---

## 3. Repository Structure and Main Execution Flows

### 3.1 Layout (verified)

```
/workspace
├── pom.xml / mvnw                 # Spring Boot backend
├── src/main/java/...              # Backend code (package com.review.shop + entry org.spring.project.re_view)
├── src/main/resources/mapper/**   # MyBatis XML
├── src/test/**                    # Minimal tests
├── View/                          # React frontend + nginx
├── infra/                         # docker-compose + .env.example
├── .github/workflows/deploy.yml
├── docs/images/                   # Architecture / ERD images
├── logs/                          # Internal notes (ERD, endpoints, troubleshooting)
└── README.md
```

**Git (verified at audit start):** branch `main`, clean working tree, tip `c95dd83d` (“Merge pull request #16 … remove-minio-public-url”).

### 3.2 Application entry

```8:15:src/main/java/org/spring/project/re_view/ReViewApplication.java
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.review.shop")
@MapperScan("com.review.shop.repository")
public class ReViewApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReViewApplication.class, args);
    }
}
```

### 3.3 Representative execution flows (traced in code)

**A. Session login**

1. `POST /api/auth/login` → `UserController.login`  
2. Load user + ban check + `passwordEncoder.matches`  
3. `AuthenticationManager.authenticate` + store `SecurityContext` in HTTP session  

**B. Personalized recommendations**

1. `POST /api/recommendations/all` (authenticated matcher)  
2. Resolve current user’s Baumann axes → `RecommendationsMapper.findRecommendProducts` / `findRecommendReviews`  
3. Convert image object keys → MinIO GET presigned URLs in service  

**C. Order placement**

1. `POST /api/orders` sets `user_id` from session (`OrderController`)  
2. `OrderService.processOrder` `@Transactional`: `pointService.usePoint` → conditional stock deduct → insert order/items  

**D. Monthly best reviews**

1. `ReviewScheduler` cron `0 0 0 1 * *` `Asia/Seoul`  
2. `ProductReviewService.updateBestReviews`: reset flags → select top 10% → set flags → `addBestReviewPoint`  

**E. Image upload (presigned)**

1. Client requests PUT URL (+ object key) from image APIs  
2. Client uploads binary to MinIO  
3. Domain APIs persist object key; read APIs mint short-lived GET URLs  

---

## 4. Technology Stack and Verified Usage

| Technology | Declared / configured | Verified usage in this repo |
| --- | --- | --- |
| Java 21 | `pom.xml` `<java.version>21</java.version>` | Source uses Java language features; Dockerfile uses Temurin 21 |
| Spring Boot 3.5.6 | parent POM | Controllers, Security, Mail, JDBC starters present |
| Spring Security | dependency + `SecurityConfig` | Session login, CSRF cookie repo, matchers, BCrypt |
| Spring Validation | dependency | Used on auth DTOs (`UserInfoDTO`, login/password/temp-password); **not** broadly on cart/order/review controllers |
| MyBatis 3.0.5 starter | POM + `mapper/**/*.xml` | Primary persistence mechanism |
| Oracle JDBC | `ojdbc11` + `application.properties` | Datasource URL placeholders; Oracle SQL dialect in mappers (`FETCH FIRST`, `SYSDATE`, `DBMS_RANDOM`) |
| MinIO Java SDK 8.6.0 | POM + `MinioClient` bean | Presigned PUT/GET issuance |
| springdoc OpenAPI | POM | Swagger paths permitted in SecurityConfig |
| H2 (test) | test scope | Referenced in `application-test.properties` only |
| React frontend | `View/` | Present; not deeply audited |
| Docker Compose | `infra/docker-compose.yml` | minio, oracle-xe, be, fe services |
| GitHub Actions | `deploy.yml` | self-hosted runner; secrets → `.env`; compose rebuild |

**Not verified as runtime-proven here:** actual Oracle connectivity, MinIO bucket existence in deploy, mail delivery, or EC2 runner health.

---

## 5. Feature-Level Implementation Analysis

### 5.1 Baumann-weighted recommendations — **Implemented (code)**

**Evidence:** `RecommendationsMapper.xml` product score (30/10/0 per axis + review-count bonus, threshold ≥ 40, `FETCH FIRST 16`); review score with author/product axes, rating similarity, `is_checked` ×1.2, `ROW_NUMBER() PARTITION BY product_id` then `rn = 1`.

**Verified observation:** SQL implements the weighting scheme described in README §3-1.  
**Interpretation:** This is a real, project-specific ranking feature—not a framework default.  
**Not demonstrated:** Ranking quality on production data, latency, or A/B outcomes.  
**Confidence:** High (implementation), Unknown (effectiveness).

### 5.2 Presigned MinIO image pipeline — **Implemented (code), dual-client deferred**

**Evidence:** `ImageService.presignedUrlPost` / `presignedUrlGet`; single `MinioClient` in `MinioConfig`; README and commit `d6d2c680` state public-url split removed/deferred.

**Verified observation:** BE issues URLs; DB is expected to store object keys; review update filters values starting with `http` via `filterObjectKeys`.  
**Gap:** `ImageController` `convert-data(s)` accepts client `folder` and skips the role checks used by `uploadReviewImages` / `uploadProductImages`.  
**Confidence:** High.

### 5.3 Points + row lock — **Implemented with known ordering flaw**

**Evidence:** `PointMapper.xml` `SELECT point … FOR UPDATE`; `PointService.updateUserPoint` under `@Transactional`; reward constants 100/500/700.

**Verified observation:** Lock-then-compute-update pattern exists. Best-reward duplicate check precedes lock (README acknowledges).  
**Confidence:** High.

### 5.4 Stock deduction concurrency pattern — **Implemented**

**Evidence:** `OrdersMapper.xml` `UPDATE PRODUCT SET stock = stock - #{quantity} WHERE … AND stock >= #{quantity}`; `OrderService` treats `affected == 0` as insufficient stock.

**Verified observation:** Conditional update avoids TOCTOU of “read then write” for stock. Unused `stockMap` remains after a prior read query (maintainability noise, not a functional proof of failure).  
**Confidence:** High for pattern presence; concurrent correctness **Not Verified** by test.

### 5.5 Search — **Implemented**

**Evidence:** Keyword length ≥ 2 in header/admin controllers; MyBatis `#{}` binds; sort via `<choose>/<when>` fixed columns; admin order search filters status/sort in Java after SQL.

**Confidence:** High for existence; performance under large datasets Unknown.

### 5.6 Monthly best-review batch — **Code present; scheduling enabled**

**Evidence:** `selectBestReviewIds` (likes > 20, product reviews > 30, top 10% per product); `@EnableScheduling` on application class; cron monthly Seoul midnight.

**Not verified:** Job has run successfully in production; timezone/host clock behavior.

### 5.7 AuthN/Z hardening (2026.05 claims) — **Partially implemented**

**Evidence:** Matcher lists, JSON 401/403 handlers, CSRF cookie config, validation on auth DTOs, `@RestControllerAdvice`.  
**Also verified:** final `.anyRequest().permitAll()` and README admission that off-list APIs remain open; security tests claimed on another branch (`testcode`) — **not present on `main`**.

---

## 6. Architecture and Code Organization

### Finding ARCH-001 — Layered package structure

- **Category:** Architecture  
- **Evidence:** Packages `controller/`, `service/`, `repository/`, `dto/`, `config/`, `exception/`, `image/`, `util/` under `com.review.shop`.  
- **Verified observation:** Request handling, business rules, and SQL are generally separated.  
- **Interpretation:** Consistent with a maintainable Spring/MyBatis teaching/portfolio style.  
- **Impact:** Supports onboarding and ownership mapping in README “code map”.  
- **Confidence / Severity / Status:** High / Informational / Verified  

### Finding ARCH-002 — Inconsistent validation boundary

- **Category:** Architecture  
- **Evidence:** `@Valid` appears on auth controllers; cart/order/review/admin endpoints largely use bare `@RequestBody` (grep across `*Controller.java`). Service methods sometimes re-validate (e.g., review content/rating in `ProductReviewService`).  
- **Interpretation:** Auth path shows Bean Validation skill; other domains rely on ad hoc checks → uneven API contract enforcement.  
- **Impact:** Invalid payloads may reach mappers or produce inconsistent error shapes.  
- **Confidence / Severity / Status:** High / Medium / Verified  

### Finding ARCH-003 — Method-security annotation likely inert

- **Category:** Architecture / Security  
- **Evidence:** `AdminReviewService` uses `@RolesAllowed("ADMIN")`; no `@EnableMethodSecurity` observed in inspected config classes; URL rules use `.hasRole("ADMIN")` for `/api/admin/**`.  
- **Interpretation:** Effective admin protection for mapped admin URLs likely comes from HTTP security, not method annotations.  
- **Impact:** Developers may believe method security is active when it is not.  
- **Confidence:** Medium (absence of enablement in inspected files; full classpath not exhaustively proven).  
- **Severity:** Low–Medium / Partially Verified  

### Finding ARCH-004 — Team authorship is multi-developer

- **Category:** Architecture / Capability context  
- **Evidence:** `git shortlog` shows multiple authors (e.g., VenyVince/Veny, LeeJaeBin, others). README states 6-person team; frontend mostly teammates.  
- **Interpretation:** Repository alone cannot attribute every line to one person.  
- **Impact:** Capability claims must be scoped to owned paths + commit evidence, not whole-repo authorship.  
- **Confidence / Status:** High / Verified  

---

## 7. Database Design and SQL Analysis

### Verified sources

- Mapper XML statements (28 files)  
- `logs/Other/ERD.md` (mapper-derived summary)  
- README-linked `docs/images/erd-*.png` (files exist; DDL not extracted)  

**No Oracle DDL scripts were found in this repository.** Production constraints (PK/FK/unique/indexes) are therefore **Unknown** beyond what SQL implies.

### Finding DB-001 — Core relational model is coherent in mappers

- **Evidence:** ERD notes + joins among `USER_TABLE`, `BAUMANN`, `PRODUCT`, `PRODUCT_IMAGE`, `ORDERS`/`ORDER_ITEM`, `REVIEW`/`REVIEW_IMAGES`/`REVIEW_COMMENT`, `POINT_HISTORY`, cart/wishlist/payment tables.  
- **Verified observation:** Application SQL assumes these relationships.  
- **Cannot claim:** Full normalization, index optimality, or FK enforcement without DDL.  
- **Confidence / Status:** High for mapper usage / Not Verified for physical schema  

### Finding DB-002 — Parameter binding practice reduces classic SQLi surface

- **Evidence:** Workspace-wide mapper search found **no** `${...}` interpolations; dynamic SQL uses `#{}` and MyBatis `<if>/<choose>`.  
- **Interpretation:** Keyword/filter inputs are bound as parameters in inspected search SQL.  
- **Impact:** Low likelihood of classic MyBatis string-interpolation injection in inspected XMLs.  
- **Confidence / Severity / Status:** High / Informational (positive) / Verified for searched XMLs  

### Finding DB-003 — Recommendation / best-review SQL is non-trivial Oracle SQL

- **Evidence:** Lateral joins, analytic `ROW_NUMBER`, score `CASE` expressions, `DBMS_RANDOM.VALUE` for admin-pick.  
- **Interpretation:** Demonstrates comfort writing ranking SQL rather than only CRUD.  
- **Not demonstrated:** Explain-plan efficiency or index use.  
- **Confidence:** High  

### Finding DB-004 — Payment card numbers stored/retrieved as plain fields

- **Evidence:** `Payment_MethodMapper.xml` inserts/selects `card_number` with no tokenization/masking in SQL.  
- **Interpretation:** Sensitive payment-like data appears stored in application DB as ordinary columns.  
- **Impact:** Potential PCI/privacy exposure if real card data were used (unknown whether demo numbers only).  
- **Confidence / Severity / Status:** High for storage shape / Medium severity pending real-data confirmation / Verified code path  

---

## 8. Transaction and Concurrency Analysis

### Finding CON-001 — Best-review duplicate check before lock

- **Category:** Concurrency  
- **Evidence:**

```136:145:src/main/java/com/review/shop/service/userinfo/other/PointService.java
    public void addBestReviewPoint(int user_id, int review_id) {
        try {
            // 중복 지급 여부
            boolean paid = pointMapper.checkBestReviewPoint(user_id, review_id, PointConstants.BestREVIEW, "EARN");
            if (paid) return;

            processPoint(user_id, review_id, PointConstants.BestREVIEW, "EARN", "Best 리뷰 선정 보상");
```

`processPoint` → `updateUserPoint` → `selectForUpdate` (lock). README “한계” states the same ordering issue.

- **Verified observation:** Duplicate detection is not performed under the same locked read as the balance update.  
- **Interpretation:** Concurrent scheduler/admin paths could double-award before either inserts history (**Hypothesis** without a race test).  
- **Impact:** Potential duplicate +700 point grants.  
- **Confidence:** High for ordering; Medium for exploitability in production.  
- **Severity:** Medium  
- **Status:** Partially Verified (code ordering verified; race not reproduced)  
- **Recommendation:** Check-or-insert unique constraint on `(user_id, review_id, amount, type)` (or equivalent) **after** `FOR UPDATE`, or lock user row before the duplicate query.

### Finding CON-002 — Admin selected-review points: no duplicate guard; awarded on both select values

- **Evidence:**

```36:48:src/main/java/com/review/shop/service/admin/AdminReviewService.java
    public void setReviewSelection(int review_id, Integer is_selected) {
        ...
        int affected = adminReviewMapper.setReviewSelection(review_id, is_selected);
        ...
        int user_id = adminReviewMapper.findReviewer(review_id);
        pointService.addSelectedReviewPoint(user_id, review_id);
    }
```

`addSelectedReviewPoint` always earns +500 with no `check*` analogue.

- **Verified observation:** Deselect (`is_selected = 0`) still calls award; re-select can award again.  
- **Impact:** Incorrect point inflation / reward on unselect.  
- **Confidence / Severity / Status:** High / High (logic defect) / Verified in code  
- **Recommendation:** Award only when transitioning to selected; add idempotency; optionally claw back on unselect.

### Finding CON-003 — Point row lock pattern is real

- **Evidence:** `PointMapper.xml` lines 22–27 `FOR UPDATE`; `OrderService.processOrder` joins point use + stock + inserts in one `@Transactional` method.  
- **Interpretation:** Author understood lost-update risk for balances.  
- **Not demonstrated:** Isolation level tuning, deadlock handling, or load-test proof.  
- **Confidence:** High for implementation intent  

### Finding CON-004 — Stock conditional update is a sound pattern

- **Evidence:** `OrdersMapper.xml` `deductStock`.  
- **Status:** Verified pattern; concurrent correctness Requires Additional Evidence (no concurrency tests).

### Finding CON-005 — Admin review delete lacking class-level transaction wrapping

- **Evidence:** `AdminReviewService.deleteReview` soft-deletes then calls `removeReviewPoint` without `@Transactional` on that method.  
- **Interpretation:** If point removal fails after delete, partial failure is possible unless a broader transaction exists (**Hypothesis**).  
- **Confidence:** Medium / Severity: Medium / Partially Verified  

---

## 9. Security Findings

### SEC-001 — Default allow for unmatched requests

- **Category:** Security  
- **Evidence:** `SecurityConfig.java` line 112 `.anyRequest().permitAll()`. README acknowledges this.  
- **Verified observation:** Any endpoint not matched earlier is publicly authorized at the filter chain.  
- **Conditions:** Attacker discovers an unlisted mutating API (examples below).  
- **Impact:** Authorization bypass for off-list routes.  
- **Confidence / Severity / Status:** High / High / Verified  
- **Recommendation:** Deny-by-default (`.anyRequest().authenticated()` or `.denyAll()`), then explicitly open public GETs.

**Related open surfaces under default-permit (verified paths exist, not in authenticated/admin lists):**

- `GET /minio/test` (`MinioTestController`)  
- `POST /api/images/reviews`, `POST /api/images/products` (`ImageUploadController`) — service attempts role checks via `Security_Util`, but filter allows anonymous entry  
- Swagger UI (explicitly `permitAll` — intentional exposure risk)

### SEC-002 — Client-controlled role on registration

- **Category:** Security  
- **Evidence:** `UserInfoDTO.role` has no server-side force-to-`USER`; `UserService.registerUser` passes `userDTO.getRole()`; `UserMapper.xml` inserts `#{role}`.  
- **Observed behavior:** A crafted register JSON may set `role=ADMIN` if DB accepts it.  
- **Conditions:** Open registration endpoint; no DB check/constraint forcing `USER` (constraint Unknown).  
- **Impact:** Privilege escalation to admin APIs (`/api/admin/**`).  
- **Confidence:** High for code path; Medium for production exploitability (depends on DB defaults/constraints/FE-only assumptions).  
- **Severity:** High  
- **Status:** Verified code risk / Requires Additional Evidence for live exploit  
- **Recommendation:** Ignore client role; hardcode `USER` in service; constrain column; integration test.

### SEC-003 — MinIO connectivity test endpoint

- **Evidence:** `MinioTestController` `@GetMapping("/minio/test")` + default-permit.  
- **Impact:** Information about bucket reachability; unnecessary attack surface.  
- **Severity:** Medium / Confidence: High / Verified  
- **Recommendation:** Remove from production builds or protect with admin + profile flag.

### SEC-004 — Presigned URL issuance with client-chosen folder (authenticated)

- **Evidence:** `ImageController` `convert-data` / `convert-datas` pass `folder` into `presignedUrlPostWithDTO` without role/folder allow-list; these routes **are** in the authenticated matcher list.  
- **Impact:** Authenticated non-admin users may obtain PUT URLs under arbitrary key prefixes (e.g., `products/...`), bypassing `uploadProductImages` role gate.  
- **Confidence / Severity / Status:** High / Medium / Verified  
- **Recommendation:** Map endpoints to fixed folders; reuse role-checked service methods; validate content-type/extension.

### SEC-005 — Exception messages returned to clients

- **Evidence:** `ExceptionHandlers.handleGlobalException` puts `e.getMessage()` into `ErrorResponseDTO`.  
- **Impact:** Potential information disclosure (SQL/internal details) depending on thrown exceptions.  
- **Severity:** Low–Medium / Confidence: High / Verified  

### SEC-006 — CSRF configured for cookie SPA pattern

- **Evidence:** `CookieCsrfTokenRepository.withHttpOnlyFalse()`, login path ignored, `CsrfTokenRequestAttributeHandler` with null attribute name (common SPA header pattern).  
- **Interpretation:** CSRF is intentionally enabled rather than blindly disabled.  
- **Not verified:** Frontend always sends token correctly for all state-changing calls.  
- **Confidence:** High for backend config  

### SEC-007 — Secrets handling (config hygiene)

- **Evidence:** Datasource/MinIO via env placeholders; `infra/.env.example` uses placeholders; Dockerfile does **not** copy `.env` (matches README). Mail password uses placeholder `${your_app_password_here}`; SMTP usernames appear hardcoded in properties files.  
- **Impact:** Hardcoded mailbox identifiers are exposure of personal/project emails; real secrets appear intended for env/GitHub Secrets.  
- **Severity:** Low (usernames) / Unknown for whether real secrets ever committed historically  
- **Status:** Verified for current tree placeholders  

**Note:** No live secret values are reproduced in this report.

### SEC-008 — XSS

- **Status:** Insufficient evidence from backend alone. React escaping behavior in `View/` not audited. Stored content fields (`content`, QnA) exist; XSS risk is **Hypothesis** pending FE review.

---

## 10. Testing and Verification Results

### What exists (verified)

| Artifact | Observation |
| --- | --- |
| `ReViewApplicationTests` | Single `@Test void contextLoads() {}` |
| `@ActiveProfiles("Test")` | Profile name `Test` |
| `application-test.properties` | File name uses lowercase `test`; H2 URL; Oracle driver class name still set; MinIO dummy values |
| POM | `spring-boot-starter-test`, `mockito-core`, `spring-security-test`, MyBatis test starter |
| Dockerfile | `mvn clean package -Dmaven.test.skip=true` |

### Execution in this audit

**No tests were executed.**  
Therefore: pass/fail, coverage %, and context-load success are **Not Verified**.

### Interpretation

- **Test code exists:** yes (minimal).  
- **Behavior verified by tests:** no meaningful domain assertions on `main`.  
- README claim that main tests are `contextLoads()` level: **Supported**.  
- README claim that security tests live on `testcode` branch: **Not Verified** here (branch not checked out).

### TEST-001

- **Severity:** High for engineering assurance (not a runtime prod outage by itself)  
- **Recommendation:** Add focused unit/integration tests for points locking order, stock deduct, registration role forcing, and Security filter deny-by-default; fix profile naming; stop skipping tests in Docker builds once green.

---

## 11. Performance and Operational Observations

### PERF-001 — Possible N+1 / repeated MinIO signing

- **Evidence:** Loops calling `imageService.presignedUrlGet` per product/review in recommendations, search, cart, product reviews.  
- **Classification:** Potential inefficiency pattern, **not** a measured latency problem.  
- **Confidence:** High for pattern / Unknown for user impact  

### PERF-002 — Admin order search filters in memory

- **Evidence:** `HeaderSearchService.searchOrders` loads via LIKE query then filters/sorts in Java streams.  
- **Impact:** May scale poorly with order volume (**Hypothesis**).  

### OPS-001 — Compose + Actions deploy path exists

- **Evidence:** `docker-compose.yml` services; `deploy.yml` writes secrets to `infra/.env` and `View/.env.production`, `compose down/build/up`.  
- **Verified:** Automation scripts present.  
- **Not verified:** Successful production deploys, rollback, monitoring, healthchecks (Compose has `restart: always` but no healthcheck blocks observed).  

### OPS-002 — Observability

- **Evidence:** `logging.level.com.review.shop=INFO`; `DatabaseException` logs stack traces.  
- **Missing from repo evidence:** metrics, tracing, structured alert config.  

### OPS-003 — MyBatis cache enabled

- **Evidence:** `mybatis.configuration.cache-enabled=true` in application properties.  
- **Interpretation:** Local cache settings exist; correctness under updates depends on flush usage (**Unknown** without mapper cache config review).  

---

## 12. Evidence-Based Strengths

| ID | Strength | Evidence | Why it matters | Confidence |
| --- | --- | --- | --- | --- |
| STR-001 | Domain SQL for recommendations | `RecommendationsMapper.xml` | Shows ability to encode product rules in SQL | High |
| STR-002 | Concurrency awareness (points + stock) | `FOR UPDATE`; conditional stock update; comments in `OrderService` | Not only CRUD; anticipates races | High |
| STR-003 | Presigned object-storage pipeline | `ImageService`, MinIO config | Separates binary upload from app servers | High |
| STR-004 | Session+CSRF SPA security choices documented and coded | `SecurityConfig`, README rationale | Conscious tradeoff vs JWT | High |
| STR-005 | Honest limitation writing | README “한계”, `logs/Other/TS.md` self-critique | Reduces resume overclaim risk | High |
| STR-006 | Env-injected deploy, no `.env` in image | Dockerfile, compose `env_file`, Actions secrets | Basic secret-hygiene improvement | High |
| STR-007 | Ownership-scoped user APIs | Cart/points/orders set `user_id` from `Security_Util` | Reduces trivial IDOR on those controllers | High (for inspected controllers) |
| STR-008 | Global error DTO + auth validation | `ExceptionHandlers`, auth `@Valid` DTOs | Baseline API consistency on auth path | High |

---

## 13. Confirmed Issues and Potential Risks

### Confirmed issues (code-verified defects or unsafe defaults)

1. **SEC-001** Default-permit authorization fallback  
2. **SEC-002** Registration role taken from client  
3. **SEC-003** Public MinIO test endpoint  
4. **SEC-004** Authenticated arbitrary folder presign  
5. **CON-001** Best-point duplicate check ordering (acknowledged)  
6. **CON-002** Selected-review point award logic incorrect/idempotent-unsafe  
7. **TEST-001** No meaningful automated regression suite on `main`  
8. **ARCH-002** Validation mostly limited to auth DTOs  

### Potential risks (need runtime/FE/DDL confirmation)

- Payment card plaintext storage misuse if real PANs used (DB-004)  
- Exception message leakage (SEC-005)  
- Dual-write failure windows on non-transactional admin delete (CON-005)  
- XSS in rich text fields (SEC-008)  
- Profile mismatch may break even `contextLoads` (`Test` vs `application-test.properties`)  

### Documentation conflicts

- **DOC-001:** `logs/Other/TS.md` still documents dual MinIO clients / `MINIO_PUBLIC_URL`; current `main` uses one client and removed the env example (`d6d2c680`). README correctly describes deferral. Treat TS.md as historical notes, not current architecture.

---

## 14. Prioritized Recommendations

1. **P0 — Deny-by-default security** (SEC-001): flip `anyRequest` policy; inventory every endpoint against `logs/Other/endpointList.md`.  
2. **P0 — Force role=`USER` on register** (SEC-002) + DB check constraint.  
3. **P0 — Fix admin selection point logic** (CON-002): award only on 0→1; add idempotency.  
4. **P1 — Move best-point duplicate check under lock / unique constraint** (CON-001).  
5. **P1 — Remove or lock down `/minio/test` and unused image upload routes** (SEC-003/004).  
6. **P1 — Expand `@Valid` to order/cart/review/payment DTOs** (ARCH-002).  
7. **P1 — Add regression tests** for authz, points, stock; stop `-Dmaven.test.skip=true` once green (TEST-001).  
8. **P2 — Align internal docs** with single MinIO client (DOC-001).  
9. **P2 — Mask/tokenize payment fields**; confirm demo-only data (DB-004).  
10. **P2 — Measure recommendation/search/presign latency** before claiming performance work (PERF-*).

---

## 15. Resume and Documentation Consistency

**Resume/CV file in repository:** Not found.  
Claim-level resume verification **requires a separate resume document**.

Below: material claims from **`README.md`** (primary portfolio doc) vs implementation.

| Claim (README) | Evidence | Verdict | Notes / safer wording |
| --- | --- | --- | --- |
| Java 21, Spring Boot 3, Security session+CSRF, MyBatis, Validation, Swagger | `pom.xml`, `SecurityConfig`, mappers, springdoc | **Fully supported** | Validation usage is partial (auth-heavy) |
| Oracle XE + MinIO Presigned URL | compose, ImageService, properties | **Fully supported** (config/code); runtime **Not Verified** | |
| Docker Compose + nginx `/api` proxy + Actions self-hosted | compose, `View/nginx/default.conf`, `deploy.yml` | **Fully supported** as configs | Production success not verified |
| Weighted Baumann recommendation SQL + product-one-review | `RecommendationsMapper.xml` | **Fully supported** | Effectiveness not measured |
| Point update uses `SELECT … FOR UPDATE` | `PointMapper.xml`, `PointService` | **Fully supported** | Concurrent proof not tested |
| Best-review monthly scheduler; `@EnableScheduling` fixed | `ReviewScheduler`, `ReViewApplication` | **Fully supported** in code | Ops execution not verified |
| Image pipeline stores keys; filters `http` on review update | `ImageService`, `filterObjectKeys` | **Fully supported** | Heuristic is brittle (README admits) |
| `anyRequest().permitAll()` remains | `SecurityConfig:112` | **Fully supported** (limitation) | Honest |
| Best point duplicate check before lock | `addBestReviewPoint` | **Fully supported** (limitation) | Honest |
| Main tests ≈ `contextLoads` | test sources | **Fully supported** | |
| Security tests on `testcode` only | not present on `main` | **Not verifiable here** | Need that branch |
| Personal scope: search, member, cart/wish/pay, points, recommend, scheduler, images, security, infra | Controllers/services/mappers + git history samples | **Partially supported** | Multi-author repo; individual contribution ratios Unknown |
| “동시성 해결” for points | lock code + TS notes; no race tests | **Partially supported** | Prefer “도입/적용” over “해결 입증” |
| Dual MinIO public/internal client | removed on main; TS.md outdated | **Unsupported as current state** | README correctly says deferred |

**External links** (Notion, YouTube, docs repo): existence of URLs verified in README text only; contents **Not Verified**.

---

## 16. Developer Capability Evidence Matrix

Scoped primarily to README-claimed owner **Veny (김석현)** using owned-path evidence + commit samples. This is **repository evidence**, not a personal skill certificate.

| Capability | Supporting evidence | Demonstrates | Does **not** demonstrate | Remaining questions |
| --- | --- | --- | --- | --- |
| Implementation ability | End-to-end features in points, search, recommendations, images, deploy configs | Can ship multi-file backend features in Spring/MyBatis | Solo authorship of every related line; interview explanation quality | Which PRs were self-authored vs pair/agent-assisted? |
| Backend API design | Controllers + DTO + service boundaries; session auth flows | Practical REST+session design for SPA | Uniform validation/versioning/HATEOAS maturity | Consistency of error contracts across all domains |
| Database/SQL | Complex analytic SQL; parameterized MyBatis | Solid SQL expressiveness for rankings | Schema design ownership; indexing strategy under load | Where is authoritative DDL maintained? |
| Transactions/concurrency | `@Transactional`, `FOR UPDATE`, conditional stock update; known leftover race | Awareness of concurrency hazards and partial mitigations | Proven correctness under contention | Any load/race tests elsewhere? |
| Security awareness | CSRF retention, matchers, JSON 401/403, BCrypt; also default-permit + role bind | Can implement and critique security; still leaves serious gaps | Production-hardening completeness | Was default-permit temporary or accepted residual risk? |
| Testing/debugging | Thin main tests; rich narrative troubleshooting docs | Debugging/documentation culture | Automated regression discipline on main | Does `testcode` branch contain meaningful suites? |
| Maintainability | Clear package map; some dead code (`stockMap`); naming inconsistencies | Generally navigable codebase | Long-term modularization/hexagonal purity | Plan to consolidate duplicate image controllers? |
| Deployment/ops | Compose, Dockerfile, Actions, env injection | Can operationalize a demo stack on EC2-style host | Monitoring/SRE practices | Deploy success rate / incident history? |
| Decision-making | README rationale (MyBatis for SQL control; MinIO vs S3; session vs JWT) | Articulates tradeoffs in writing | That every tradeoff was optimal | Product metrics validating recommendation approach? |

**Cannot be inferred from this repository alone**

- Exact personal skill level or ranking among candidates  
- Probability of job offer  
- Ability to explain under interview pressure  
- Full individual contribution percentage on shared files  

---

## 17. Unverified Areas and Required Follow-Up

1. Execute (in an isolated environment) unit/integration/security tests; fix profile naming; report real results.  
2. Checkout/compare `testcode` branch for claimed security tests.  
3. Obtain Oracle DDL and reconcile with mappers/ERD images.  
4. Dynamic authorization probe of every controller mapping vs `SecurityConfig` (especially image, util, leftover routes).  
5. Controlled concurrency tests for points (best reward) and stock.  
6. Confirm whether payment `card_number` values are synthetic.  
7. FE audit for CSRF header wiring and XSS sinks.  
8. Runtime verification of Compose stack, MinIO browser reachability with single client, and scheduler firing.  
9. Provide resume PDF/Notion export for claim-by-claim resume audit.  
10. Review agent-assisted commits (`Cursor Agent` appears in history) when attributing authorship.

---

## 18. Evidence Index

| Ref | Path | Used for |
| --- | --- | --- |
| E01 | `pom.xml` | Stack versions, test deps |
| E02 | `README.md` | Claims, limitations, ownership |
| E03 | `src/main/java/org/spring/project/re_view/ReViewApplication.java` | Scheduling enablement, scan base |
| E04 | `src/main/resources/application.properties` | Datasource/MinIO/mail/MyBatis config |
| E05 | `.../config/SecurityConfig.java` | Authz matchers, CSRF, 401/403, default permit |
| E06 | `.../config/ExceptionHandlers.java` | Global exception handling |
| E07 | `.../config/CorsConfig.java` | CORS credentials/origins |
| E08 | `.../controller/user/UserController.java` | Login/register/session |
| E09 | `.../service/user/UserService.java` | BCrypt encode/register/role pass-through |
| E10 | `mapper/user/UserMapper.xml` | Role insert |
| E11 | `.../dto/user/UserInfoDTO.java` | Validation + role field |
| E12 | `.../service/userinfo/other/PointService.java` | Point TX, best duplicate check order |
| E13 | `mapper/userinfo/other/point/PointMapper.xml` | `FOR UPDATE`, history, duplicate query |
| E14 | `.../service/order/OrderService.java` | Order TX orchestration |
| E15 | `mapper/order/OrdersMapper.xml` | Conditional stock deduct |
| E16 | `.../controller/orders/OrderController.java` | Session-bound `user_id` |
| E17 | `mapper/recommendations/RecommendationsMapper.xml` | Weighted recommendation SQL |
| E18 | `.../service/recommendations/RecommendationsService.java` | Presign conversion |
| E19 | `.../image/ImageService.java` | Presigned PUT/GET |
| E20 | `.../image/minio/MinioConfig.java` | Single MinIO client |
| E21 | `.../image/minio/MinioTestController.java` | `/minio/test` |
| E22 | `.../controller/image/ImageController.java` | Client `folder` presign |
| E23 | `.../service/review/ProductReviewService.java` | `filterObjectKeys`, best update |
| E24 | `mapper/review/ProductreviewMapper.xml` | Best-review selection SQL |
| E25 | `.../util/ReviewScheduler.java` | Cron schedule |
| E26 | `.../service/admin/AdminReviewService.java` | Selected-review points |
| E27 | `.../service/search/HeaderSearchService.java` | Search + admin order post-filter |
| E28 | `mapper/search/header/HeaderSearchProductMapper.xml` | Bound LIKE + sort choose |
| E29 | `infra/docker-compose.yml` | Runtime topology |
| E30 | `infra/.env.example` | Env contract (no secrets) |
| E31 | `.github/workflows/deploy.yml` | CI/CD steps |
| E32 | `Dockerfile` | Multi-stage build; tests skipped |
| E33 | `View/nginx/default.conf` | `/api` reverse proxy |
| E34 | `src/test/.../ReViewApplicationTests.java` | Only smoke test |
| E35 | `src/test/resources/application-test.properties` | Test profile file |
| E36 | `logs/Other/ERD.md` | Mapper-oriented schema notes |
| E37 | `logs/Other/endpointList.md` | Authz inventory notes |
| E38 | `logs/Other/TS.md` | Troubleshooting narratives (partially stale) |
| E39 | Git history / shortlog | Multi-author evidence; MinIO public-url removal commit |

### Quality-control notes (auditor self-check)

- Cited files were confirmed present; sampled line references were re-checked against file contents before finalization.  
- No test was marked passing.  
- No performance improvement percentages invented.  
- No numerical “candidate score” or hiring probability assigned.  
- Strengths and defects use the same evidence standard.  
- Areas not opened are listed in §2 and §17 rather than assumed absent of issues.

---

*End of report.*
