# Phase 6 AII Completion - Extraction Reward Cleanup-Seal Flag Source

Date: 2026-05-27
Unit of Work: UOW-1407
Status: live extraction reward packet construction now computes cleanup/seal flag context from static data.

## Completed

- Updated `GameServerConnection.CompleteExtractUseItemAsync` to pass `StaticData.ItemRestrictionCleanups` into extraction reward packet construction.
- Updated `SendExtractRewardPacketsAsync` so both stacked reward updates and newly added reward items compute Java's cleanup/seal general-info flag through `GetGeneralInfoWarehouseRestrictionFlag`.
- Added connection-level extraction reward tests with a synthetic extract tool, deterministic mythic weapon break target, and restricted extraction reward item `166000195`.
- Covered both `SmInventoryAddItem.ItemCollect` and `SmInventoryUpdateItem.IncreaseItemCollect` reward branches carrying cleanup/seal flag `3`.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_Extract|FullyQualifiedName~EnchantServiceTests.CreateBreakItemPlan|FullyQualifiedName~GamePacketTests.SmInventoryAddItem_WritesCleanupSealFlagInItemBlobLikeJava|FullyQualifiedName~GamePacketTests.SmInventoryUpdateItem_WritesCleanupSealFlagInItemBlobLikeJava|FullyQualifiedName~GamePacketTests.GameServerConnection_GetGeneralInfoWarehouseRestrictionFlag_UsesJavaCleanupPredicate"`.
- Result: passed 8 tests.

## Migration Parity Table - UOW-1407

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.ExtractAction` | `Aion.GameServer.Network.Aion.GameServerConnection.CompleteExtractUseItemAsync` and `SendExtractRewardPacketsAsync` | Service / Packet Orchestration | Partial | Regression Tested | Partial Parity | Live C# extraction reward packets now compute cleanup/seal flag context from `StaticData.ItemRestrictionCleanups` for updated and added reward items. New connection tests cover reward add and stack-merge packet emission with restricted item id `166000195`; Java runtime byte comparison remains unavailable. Delayed use ordering and target validation are source-reviewed but not fully Java-runtime compared. |
| `com.aionemu.gameserver.services.EnchantService.breakItem` | `Aion.GameServer.Services.EnchantService.CreateBreakItemPlan` as consumed by `GameServerConnection` | Service | Partial | Regression Tested | Partial Parity | Extraction reward item id/count planning was already present. This unit wires the resulting add/update packets to cleanup/seal flag context; broader enchant/break edge cases remain partial. |
| `com.aionemu.gameserver.services.item.ItemService.addItem` / `com.aionemu.gameserver.model.items.storage.Storage.add` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem.CreateItemCollect` and `SmInventoryUpdateItem` as consumed by extraction rewards | Service / Packet Orchestration | Partial | Regression Tested | Partial Parity | Extraction reward add/update sends now supply the explicit Java-shaped flag to existing packet wrappers. Broader `Storage.add` side effects such as quest callbacks, cube update fanout, and all caller families remain incomplete. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_ADD_ITEM` item-collect branch | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem.CreateItemCollect` | Packet | Partial | Regression Tested | Partial Parity | Existing packet coverage plus new connection-level extraction test verify restricted reward item-add packets carry cleanup/seal flag `3`. Java runtime byte comparison remains unavailable. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` full-blob branch | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` | Packet | Partial | Regression Tested | Partial Parity | Existing packet coverage plus new connection-level extraction test verify restricted stack-merge reward updates carry cleanup/seal flag `3`. Partial update families such as charge/polish remain intentionally excluded. |
| `com.aionemu.gameserver.dataholders.ItemRestrictionCleanupData` | `Aion.GameServer.Dataholders.ItemRestrictionCleanupTable` and `GameServerConnection.GetGeneralInfoWarehouseRestrictionFlag` | Dataholder / Utility | Partial | Unit Tested | Partial Parity | The shared helper is reused by extraction rewards; missing static data still falls back to flag `0`. Optional byte defaults and the Java `awh == 0 || lwh == 0` predicate are already covered by focused tests. |
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` cleanup/seal flag | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteGeneralInfoBlob` | Serialization Entry | Partial | Regression Tested | Partial Parity | Extraction rewards can now feed the Java-shaped field through inventory add/update packets. Temporary-exchange remaining seconds and time-dependent fields remain unresolved. |
| `com.aionemu.gameserver.model.templates.item.actions.DecomposeAction` | future `GameServerConnection` decompose reward flag inputs | Service / Packet Orchestration | Partial | No Tests | Needs Verification | Decompose normal/selectable reward packet construction still needs cleanup/seal source audit and likely a focused fixture update. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ExtractAddsRestrictedRewardWithCleanupSealFlag` | Integration / connection regression | `ExtractAction.act`, `EnchantService.breakItem`, `ItemService.addItem`, `SM_INVENTORY_ADD_ITEM`, `GeneralInfoBlobEntry` | Extraction use-item completion emits an item-collect add packet for restricted reward item `166000195` with cleanup/seal flag `3`, after target deletion and source item decrement. | C# connection-level packet assertion using Java-shaped extract marker, deterministic mythic weapon target, and cleanup row. | Does not compare against Java runtime bytes; reward count uses C# runtime roll and is asserted from the resulting player state. |
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ExtractMergesRestrictedRewardWithCleanupSealFlag` | Integration / connection regression | `ExtractAction.act`, `EnchantService.breakItem`, `Storage.increaseItemCount`, `SM_INVENTORY_UPDATE_ITEM`, `GeneralInfoBlobEntry` | Extraction use-item completion emits an `IncreaseItemCollect` full-blob update for an existing restricted reward stack with cleanup/seal flag `3`. | C# connection-level packet assertion using Java-shaped extract marker and cleanup row. | Does not compare against Java runtime bytes; random reward count is only bounded through resulting player state. |
| `EnchantServiceTests.CreateBreakItemPlan_*` | Unit | `EnchantService.breakItem` | Existing service coverage for source/target consumption, reward stone selection, and guard failures. | Source-derived C# assertions. | Does not serialize packets. |
| `GamePacketTests.SmInventoryAddItem_WritesCleanupSealFlagInItemBlobLikeJava` | Regression / packet byte layout | `SM_INVENTORY_ADD_ITEM.writeItemInfo`, `GeneralInfoBlobEntry` | Existing guard that item-collect inventory add packets write cleanup/seal field `3` when supplied. | C# packet-byte assertion against deterministic Java source order/field value. | Not a Java runtime comparison. |
| `GamePacketTests.SmInventoryUpdateItem_WritesCleanupSealFlagInItemBlobLikeJava` | Regression / packet byte layout | `SM_INVENTORY_UPDATE_ITEM.writeImpl`, `GeneralInfoBlobEntry` | Existing guard that normal full-blob inventory update packets write cleanup/seal field `3` when supplied. | C# packet-byte assertion against deterministic Java source order/field value. | Not a Java runtime comparison. |

