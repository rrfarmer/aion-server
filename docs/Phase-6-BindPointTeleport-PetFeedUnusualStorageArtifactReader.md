# Phase 6 - Pet Feed Unusual Storage Artifact Reader

Date: May 27, 2026
Unit of Work: UOW-1345

## Scope

This unit adds a guarded C# test-side reader/comparator for future Java schema-v1 unusual-storage rejected-food runtime artifacts.

Java source of truth:

- `com.aionemu.gameserver.services.toypet.PetService.checkFeeding`
- `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUnlockPacket`
- `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE`
- `com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob`

C# touched:

- `Aion.GameServer.Tests.PetFeedUnusualStorageJavaVectorArtifactReaderTests`
- `Aion.GameServer.Services.ToyPet.PetFeedPacketMetadataBridge`
- `Aion.GameServer.Network.Aion.ServerPackets.SmWarehouseAddItem`
- `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate`
- `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo`

## What Changed

- Added `PetFeedUnusualStorageJavaVectorArtifactReaderTests`.
- Mirrored the existing Java vector reader pattern:
  - deserialize inline schema-v1 JSON with case-insensitive property names;
  - assert schema semantics and packet order;
  - scan `parity-artifacts/pet-feed-unusual-storage/java/*.json`;
  - report `Needs Verification` and return early when artifacts are absent.
- Validates Java storage id to C# modeled Java ordinal mapping through `SmCubeUpdate.TryGetJavaStorageOrdinal`.
- Validates future artifact packet order as `SM_WAREHOUSE_ADD_ITEM` followed by `SM_CUBE_UPDATE`.
- Validates warehouse-add decoded route fields: warehouse type equals Java storage id, add mask equals `ALL_SLOT`, and item count is `1`.
- Validates cube-update decoded fields: action `0`, action value equals Java ordinal, and all count/expand fields are zero.
- Reconstructs the guarded unusual-storage unlock sequence through `PetFeedPacketMetadataBridge` with supplied item/template context.
- Compares future `SM_CUBE_UPDATE` body and canonical payload bytes when Java artifacts include `bodyHex` or `canonicalPayloadHex`.
- Does not compare `SM_WAREHOUSE_ADD_ITEM` bytes yet because full item blob byte parity is still guarded.

## Known Blob Gap Classification

The reader carries explicit known serializer-gap categories so future warehouse-add bytes are not treated as generic failures:

- Java `STAT_BONUSES` blob entry.
- Fusion random bonus stats id.
- Temporary exchange and cleanup/account-legion warehouse seal flags.
- Plume tempering stat payload.
- Runtime conditioning presence.
- Time-dependent expiration and dye values.

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ParseUnusualStorageArtifact_ReadsSchemaV1PacketAndBlobFields` | Unit / Artifact Reader | UOW-1344 schema plus Java packet source review | Parses schema-v1 sample, validates timing/storage/blob metadata, checks guarded bridge sequence, and compares sample cube-update body/canonical bytes to C#. | Deterministic C# packet comparison for `SM_CUBE_UPDATE`; source-derived route assertions for `SM_WAREHOUSE_ADD_ITEM`. | Inline sample only; no Java runtime artifact bytes. Warehouse-add byte comparison guarded due item-blob gaps. |
| `FindUnusualStorageJavaArtifacts_IsGuardedUntilGeneratorOutputExists` | Guarded Artifact Scanner | `parity-artifacts/pet-feed-unusual-storage/java` contract | Scans future Java artifacts, validates schema semantics, compares cube-update bytes when present, and reports missing artifacts as `Needs Verification`. | Guarded reader behavior proven. | No Java generator/output exists yet. |

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PetFeedUnusualStorageJavaVectorArtifactReader"` passed 2 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PetFeedSubtype7JavaVectorArtifactReader|PetFeedUnusualStorageJavaVectorArtifactReader|PetFeedPacketMetadataBridge|SmWarehouseAddItem|SmCubeUpdate"` passed 133 tests.
- `dotnet build dotnetConversion/src/Aion.GameServer/Aion.GameServer.csproj` passed with 0 warnings and 0 errors.

No Java artifact generator, Java observer hook, live storage lookup, inventory mutation, packet send, live item/template/player/account/legion/house/pet hydration, scheduler execution, reward item creation, DAO write, warehouse-add byte comparison, or Java runtime packet comparison was enabled.

## Migration Parity Table - UOW-1345

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetService.checkFeeding` delayed rejected-food flow | `Aion.GameServer.Tests.PetFeedUnusualStorageJavaVectorArtifactReaderTests` | Service Flow / Artifact Reader | Partial | Unit Tested | Needs Verification | Reader validates schema timing fields for pre-delay lookup/post-delay rejection and mutable item reference. No Java runtime artifacts exist. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket` default branch | `Aion.GameServer.Services.ToyPet.PetFeedPacketMetadataBridge`; reader bridge assertion | Packet Service / Artifact Reader | Partial | Unit Tested | Partial Parity | Test reconstructs guarded unusual-storage sequence through the bridge as warehouse-add then cube-update. Live assembler still blocks unusual ids. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmWarehouseAddItem`; reader decoded-field validation | Packet | Partial | Unit Tested decoded fields only | Needs Verification | Reader validates warehouse type/add mask/item count and packet order. Full bytes remain guarded because item-blob parity is not proven. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE.cubeSize` unusual storage fallback | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate`; reader byte comparator | Packet | Partial | Unit Tested | Partial Parity | Inline sample validates zero-count cube-update body/canonical payload for storage id `32`/ordinal `4`; future artifact scanner compares bytes when Java output exists. |
| `com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteItemInfoBlob`; known-gap classifications | Serialization Helper | Partial | Unit Tested metadata only | Needs Verification | Reader requires blob entry ids/order and decoded metadata, and reports known serializer gaps. It does not claim byte parity. |

## Remaining Risks

- Java runtime artifacts are still absent.
- Java observer hook/generator is still absent.
- Warehouse-add byte comparison remains guarded by item blob serializer gaps.
- `PetFeedUnlockPacketContextAssembler` still intentionally rejects unusual storage ids.
- Live pet/house/broker/mailbox ownership, storage hydration, mutation, and dispatch remain disabled.
- C# snapshots do not model Java encode-time mutation of live `Item` references.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 guarded C# artifact reader/comparator test file
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime artifact generator, Java observer hook, warehouse-add item blob byte comparator, live unusual-storage adapter, socket dispatch
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Draft or implement the no-op-by-default Java serialization observer hook for unusual-storage rejected-food artifacts, or first add a more focused item-blob comparator fixture if Java network-core changes are not ready.
