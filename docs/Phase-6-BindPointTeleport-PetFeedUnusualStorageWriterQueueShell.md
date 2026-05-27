# Phase 6 - Pet Feed Unusual Storage Writer Queue Shell

Date: May 27, 2026
Unit of Work: UOW-1362

## Scope

This unit adds a disabled bounded writer queue shell to the Java unusual-storage capture registry. It does not add a writer worker, JSON serialization, file output, byte retention, or observer installation.

Java source touched:

- `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture`
- `com.aionemu.gameserver.configs.main.PetFeedUnusualStorageArtifactCaptureConfig`

## What Changed

- Added a private `artifactQueueLock`.
- Added a private bounded `Deque<ArtifactSnapshot>` named `queuedArtifacts`.
- Added a private `droppedArtifactCount`.
- Added `getMaxQueuedArtifacts()`, clamped to at least one and backed by `PetFeedUnusualStorageArtifactCaptureConfig.MAX_QUEUED_ARTIFACTS`.
- `onSnapshotReady(...)` now enqueues completed no-output snapshots when there is capacity.
- When the queue is full, `onSnapshotReady(...)` drops the newest snapshot and increments `droppedArtifactCount`.

## Queue Boundary

This is only a queue shell. Because there is no writer worker yet, queued snapshots remain in memory up to the configured bound. This preserves packet-serialization safety by avoiding blocking or filesystem work at the observer boundary.

The queue stores metadata-only `ArtifactSnapshot` objects. Raw packet bytes are still not retained.

## Boundaries Preserved

- Capture remains disabled by default.
- No observer installation was added.
- No writer worker was added.
- No file output, directory creation, JSON serialization, byte copying, or byte encoding was added.
- No C# reader/schema behavior changed.
- No parity was promoted to verified.

## Validation

- Ran `git diff --check`.
- Attempted `mvn -pl game-server -am -DskipTests compile`.
- Java validation remains blocked locally because `mvn` is not available on PATH and no Maven wrapper exists.
- No Java runtime artifacts were generated.

## Migration Parity Table - UOW-1362

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture` | future C# artifact/schema reader | Utility / Capture Context | Partial | Manual Only | Needs Verification | Adds bounded metadata snapshot queue and dropped-count tracking. No writer worker, JSON output, raw bytes, or runtime validation exists. |
| `com.aionemu.gameserver.configs.main.PetFeedUnusualStorageArtifactCaptureConfig` | future C# artifact capture configuration | Config Class | Partial | Manual Only | Needs Verification | Existing `MAX_QUEUED_ARTIFACTS` is now consumed by the queue shell and clamped to at least one. Runtime config-load validation remains unavailable locally. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Implementation / Compile Attempt | Java capture/config source review | Adds a bounded no-output queue shell without file output or packet blocking. | Source implementation only. | Maven/Java 25 compile unavailable locally; no runtime queue/drop validation, writer worker, JSON artifact, or C# reader validation. |

## Remaining Risks

- Java compile validation remains blocked locally by missing Maven/Java 25 tooling.
- No writer worker drains the queue yet; if capture is enabled prematurely, snapshots remain bounded in memory.
- Dropped-count tracking is private and not exported yet.
- JSON serialization, atomic file output, and output-path validation are still missing.
- C# artifact reader/schema validation remains missing.

## Summary Metrics

- Total Java artifacts discovered: 2 grouped artifact rows in this unit
- Total artifacts ported: 1 disabled bounded writer queue shell
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 2 grouped rows
- Total blocked artifacts: Java compile validation, writer worker, JSON serialization, output-path validation, atomic file output, C# reader/schema validation, Java runtime artifacts
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a disabled no-op writer drain boundary: a private method that removes one queued metadata snapshot and hands it to a no-op `writeArtifact(...)` stub, without creating files or serializing JSON. Keep capture disabled and do not install the observer.