## Remaining Risks

- Decompose rewards, loot, quest/custom reward paths, other inventory add/update, warehouse-add, and future pet-feed/live callers still need cleanup-table or precomputed cleanup/seal context.
- Extraction reward tests use synthetic C# static-data fixtures rather than generated Java runtime packet artifacts.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Temporary-exchange remaining seconds still serialize as zero.
- Runtime conditioning presence and wall-clock expiration/dye fields can still differ from Java.

## Summary Metrics

- Total Java artifacts discovered: 8 grouped artifact rows in this unit
- Total artifacts ported: 2 C# caller/packet surfaces changed or consumed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 8 grouped rows
- Total blocked artifacts: decompose reward cleanup flag source, world loot service cleanup flag source, remaining inventory add/update caller flag sources, warehouse-add/pet-feed live flag source, Java runtime artifact generation, temporary-exchange model/template/drop/task fields, time-normalized expiration/dye comparison
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: wire decompose reward packet cleanup/seal flag sourcing.
- Scope:
  - inspect `GameServerConnection.CreateDecomposeRewardInventoryPlan` and `SendDecomposeRewardItemsAsync`;
  - pass `StaticData.ItemRestrictionCleanups` from normal and selectable decompose completion paths if available;
  - compute flags with `GetGeneralInfoWarehouseRestrictionFlag` for updated and added rewards;
  - cover normal and/or selectable decompose reward branches if the existing fixture can remain contained.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Decompose reward helper audit | read-only `GameServerConnection.cs`, `DecomposeAction.java`, `DecomposeService.cs` | Low | Analysis can run in parallel, but implementation should be exclusive because it edits `GameServerConnection.cs`. |
| B | World loot cleanup/seal implementation prep | `WorldNpcLootService.cs`, `WorldNpcLootServiceTests` | Medium | Needs optional `ItemRestrictionCleanupTable?` threaded through `RequestDropItem` and focused add/update loot tests. |
| C | Extraction Java runtime artifact design | Java observer docs/tests only | Medium | Useful later for real byte comparison; blocked locally by Java/Maven tooling availability. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Agent A | Read-only decompose reward packet audit | Java decompose action; read-only `GameServerConnection.cs`; read-only `DecomposeService.cs` | all writes, shared docs |
| Agent B | Read-only world loot flag-source design | read-only `WorldNpcLootService.cs` and tests | all writes, shared docs |

## Do Not Parallelize

- Do not edit `GameServerConnection.cs` from multiple workers at once.
- Do not edit shared progress/handoff/parity docs from sub-agents; orchestrator owns docs.
- Do not claim verified parity for extraction rewards until there is Java runtime packet evidence or generated artifact comparison.

## Context For Next Session

- Current phase: Phase 6, cleanup/seal full item-blob flag propagation across live inventory-producing callers.
- Last completed UOW: UOW-1407.
- Last commit planned: `[Phase 6][UOW-1407] Wire extract cleanup seal flag source`.
- Key changed files in this handoff:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCleanupSealStaticDataAudit.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCSharpBlobGapAudit.md`
  - `docs/Phase-6AII-Completion.md`
