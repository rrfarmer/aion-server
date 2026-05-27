# Phase 6AJF Completion - Assembly And Extraction Source Cleanup Seal Metadata

Date: 2026-05-27
Unit of Work: UOW-1430
Status: Complete and committed after validation.

## Scope

Continue Phase 6 cleanup/seal full item-blob propagation by wiring Java-confirmed remaining-stack consumed item full update packets for assembly parts and extraction tools. Java remains the source of truth; this unit does not claim runtime byte parity.

## Completed Work

- Performed parallel work discovery across assembly, extraction, and composition consumed item paths.
- Spawned a read-only Java explorer and integrated packet-order findings for `AssemblyItemAction`, `ExtractAction`, and `CompositionAction`.
- Passed cleanup/seal static-data context into `GameServerConnection.SendAssemblyConsumedPartPacketsAsync` and `SendExtractConsumedItemPacketsAsync`.
- Updated existing assembly/extraction connection tests so restricted consumed part/tool full update packets assert cleanup/seal flag `3` and Java `DEC_ITEM_USE` mask `0x16`.
- Kept composition consumed item wiring, extraction target direct-delete/cube behavior, delete-only consumed branches, reward fanout, and Java no-rollback edges out of this narrow metadata unit.
- Updated `docs/PHASE-6-PROGRESS.md`, `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCleanupSealStaticDataAudit.md`, and `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCSharpBlobGapAudit.md`.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_AssemblyAddsRestrictedRewardWithCleanupSealFlag|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_AssemblyMergesRestrictedRewardWithCleanupSealFlag|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ExtractAddsRestrictedRewardWithCleanupSealFlag|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ExtractMergesRestrictedRewardWithCleanupSealFlag|FullyQualifiedName~GamePacketTests.SmInventoryUpdateItem_WritesCleanupSealFlagInItemBlobLikeJava"`.
- Result: passed 5 tests.

