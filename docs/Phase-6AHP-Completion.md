# Phase 6 AHP Completion - STAT_BONUSES Blob Serialization

Date: 2026-05-27
Unit of Work: UOW-1388
Status: Focused C# `STAT_BONUSES` serializer implementation complete.

## Completed

- Updated `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmInventoryInfo.cs`.
- Updated `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`.
- Updated `dotnetConversion/tests/Aion.GameServer.Tests/PetFeedUnusualStorageJavaVectorArtifactReaderTests.cs`.
- Added `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageStatBonusesBlob.md`.
- Added `STAT_BONUSES` serialization after `PREMIUM_OPTION` and before `GENERAL_INFO`, matching Java `ItemInfoBlob.getFullBlob` entry order.
- Added Java `StatEnum` item-stone mask/sign mapping for nonzero mask values used by the item-blob protocol.
- Added focused regression coverage for `MAXHP` add bonus, `ATTACK_SPEED` rate bonus sign inversion, non-bonus skip, and conditioned bonus skip.
- Removed `StatBonuses` from guarded unusual-storage artifact reader known-gap diagnostics.
- Updated live-adapter readiness and progress/handoff notes.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GamePacketTests.SmInventoryInfo_WritesStatBonusBlobsAfterPremiumOptionLikeJava|FullyQualifiedName~GamePacketTests.SmInventoryInfo_WritesItemStoneAndIdianDetailsInItemBlobs|FullyQualifiedName~PetFeedUnusualStorageJavaVectorArtifactReaderTests" --no-restore`.
- Result: Passed, 4 tests.
- Ran `git diff --check`.

## Migration Parity Table - UOW-1388

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.iteminfo.BonusInfoBlobEntry` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteStatBonusBlobs` | Serialization Entry | Complete for currently modeled condition-free template modifiers | Regression Tested | Partial Parity | C# writes mask, Java-signed value, and rate flag for bonus modifiers with known nonzero masks. Java runtime artifact comparison is still missing. Broader Java condition objects are not fully modeled in C#. |
| `com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteItemInfoBlob` | Serialization Utility | Partial | Regression Tested for stat-bonus entry order | Needs Verification | `STAT_BONUSES` now appears after `PREMIUM_OPTION` and before `GENERAL_INFO`. Full blob parity remains blocked by temporary exchange/seal fields, plume tempering stats, conditioning presence, and time-normalized expiration/dye fields. |
| `com.aionemu.gameserver.model.stats.container.StatEnum` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.TryGetJavaItemStoneMaskAndSign` | Enum / Mapping | Partial | Regression Tested for representative values | Needs Verification | C# contains nonzero item-stone masks from Java `StatEnum` and the `ATTACK_SPEED` negative sign. Tests cover `MAXHP` and `ATTACK_SPEED`; all other mappings remain source-reviewed only. |
| `com.aionemu.gameserver.model.stats.calc.functions.StatFunction` | `Aion.GameServer.Dataholders.ItemStatModifier` | DTO / Modifier Model | Partial | Regression Tested for bonus and conditioned skip | Needs Verification | C# serializes only `Bonus == true` and `ChargeCondition == 0`. Other Java condition types are not preserved in the current C# modifier model. |
| `com.aionemu.gameserver.model.stats.calc.functions.StatRateFunction` | `Aion.GameServer.Dataholders.ItemStatModifier.Operation == "rate"` | Modifier Function | Partial | Regression Tested | Partial Parity | C# maps `Operation == "rate"` to Java rate flag `1`; non-rate modifiers write `0`. Runtime artifact comparison still missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmWarehouseAddItem` | Server Packet | Partial | Regression Tested indirectly | Needs Verification | Shared item-blob writer now includes stat bonuses, but warehouse-add byte comparison remains guarded until Java artifacts and remaining blob gaps are resolved. |
| `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture` | `Aion.GameServer.Tests.PetFeedUnusualStorageJavaVectorArtifactReaderTests` | Artifact Reader / Diagnostics | Partial | Unit Tested | Needs Verification | Known serializer-gap diagnostics now omit the resolved `STAT_BONUSES` gap. Generated Java runtime artifacts are still absent. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `SmInventoryInfo_WritesStatBonusBlobsAfterPremiumOptionLikeJava` | Regression | `ItemInfoBlob.getFullBlob`, `BonusInfoBlobEntry.writeThisBlob`, `StatEnum` | Verifies `STAT_BONUSES` order, `MAXHP` mask/value/rate flag, `ATTACK_SPEED` sign inversion and rate flag, non-bonus skip, and conditioned skip. | Deterministic source-derived byte assertions in C#. | Does not compare against generated Java runtime bytes and covers representative mappings only. |
| `SmInventoryInfo_WritesItemStoneAndIdianDetailsInItemBlobs` | Regression | Existing item-blob entry coverage | Ensures existing composite/enchant/polish blob coverage still passes with stat-bonus writer added. | C# regression coverage. | Does not compare generated Java bytes. |
| `ParseUnusualStorageArtifact_ReadsSchemaV1PacketAndBlobFields` | Unit / Diagnostic | Java capture schema and C# known-gap list | Confirms `StatBonuses` is no longer reported as a known serializer gap. | Synthetic artifact reader still passes. | No generated Java artifact. |
| `FindUnusualStorageJavaArtifacts_IsGuardedUntilGeneratorOutputExists` | Guarded Unit | Java artifact output plan | Keeps artifact ingestion guarded and diagnostics current. | Guarded test pass. | No generated Java artifact. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Warehouse-add byte comparison remains blocked by generated artifact absence and remaining item-blob gaps.
- C# currently models only charge conditions for item-template modifiers; Java has broader condition objects.
- Unknown or zero-mask stat names are skipped until Java evidence proves they should be serialized.
- Temporary-exchange and cleanup/seal fields need additional C# model/static-data inputs.
- Expiration/dye comparisons need artifact-normalized Java remaining seconds instead of replay-time local clock.
- Conditioning entry presence and plume tempering stat payloads may still differ from Java runtime state.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 1 focused C# stat-bonus blob entry serializer plus Java stat mask/sign helper
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: Java runtime artifact generation, warehouse-add byte comparison, remaining item-blob serializer gaps
- Estimated overall migration completion: Phase 6 remains about 71% complete

# Next Work Options

## Recommended Sequential Task

- Task: Add a read-only temporary-exchange and cleanup/seal item-blob input audit.
- Scope:
  - inspect Java `GeneralInfoBlobEntry`, `Item.getTemporaryExchangeTimeRemaining`, and `DataManager.ITEM_CLEAN_UP.hasAccountOrLegionWhStorabilityDisabled`;
  - identify where C# item snapshots/static data can carry these values;
  - do not implement serializer fields until model/static-data ownership is clear.

## Safe Parallel Candidates

- Java tooling task: generate the first unusual-storage runtime artifact in a Maven/JDK environment.
- Test helper task: add reusable blob-entry scanner helpers for packet tests.
- Read-only plume audit: map Java plume tempering stat payload to C# `TemperingTable`/`RandomPlumeBonus`.

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Agent A | Temporary-exchange / cleanup-seal audit | read-only Java item/model/dataholder files and C# item/static-data files | all writes |
| Agent B | Plume tempering stat payload audit | read-only Java `EnchantInfoBlobEntry`, `PlumStatEnum`, C# `TemperingTable` | all writes |
| Orchestrator | Docs/parity integration | shared docs only after audits | production serializer files unless selected for next UOW |

## Do Not Parallelize

- General-info serializer implementation with static-data/model discovery.
- Warehouse-add byte comparison with any item-blob serializer changes.
- Shared progress/handoff docs between agents.
