# Phase 6 - Pet Feed Unusual Storage Correlation Registry

Date: May 27, 2026
Unit of Work: UOW-1349

## Scope

This unit adds a disabled, bounded correlation registry inside `PetFeedUnusualStorageArtifactCapture` so future artifact capture can pair construction-time unusual-storage context with serialized packet observations.

Java source touched:

- `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture`
- `com.aionemu.gameserver.network.aion.capture.ServerPacketCaptureObserver`
- `com.aionemu.gameserver.network.aion.AionConnection`
- `com.aionemu.gameserver.network.aion.AionServerPacket.write`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE`

## What Changed

- Added a bounded per-player pending context queue to `PetFeedUnusualStorageArtifactCapture`.
- Added `PetFeedUnusualStorageArtifactCapture.observer()` as a future `ServerPacketCaptureObserver` integration point.
- `registerStorageUpdate(...)` now enqueues construction-time storage id, storage ordinal, and item object id when capture is enabled and the existing unusual-storage guard passes.
- The observer advances the first pending context for the active connection player when it sees the expected packet order:
  - `SM_WAREHOUSE_ADD_ITEM`
  - `SM_CUBE_UPDATE`
- The observer catches runtime exceptions so capture code cannot interrupt packet dispatch.
- The registry remains inert because `enabled` is still `false` and no code installs the observer into `AionServerPacket`.

## Boundaries Preserved

- No artifact writing was added.
- No byte copying was added.
- No JSON schema generation was added.
- No config flag or runtime enable path was added.
- No packet serializer behavior was intentionally changed.
- No live C# unusual-storage dispatch behavior was enabled.

## Validation

- Attempted `mvn -pl game-server -am -DskipTests compile`.
- Validation was blocked because `mvn` is not available on PATH and no Maven wrapper exists in the repository (`mvnw`/`mvnw.cmd` absent).
- No Java runtime artifacts were generated.
- No .NET tests were required for this Java-only disabled registry.

## Migration Parity Table - UOW-1349

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture` | future C# artifact/schema reader | Utility / Capture Context | Partial | Manual Only | Needs Verification | Added disabled bounded correlation registry and future observer accessor. Missing enable/config path, artifact writer, byte snapshot copying, decoded item/blob output, and runtime validation. |
| `com.aionemu.gameserver.network.aion.capture.ServerPacketCaptureObserver` | future C# artifact comparison contract | Interface | Partial | Manual Only | Needs Verification | Existing observer contract is now consumable by the pet-feed capture shell, but no observer is installed while `enabled` is false. |
| `com.aionemu.gameserver.network.aion.AionConnection.getActivePlayer` | future C# artifact reader player/scenario fields | Connection Context | Partial | Manual Only | Needs Verification | Registry uses active player to look up per-player pending contexts. Runtime connection/player timing is unverified. |
| `com.aionemu.gameserver.network.aion.AionServerPacket.write` | future Java runtime artifact producer; C# guarded artifact readers | Serialization Hook | Partial | Manual Only | Needs Verification | Existing disabled observer hook can call this registry only after a future install/enable path. No bytes are copied or persisted. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM` | `Aion.GameServer.Tests.PetFeedUnusualStorageJavaVectorArtifactReaderTests` | Packet Target | Partial | Unit Tested reader only | Needs Verification | Registry advances on packet class identity as the first expected serialized packet. It does not inspect warehouse type or item blob bytes yet. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate`; future artifacts | Packet Target | Partial | Unit Tested reader only | Needs Verification | Registry advances on packet class identity as the second expected serialized packet. It does not inspect action/actionValue bytes yet. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Implementation / Compile Attempt | `ItemPacketService.sendStorageUpdatePacket`, `AionServerPacket.write`, `AionConnection.getActivePlayer`, `SM_WAREHOUSE_ADD_ITEM`, and `SM_CUBE_UPDATE` source review | Adds a disabled correlation registry for the documented packet order. | Source implementation only. | Maven/Java 25 compile unavailable locally; no runtime artifacts; no enabled observer; no byte or schema comparison. |

## Remaining Risks

- Java compile validation is blocked locally by missing Maven/Java 25 tooling.
- The registry is not installed into `AionServerPacket` and `enabled` remains false.
- Matching uses packet class and active player only; future work must add scenario ids or route checks before writing artifacts.
- Bounded queues reduce leak risk but do not solve stale-context expiry if capture is enabled and packet order is disrupted.
- No packet bytes are copied, so serialization parity remains unverified.
- Reflection differences are not exercised because no reflective serializer or JSON writer exists.
- Threading behavior remains unproven under real connection queue serialization.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 disabled correlation registry inside the Java capture shell
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: Java compile validation, enable/config path, observer installation, artifact writer, byte copying, item blob decoder, live unusual-storage adapter
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a disabled artifact snapshot shape for the correlated context without writing files: define the in-memory DTO fields for storage route, packet order, clear-frame length/opcode metadata, and item/blob placeholders. Keep JSON output and capture enablement disabled until Java compile validation is available.
