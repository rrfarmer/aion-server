# Phase 6 - Pet Feed Unusual Storage Lifecycle Wiring

Date: May 27, 2026
Unit of Work: UOW-1367

## Scope

This unit wires the disabled unusual-storage artifact capture lifecycle API into Java startup and shutdown. Capture remains disabled by default through `PetFeedUnusualStorageArtifactCaptureConfig.ENABLED=false`. No JSON writer, file output, raw byte retention, live storage mutation, live packet dispatch change, or C# runtime comparison was added.

Java remains the source of truth. Source breadcrumbs touched in this unit:

- `com.aionemu.gameserver.GameServer`
- `com.aionemu.gameserver.ShutdownHook`
- `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture`
- `com.aionemu.gameserver.network.aion.AionServerPacket`

## Implementation

`GameServer.main()` now calls `PetFeedUnusualStorageArtifactCapture.installIfEnabled()` after config/services have initialized and before `initNioServer()` starts client packet serialization. With the default config this method returns immediately.

`ShutdownHook.run()` now calls `PetFeedUnusualStorageArtifactCapture.shutdown()` at the beginning of shutdown processing. This resets the process-wide packet observer to no-op and stops the private writer worker shell before shutdown announcements, disconnects, and save fanout can be observed.

The lifecycle calls are intentionally limited to the existing disabled capture surface. They do not implement artifact output or change the normal item/storage packet path while capture remains disabled.

## Migration Parity Table - UOW-1367

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.GameServer` | future C# artifact-capture startup/lifecycle boundary | Bootstrap | Partial | Manual Only | Needs Verification | Calls disabled `PetFeedUnusualStorageArtifactCapture.installIfEnabled()` after config/service initialization and before NIO startup. Default config keeps this path no-op. Compile/runtime validation unavailable locally. |
| `com.aionemu.gameserver.ShutdownHook` | future C# artifact-capture shutdown/lifecycle boundary | Bootstrap / Shutdown | Partial | Manual Only | Needs Verification | Calls `PetFeedUnusualStorageArtifactCapture.shutdown()` at shutdown start to reset the global observer and stop the worker before shutdown packet fanout. Runtime shutdown ordering is source-reviewed only. |
| `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture` | future C# artifact/schema reader | Utility / Capture Context | Partial | Manual Only | Needs Verification | Existing lifecycle shell is now called from startup/shutdown, but capture remains disabled unless config is explicitly enabled. Writer output and raw bytes are still missing. |
| `com.aionemu.gameserver.network.aion.AionServerPacket` | future C# packet writer/capture equivalent | Packet Serialization Base | Partial | Manual Only | Needs Verification | Wiring may install/reset the existing global observer only through the disabled lifecycle API. No serialization body changed in this unit. |
| `com.aionemu.gameserver.configs.main.PetFeedUnusualStorageArtifactCaptureConfig` | future C# artifact-capture config | Config Class | Partial | Manual Only | Needs Verification | `ENABLED=false` remains the default and is the only startup activation gate. No config semantics changed. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Implementation / Compile Attempt | Java startup/shutdown source review | Adds disabled lifecycle calls at the audited startup/shutdown boundaries. | Source implementation only. | Maven/Java 25 compile unavailable locally; no runtime lifecycle validation, no observer leak test, no JSON artifact, and no C# reader validation. |

## Remaining Risks

- Java compile validation remains blocked locally by missing Maven/Java 25 tooling.
- Runtime startup/shutdown behavior is not validated in a running Java server.
- `shutdown()` resets the process-wide observer to no-op; future shared-observer scenarios need explicit ownership policy.
- Capture output is still absent: `writeArtifact(...)` remains no-op and no bytes are retained.
- JSON serialization, atomic file output, output path validation, C# artifact reader/schema validation, and warehouse-add byte comparison remain missing.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 disabled startup/shutdown lifecycle wiring slice
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java compile validation, runtime lifecycle validation, real writer implementation, JSON writer, atomic output, Java runtime artifacts, C# artifact reader/schema validation, warehouse-add byte comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a read-only output-path validation and filename audit for the future unusual-storage JSON writer, then implement only the safe path helper if the boundary is clear. Do not serialize JSON or retain packet bytes until the output safety rules are documented.
