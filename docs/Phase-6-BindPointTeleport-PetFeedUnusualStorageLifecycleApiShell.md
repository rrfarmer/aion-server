# Phase 6 - Pet Feed Unusual Storage Lifecycle API Shell

Date: May 27, 2026
Unit of Work: UOW-1366

## Scope

This unit adds a disabled public lifecycle API shell for unusual-storage artifact capture. It does not wire the API into `GameServer` or `ShutdownHook`, does not enable capture by default, does not create files, does not serialize JSON, does not copy packet bytes, and does not affect live storage mutation or packet dispatch unless a future caller explicitly enables the config and invokes the lifecycle method.

Java remains the source of truth. Source breadcrumbs touched in this unit:

- `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture`
- `com.aionemu.gameserver.network.aion.AionServerPacket`

## Implementation

`PetFeedUnusualStorageArtifactCapture.installIfEnabled()` now provides a narrow startup seam. It returns immediately when `PetFeedUnusualStorageArtifactCaptureConfig.ENABLED` is false. When explicitly enabled by future wiring, it installs `PetFeedUnusualStorageArtifactCapture.observer()` through `AionServerPacket.setCaptureObserver(...)` and starts the private writer worker shell.

`PetFeedUnusualStorageArtifactCapture.shutdown()` now provides a narrow stop seam. It resets the global packet observer with `AionServerPacket.setCaptureObserver(null)` and stops the private writer worker shell.

Both methods are intentionally unused in production startup/shutdown. This keeps the runtime behavior unchanged while creating a future integration boundary for controlled artifact generation.

## Migration Parity Table - UOW-1366

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture` | future C# artifact/schema reader | Utility / Capture Context | Partial | Manual Only | Needs Verification | Adds public disabled lifecycle seams `installIfEnabled()` and `shutdown()`. Startup remains config-gated and uncalled; shutdown resets the global observer and stops the private worker. No writer output, JSON, raw byte retention, or runtime validation exists. |
| `com.aionemu.gameserver.network.aion.AionServerPacket` | future C# packet writer/capture equivalent | Packet Serialization Base | Partial | Manual Only | Needs Verification | Lifecycle shell uses the existing global `setCaptureObserver(...)` hook but does not change packet serialization behavior because no startup/shutdown caller was added. Global observer leakage remains a future test/lifecycle risk. |
| `com.aionemu.gameserver.configs.main.PetFeedUnusualStorageArtifactCaptureConfig` | future C# artifact-capture config | Config Class | Partial | Manual Only | Needs Verification | `installIfEnabled()` depends on `ENABLED`; default remains false. No config semantics changed. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Implementation / Compile Attempt | Java capture lifecycle source review | Adds a config-gated public activation method and public shutdown method without wiring them into server lifecycle. | Source implementation only. | Maven/Java 25 compile unavailable locally; no runtime install/shutdown validation, no observer leak test, no JSON artifact, and no C# reader validation. |

## Remaining Risks

- Java compile validation remains blocked locally by missing Maven/Java 25 tooling.
- The lifecycle API is uncalled; future `GameServer`/`ShutdownHook` wiring still needs ordering validation.
- `shutdown()` resets the process-wide packet observer to no-op; future shared-observer scenarios would need explicit ownership policy.
- Writer stop/drain semantics are still minimal and unvalidated.
- `writeArtifact(...)` remains a no-op; no JSON serialization, atomic file output, raw/canonical byte retention, C# artifact reader/schema validation, or warehouse-add byte comparison exists.

## Summary Metrics

- Total Java artifacts discovered: 3 grouped artifact rows in this unit
- Total artifacts ported: 1 disabled public lifecycle API shell
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 3 grouped rows
- Total blocked artifacts: Java compile validation, GameServer/ShutdownHook wiring, real writer implementation, JSON writer, atomic output, Java runtime artifacts, C# artifact reader/schema validation, warehouse-add byte comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add disabled startup/shutdown wiring around the lifecycle API: call `PetFeedUnusualStorageArtifactCapture.installIfEnabled()` after config load and before NIO startup, and call `PetFeedUnusualStorageArtifactCapture.shutdown()` at the beginning of `ShutdownHook.run()`. Keep `ENABLED=false` as the default and do not implement JSON/file output yet.
