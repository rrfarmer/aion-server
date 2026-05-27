# Phase 6 AHU Completion - Cleanup/Seal Static-Data Projection

Date: 2026-05-27
Unit of Work: UOW-1393
Status: Cleanup/seal static-data projection implemented and focused tests added. Packet serialization plumbing remains intentionally deferred.

## Completed

- Added `ItemRestrictionCleanupTable` and `ItemRestrictionCleanupSummary`.
- Wired `StaticData.LoadFromCacheAsync` to parse Java `<item_restriction_cleanups>/<cleanup>` rows.
- Exposed `StaticData.ItemRestrictionCleanups`.
- Preserved Java optional byte defaults for missing `trade`, `sell`, `wh`, `awh`, and `lwh` as `-1`.
- Implemented Java predicate parity for `ItemRestrictionCleanupData.hasAccountOrLegionWhStorabilityDisabled(itemId)`: matching item id with `awh == 0 || lwh == 0`.
- Added focused static-data loader coverage for Java defaults and predicate behavior.
- Extended the real Java static-data manifest test to assert cleanup row count and the known `188053996` account/legion warehouse restriction.
- Spawned and closed two read-only sidecar audit agents:
  - temporary-exchange source audit;
  - broker plume template-context audit.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~StaticDataLoadingTests.StaticData_LoadsItemRestrictionCleanupFlagsLikeJava|FullyQualifiedName~StaticDataLoadingTests.DataManager_LoadsRealJavaStaticDataManifestCounts" --no-restore`.
- Result: passed 2 tests.
- Ran `git diff --check`.

## Migration Parity Table - UOW-1393

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dataholders.StaticData.itemCleanup` | `Aion.GameServer.Dataholders.StaticData.ItemRestrictionCleanups` | Static Data Root | Partial | Regression Tested | Partial Parity | C# now projects cleanup rows from merged static data. Packet consumers are not wired yet, so full general-info parity remains incomplete. |
| `com.aionemu.gameserver.dataholders.DataManager.ITEM_CLEAN_UP` | `Aion.GameServer.Dataholders.StaticData.ItemRestrictionCleanups` runtime access | Static Data Service | Partial | Regression Tested | Needs Verification | C# exposes the table through `StaticData`, not a Java-style global field. This is a C# dependency-flow choice; packet call sites still need explicit bridge wiring. |
| `com.aionemu.gameserver.dataholders.ItemRestrictionCleanupData` | `Aion.GameServer.Dataholders.ItemRestrictionCleanupTable` | Dataholder | Complete for known predicate | Unit Tested | Partial Parity | `HasAccountOrLegionWarehouseStorabilityDisabled` matches Java's item-id and `awh/lwh` predicate. Other cleanup uses such as template-mask mutation are not implemented here. |
| `com.aionemu.gameserver.model.templates.restriction.ItemCleanupTemplate` | `Aion.GameServer.Dataholders.ItemRestrictionCleanupSummary` | DTO | Complete for parsed attributes | Unit Tested | Partial Parity | C# preserves optional byte defaults as `-1` and parses `id`, `trade`, `sell`, `wh`, `awh`, and `lwh`. JAXB schema validation and duplicate row semantics are only partially modeled. |
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` cleanup/seal flag | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteGeneralInfoBlob` | Serialization Entry | Partial | Manual Only | Needs Verification | C# still writes zero for the cleanup/seal flag. This unit intentionally stops before packet call-site plumbing. |
| `com.aionemu.gameserver.model.gameobjects.Item` temporary exchange fields | `Aion.GameServer.Model.GameObjects.InventoryItem` future temporary-exchange field | Model | Not Started | Manual Only | Needs Verification | Sidecar audit confirmed Java temporary exchange is runtime-only, not DB-persisted, and C# lacks item/template fields for it. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_BROKER_SERVICE` broker item enchant info | `Aion.GameServer.Network.Aion.ServerPackets.SmBrokerService` | Packet Call Site | Partial | Manual Only | Needs Verification | Sidecar audit confirmed Java broker items retain template context, while C# broker packet paths still use the template-less enchant-info overload. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `StaticDataLoadingTests.StaticData_LoadsItemRestrictionCleanupFlagsLikeJava` | Unit / static-data loader regression | `ItemRestrictionCleanupData` and `ItemCleanupTemplate` source plus Java XML schema | Parses cleanup rows, keeps missing byte attributes at `-1`, ignores `trade/sell/wh` for the packet predicate, and treats either `awh=0` or `lwh=0` as restricted. | Deterministic Java source/schema behavior. | Does not test packet serialization or Java runtime output. |
| `StaticDataLoadingTests.DataManager_LoadsRealJavaStaticDataManifestCounts` | Regression / real static data load | Java `items/item_restriction_cleanups.xml` | Confirms real merged Java static data exposes the cleanup row count and known restricted item id `188053996`. | Real Java static-data corpus loaded by C# merger/loader. | Does not compare Java runtime `DataManager` object directly. |

## Sidecar Audit Results

- Temporary exchange audit: Java `temporaryExchangeTime` is an item instance `int` runtime field, set from multi-looter drop collection using template `temp_exchange_time` minutes, cleared by `TemporaryTradeTimeTask`, not loaded from or saved to `InventoryDAO`, and serialized as absolute epoch seconds minus current epoch seconds without clamping unless the task has cleared it.
- Broker plume audit: Java broker packet paths call enchant-info serialization on full `Item` objects that retain `ItemTemplate`, so tempered plume stat pairs are available. C# broker packet paths still use template-less `WriteEnchantInfo`, so brokered tempered plume stat pairs remain zero.

## Remaining Risks

- `SmInventoryInfo.WriteGeneralInfoBlob` still writes zero for cleanup/seal and temporary-exchange fields.
- Packet call sites need a deterministic cleanup/seal flag bridge without global serializer coupling.
- Java `ItemData.cleanup()` template-mask mutation from cleanup rows is not implemented in this unit.
- Temporary-exchange static template field and runtime item field are still missing.
- Broker plume template context remains a separate packet parity gap.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 4 C# surfaces changed or added
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: cleanup/seal packet flag plumbing, Java runtime artifact generation, temporary-exchange model/template fields, broker plume template context, runtime conditioning presence, time-normalized expiration/dye comparison
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: wire cleanup/seal flag into item-blob general-info serialization with focused packet tests.
- Scope:
  - add a deterministic packet input/options path for the cleanup/seal flag;
  - keep default behavior zero for callers that do not supply cleanup data;
  - add a focused item-blob test for item id `188053996` expecting the general-info `H` field to be `3`;
  - do not attempt temporary-exchange or wall-clock normalization in the same unit.

## Safe Parallel Candidates

- Temporary exchange model/static source unit: load `temp_exchange_time` into `ItemTemplateSummary` and add an `InventoryItem` runtime absolute epoch field with injectable remaining-time helper tests.
- Broker plume bridge unit: add template-aware `SmBrokerService` enchant-info calls and broker plume packet tests.
- Java tooling task: generate the first unusual-storage runtime artifact in a Maven/JDK environment.

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Agent A | Cleanup/seal packet flag plumbing | `SmInventoryInfo.cs`, focused packet tests | `StaticData.cs` unless new table bug found |
| Agent B | Broker plume bridge audit/implementation | `SmBrokerService.cs`, broker packet tests, `GameServerConnection.cs` if needed | `SmInventoryInfo.cs` except existing public overload use |
| Agent C | Temporary exchange model analysis | read-only Java/C# model/static-data files | all writes |

## Do Not Parallelize

- Do not let multiple agents edit `SmInventoryInfo.cs`; it is the shared item-blob serializer.
