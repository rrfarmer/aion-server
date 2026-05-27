# Phase 6AGC Completion Handoff

Date: May 27, 2026
Completed Unit of Work: UOW-1349
Latest Commit: included in `[Phase 6][UOW-1349] Add unusual storage correlation registry`
Status: Disabled Java correlation registry exists for unusual-storage rejected-food unlock artifact capture; validation remains blocked locally by missing Maven/Java 25 tooling.

## What Changed

- Updated `game-server/src/com/aionemu/gameserver/services/toypet/PetFeedUnusualStorageArtifactCapture.java`.
- Added a bounded per-player pending context queue.
- Added `PetFeedUnusualStorageArtifactCapture.observer()` as a future `ServerPacketCaptureObserver` install point.
- When future capture is enabled, `registerStorageUpdate(...)` records storage id, storage ordinal, and item object id for guarded `ALL_SLOT` unusual-storage updates.
- The observer advances pending contexts on serialized `SM_WAREHOUSE_ADD_ITEM` followed by `SM_CUBE_UPDATE`.
- Runtime exceptions from the observer path are swallowed so capture cannot interrupt packet dispatch.
- Added `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCorrelationRegistry.md`.
- Updated `docs/PHASE-6-PROGRESS.md`.
- Updated `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`.

## Code Changed

- `game-server/src/com/aionemu/gameserver/services/toypet/PetFeedUnusualStorageArtifactCapture.java`

## Documentation Changed

- `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCorrelationRegistry.md`
- `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AGC-Completion.md`

## Validation Completed

- Attempted `mvn -pl game-server -am -DskipTests compile`.
- Java validation was blocked because `mvn` is not available on PATH.
- Confirmed no Maven wrapper exists in the repository (`mvnw` and `mvnw.cmd` absent).

No enabled observer, artifact writer, file output, live storage lookup, inventory mutation, packet send, live item/template/player/account/legion/house/pet hydration, scheduler execution, reward item creation, DAO write, warehouse-add byte comparison, or Java runtime packet comparison was enabled.

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
| None | Implementation / Compile Attempt | Java construction and serialization source review | Adds a disabled correlation registry for the documented packet order. | Source implementation only. | Maven/Java 25 compile unavailable locally; no runtime artifacts; no enabled observer; no byte or schema comparison. |

## Remaining Risks

- Java compile validation is blocked locally by missing Maven/Java 25 tooling.
- The registry is not installed into `AionServerPacket` and `enabled` remains false.
- Matching uses packet class and active player only; future work must add scenario ids or route checks before writing artifacts.
- Bounded queues reduce leak risk but do not solve stale-context expiry if capture is enabled and packet order is disrupted.
- No packet bytes are copied, so serialization parity remains unverified.
- Threading behavior remains unproven under real connection queue serialization.
- Full `SM_WAREHOUSE_ADD_ITEM` byte parity remains blocked by item-blob serializer gaps.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 disabled correlation registry inside the Java capture shell
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: Java compile validation, enable/config path, observer installation, artifact writer, byte copying, item blob decoder, live unusual-storage adapter
- Estimated overall migration completion: Phase 6 remains about 71% complete

# Next Work Options

## Recommended Sequential Task

- Task: Add a disabled artifact snapshot shape for the correlated context without writing files.
- Why: The registry can now pair construction and serialization events by packet order, but future schema-v1 output still needs an in-memory object that names storage route, packet order, clear-frame metadata, and item/blob placeholders before any writer is added.
- Files:
  - `game-server/src/com/aionemu/gameserver/services/toypet/PetFeedUnusualStorageArtifactCapture.java`
  - docs/progress/handoff

## Safe Parallel Candidates

- Read-only audit: identify Java JSON libraries or existing server config/output conventions suitable for a bounded artifact writer.
- Read-only audit: map `ItemInfoBlob` entry ids/order to the existing C# known-gap classifications for future decoded blob output.
- C# test-only extension: add guarded fixture expectations for correlation/snapshot schema fields after the Java schema is finalized.

## Validation Recommendation

- Re-run `mvn -pl game-server -am -DskipTests compile` in an environment with Maven and Java 25 before enabling or extending capture.

## Do Not Parallelize

- Shared Java capture files with observer/correlation/snapshot changes unless one owner coordinates both.
- Artifact writer implementation with schema/correlation changes.
- Shared progress/handoff docs: orchestrator-owned only.

## Context Files

- Java source:
  - `game-server/src/com/aionemu/gameserver/services/toypet/PetFeedUnusualStorageArtifactCapture.java`
  - `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/AionServerPacket.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/capture/ServerPacketCaptureObserver.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/AionConnection.java`
- C# source/tests:
  - `dotnetConversion/tests/Aion.GameServer.Tests/PetFeedUnusualStorageJavaVectorArtifactReaderTests.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/ToyPet/PetFeedPacketMetadataBridge.cs`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmCubeUpdate.cs`
- Docs:
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCorrelationRegistry.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageConstructionContextSeam.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedJavaPacketCaptureObserverShell.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageArtifactReader.md`
  - `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
