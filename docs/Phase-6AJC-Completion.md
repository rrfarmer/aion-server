# Phase 6AJC Completion - Item-Use Source Cleanup Seal Metadata

Date: 2026-05-27
Unit of Work: UOW-1427
Status: Complete and committed after validation.

## Scope

Continue Phase 6 cleanup/seal full item-blob propagation by wiring implemented item-use source/tool remaining-stack update packets for decompose, selectable decompose, XP extraction, and AP extraction. Java remains the source of truth; this unit does not claim runtime byte parity.

## Completed Work

- Performed parallel work discovery across armsfusion, admin/house dye, unwrap/tune, and implemented item-use source-decrement paths.
- Spawned a read-only Java explorer for decompose/selectable-decompose/XP-extract/AP-extract packet behavior and integrated the packet-order findings.
- Passed cleanup/seal static-data context into `GameServerConnection.ApplySourceItemMutationAsync`, `ApplyExpExtractSourceMutationAsync`, and `SendApExtractConsumedItemPacketsAsync`.
- Updated existing connection tests so covered restricted source/tool full update packets assert cleanup/seal flag `3` and Java `DEC_ITEM_USE` mask `0x16`.
- Kept delete/cube branches, AP target delete behavior, AP no-rollback semantics, XP extraction by-item-id source selection, and reward packet metadata out of this narrow source full-update metadata unit.
- Updated `docs/PHASE-6-PROGRESS.md`, `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCleanupSealStaticDataAudit.md`, and `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCSharpBlobGapAudit.md`.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_DecomposeAddsRestrictedRewardWithCleanupSealFlag|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_DecomposeMergesRestrictedRewardWithCleanupSealFlag|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleSelectDecomposableAsync_SelectableRewardConsumesSourceAndAddsReward|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleSelectDecomposableAsync_FullCubeStillAddsSelectableRewardLikeJavaOverflow|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ExpExtractAddsRestrictedRewardWithCleanupSealFlag|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ApExtractSendsAbyssPointsPlannerPackets|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ApExtractHonorsConfiguredAbyssPointCap|FullyQualifiedName~GamePacketTests.SmInventoryUpdateItem_WritesCleanupSealFlagInItemBlobLikeJava"`.
- Result: passed 9 tests.

