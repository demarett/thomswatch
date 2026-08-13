# Overwatch Tracker Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor Overwatch Tracker into a testable, reload-safe personal application with short database transactions, correct snapshot queries, effective HTTP timeouts, versioned persistence, and healthy reproducible containers.

**Architecture:** Split the backend into an HTTP gateway, pure mapper, non-transactional application service, and transactional snapshot service. Split the frontend into a route-driven API/store layer and page components, so URLs—not ephemeral memory—identify players. Reset the disposable PostgreSQL schema to a typed JSONB/versioned model and verify database behavior against PostgreSQL.

**Tech Stack:** Java 21, Spring Boot 3.5.16, Spring MVC, Spring Data JPA, Flyway, PostgreSQL 17, Caffeine, MockWebServer, Testcontainers, Angular 22.1, TypeScript 6, Angular Material, Angular unit-test builder with Vitest, Docker Compose.

## Global Constraints

- The application is personal and local; do not add authentication or distributed rate limiting.
- Existing PostgreSQL snapshots may be deleted; no data migration is required.
- Keep the current visible MVP behavior and French user-facing messages.
- No HTTP call may execute inside a database transaction.
- Persist payload schema version `1` explicitly.
- Use Java 21 and Node 24.15.0 or newer within the Node 24 LTS line for documented builds.
- Follow red-green-refactor for every behavioral change.

---

## File Structure

- `backend/src/main/java/fr/overwatchtracker/integration/OverfastGateway.java`: only OverFast HTTP/cache/error behavior.
- `backend/src/main/java/fr/overwatchtracker/service/PlayerProfileMapper.java`: pure JSON-to-profile mapping.
- `backend/src/main/java/fr/overwatchtracker/service/PlayerApplicationService.java`: non-transactional orchestration.
- `backend/src/main/java/fr/overwatchtracker/service/SnapshotService.java`: short transactional persistence and reads.
- `backend/src/main/java/fr/overwatchtracker/service/SnapshotPayloadCodec.java`: payload version envelope serialization.
- `backend/src/main/java/fr/overwatchtracker/domain/PlayerSnapshot.java`: versioned JSONB snapshot entity.
- `backend/src/main/java/fr/overwatchtracker/domain/PlayerSnapshotRepository.java`: bounded PostgreSQL queries.
- `frontend/src/app/player-api.ts`: HTTP transport only.
- `frontend/src/app/player-store.ts`: selected profile/loading/error state only.
- `frontend/src/app/player-route.ts`: BattleTag URL conversion helpers.
- Page components: derive identity from `ActivatedRoute` and call API/store.

### Task 1: Reproducible Backend Test Harness

**Files:**
- Create: `backend/mvnw`
- Create: `backend/mvnw.cmd`
- Create: `backend/.mvn/wrapper/maven-wrapper.properties`
- Modify: `backend/pom.xml`
- Modify: `.gitignore`

**Interfaces:**
- Produces: `./mvnw test` on Unix and `mvnw.cmd test` on Windows.
- Produces: Spring MVC tests, Testcontainers PostgreSQL tests, and Actuator health support for later tasks.

- [ ] **Step 1: Confirm the missing harness failure**

Run: `cd backend && ./mvnw test`

Expected: FAIL with `no such file or directory`.

- [ ] **Step 2: Add the Maven Wrapper and dependencies**

Generate Maven Wrapper 3.3.4 files from the official Maven wrapper distribution, set:

```properties
wrapperVersion=3.3.4
distributionType=only-script
distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.zip
```

Add these dependencies to `pom.xml`:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>junit-jupiter</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>postgresql</artifactId>
  <scope>test</scope>
