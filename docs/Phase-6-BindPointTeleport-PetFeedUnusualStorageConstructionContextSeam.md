# Phase 6 - Pet Feed Unusual Storage Construction Context Seam

Date: May 27, 2026
Unit of Work: UOW-1348

## Scope

This unit adds the disabled-by-default Java construction context seam identified by UOW-1346 and left as the recommended next step by UOW-1347.

Java source touched:

- `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket`
- `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture`
- `com.aionemu.gameserver.model.items.storage.StorageType`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE.cubeSize`

## What Changed

- Added `PetFeedUnusualStorageArtifactCapture`.
- Added a guarded call from `ItemPacketService.sendStorageUpdatePacket(...)` before the Java packet-selection switch.
- Added an unusual-storage predicate using Java `StorageType` source constants:
  - pet bag ids `PET_BAG_MIN` through `PET_BAG_MAX`;
  - house warehouse ids `HOUSE_WH_MIN` through `HOUSE_WH_MAX`;
  - exact `BROKER` and `MAILBOX` enum values.
- Kept the seam disabled by default. `enabled` is `false`, no config flag is wired, and the current registration method does not write artifacts.

The future capture point is now adjacent to Java's construction-time routing facts: `Player`, `StorageType`, `Item`, and `ItemAddType`. The Java source of truth still sends unusual rejected-food unlock storage as `SM_WAREHOUSE_ADD_ITEM(storageType.getId(), ...)` followed by `SM_CUBE_UPDATE.cubeSize(storageType, player)`.

## Boundaries Preserved

- No artifact writing was added.
- No observer implementation was enabled.
- No scenario correlation token or registry was added.
- No packet byte capture was added by this seam.
- No item/blob decoding was added.
- No live dispatch behavior is intended to change because the seam remains disabled.
- No C# live unusual-storage adapter was enabled.

## Validation

- Attempted `mvn -pl game-server -am -DskipTests compile`.
- Validation was blocked because `mvn` is not available on PATH and no Maven wrapper exists in the repository (`mvnw`/`mvnw.cmd` absent).
- No Java runtime artifacts were generated.
- No .NET tests were required for this Java-only disabled seam.

## Migration Parity Table - UOW-1348

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket` | `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture.registerStorageUpdate`; future C# artifact reader | Service Flow / Observer Context | Partial | Manual Only | Needs Verification | Registration call added before the packet-selection switch. Guard keeps runtime no-op unless a future enable path sets `enabled`, `addType` is `ALL_SLOT`, and storage is unusual. Maven compile unavailable locally. |
| `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture` | future C# artifact/schema reader | Utility / Capture Context | Partial | No Tests | Needs Verification | New disabled no-op context seam. Missing enable/config method, scenario correlation, artifact writer, packet-byte pairing, and item/blob extraction. |
| `com.aionemu.gameserver.model.items.storage.StorageType` pet/house/broker/mailbox ids | `PetFeedUnusualStorageArtifactCapture.isUnusualStorage`; `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate.TryGetJavaStorageOrdinal` | Enum / Predicate | Partial | Manual Only | Needs Verification | Predicate uses Java min/max constants and exact enum identity for broker/mailbox. No Java compile/runtime validation. C# ordinal helper remains separately tested but not runtime-compared to Java artifacts. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM` | future Java artifacts; `Aion.GameServer.Tests.PetFeedUnusualStorageJavaVectorArtifactReaderTests` | Packet Target | Partial | Unit Tested reader only | Needs Verification | Context seam sits before warehouse-add construction but does not yet capture construction facts, clear bytes, or decoded item/blob data. Full byte parity remains blocked by missing Java artifacts and item-blob gaps. |
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
- Threading behavior for future artifact writes is undefined; capture must avoid blocking packet dispatch.
- Reflection differences are not exercised because no reflection-based capture or serializer exists yet.
- Serialization parity is unverified because no Java bytes were generated.
- Date/time and precision risks remain in future item-blob fields such as expiration, dye, conditioning, and temporary exchange/seal metadata.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 disabled construction seam plus 1 Java call-site hook
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java compile validation, enable/config path, scenario correlation, artifact writer, item blob decoder, live unusual-storage adapter
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a disabled scenario correlation registry between `PetFeedUnusualStorageArtifactCapture.registerStorageUpdate(...)` and `ServerPacketCaptureObserver.onPacketSerialized(...)`, still without artifact writing by default. The registry should be narrow to pet-feed unusual-storage unlock packets, bounded, and exception-isolated before any JSON artifact writer is added.
