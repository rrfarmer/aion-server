# Phase 6 AIJ Completion - Decompose Reward Cleanup-Seal Flag Source

Date: 2026-05-27
Unit of Work: UOW-1408
Status: live decompose reward packet construction now computes cleanup/seal flag context from static data.

## Completed

- Updated normal and selectable decompose completion paths to pass `StaticData.ItemRestrictionCleanups` into shared decompose reward packet construction.
- Updated `SendDecomposeRewardItemsAsync` so decomposable add packets and stacked reward update packets compute Java's cleanup/seal general-info flag through `GetGeneralInfoWarehouseRestrictionFlag`.
- Added normal-decompose connection tests for restricted reward item `200`.
- Covered both `SmInventoryAddItem.Decomposable` and `SmInventoryUpdateItem.IncreaseItemCollect` reward branches carrying cleanup/seal flag `3`.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_Decompose|FullyQualifiedName~DecomposeServiceTests|FullyQualifiedName~GamePacketTests.SmInventoryAddItem_WritesCleanupSealFlagInItemBlobLikeJava|FullyQualifiedName~GamePacketTests.SmInventoryUpdateItem_WritesCleanupSealFlagInItemBlobLikeJava|FullyQualifiedName~GamePacketTests.GameServerConnection_GetGeneralInfoWarehouseRestrictionFlag_UsesJavaCleanupPredicate"`.
- Result: passed 12 tests.

## Migration Parity Table - UOW-1408

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.DecomposeAction` | `Aion.GameServer.Network.Aion.GameServerConnection.CompleteDecomposeUseItemAsync`, `HandleSelectDecomposableAsync`, and `SendDecomposeRewardItemsAsync` | Service / Packet Orchestration | Partial | Regression Tested | Partial Parity | Live C# decompose reward packets now compute cleanup/seal flag context from `StaticData.ItemRestrictionCleanups` for normal and selectable reward item construction. New normal-decompose connection tests cover reward add and stack-merge packet emission with restricted item id `200`; selectable reward packets share the same sender but do not yet have a dedicated cleanup/seal assertion. |
| `com.aionemu.gameserver.services.item.ItemService.addItem` / `com.aionemu.gameserver.model.items.storage.Storage.add` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem.CreateDecomposable` and `SmInventoryUpdateItem` as consumed by decompose rewards | Service / Packet Orchestration | Partial | Regression Tested | Partial Parity | Decompose reward add/update sends now supply the explicit Java-shaped flag to existing packet wrappers. Broader `Storage.add` side effects such as quest callbacks, cube update fanout, and all caller families remain incomplete. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_ADD_ITEM` decomposable branch | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem.CreateDecomposable` | Packet | Partial | Regression Tested | Partial Parity | New connection-level decompose test verifies restricted decomposable reward item-add packets carry cleanup/seal flag `3`. Java runtime byte comparison remains unavailable. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` full-blob branch | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` | Packet | Partial | Regression Tested | Partial Parity | New connection-level decompose test verifies restricted stack-merge reward updates carry cleanup/seal flag `3`. Partial update families such as charge/polish remain intentionally excluded. |
| `com.aionemu.gameserver.dataholders.ItemRestrictionCleanupData` | `Aion.GameServer.Dataholders.ItemRestrictionCleanupTable` and `GameServerConnection.GetGeneralInfoWarehouseRestrictionFlag` | Dataholder / Utility | Partial | Unit Tested | Partial Parity | The shared helper is reused by decompose rewards; missing static data still falls back to flag `0`. Optional byte defaults and the Java `awh == 0 || lwh == 0` predicate are already covered by focused tests. |
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` cleanup/seal flag | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteGeneralInfoBlob` | Serialization Entry | Partial | Regression Tested | Partial Parity | Decompose rewards can now feed the Java-shaped field through inventory add/update packets. Temporary-exchange remaining seconds and time-dependent fields remain unresolved. |
| `com.aionemu.gameserver.services.drop.DropService` / loot item add paths | future `Aion.GameServer.Services.WorldNpcLootService.RequestDropItem` flag input | Service / Packet Orchestration | Partial | No Tests | Needs Verification | World loot remains the next cleanup/seal caller family but needs an explicit cleanup-table parameter threaded through the service and focused loot tests. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_DecomposeAddsRestrictedRewardWithCleanupSealFlag` | Integration / connection regression | `DecomposeAction.act`, `ItemService.addItem`, `SM_INVENTORY_ADD_ITEM`, `GeneralInfoBlobEntry` | Normal decompose completion emits a decomposable add packet for restricted reward item `200` with cleanup/seal flag `3`. | C# connection-level packet assertion using Java-shaped decomposable item metadata and cleanup row. | Does not compare against Java runtime bytes; selectable branch uses the same sender but lacks a dedicated cleanup/seal assertion. |
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_DecomposeMergesRestrictedRewardWithCleanupSealFlag` | Integration / connection regression | `DecomposeAction.act`, `Storage.increaseItemCount`, `SM_INVENTORY_UPDATE_ITEM`, `GeneralInfoBlobEntry` | Normal decompose completion emits an `IncreaseItemCollect` full-blob update for an existing restricted reward stack with cleanup/seal flag `3`. | C# connection-level packet assertion using Java-shaped decomposable item metadata and cleanup row. | Does not compare against Java runtime bytes. |
| `DecomposeServiceTests` | Unit | `DecomposeAction.canAct` and reward selection | Existing service coverage for selectable rewards, normal reward selection, special-cube full behavior, and guard failures. | Source-derived C# assertions. | Does not serialize packets. |
| `GamePacketTests.SmInventoryAddItem_WritesCleanupSealFlagInItemBlobLikeJava` | Regression / packet byte layout | `SM_INVENTORY_ADD_ITEM.writeItemInfo`, `GeneralInfoBlobEntry` | Existing guard that item-collect inventory add packets write cleanup/seal field `3` when supplied. | C# packet-byte assertion against deterministic Java source order/field value. | Not a Java runtime comparison and does not specifically use decomposable add type. |
| `GamePacketTests.SmInventoryUpdateItem_WritesCleanupSealFlagInItemBlobLikeJava` | Regression / packet byte layout | `SM_INVENTORY_UPDATE_ITEM.writeImpl`, `GeneralInfoBlobEntry` | Existing guard that normal full-blob inventory update packets write cleanup/seal field `3` when supplied. | C# packet-byte assertion against deterministic Java source order/field value. | Not a Java runtime comparison. |

