# Phase 6AGB Completion Handoff

Date: May 27, 2026
Completed Unit of Work: UOW-1348
Latest Commit: included in `[Phase 6][UOW-1348] Add unusual storage construction capture seam`
Status: Disabled Java construction context seam exists for unusual-storage rejected-food unlock artifact capture; validation remains blocked locally by missing Maven/Java 25 tooling.

## What Changed

- Added `game-server/src/com/aionemu/gameserver/services/toypet/PetFeedUnusualStorageArtifactCapture.java`.
- Updated `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`.
- `sendStorageUpdatePacket(...)` now calls `PetFeedUnusualStorageArtifactCapture.registerStorageUpdate(...)` before the Java packet-selection switch.
- The capture shell is disabled by default and currently no-ops.
- The unusual-storage predicate covers:
  - Java pet bag ids `32` through `43`;
  - Java house warehouse ids `60` through `79`;
  - Java `BROKER`;
  - Java `MAILBOX`.
- Added `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageConstructionContextSeam.md`.
- Updated `docs/PHASE-6-PROGRESS.md`.
- Updated `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`.

## Code Changed

- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
- `game-server/src/com/aionemu/gameserver/services/toypet/PetFeedUnusualStorageArtifactCapture.java`

## Documentation Changed

- `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageConstructionContextSeam.md`
- `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AGB-Completion.md`

## Validation Completed

- Attempted `mvn -pl game-server -am -DskipTests compile`.
- Java validation was blocked because `mvn` is not available on PATH.
- Confirmed no Maven wrapper exists in the repository (`mvnw` and `mvnw.cmd` absent).

No enabled observer, scenario context registry, artifact writer, live storage lookup, inventory mutation, packet send, live item/template/player/account/legion/house/pet hydration, scheduler execution, reward item creation, DAO write, warehouse-add byte comparison, or Java runtime packet comparison was enabled.

## Migration Parity Table - UOW-1348

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket` | `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture.registerStorageUpdate`; future C# artifact reader | Service Flow / Observer Context | Partial | Manual Only | Needs Verification | Registration call added before the packet-selection switch. Guard keeps runtime no-op unless a future enable path sets `enabled`, `addType` is `ALL_SLOT`, and storage is unusual. Maven compile unavailable locally. |
| `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture` | future C# artifact/schema reader | Utility / Capture Context | Partial | No Tests | Needs Verification | New disabled no-op context seam. Missing enable/config method, scenario correlation, artifact writer, packet-byte pairing, and item/blob extraction. |
| `com.aionemu.gameserver.model.items.storage.StorageType` pet/house/broker/mailbox ids | `PetFeedUnusualStorageArtifactCapture.isUnusualStorage`; `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate.TryGetJavaStorageOrdinal` | Enum / Predicate | Partial | Manual Only | Needs Verification | Predicate uses Java min/max constants and exact enum identity for broker/mailbox. No Java compile/runtime validation. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM` | future Java artifacts; `Aion.GameServer.Tests.PetFeedUnusualStorageJavaVectorArtifactReaderTests` | Packet Target | Partial | Unit Tested reader only | Needs Verification | Context seam sits before warehouse-add construction but does not yet capture construction facts, clear bytes, or decoded item/blob data. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE.cubeSize` | future Java artifacts; `Aion.GameServer.Tests.PetFeedUnusualStorageJavaVectorArtifactReaderTests` | Packet Target | Partial | Unit Tested reader only | Needs Verification | Context seam sits before the trailing cube update is queued. It does not yet pair construction context with `AionServerPacket.write` serialization bytes. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Implementation / Compile Attempt | `ItemPacketService.sendStorageUpdatePacket`, `StorageType`, `SM_WAREHOUSE_ADD_ITEM`, and `SM_CUBE_UPDATE.cubeSize` source review | Adds the disabled construction seam at the documented packet routing point. | Source implementation only. | Maven/Java 25 compile unavailable locally; no runtime artifacts; no enabled capture; no C# runtime comparison. |

## Remaining Risks

- Java compile validation is blocked locally by missing Maven/Java 25 tooling.
- `PetFeedUnusualStorageArtifactCapture.enabled` has no config or setter yet and remains false.
- Scenario correlation between construction context and `AionServerPacket.write` bytes is missing.
- Artifact writer/JSON serializer is missing.
- Item/blob decoder output is missing.
- Future capture must avoid blocking packet dispatch or leaking unrelated packet/player data.
- Full `SM_WAREHOUSE_ADD_ITEM` byte parity remains blocked by item-blob serializer gaps.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 disabled construction seam plus 1 Java call-site hook
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java compile validation, enable/config path, scenario correlation, artifact writer, item blob decoder, live unusual-storage adapter
- Estimated overall migration completion: Phase 6 remains about 71% complete

# Next Work Options

## Recommended Sequential Task

- Task: Add a disabled scenario correlation registry between `PetFeedUnusualStorageArtifactCapture.registerStorageUpdate(...)` and `ServerPacketCaptureObserver.onPacketSerialized(...)`.
- Why: The construction seam and generic bytes hook now both exist, but schema-v1 needs a narrow way to pair route context with serialized `SM_WAREHOUSE_ADD_ITEM` and `SM_CUBE_UPDATE` bytes.
- Files:
  - `game-server/src/com/aionemu/gameserver/services/toypet/PetFeedUnusualStorageArtifactCapture.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/capture/ServerPacketCaptureObserver.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/AionServerPacket.java` only if the existing callback lacks required context
  - docs/progress/handoff

## Safe Parallel Candidates

- Read-only audit: identify Java JSON libraries or existing server config/output conventions suitable for a bounded artifact writer.
- Read-only audit: map `ItemInfoBlob` entry ids/order to the existing C# known-gap classifications for future decoded blob output.
- C# test-only extension: add guarded fixture expectations for construction-context schema fields after the Java schema is finalized.

## Validation Recommendation

- Re-run `mvn -pl game-server -am -DskipTests compile` in an environment with Maven and Java 25 before enabling or extending capture.

## Do Not Parallelize

- Shared Java capture files with observer/correlation changes unless one owner coordinates both.
- Artifact writer implementation with schema/correlation changes.
- Shared progress/handoff docs: orchestrator-owned only.

## Context Files

- Java source:
  - `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
  - `game-server/src/com/aionemu/gameserver/services/toypet/PetFeedUnusualStorageArtifactCapture.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/AionServerPacket.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/capture/ServerPacketCaptureObserver.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/capture/NoOpServerPacketCaptureObserver.java`
- C# source/tests:
  - `dotnetConversion/tests/Aion.GameServer.Tests/PetFeedUnusualStorageJavaVectorArtifactReaderTests.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/ToyPet/PetFeedPacketMetadataBridge.cs`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmCubeUpdate.cs`
- Docs:
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageConstructionContextSeam.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedJavaPacketCaptureObserverShell.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageJavaObserverPlacement.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageArtifactReader.md`
  - `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