## Migration Parity Table - UOW-1430

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.AssemblyItemAction` | `Aion.GameServer.Network.Aion.GameServerConnection.CompleteAssemblyUseItemAsync` / `SendAssemblyConsumedPartPacketsAsync` | Item Action / Connection Packet Caller | Partial | Regression Tested | Partial Parity | Java decreases each recipe part by item id; remaining part stacks send full `SM_INVENTORY_UPDATE_ITEM` with `DEC_ITEM_USE`. C# covered remaining-stack part updates now carry cleanup/seal flag `3`. Java partial-consume/no-rollback edge and exhausted part delete/cube parity remain unverified. |
| `com.aionemu.gameserver.model.templates.item.actions.ExtractAction` | `Aion.GameServer.Network.Aion.GameServerConnection.CompleteExtractUseItemAsync` / `SendExtractConsumedItemPacketsAsync` | Item Action / Connection Packet Caller | Partial | Regression Tested | Partial Parity | Java direct-deletes the target, then consumes extraction tool/source by object id; remaining tool stacks send full `DEC_ITEM_USE` blobs. C# covered remaining-stack tool updates now carry cleanup/seal flag `3`. Target default delete/cube behavior and tool-consume failure edge remain unverified. |
| `com.aionemu.gameserver.services.EnchantService.breakItem` | `Aion.GameServer.Services.EnchantService.CreateBreakItemPlan` plus connection packet fanout | Service / Connection Packet Caller | Partial | Regression Tested | Partial Parity | This unit covered the packet metadata on remaining extraction tool updates only. Java deletes the target before source/tool consume and can report success after target delete even if tool consume fails; C# composed plan behavior remains a documented risk. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_COMPOSITE_STONES` / `CompositionAction` | `Aion.GameServer.Network.Aion.GameServerConnection.CompleteCompositeStonesAsync` / `SendConsumedItemPacketsAsync` | Discovered Dependency | Partial | Manual Only | Needs Verification | Java composition tool/stone remaining-stack updates are also `DEC_ITEM_USE` full blobs, but C# wiring was left for a focused follow-up because current shared helper is also used by housing consumption paths and no composition packet test was added in this unit. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseByItemId` / `decreaseByObjectId` | `Aion.GameServer.Network.Aion.GameServerConnection` consumed item packet callers | Storage / Packet Caller | Partial | Regression Tested | Partial Parity | Covered assembly part and extraction tool remaining-stack updates now pass cleanup/seal context. Exhausted delete plus cube update paths carry no item blob and were not changed. |
| `com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType.DEC_ITEM_USE` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem.DecreaseItemUse` | Packet Update Type | Partial | Regression Tested | Partial Parity | Focused tests assert mask `0x16` on covered assembly/extraction consumed full update packets. No Java runtime byte capture was available. |
| `com.aionemu.gameserver.dataholders.ItemRestrictionCleanupData` | `Aion.GameServer.Dataholders.ItemRestrictionCleanupTable` consumed by assembly/extraction connection paths | Dataholder Context | Partial | Regression Tested | Partial Parity | Existing cleanup table is now consumed by covered assembly part and extraction tool source full update paths. Loader behavior was not changed; missing/static-data absence still falls back to flag `0`. |
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` cleanup/seal flag | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteGeneralInfoBlob` via covered consumed full updates | Serialization Entry | Partial | Regression Tested | Partial Parity | Covered consumed full update packets can now feed the Java-shaped field. Temporary-exchange remaining seconds, runtime conditioning presence, and Java runtime byte comparison remain unresolved. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_AssemblyAddsRestrictedRewardWithCleanupSealFlag` | Regression / connection packet serialization | `AssemblyItemAction`, `Storage.decreaseByItemId`, `SM_INVENTORY_UPDATE_ITEM`, `GeneralInfoBlobEntry` | Assembly consumed part remaining-stack updates and restricted reward add carry cleanup/seal flag `3`; parts use `DEC_ITEM_USE`. | C# packet parsing against reviewed Java full-blob consumed-part shape and static-data predicate. | No Java runtime bytes; exhausted parts and partial-consume no-rollback edge not covered. |
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_AssemblyMergesRestrictedRewardWithCleanupSealFlag` | Regression / connection packet serialization | Same as above plus reward merge path | Assembly consumed part remaining-stack updates and reward merge update carry cleanup/seal flag `3`. | Deterministic C# packet parsing from Java-reviewed masks. | No Java runtime bytes; reward full-cube edge not covered. |
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ExtractAddsRestrictedRewardWithCleanupSealFlag` | Regression / connection packet serialization | `ExtractAction`, `EnchantService.breakItem`, `Storage.decreaseByObjectId` | Extraction target delete remains first and remaining-stack tool/source update plus restricted reward add carry cleanup/seal flag `3`. | C# packet parsing against reviewed Java mask/order for the full update path. | Target default delete/cube parity and tool-consume failure edge remain unverified. |
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ExtractMergesRestrictedRewardWithCleanupSealFlag` | Regression / connection packet serialization | Same as above plus reward merge path | Extraction remaining-stack tool/source update and reward merge update carry cleanup/seal flag `3`. | Deterministic C# packet parsing from Java-reviewed masks. | No Java runtime bytes; target delete/cube branch remains different/untested. |
| `GamePacketTests.SmInventoryUpdateItem_WritesCleanupSealFlagInItemBlobLikeJava` | Regression / packet serialization | `SM_INVENTORY_UPDATE_ITEM`, `GeneralInfoBlobEntry` | Existing full update packet cleanup/seal wrapper coverage still passes. | Deterministic C# packet parsing. | No Java runtime bytes. |

## Remaining Risks

- Composition tool/stone remaining-stack consumed full updates still need cleanup/seal context and focused tests.
- Assembly Java sequential part consume has no rollback if a later part consume fails; C# composed mutation behavior remains different and untested.
- Extraction Java direct-deletes the target with default delete mask/cube update before consuming the tool; C# still sends use-delete for target in current tested path.
- Covered delete-only branches still need Java cube-size packet parity where applicable.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.

## Summary Metrics

- Total Java artifacts discovered: 8 grouped artifact rows in this unit
- Total artifacts ported: 1 C# connection consumed full-update helper pair changed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 8 grouped rows
- Total blocked artifacts: Java runtime artifact generation, composition consumed item wiring/tests, assembly no-rollback edge, extraction target default-delete/cube parity, covered consumed delete/cube parity
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: wire composition tool/stone consumed remaining-stack full updates with focused `CM_COMPOSITE_STONES` tests, or correct extraction target default-delete/cube parity.
- Why: Java analysis confirmed composition consumed items are the next cleanup/seal full-blob path, while extraction target delete parity is a nearby packet semantic gap.
- Candidate files:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Composition C# test seam discovery | read-only C# packet/handler/test sources | Low | Find or add the least intrusive `CM_COMPOSITE_STONES` test path before wiring shared helper context. |
| B | Extraction target delete parity analysis | read-only Java/C# extraction/storage sources | Medium | Confirm target default delete plus cube packet order and whether it should be fixed before more cleanup/seal work. |
| C | Toy-pet source consume readiness | read-only Java/C# toy-pet/kisk sources | Medium | Scheduling/world-spawn packet order still needs careful mapping. |
| D | Admin/house dye source cleanup audit | read-only Java/C# source | Medium | Candidate source full update callers, but may cross admin/housing seams. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Explorer A | Locate focused composition handler/test seam | read-only C# composition packet/handler/test sources | all writes, docs, commits |
| Explorer B | Analyze extraction target delete/cube parity | Java/C# extraction/storage sources, read-only | all writes, docs, commits |
| Orchestrator | Implement one selected packet slice after analysis | selected production/test files only | shared docs until validation; unrelated files |

## Do Not Parallelize

- `GameServerConnection.cs` implementation changes.
- Shared item-use test fixture edits.
- Shared progress/handoff/audit docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1430] Wire assembly extraction source cleanup seal metadata`.
- Java source of truth for this unit:
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/AssemblyItemAction.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/ExtractAction.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/CompositionAction.java`
  - `game-server/src/com/aionemu/gameserver/services/EnchantService.java`
  - `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_COMPOSITE_STONES.java`
- C# files changed in this unit:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCleanupSealStaticDataAudit.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCSharpBlobGapAudit.md`
  - `docs/Phase-6AJF-Completion.md`