</dependency>
```

Add `.mvn/wrapper/maven-wrapper.jar` to `.gitignore`; the `only-script` wrapper downloads Maven without committing a wrapper JAR.

- [ ] **Step 3: Verify the harness**

Run: `cd backend && ./mvnw test`

Expected: existing backend tests PASS.

- [ ] **Step 4: Commit**

```bash
git add backend/mvnw backend/mvnw.cmd backend/.mvn backend/pom.xml .gitignore
git commit -m "build: add reproducible backend test harness"
```

### Task 2: Uniform BattleTag Validation

**Files:**
- Create: `backend/src/test/java/fr/overwatchtracker/api/PlayerControllerValidationTest.java`
- Create: `backend/src/main/java/fr/overwatchtracker/service/BattleTag.java`
- Modify: `backend/src/main/java/fr/overwatchtracker/dto/PlayerDtos.java`
- Modify: `backend/src/main/java/fr/overwatchtracker/api/PlayerController.java`
- Modify: `backend/src/main/java/fr/overwatchtracker/api/ApiExceptionHandler.java`

**Interfaces:**
- Produces: `BattleTag.parse(String): BattleTag`, `value(): String`, `overfastId(): String`, `urlValue(): String`.
- Produces: HTTP 400 `ApiError("INVALID_BATTLE_TAG", ...)` for missing, null, blank, malformed body or malformed path values.

- [ ] **Step 1: Write failing MVC validation tests**

Use `@WebMvcTest(PlayerController.class)`, `@MockBean PlayerApplicationService`, and assertions equivalent to:

```java
mockMvc.perform(post("/api/players/lookup").contentType(APPLICATION_JSON).content("{}"))
    .andExpect(status().isBadRequest())
    .andExpect(jsonPath("$.code").value("INVALID_BATTLE_TAG"));
mockMvc.perform(post("/api/players/lookup").contentType(APPLICATION_JSON).content("{\"battleTag\":null}"))
    .andExpect(status().isBadRequest());
mockMvc.perform(post("/api/players/lookup").contentType(APPLICATION_JSON).content("{\"battleTag\":\"bad\"}"))
    .andExpect(status().isBadRequest());
mockMvc.perform(get("/api/players/bad/history")).andExpect(status().isBadRequest());
```

- [ ] **Step 2: Run the tests and confirm RED**

Run: `cd backend && ./mvnw -Dtest=PlayerControllerValidationTest test`

Expected: missing/null JSON reaches the service or returns the wrong error contract.

- [ ] **Step 3: Implement the value object and bean validation**

Define `BattleTag` as an immutable record with one compiled pattern:

```java
public record BattleTag(String value) {
  private static final Pattern FORMAT = Pattern.compile("^[^#\\s-]{2,32}(?:#|-)\\d{3,12}$");
  public static BattleTag parse(String raw) {
    if (raw == null || !FORMAT.matcher(raw.trim()).matches()) {
      throw new IllegalArgumentException("Format attendu : Pseudo#1234");
    }
    return new BattleTag(raw.trim().replaceFirst("-(?=\\d{3,12}$)", "#"));
  }
  public String overfastId() { return value.replace('#', '-'); }
  public String urlValue() { return overfastId(); }
}
```

Annotate `LookupRequest.battleTag` with `@NotBlank` and `@Pattern`. Keep path validation at the controller boundary and translate `IllegalArgumentException`, constraint violations, unreadable JSON, and method argument validation to the uniform API error.

- [ ] **Step 4: Verify GREEN**

Run: `cd backend && ./mvnw -Dtest=PlayerControllerValidationTest test`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/fr/overwatchtracker backend/src/test/java/fr/overwatchtracker/api
git commit -m "fix: validate battle tags uniformly"
```

### Task 3: Versioned PostgreSQL Snapshot Model and Correct Queries

**Files:**
- Modify: `backend/src/main/resources/db/migration/V1__create_snapshots.sql`
- Modify: `backend/src/main/java/fr/overwatchtracker/domain/PlayerSnapshot.java`
- Modify: `backend/src/main/java/fr/overwatchtracker/domain/PlayerSnapshotRepository.java`
- Create: `backend/src/main/java/fr/overwatchtracker/service/SnapshotPayloadCodec.java`
- Create: `backend/src/main/java/fr/overwatchtracker/service/SnapshotService.java`
- Create: `backend/src/test/java/fr/overwatchtracker/domain/PlayerSnapshotRepositoryTest.java`
- Create: `backend/src/test/java/fr/overwatchtracker/service/SnapshotPayloadCodecTest.java`

