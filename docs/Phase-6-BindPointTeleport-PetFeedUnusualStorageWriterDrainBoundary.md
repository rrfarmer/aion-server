# Phase 6 - Pet Feed Unusual Storage Writer Drain Boundary

Date: May 27, 2026
Unit of Work: UOW-1363

## Scope

This unit adds a disabled no-op queue drain boundary to the Java unusual-storage capture registry. It does not add a writer worker, JSON serialization, file output, byte retention, or observer installation.

Java source touched:

- `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture`

## What Changed

- Added `drainQueuedArtifact()`.
- Added `writeArtifact(ArtifactSnapshot snapshot)` as a no-op writer boundary.
- `drainQueuedArtifact()` removes one queued metadata snapshot under the queue lock, releases the lock, then passes the snapshot to `writeArtifact(...)`.
- `writeArtifact(...)` intentionally performs no JSON serialization or filesystem work.

## Boundary Notes

The method is not wired to a thread or scheduler in this unit. That is intentional. The next implementation step can attach a disabled worker without mixing drain semantics with file output or observer installation.

The queue remains bounded by `MAX_QUEUED_ARTIFACTS`, and queue-full behavior still drops the newest completed snapshot with private dropped-count tracking.

## Boundaries Preserved

- Capture remains disabled by default.
- No observer installation was added.
- No writer worker or scheduler was added.
- No file output, directory creation, JSON serialization, byte copying, or byte encoding was added.
- No C# reader/schema behavior changed.
- No parity was promoted to verified.

## Validation

- Ran `git diff --check`.
- Attempted `mvn -pl game-server -am -DskipTests compile`.
- Java validation remains blocked locally because `mvn` is not available on PATH and no Maven wrapper exists.
- No Java runtime artifacts were generated.

## Migration Parity Table - UOW-1363

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture` | future C# artifact/schema reader | Utility / Capture Context | Partial | Manual Only | Needs Verification | Adds a private queue drain method and no-op writer boundary. No worker, JSON output, raw bytes, or runtime validation exists. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Implementation / Compile Attempt | Java capture source review | Adds a no-op drain/write boundary without file output or packet blocking. | Source implementation only. | Maven/Java 25 compile unavailable locally; no runtime drain validation, writer worker, JSON artifact, or C# reader validation. |

## Remaining Risks

- Java compile validation remains blocked locally by missing Maven/Java 25 tooling.
- Drain boundary is not invoked by a worker yet.
- Dropped-count tracking is private and not exported yet.
- JSON serialization, atomic file output, and output-path validation are still missing.
- C# artifact reader/schema validation remains missing.

## Summary Metrics

- Total Java artifacts discovered: 1 grouped artifact row in this unit
- Total artifacts ported: 1 disabled no-op writer drain boundary
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 1 grouped row
- Total blocked artifacts: Java compile validation, writer worker, JSON serialization, output-path validation, atomic file output, C# reader/schema validation, Java runtime artifacts
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a disabled writer worker lifecycle shell: private start/stop hooks and a worker loop stub that can call `drainQueuedArtifact()` only when capture is enabled, without installing the observer, creating files, serializing JSON, or starting the worker automatically.
