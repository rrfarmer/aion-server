# Phase 6 - Pet Feed Unusual Storage Writer Worker Shell

Date: May 27, 2026
Unit of Work: UOW-1364

## Scope

This unit adds a disabled writer worker lifecycle shell to the Java unusual-storage capture registry. It does not start the worker automatically, install the observer, serialize JSON, write files, or retain raw bytes.

Java source touched:

- `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture`

## What Changed

- Added private writer worker state:
  - `writerWorkerLock`
  - `writerWorkerRunning`
  - `writerWorker`
- Added `startWriterWorker()`.
- Added `stopWriterWorker()`.
- Added `runWriterWorker()`.
- The worker loop can call `drainQueuedArtifact()` and sleeps briefly when no queued snapshot is available.
- The worker exits when capture is disabled, when `writerWorkerRunning` is false, or when interrupted.
- The worker clears its thread reference on exit.

## Boundary Notes

The lifecycle methods are private and intentionally not invoked in this unit. This is a shell for a future explicitly enabled capture path, not active runtime behavior.

`writeArtifact(...)` remains a no-op. There is still no JSON serialization, output path validation, atomic file output, or raw/canonical packet byte retention.

## Boundaries Preserved

- Capture remains disabled by default.
- No observer installation was added.
- No automatic worker startup was added.
- No file output, directory creation, JSON serialization, byte copying, or byte encoding was added.
- No C# reader/schema behavior changed.
- No parity was promoted to verified.

## Validation

- Ran `git diff --check`.
- Attempted `mvn -pl game-server -am -DskipTests compile`.
- Java validation remains blocked locally because `mvn` is not available on PATH and no Maven wrapper exists.
- No Java runtime artifacts were generated.

## Migration Parity Table - UOW-1364

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture` | future C# artifact/schema reader | Utility / Capture Context | Partial | Manual Only | Needs Verification | Adds private disabled writer worker lifecycle hooks around the existing queue drain boundary. Worker is not started automatically and writer remains no-op. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Implementation / Compile Attempt | Java capture source review | Adds private worker lifecycle shell without enabling output or observer installation. | Source implementation only. | Maven/Java 25 compile unavailable locally; no runtime worker validation, JSON artifact, or C# reader validation. |

## Remaining Risks

- Java compile validation remains blocked locally by missing Maven/Java 25 tooling.
- Worker lifecycle is private and uninvoked; runtime behavior is unvalidated.
- `writeArtifact(...)` is still a no-op.
- JSON serialization, atomic file output, and output-path validation are still missing.
- C# artifact reader/schema validation remains missing.

## Summary Metrics

- Total Java artifacts discovered: 1 grouped artifact row in this unit
- Total artifacts ported: 1 disabled writer worker lifecycle shell
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 1 grouped row
- Total blocked artifacts: Java compile validation, real writer implementation, JSON serialization, output-path validation, atomic file output, C# reader/schema validation, Java runtime artifacts
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a read-only writer activation audit: determine the safest future method to install `PetFeedUnusualStorageArtifactCapture.observer()` into `AionServerPacket.setCaptureObserver(...)` and start/stop the writer worker from server lifecycle/config without enabling either path yet.
