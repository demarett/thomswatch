# Task 3 — Versioned PostgreSQL Snapshot Model and Correct Queries

## Status

DONE

## Commit

- `1c50559 refactor: version snapshot persistence`

## Result

- Replaced the disposable payload schema with PostgreSQL `JSONB` and a positive, non-null `payload_version`.
- Mapped payloads as Jackson `JsonNode` values through Hibernate's JSON JDBC type.
- Added native, explicitly bounded history and recent-distinct queries.
- Added the version-aware `SnapshotPayloadCodec` and transactional `SnapshotService`.
- Kept history selection descending in SQL and reversed it chronologically in `SnapshotService`.
- Minimally adapted the legacy `PlayerService` to compile against the new model and queries; Task 4 still owns replacing that orchestration.

## TDD evidence

### RED

After adding the codec and PostgreSQL repository tests, this focused command was run before production changes:

`JAVA_HOME=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home ./mvnw -Dtest=SnapshotPayloadCodecTest,PlayerSnapshotRepositoryTest test`

It failed during test compilation for the intended missing behavior:

- `SnapshotPayloadCodec` did not exist.
- `findLatestHistory(String)` did not exist.
- `findRecentDistinct()` did not exist.
- `PlayerSnapshot` did not accept a JSON payload plus payload version.

### GREEN

The first sandbox-local GREEN attempt compiled and passed both codec tests, but Testcontainers could not access either Docker socket (`SocketException: Operation not permitted`). The exact focused command was rerun with Docker socket permission and passed against PostgreSQL 16:

```text
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The repository tests inserted 105 Busy snapshots plus nine distinct players and proved:

- only the latest 100 Busy snapshots are returned;
- the last descending result is exactly `base.plusSeconds(5)`;
- recent results contain exactly eight rows and eight distinct BattleTags;
- the recent rows are ordered by their latest capture time.

## Full verification

`JAVA_HOME=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home ./mvnw test`

Run with Docker/local-socket permission:

```text
Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

`git diff --check` also completed with no whitespace errors before commit.

## Self-review

- Flyway created the JSONB/versioned schema successfully and Hibernate schema validation passed against the PostgreSQL Testcontainer.
- The history query orders newest-first and applies `LIMIT 100` before the service returns chronological DTOs.
- The recent query performs `DISTINCT ON (battle_tag)` in its inner query and applies newest-first ordering plus `LIMIT 8` in its outer query.
- `SnapshotService.save` writes the codec's current version adjacent to the encoded JSON tree.
- `SnapshotPayloadCodec.decode` switches on the stored version before attempting deserialization and rejects unknown versions.
- Every public `SnapshotService` operation is transactional; the three read operations are read-only.

## Concerns

- This is an intentionally staged refactor. The legacy `PlayerService` still owns its old transactions and was only made compatible with the new entity/query signatures. Task 4 must replace that orchestration so network calls no longer run inside a transaction.
- Maven reports existing future-JDK warnings for native access, `Unsafe`, Mockito dynamic agent loading, and a deprecated mapper API. They do not fail the build and are unrelated to Task 3.