## Migration Parity Table - UOW-1427

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.DecomposeAction` normal delayed branch | `Aion.GameServer.Network.Aion.GameServerConnection.CompleteDecomposeUseItemAsync` / `ApplySourceItemMutationAsync` | Item Action / Connection Packet Caller | Partial | Regression Tested | Partial Parity | Remaining-stack decompose source updates now carry cleanup/seal flag `3` and Java `DEC_ITEM_USE` mask. Delete/cube branches, random reward source selection, scheduled observer cancellation, Java runtime byte comparison, and full post-validate ordering remain unverified. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE` | `Aion.GameServer.Network.Aion.ClientPackets.CmSelectDecomposable` / `GameServerConnection.HandleSelectDecomposableAsync` | Client Packet / Handler | Partial | Regression Tested | Partial Parity | Remaining-stack selectable source updates now carry cleanup/seal flag `3`. Java does not check source decrease return before closing UI and adding reward; C# planner/persistence still has a safer mutation boundary, documented as a parity risk. |
| `com.aionemu.gameserver.model.templates.item.actions.ExpExtractAction` | `Aion.GameServer.Services.ExpExtractService` plus `GameServerConnection.ApplyExpExtractSourceMutationAsync` | Item Action / Connection Packet Caller | Partial | Regression Tested | Partial Parity | Remaining-stack XP extraction source updates now carry cleanup/seal flag `3`; reward add/update metadata was already covered. Java consumes by item id rather than object id, so multi-stack source-object parity remains unverified. |
| `com.aionemu.gameserver.model.templates.item.actions.ApExtractAction` | `Aion.GameServer.Services.ApExtractService` plus `GameServerConnection.SendApExtractConsumedItemPacketsAsync` | Item Action / Connection Packet Caller | Partial | Regression Tested | Partial Parity | Remaining-stack AP extraction tool/source updates now carry cleanup/seal flag `3`. Java deletes the target first with default delete mask `0` and only then consumes the tool; C# remains planned as a single mutation and does not model Java's no-rollback edge. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseByObjectId` / `decreaseByItemId` | `Aion.GameServer.Network.Aion.GameServerConnection` source/tool mutation helpers | Storage / Packet Caller | Partial | Regression Tested | Partial Parity | Remaining-stack updates now pass cleanup/seal context for the covered item-use helpers. Exhausted source/tool delete plus cube update paths carry no item blob and were not changed; AP target delete default-mask/cube behavior remains a separate verification gap. |
| `com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType.DEC_ITEM_USE` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem.DecreaseItemUse` | Packet Update Type | Partial | Regression Tested | Partial Parity | Focused tests assert mask `0x16` on covered source/tool full update packets. No Java runtime byte capture was available. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` | Packet Serialization | Partial | Regression Tested | Partial Parity | Covered source/tool updates now route a cleanup/seal flag into the existing full item-blob serializer. Runtime conditioning, time-normalized expiration/dye fields, and Java byte comparison remain unresolved. |
| `com.aionemu.gameserver.dataholders.ItemRestrictionCleanupData` | `Aion.GameServer.Dataholders.ItemRestrictionCleanupTable` consumed by source/tool connection paths | Dataholder Context | Partial | Regression Tested | Partial Parity | Existing cleanup table is now consumed by decompose/selectable/XP/AP source full update paths. Loader behavior was not changed; missing/static-data absence still falls back to flag `0`. |
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` cleanup/seal flag | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteGeneralInfoBlob` via covered source/tool full updates | Serialization Entry | Partial | Regression Tested | Partial Parity | Covered source/tool full update packets can now feed the Java-shaped field. Temporary-exchange remaining seconds, runtime conditioning presence, and Java runtime byte comparison remain unresolved. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_DecomposeAddsRestrictedRewardWithCleanupSealFlag` | Regression / connection packet serialization | `DecomposeAction`, `Storage.decreaseByObjectId`, `SM_INVENTORY_UPDATE_ITEM`, `GeneralInfoBlobEntry` | Normal decompose remaining-stack source update and restricted reward add both carry cleanup/seal flag `3`; source uses `DEC_ITEM_USE`. | C# packet parsing against reviewed Java full-blob packet shape and static-data predicate. | No Java runtime bytes; source delete/cube branch not covered. |
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_DecomposeMergesRestrictedRewardWithCleanupSealFlag` | Regression / connection packet serialization | Same as above plus reward merge path | Normal decompose source update and reward merge update carry cleanup/seal flag `3`. | Deterministic C# packet parsing from Java-reviewed masks. | No Java runtime bytes; random reward ordering not covered. |
| `GameServerConnectionInventoryExpansionUseItemTests.HandleSelectDecomposableAsync_SelectableRewardConsumesSourceAndAddsReward` | Regression / connection packet serialization | `CM_SELECT_DECOMPOSABLE`, `Storage.decreaseByObjectId` | Selectable decompose remaining-stack source update carries cleanup/seal flag `3` with `DEC_ITEM_USE`. | C# packet parsing against reviewed Java packet order. | Does not model Java unchecked source-decrease failure edge. |
| `GameServerConnectionInventoryExpansionUseItemTests.HandleSelectDecomposableAsync_FullCubeStillAddsSelectableRewardLikeJavaOverflow` | Regression / connection packet serialization | `CM_SELECT_DECOMPOSABLE` overflow behavior | Existing Java-shaped full-cube overflow behavior still sends restricted source update with cleanup/seal flag `3`. | Deterministic C# packet parsing. | Java runtime artifact comparison remains optional/blocked. |
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ExpExtractAddsRestrictedRewardWithCleanupSealFlag` | Regression / connection packet serialization | `ExpExtractAction`, `Storage.decreaseByItemId` | XP extraction remaining-stack source update and reward add carry cleanup/seal flag `3`. | C# packet parsing against reviewed Java source consume mask. | Does not cover Java by-item-id multi-stack source-object selection. |
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag` | Regression / connection packet serialization | `ExpExtractAction`, reward merge path | XP extraction source update and reward merge update carry cleanup/seal flag `3`. | Deterministic C# packet parsing. | No Java runtime bytes. |
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ApExtractSendsAbyssPointsPlannerPackets` | Regression / connection packet serialization | `ApExtractAction`, `Storage.delete`, `Storage.decreaseByObjectId` | AP extraction target delete remains first and tool/source remaining-stack update carries cleanup/seal flag `3`. | C# packet parsing against reviewed Java mask/order. | Does not model Java target-delete/tool-consume no-rollback edge or target delete cube update. |
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ApExtractHonorsConfiguredAbyssPointCap` | Regression / connection packet serialization | `ApExtractAction`, `AbyssPointsService.addAp` | AP cap behavior remains stable while source update carries cleanup/seal flag `3`. | Existing AP planner packets plus source full-blob parsing. | Does not compare Java runtime AP side effects. |
| `GamePacketTests.SmInventoryUpdateItem_WritesCleanupSealFlagInItemBlobLikeJava` | Regression / packet serialization | `SM_INVENTORY_UPDATE_ITEM`, `GeneralInfoBlobEntry` | Existing full update packet cleanup/seal wrapper coverage still passes. | Deterministic C# packet parsing. | No Java runtime bytes. |