**Interfaces:**
- Produces: `SnapshotPayloadCodec.currentVersion(): int`, `encode(PlayerProfileDto): JsonNode`, and `decode(int, JsonNode): PlayerProfileDto`.
- Produces: `SnapshotService.save`, `history(BattleTag)`, `recent()`, and `stored(BattleTag)` with transactions confined to this class.

- [ ] **Step 1: Write failing codec tests**

Assert that the codec exposes an explicit version, round-trips the JSONB profile payload, and rejects unsupported versions:

```java
assertEquals(1, codec.currentVersion());
assertEquals(profile, codec.decode(codec.currentVersion(), codec.encode(profile)));
assertThrows(IllegalStateException.class, () -> codec.decode(2, json.createObjectNode()));
```

- [ ] **Step 2: Write failing repository tests against PostgreSQL**

Use `@DataJpaTest`, a static `PostgreSQLContainer<?>`, and `@DynamicPropertySource`. Insert 105 snapshots for `Busy#1234` and one each for nine other players. Assert:

```java
assertEquals(100, repository.findLatestHistory("Busy#1234").size());
assertEquals(base.plusSeconds(5), repository.findLatestHistory("Busy#1234").getLast().getCapturedAt());
assertEquals(8, repository.findRecentDistinct().size());
assertEquals(8, repository.findRecentDistinct().stream().map(PlayerSnapshot::getBattleTag).distinct().count());
```

The precise timestamp assertion should prove that the oldest five Busy snapshots are excluded.

- [ ] **Step 3: Run the focused tests and confirm RED**

Run: `cd backend && ./mvnw -Dtest=SnapshotPayloadCodecTest,PlayerSnapshotRepositoryTest test`

Expected: FAIL because the codec and correct bounded queries do not exist.

- [ ] **Step 4: Replace the disposable schema**

Create `payload JSONB NOT NULL`, `payload_version INTEGER NOT NULL CHECK (payload_version > 0)`, retain typed chart columns, and retain the `(battle_tag, captured_at DESC)` index. Map JSONB using Hibernate `@JdbcTypeCode(SqlTypes.JSON)` and `JsonNode`.

Use native repository queries with explicit limits:

```sql
SELECT * FROM player_snapshots
WHERE battle_tag = :battleTag
ORDER BY captured_at DESC
LIMIT 100
```

```sql
SELECT DISTINCT ON (battle_tag) * FROM player_snapshots
ORDER BY battle_tag, captured_at DESC
```

Wrap the second query in an outer query ordered by `captured_at DESC LIMIT 8`.

- [ ] **Step 5: Implement codec and transactional snapshot service**

The JSONB column contains the serialized profile object. The adjacent `payload_version` column is always populated from `codec.currentVersion()`; `decode` switches on that column before reading the JSON:

```java
return switch (version) {
  case 1 -> objectMapper.treeToValue(payload, PlayerProfileDto.class);
  default -> throw new IllegalStateException("Version de snapshot non supportée : " + version);
};
```

`SnapshotService.history` reverses the repository's descending result into chronological order. All four public methods use `@Transactional`; reads use `readOnly=true`.

- [ ] **Step 6: Verify GREEN**

Run: `cd backend && ./mvnw -Dtest=SnapshotPayloadCodecTest,PlayerSnapshotRepositoryTest test`