## Remaining Risks

- Loot, quest/custom reward paths, other inventory add/update, warehouse-add, and future pet-feed/live callers still need cleanup-table or precomputed cleanup/seal context.
- Decompose reward tests use synthetic C# static-data fixtures rather than generated Java runtime packet artifacts.
- Selectable decompose uses the same sender but does not yet have its own cleanup/seal assertion.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Temporary-exchange remaining seconds still serialize as zero.
- Runtime conditioning presence and wall-clock expiration/dye fields can still differ from Java.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 2 C# caller/packet surfaces changed or consumed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: world loot service cleanup flag source, remaining inventory add/update caller flag sources, warehouse-add/pet-feed live flag source, Java runtime artifact generation, temporary-exchange model/template/drop/task fields, time-normalized expiration/dye comparison
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: wire `WorldNpcLootService.RequestDropItem` cleanup/seal flag sourcing.
- Scope:
  - inspect Java `DropService`/loot item-add path and current C# `WorldNpcLootService`;
  - thread `ItemRestrictionCleanupTable?` into the C# service call boundary that constructs `SmInventoryAddItem`/`SmInventoryUpdateItem`;
  - compute flags with the Java cleanup predicate for loot add/update packets;
  - add focused loot add/update packet tests using restricted item fixtures.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | World loot cleanup/seal implementation prep | read-only `WorldNpcLootService.cs`, tests, Java drop service classes | Medium | Best next work, but service signature and tests should be edited by one owner only. |
| B | Selectable decompose cleanup/seal assertion | `GameServerConnectionInventoryExpansionUseItemTests.cs` only | Low | Optional follow-up if wanting explicit selectable coverage; code path already shares the sender wired in UOW-1408. |
| C | Quest/custom reward path audit | read-only quest reward services and Java quest reward handlers | Medium | Analysis-only candidate before touching broader quest reward plumbing. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Agent A | Read-only world loot flag-source design | read-only `WorldNpcLootService.cs`, Java drop service classes, loot tests | all writes, shared docs |
| Agent B | Read-only quest/custom reward cleanup/seal caller audit | read-only quest reward service/code paths | all writes, shared docs |

## Do Not Parallelize

- Do not edit `WorldNpcLootService.cs` and its tests from multiple workers at once.
- Do not edit shared progress/handoff/parity docs from sub-agents; orchestrator owns docs.
- Do not claim verified parity for decompose rewards until there is Java runtime packet evidence or generated artifact comparison.

## Context For Next Session

- Current phase: Phase 6, cleanup/seal full item-blob flag propagation across live inventory-producing callers.
- Last completed UOW: UOW-1408.
- Last commit planned: `[Phase 6][UOW-1408] Wire decompose cleanup seal flag source`.
- Key changed files in this handoff:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCleanupSealStaticDataAudit.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCSharpBlobGapAudit.md`
  - `docs/Phase-6AIJ-Completion.md`