## Remaining Risks

- Remaining `GameServerConnection` full-blob update callers still need cleanup/seal flag wiring: armsfusion once live C# handlers exist, admin dye, house-object dye source consume, craft/skill/title/emotion/inventory-expansion source item actions, kisk/toy-pet source consume, and other item-action paths.
- Covered delete-only branches still need Java cube-size packet parity where applicable.
- `ExpExtractAction` Java consumes by item id, so multiple stacks can produce a different source-object update than current C# object-id-based planning.
- `ApExtractAction` Java deletes the target before confirming tool consumption and can leave a no-rollback edge; C# remains a composed mutation plan.
- `CM_SELECT_DECOMPOSABLE` Java does not check source decrease success before closing the UI and adding reward; C# persistence guard differs.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.

## Summary Metrics

- Total Java artifacts discovered: 9 grouped artifact rows in this unit
- Total artifacts ported: 1 C# connection source/tool full-update helper family changed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 9 grouped rows
- Total blocked artifacts: remaining `GameServerConnection` full-blob update caller flag sources, Java runtime artifact generation, source delete/cube parity for covered actions, XP extraction by-item-id source selection, AP extraction no-rollback edge, selectable decompose unchecked source-decrease edge
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: continue cleanup/seal source full-update wiring for another implemented item-use source caller.
- Scope:
  - inspect craft-learn, skill-learn, title-add, emotion-learn, inventory expansion, or toy-pet/kisk source consume Java packet order;
  - wire only remaining-stack full update packets with `GameServerConnection.GetGeneralInfoWarehouseRestrictionFlag` or optional helper context;
  - keep delete/cube branches and non-source side effects out of cleanup/seal wiring unless explicitly scoped;
  - add focused connection-level packet tests for restricted source items;
  - update all parity/progress/handoff docs and commit the completed unit.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Craft/skill/title/emotion Java packet shape | read-only Java item-action sources | Low | Similar source-decrement patterns; good read-only batch. |
| B | Inventory expansion source packet shape | read-only Java expand-inventory action source | Low | Existing C# tests already cover expansion; source update may be easy to wire. |
| C | Toy-pet/kisk source consume packet shape | read-only Java toy-pet/kisk action sources | Medium | Scheduling/world-spawn side effects make implementation riskier. |
| D | Armsfusion readiness audit | read-only Java/C# source | Medium | Service planner exists, but no current live C# packet handler; likely needs parser/handler work before cleanup/seal wiring. |

## Do Not Parallelize

- `GameServerConnection.cs` implementation changes.
- `GameServerConnectionInventoryExpansionUseItemTests.cs` shared fixture edits.
- Shared progress/handoff/audit docs.
- Git staging and commit.

## Context For Next Session

- Current unit was committed with message `[Phase 6][UOW-1427] Wire item-use source cleanup seal metadata`.
- Java source of truth for this unit:
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/DecomposeAction.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_SELECT_DECOMPOSABLE.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/ExpExtractAction.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/ApExtractAction.java`
  - `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_INVENTORY_UPDATE_ITEM.java`
- C# files changed in this unit:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCleanupSealStaticDataAudit.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCSharpBlobGapAudit.md`
  - `docs/Phase-6AJC-Completion.md`