Expected: PASS with Docker providing PostgreSQL for Testcontainers.

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/resources/db/migration backend/src/main/java/fr/overwatchtracker/domain backend/src/main/java/fr/overwatchtracker/service backend/src/test/java/fr/overwatchtracker/domain backend/src/test/java/fr/overwatchtracker/service
git commit -m "refactor: version snapshot persistence"
```

### Task 4: Gateway and Non-Transactional Application Orchestration

**Files:**
- Rename: `backend/src/main/java/fr/overwatchtracker/integration/OverfastClient.java` → `OverfastGateway.java`
- Rename: `backend/src/main/java/fr/overwatchtracker/service/PlayerMapper.java` → `PlayerProfileMapper.java`
- Replace: `backend/src/main/java/fr/overwatchtracker/service/PlayerService.java` → `PlayerApplicationService.java`
- Modify: `backend/src/main/java/fr/overwatchtracker/config/AppConfig.java`
- Modify: `backend/src/main/java/fr/overwatchtracker/api/PlayerController.java`
- Modify: `backend/src/main/java/fr/overwatchtracker/api/HeroController.java`
- Rename tests to match production classes.
- Create: `backend/src/test/java/fr/overwatchtracker/service/PlayerApplicationServiceTest.java`
- Create: `backend/src/test/java/fr/overwatchtracker/config/AppConfigTest.java`

**Interfaces:**
- `OverfastGateway.getPlayer(String)` and `getHeroes()` preserve existing DTO-neutral JSON behavior.
- `PlayerApplicationService.load(BattleTag, boolean)`, `history`, `recent`, and `stored` orchestrate gateway/mapper/snapshots without `@Transactional`.

- [ ] **Step 1: Write failing orchestration and timeout tests**

In `PlayerApplicationServiceTest`, make the gateway return JSON and verify the resulting profile is passed to `SnapshotService.save`. Assert the application service class and `load` method have no `@Transactional` annotation using Spring's `AnnotatedElementUtils`.

In `AppConfigTest`, build the configured `RestClient` against an unroutable local endpoint and assert connection failure occurs within a bounded interval greater than the configured value but less than twice that value. Keep the test tolerance broad enough for CI scheduling.

- [ ] **Step 2: Run focused tests and confirm RED**

Run: `cd backend && ./mvnw -Dtest=PlayerApplicationServiceTest,AppConfigTest test`

Expected: FAIL because orchestration remains transactional and connect timeout is unused.

- [ ] **Step 3: Apply the gateway/application split**

Build a JDK client explicitly:

```java
var httpClient = HttpClient.newBuilder().connectTimeout(connect).build();
var factory = new JdkClientHttpRequestFactory(httpClient);
factory.setReadTimeout(read);
builder.requestFactory(factory);
```

Keep `@Cacheable` on gateway reads. On refresh, evict the `overfastPlayers` key before calling the gateway. Pass `BattleTag` values between controller and application service; only the gateway receives `overfastId()`.

- [ ] **Step 4: Verify GREEN and the existing gateway/mapper tests**

Run: `cd backend && ./mvnw test`

Expected: all non-container and container-backed tests PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java backend/src/test/java
git commit -m "refactor: isolate upstream calls from transactions"
```

### Task 5: Angular 22 Test Runner and Route/API/Store Foundations

**Files:**
- Modify: `frontend/angular.json`
- Modify: `frontend/package.json`
- Modify: `frontend/package-lock.json`
- Create: `frontend/tsconfig.spec.json`
- Create: `frontend/src/app/player-route.ts`
- Create: `frontend/src/app/player-route.spec.ts`
- Create: `frontend/src/app/player-api.ts`
- Create: `frontend/src/app/player-api.spec.ts`
- Create: `frontend/src/app/player-store.ts`
- Create: `frontend/src/app/player-store.spec.ts`
- Remove after migration: `frontend/src/app/player.service.ts`

**Interfaces:**
- `toRouteBattleTag(tag: string): string` converts `Name#1234` to `Name-1234`.
- `fromRouteBattleTag(tag: string): string` converts only the final numeric separator to `#`.
- `PlayerApi` exposes lookup, recent, stored, refresh, history, and heroes Observables.
- `PlayerStore.profile`, `loading`, and `error` are signals; `setProfile`, `begin`, `fail`, and `clear` mutate them.

- [ ] **Step 1: Make the test runner itself green with one smoke test**

Add the Angular target:

```json
"test": {
  "builder": "@angular/build:unit-test",
  "options": {"runner": "vitest", "tsConfig": "tsconfig.spec.json", "watch": false}
}
```

Set the package script to `"test":"ng test"`; add `vitest@^4.0.8` and a Node-24-compatible `jsdom` release as dev dependencies. Add `tsconfig.spec.json` extending the root config with `types: ["vitest/globals"]`. Create a smoke spec with `expect(true).toBe(true)`.

