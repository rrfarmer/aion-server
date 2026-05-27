# Phase 6 - Pet Feed Unusual Storage Writer Activation Audit

Date: May 27, 2026
Unit of Work: UOW-1365

## Scope

This is a read-only audit of the safest future startup/shutdown points for the disabled unusual-storage artifact capture path. It does not install the packet observer, start the writer worker, create files, serialize JSON, copy packet bytes, mutate storage, dispatch packets, or enable live behavior.

Java remains the source of truth. Source breadcrumbs reviewed in this unit:

- `com.aionemu.gameserver.GameServer`
- `com.aionemu.gameserver.ShutdownHook`
- `com.aionemu.gameserver.network.aion.AionServerPacket`
- `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture`
- `com.aionemu.gameserver.configs.main.PetFeedUnusualStorageArtifactCaptureConfig`

## Startup Findings

`GameServer.main()` initializes utility services and config before the NIO server is started. `initUtilityServicesAndConfig()` calls `Config.load()`, initializes database and utility services, and starts cron infrastructure before `initNioServer()` is invoked.

The future capture activation point should therefore be after `Config.load()` has populated `PetFeedUnusualStorageArtifactCaptureConfig` and before client packet serialization can begin through the NIO server. A future public lifecycle method on `PetFeedUnusualStorageArtifactCapture` should be explicitly disabled-by-default and should be a no-op unless `PetFeedUnusualStorageArtifactCaptureConfig.ENABLED` is true.

`AionServerPacket.setCaptureObserver(...)` is a process-wide static observer hook. Installing the unusual-storage observer would affect the serialization path for every outgoing server packet, even though the capture registry filters by pending context and expected packet sequence. That global surface makes config gating, idempotent install, and deterministic shutdown mandatory.

## Serialization Boundary

`AionServerPacket.write(...)` currently invokes the observer after `writeImpl(con)`, after the packet length is stamped, and before `con.encrypt(...)`. This is the correct clear-byte boundary for future runtime artifacts because the observer receives a read-only duplicate of the serialized frame before encryption changes the buffer.

The observer is disabled by default through `NoOpServerPacketCaptureObserver`. Calling `AionServerPacket.setCaptureObserver(null)` resets the global hook to that no-op observer.

## Shutdown Findings

`ShutdownHook.run()` begins its final shutdown flow by calling `GameServer.shutdownNioServer()`, then runs save/service shutdown work and eventually stops cron, thread pools, and logging. If unusual-storage artifact capture is later activated, the capture hook should be deactivated at the beginning of shutdown or before NIO shutdown starts. Otherwise disconnect/save fanout during shutdown could be observed unintentionally.

A future shutdown path should:

- reset the global observer with `AionServerPacket.setCaptureObserver(null)`;
- stop the writer worker through a public capture lifecycle method;
- use a bounded drain/flush policy if final artifact output is desired;
- complete before thread-pool/logger teardown can strand worker output.

## Lifecycle API Gap

`PetFeedUnusualStorageArtifactCapture` currently exposes a disabled observer accessor and has private writer worker lifecycle hooks, but there is no public activation/deactivation API. Future implementation should add a narrow idempotent lifecycle surface before wiring `GameServer` or `ShutdownHook`.

Candidate future API shape:

- `installIfEnabled()` or equivalent startup method: config-gated, idempotent, installs `observer()` into `AionServerPacket` and starts the writer worker only when enabled.
- `shutdown()` or equivalent stop method: idempotent, resets the packet observer to no-op and stops the writer worker.

This unit intentionally does not add or call that API.

## Migration Parity Table - UOW-1365

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.GameServer` | future C# artifact-capture startup/lifecycle boundary | Bootstrap | Not Started | Manual Only | Needs Verification | Read-only audit confirms config is loaded before NIO startup. Future capture activation should occur after config load and before packet serialization can begin, but no C# lifecycle equivalent or Java wiring was implemented. |
| `com.aionemu.gameserver.ShutdownHook` | future C# artifact-capture shutdown/lifecycle boundary | Bootstrap / Shutdown | Not Started | Manual Only | Needs Verification | Read-only audit confirms Java shutdown currently starts with NIO shutdown. Future capture shutdown should reset the global observer and stop/drain the worker before or at the beginning of shutdown to avoid unintended disconnect/save captures. |
| `com.aionemu.gameserver.network.aion.AionServerPacket` | future C# packet writer/capture equivalent | Packet Serialization Base | Partial | Manual Only | Needs Verification | Existing disabled observer hook is global and runs after length stamping but before encryption. No activation or C# runtime comparison exists. |
| `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture` | future C# artifact/schema reader | Utility / Capture Context | Partial | Manual Only | Needs Verification | Capture has disabled registry, queue, and private worker hooks, but no public lifecycle API, observer install, JSON writer, raw byte copy, or file output. |
| `com.aionemu.gameserver.configs.main.PetFeedUnusualStorageArtifactCaptureConfig` | future C# artifact-capture config | Config Class | Partial | Manual Only | Needs Verification | `ENABLED` defaults false. Future activation must remain explicitly config-gated and should not rely on startup ordering alone. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Read-only audit | Java startup, shutdown, packet serialization, capture config, and capture registry source review | Documents lifecycle placement and risks for future disabled-by-default activation. | Source inspection only. | No compile/runtime validation, no observer install, no worker start/stop validation, no Java runtime artifact, and no C# reader validation. |

## Remaining Risks

- Java compile validation remains blocked locally by missing Maven/Java 25 tooling.
- Capture activation is not implemented; there is no public lifecycle API yet.
- The packet observer hook is process-global, so future tests and lifecycle code must prevent observer leakage across runs.
- Writer stop/drain semantics are not implemented or runtime-validated.
- JSON serialization, atomic file output, raw/canonical byte retention, C# artifact reader/schema validation, and warehouse-add byte comparison remain missing.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 0 source artifacts; 1 read-only audit document
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: lifecycle API, observer install, writer start/stop wiring, JSON writer, atomic output, Java runtime artifacts, C# artifact reader/schema validation, warehouse-add byte comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a disabled public lifecycle API shell to `PetFeedUnusualStorageArtifactCapture`, such as `installIfEnabled()` and `shutdown()`, without calling it from `GameServer` or `ShutdownHook` yet. The API should be idempotent, config-gated, reset the global packet observer to no-op on shutdown, and only start private worker hooks when explicitly enabled.
