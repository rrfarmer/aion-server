# Phase 6 Bind-Point Teleport - Pet Feed Unusual Storage STAT_BONUSES Blob

Date: 2026-05-27
Unit of Work: UOW-1388
Status: Focused C# `STAT_BONUSES` item-blob serialization implemented and regression tested.

## Scope

This unit implements the source-reviewed Java `BonusInfoBlobEntry` rule in the shared C# item-blob writer used by `SmInventoryInfo`, `SmInventoryAddItem`, `SmInventoryUpdateItem`, `SmWarehouseInfo`, `SmWarehouseAddItem`, and item attachment packet surfaces.

Java source of truth:

- `ItemInfoBlob.getFullBlob` appends `STAT_BONUSES` after `PREMIUM_OPTION` and before `GENERAL_INFO`.
- `BonusInfoBlobEntry.writeThisBlob` writes Java stat item-stone mask, signed value, and a rate flag.
- `StatEnum` supplies the item-stone mask and the special `ATTACK_SPEED` sign of `-1`.

## Changes

- Added `WriteStatBonusBlobs(...)` to `SmInventoryInfo`.
- Added `TryGetJavaItemStoneMaskAndSign(...)` with nonzero Java `StatEnum` item-stone masks needed by the item-blob protocol.
- `STAT_BONUSES` entries now serialize when:
  - `ItemStatModifier.Bonus` is true;
  - `ChargeCondition == 0`;
  - the stat name has a known nonzero Java item-stone mask.
- Entries write:
  - `H`: Java item-stone mask;
  - `D`: raw modifier value multiplied by Java stat sign;
  - `C`: `1` only when C# `Operation == "rate"`.
- Updated unusual-storage artifact reader diagnostics so `StatBonuses` is no longer reported as a known serializer gap.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GamePacketTests.SmInventoryInfo_WritesStatBonusBlobsAfterPremiumOptionLikeJava|FullyQualifiedName~GamePacketTests.SmInventoryInfo_WritesItemStoneAndIdianDetailsInItemBlobs|FullyQualifiedName~PetFeedUnusualStorageJavaVectorArtifactReaderTests" --no-restore`.
- Result: Passed, 4 tests.

## Remaining Gaps

- Generated Java runtime artifacts are still absent, so this is deterministic source-derived parity coverage, not runtime verified parity.
- Java condition handling is broader than C# `ChargeCondition`; the implementation intentionally skips the conditioned modifiers C# currently models.
- Unknown or zero-mask stat names are skipped until Java evidence proves they should be serialized.
- Warehouse-add byte comparison remains guarded by runtime artifact absence plus temporary-exchange/seal fields, plume tempering stats, conditioning presence, and time-normalized expiration/dye fields.