Run: `cd frontend && npm test`

Expected: PASS, proving the harness is repaired before behavioral TDD.

- [ ] **Step 2: Write route, API, and store tests**

Examples:

```typescript
expect(toRouteBattleTag('Ana#1234')).toBe('Ana-1234');
expect(fromRouteBattleTag('Ana-Marie-1234')).toBe('Ana-Marie#1234');
```

Use Angular's HTTP testing provider to assert `PlayerApi.stored('Ana#1234')` requests `/api/players/Ana-1234/stored`. Test that `PlayerStore.begin()` clears the prior error, `setProfile()` stores the profile and ends loading, and `fail('x')` stores the error and ends loading.

- [ ] **Step 3: Run focused specs and confirm RED**

Run: `cd frontend && npm test -- --include src/app/player-route.spec.ts --include src/app/player-api.spec.ts --include src/app/player-store.spec.ts`

Expected: FAIL because the three production modules do not exist.

- [ ] **Step 4: Implement minimal route/API/store modules**

Move all current `HttpClient` expressions unchanged into `PlayerApi`, using `toRouteBattleTag` for path parameters. Implement the store without HTTP dependencies so it remains a pure state boundary.

- [ ] **Step 5: Verify GREEN**

Run: `cd frontend && npm test`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add frontend/angular.json frontend/package.json frontend/package-lock.json frontend/tsconfig.spec.json frontend/src/app
git commit -m "test: establish angular route and state foundations"
```

### Task 6: Route-Driven Reloadable Pages

**Files:**
- Modify: `frontend/src/app/app.routes.ts`
- Modify: `frontend/src/app/app.component.ts`
- Modify: `frontend/src/app/pages/home.component.ts`
- Modify: `frontend/src/app/pages/profile.component.ts`
- Modify: `frontend/src/app/pages/history.component.ts`
- Create: `frontend/src/app/pages/home.component.spec.ts`
- Create: `frontend/src/app/pages/profile.component.spec.ts`
- Create: `frontend/src/app/pages/history.component.spec.ts`

**Interfaces:**
- Routes: `/profil/:battleTag` and `/historique/:battleTag`.
- Profile page loads `PlayerApi.stored(fromRouteBattleTag(param))` when the store does not already hold the same player.
- History page always loads history using its route parameter.

- [ ] **Step 1: Write failing component route tests**

Use `RouterTestingHarness` with stubbed `PlayerApi`. Assert:

```typescript
await harness.navigateByUrl('/profil/Ana-1234', ProfileComponent);
expect(api.stored).toHaveBeenCalledWith('Ana#1234');
await harness.navigateByUrl('/historique/Ana-1234', HistoryComponent);
expect(api.history).toHaveBeenCalledWith('Ana#1234');
```

Test that submitting a successful lookup navigates to `/profil/Ana-1234`, a recent profile does the same after loading stored data, refresh calls `api.refresh('Ana#1234')`, and HTTP errors render the French error message.

- [ ] **Step 2: Run component specs and confirm RED**

Run: `cd frontend && npm test -- --include src/app/pages/*.spec.ts`

Expected: FAIL because existing pages rely on in-memory `PlayerService.current()` and static routes.

- [ ] **Step 3: Implement canonical parameter routes and page loading**

Declare:

```typescript
{path:'profil/:battleTag', component:ProfileComponent},
{path:'historique/:battleTag', component:HistoryComponent}
```

Have pages read `route.snapshot.paramMap.get('battleTag')`, normalize with `fromRouteBattleTag`, begin the store operation, subscribe to the API, and update/fail the store. Build profile/history navigation links from `toRouteBattleTag(profile.battleTag)`. Hide header profile/history links when no stored profile exists, but when present point them to canonical routes.

- [ ] **Step 4: Verify GREEN and production compilation**

Run: `cd frontend && npm test && npm run build`

Expected: all specs PASS and the Angular production build succeeds under Node 24.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app
git commit -m "refactor: make player pages reloadable by route"
```

### Task 7: Health-Aware and Non-Root Containers

**Files:**
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/Dockerfile`
- Modify: `frontend/Dockerfile`
- Modify: `frontend/nginx.conf`
- Modify: `docker-compose.yml`
- Create: `backend/src/test/java/fr/overwatchtracker/api/HealthEndpointTest.java`

**Interfaces:**
- Backend exposes `/actuator/health` only.
- Frontend exposes `/healthz` returning HTTP 200 without proxying.
- Compose waits for `db`, then `backend`, then `frontend` health.

- [ ] **Step 1: Write the failing health endpoint test**

Use `@SpringBootTest(webEnvironment = RANDOM_PORT)`, `@Testcontainers`, a PostgreSQL container, and `@DynamicPropertySource` for datasource properties. Assert unauthenticated GET `/actuator/health` returns 200 with `status: UP` while `/actuator/env` returns 404.

- [ ] **Step 2: Run and confirm RED**

Run: `cd backend && ./mvnw -Dtest=HealthEndpointTest test`

Expected: FAIL until Actuator exposure is configured.

- [ ] **Step 3: Configure application and images**

Set:

```yaml
management:
  endpoints.web.exposure.include: health
  endpoint.health.probes.enabled: true
```

Create a non-root `app` user in the Java runtime image and run `USER app`. Use `npm ci` in the frontend build. Use `nginxinc/nginx-unprivileged:1.29-alpine`, configure nginx to listen on port `8080`, expose `/healthz` with `return 200 'ok';`, and update its container port.

- [ ] **Step 4: Add Compose health dependencies**

Backend health command calls `http://localhost:8080/actuator/health`; frontend health command calls `http://localhost:8080/healthz`. Set `depends_on.backend.condition: service_healthy` and publish host `4200` to frontend container `8080`.

- [ ] **Step 5: Verify health configuration**

Run: `cd backend && ./mvnw -Dtest=HealthEndpointTest test`

Run: `docker compose config`

Run: `docker compose build backend frontend`

Expected: test PASS, Compose config PASS, and both images build.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/resources/application.yml backend/src/test backend/Dockerfile frontend/Dockerfile frontend/nginx.conf docker-compose.yml
git commit -m "build: add healthy non-root containers"
```

### Task 8: Documentation, Reset Instructions, and Full Verification

**Files:**
- Modify: `README.md`
- Modify: `.env.example`

**Interfaces:**
- Documents supported Java 21, Maven Wrapper, Node 24.15+ LTS, health endpoints, canonical URLs, and the intentional database reset procedure.

- [ ] **Step 1: Update documentation**

Document backend commands with `./mvnw`, frontend commands with `npm ci`, and explain that this refactor changes the initial schema. Provide the explicit, destructive reset command only for this project's named Compose volume workflow:

```bash
docker compose down --volumes
docker compose up --build
```

State clearly that the first command deletes local Overwatch Tracker snapshots. Document `/actuator/health`, `/healthz`, `/profil/Pseudo-1234`, and `/historique/Pseudo-1234`.

- [ ] **Step 2: Run complete verification from clean dependency state**

Run:

```bash
cd backend && ./mvnw clean test
cd ../frontend && npm ci && npm test && npm run build
cd .. && docker compose config && docker compose build
git diff --check
```

Expected: every command exits 0 with no test failures or whitespace errors. Do not delete the user's PostgreSQL volume during verification.

- [ ] **Step 3: Review acceptance criteria against evidence**

Confirm test names/output cover: uniform 400 errors, no transactional orchestration, both timeouts, newest-100 history, eight distinct recent players, reload-safe routes, payload version 1, frontend tests/build, backend tests, and Compose health order.

- [ ] **Step 4: Commit**

```bash
git add README.md .env.example
git commit -m "docs: document refactored local workflow"
```

- [ ] **Step 5: Request final code review**

Use `superpowers:requesting-code-review` against the implementation range. Resolve all Critical and Important findings, rerun the full verification commands, then use `superpowers:verification-before-completion` before reporting completion.
