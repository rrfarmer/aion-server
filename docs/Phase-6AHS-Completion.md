# Phase 6 AHS Completion - Plume Tempering Stat Pair Serialization

Date: 2026-05-27
Unit of Work: UOW-1391
Status: Plume tempering stat-pair serialization implemented and regression tested.

## Completed

- Updated `SmInventoryInfo.WriteItemInfoBlob` to pass `ItemTemplateSummary` into the enchant-info writer.
- Added Java-shaped plume stat-pair serialization in `SmInventoryInfo.WriteEnchantInfo`:
  - tempered physical plume writes `(42, 150 * tempering)` and `(30, 4 * tempering + randomPlumeBonus)`;
  - tempered magical plume writes `(42, 150 * tempering)` and `(35, 20 * tempering + randomPlumeBonus)`;
  - non-plume or non-tempered items keep the first two stat pairs zero.
- Kept the broker-service direct `WriteEnchantInfo` overload available; without template context it preserves prior zero plume stat-pair behavior.
- Added focused packet coverage for physical plume, magical plume, and non-plume zero behavior.
- Removed plume tempering stats from the unusual-storage known serializer gap list and added a guard assertion.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GamePacketTests.SmInventoryInfo_WritesPlumeTemperingStatsLikeJava|FullyQualifiedName~GamePacketTests.SmInventoryInfo_WritesItemStoneAndIdianDetailsInItemBlobs|FullyQualifiedName~PetFeedUnusualStorageJavaVectorArtifactReaderTests" --no-restore`.
- Result: passed 6 tests.
- Ran `git diff --check`.

## Migration Parity Table - UOW-1391

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.iteminfo.EnchantInfoBlobEntry` plume branch | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteEnchantInfo` | Serialization Entry | Partial | Regression Tested | Partial Parity | C# now writes Java-shaped first two plume stat id/value pairs for full item blobs with template context. Full enchant blob parity still needs Java runtime byte comparison because dye/expiration and other dynamic fields remain time/state dependent. |
| `com.aionemu.gameserver.model.stats.container.PlumStatEnum` | private constants in `SmInventoryInfo.WritePlumeTemperingStatPairs` | Enum / Constants | Refactored | Regression Tested | Partial Parity | Java ids/boosts are encoded directly with a Java breadcrumb: HP `(42,150)`, physical `(30,4)`, magical `(35,20)`. No standalone C# enum was added. |
| `com.aionemu.gameserver.model.templates.item.ItemTemplate` item group / tempering name access | `Aion.GameServer.Dataholders.ItemTemplateSummary.IsPlume` / `TemperingName` | DTO / Template Summary | Complete for this payload | Regression Tested | Partial Parity | Tests cover `ItemGroup == "PLUME"` and `TemperingName == "TSHIRT_PHYSICAL"` branch selection. Static-data parsing was not revalidated in this unit. |
| `com.aionemu.gameserver.model.gameobjects.Item` tempering/random plume fields | `Aion.GameServer.Model.GameObjects.InventoryItem.Tempering` / `RandomPlumeBonus` | Model | Partial | Regression Tested | Needs Verification | Serializer consumes existing fields. Java random plume mutation behavior is not implemented or verified here. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_BROKER_SERVICE` enchant-info calls | `Aion.GameServer.Network.Aion.ServerPackets.SmBrokerService` direct `WriteEnchantInfo` calls | Packet Call Site | Partial | Existing compile/test coverage only | Needs Verification | Broker call sites still lack template context and therefore retain zero plume stat-pair behavior. This preserves compatibility but may still differ from Java for brokered tempered plumes. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GamePacketTests.SmInventoryInfo_WritesPlumeTemperingStatsLikeJava` | Regression / packet byte layout | `EnchantInfoBlobEntry` and `PlumStatEnum` source inspection | Physical plume `(42,750)/(30,23)`, magical plume `(42,750)/(35,108)`, and non-plume zero stat pairs at the Java enchant-blob offset. | Deterministic constants and offsets from Java source. | Does not compare against generated Java packet bytes. |
| `GamePacketTests.SmInventoryInfo_WritesItemStoneAndIdianDetailsInItemBlobs` | Regression | Existing C# packet layout coverage | Ensures the existing 138-byte enchant-info layout still carries item stones, idian data, tempering, amplification, and buff skill. | C# regression only. | Not a Java runtime comparison. |
| `PetFeedUnusualStorageJavaVectorArtifactReaderTests` filtered tests | Guard / artifact-reader regression | Known gap list policy | Ensures `PlumeTemperingStats` is no longer listed as a known C# serializer gap. | C# guard assertion. | Artifact byte comparison still guarded by missing Java runtime artifacts and remaining gaps. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling, so parity is not verified against live Java packet bytes.
- Broker-service direct enchant-info call sites still lack template context; brokered tempered plume payloads may need a separate template-aware bridge.
- Temporary-exchange time, cleanup/seal warehouse restriction flags, runtime conditioning presence, and time-dependent expiration/dye fields still block full warehouse-add byte comparison.
- `RandomPlumeBonus` mutation parity remains outside this serializer unit.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 3 C# surfaces changed or guarded
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime artifact generation, broker plume template context, temporary-exchange/seal fields, runtime conditioning presence, time-normalized expiration/dye comparison
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: audit cleanup/seal warehouse restriction static-data ownership.
- Scope:
  - locate Java `item_restriction_cleanups` XML/static-data load path;
  - identify C# static-data/dataholder ownership for equivalent cleanup records;
  - decide how `WriteGeneralInfoBlob` should receive the cleanup/seal flag without coupling packet serialization directly to global data.

## Safe Parallel Candidates

- Temporary exchange model task: identify all Java callers that set `temporaryExchangeTime` and plan the C# inventory-item field/hydration path.
- Broker plume task: inspect Java broker packet item-template access and plan template-aware C# broker enchant-info serialization.
- Java tooling task: generate the first unusual-storage runtime artifact in a Maven/JDK environment.

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Agent A | Cleanup/seal static-data audit | read-only Java XML/dataholder files, C# static-data files | runtime packet writes |
| Agent B | Broker plume context audit | broker packet/repository source reads | shared docs until integration |
