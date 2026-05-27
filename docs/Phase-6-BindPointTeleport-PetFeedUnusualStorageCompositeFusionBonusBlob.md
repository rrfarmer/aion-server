# Phase 6 Bind-Point Teleport - Pet Feed Unusual Storage Composite Fusion Bonus Blob

Date: 2026-05-27
Unit of Work: UOW-1386
Status: Focused C# item-blob gap closed for composite fusion bonus id.

## Scope

Java `CompositeItemBlobEntry.writeThisBlob` writes the fusioned item id, six fusion stone ids, the optional fusion socket count, and `ownerItem.getFusionedItemBonusStatsId()`.

C# already carried this field as `InventoryItem.FusionRandomBonus`, but `SmInventoryInfo.WriteCompositeItemBlob` wrote zero in that byte. This unit wires the existing value into the composite blob and updates the focused packet test.

## Changes

- Updated `SmInventoryInfo.WriteCompositeItemBlob` to write `item.FusionRandomBonus`.
- Updated `GamePacketTests.SmInventoryInfo_WritesItemStoneAndIdianDetailsInItemBlobs` to set and assert the composite fusion random bonus byte.
- Removed `FusionRandomBonusStatsId` from the guarded unusual-storage artifact reader's known serializer gap list.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GamePacketTests.SmInventoryInfo_WritesItemStoneAndIdianDetailsInItemBlobs|FullyQualifiedName~PetFeedUnusualStorageJavaVectorArtifactReaderTests" --no-restore`.
- Result: Passed, 3 tests.

## Remaining Gaps

- Java runtime artifact generation is still absent, so this is deterministic source-derived parity coverage, not verified runtime parity.
- Warehouse-add byte comparison remains guarded by missing `STAT_BONUSES`, temporary exchange/seal fields, plume tempering stats, conditioning presence, and time-normalized expiration/dye fields.
