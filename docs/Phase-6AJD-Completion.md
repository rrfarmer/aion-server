# Phase 6AJD Completion - Craft And Expansion Source Cleanup Seal Metadata

Date: 2026-05-27
Unit of Work: UOW-1428
Status: Complete and committed after validation.

## Scope

Continue Phase 6 cleanup/seal full item-blob propagation by wiring Java-confirmed remaining-stack source full update packets for craft-learn and inventory-expansion item actions. Java remains the source of truth; this unit does not claim runtime byte parity.

## Completed Work

- Performed parallel work discovery across craft-learn, skill-learn, title-add, emotion-learn, inventory expansion, and toy-pet source consumption.
- Spawned a read-only Java explorer and integrated the finding that only craft-learn, inventory expansion, and toy-pet use `Storage.decreaseByObjectId` remaining-stack full updates; skill/title/emotion direct-delete their source items in Java.
- Passed cleanup/seal static-data context into `GameServerConnection.HandleCraftLearnUseItemAsync` and `HandleInventoryExpansionUseItemAsync` remaining-stack source `SmInventoryUpdateItem` calls.
- Updated the shared item-use connection fixture with restricted craft/expansion source cleanup rows.
- Added/updated focused connection tests proving craft-learn and inventory-expansion source full update packets carry cleanup/seal flag `3` and Java `DEC_ITEM_USE` mask `0x16`.
- Kept skill/title/emotion direct-delete semantics, toy-pet scheduling/world-spawn order, and consumed source delete/cube branches out of this narrow metadata unit.
- Updated `docs/PHASE-6-PROGRESS.md`, `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCleanupSealStaticDataAudit.md`, and `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCSharpBlobGapAudit.md`.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_CraftLearnTicketWritesCleanupSealFlagForRemainingSource|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_CubeExpansionTicketConsumesItemAndRefreshesCubeSize|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_WarehouseExpansionTicketConsumesItemAndRefreshesWarehouseInfo|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_WarehouseExpansionTicketAllowsQuestOffsetLikeJava|FullyQualifiedName~GamePacketTests.SmInventoryUpdateItem_WritesCleanupSealFlagInItemBlobLikeJava"`.
- Result: passed 5 tests.

## Migration Parity Table - UOW-1428

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.CraftLearnAction` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleCraftLearnUseItemAsync` / `Aion.GameServer.Services.CraftLearnService` | Item Action / Connection Packet Caller | Partial | Regression Tested | Partial Parity | Java cancels item use, consumes the source with `decreaseByObjectId`, then adds the recipe and sends usage animation only if add succeeds. C# remaining-stack source full updates now carry cleanup/seal flag `3` with `DEC_ITEM_USE`; Java consumes before recipe-add success and delete/cube branches remain unverified. |
| `com.aionemu.gameserver.model.templates.item.actions.ExpandInventoryAction` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleInventoryExpansionUseItemAsync` / `Aion.GameServer.Services.InventoryExpansionService` | Item Action / Connection Packet Caller | Partial | Regression Tested | Partial Parity | Java consumes with `decreaseByObjectId`, broadcasts usage animation, then applies cube/warehouse side effects. C# remaining-stack source full updates now carry cleanup/seal flag `3`; consumed delete/cube behavior and exact packet order remain separate gaps. |
| `com.aionemu.gameserver.model.templates.item.actions.SkillLearnAction` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleSkillLearnUseItemAsync` / `Aion.GameServer.Services.SkillLearnService` | Discovered Dependency | Partial | Manual Only | Needs Verification | Java uses direct `inventory.delete(item)` after learning, yielding default delete mask `0x00` plus cube update and no remaining-stack full update. C# currently models count decrement/update for stackable books; this was discovered but intentionally not changed in this cleanup/seal unit. |
| `com.aionemu.gameserver.model.templates.item.actions.TitleAddAction` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleTitleAddUseItemAsync` / `Aion.GameServer.Services.TitleAddService` | Discovered Dependency | Partial | Manual Only | Needs Verification | Java broadcasts animation, adds title, then direct-deletes the source on success. No Java source full-update path exists for cleanup/seal wiring; C# decrement semantics remain a parity risk for stackable source items. |
| `com.aionemu.gameserver.model.templates.item.actions.EmotionLearnAction` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleEmotionLearnUseItemAsync` / `Aion.GameServer.Services.EmotionLearnService` | Discovered Dependency | Partial | Manual Only | Needs Verification | Java broadcasts animation, adds emotion, then direct-deletes the source. No Java source full-update path exists for cleanup/seal wiring; C# decrement semantics and expiration lifecycle remain verification gaps. |
| `com.aionemu.gameserver.model.templates.item.actions.ToyPetSpawnAction` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleToyPetSpawnUseItemAsync` / `CompleteToyPetSpawnUseItemAsync` | Discovered Dependency | Partial | Manual Only | Needs Verification | Java schedules a 10s use, sends completion animation before source consume, then uses `decreaseByObjectId` and spawns/registers kisk. Remaining-stack source updates are eligible for future cleanup/seal wiring, but world-spawn/order risks kept it out of this unit. |
| `com.aionemu.gameserver.model.items.storage.PlayerStorage` / `Storage.decreaseByObjectId` | `Aion.GameServer.Network.Aion.GameServerConnection` source mutation branches | Storage / Packet Caller | Partial | Regression Tested | Partial Parity | Covered craft/expansion remaining-stack updates now pass cleanup/seal context. Java exhausted non-kinah stacks send `SM_DELETE_ITEM` use mask plus cube update; current covered delete branches were not changed. |
| `com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType.DEC_ITEM_USE` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem.DecreaseItemUse` | Packet Update Type | Partial | Regression Tested | Partial Parity | Focused tests assert mask `0x16` on covered craft/expansion source full update packets. No Java runtime byte capture was available. |
| `com.aionemu.gameserver.dataholders.ItemRestrictionCleanupData` | `Aion.GameServer.Dataholders.ItemRestrictionCleanupTable` consumed by craft/expansion connection paths | Dataholder Context | Partial | Regression Tested | Partial Parity | Existing cleanup table is now consumed by the covered craft/expansion source full update paths. Loader behavior was not changed; missing/static-data absence still falls back to flag `0`. |
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` cleanup/seal flag | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteGeneralInfoBlob` via craft/expansion source full updates | Serialization Entry | Partial | Regression Tested | Partial Parity | Covered source full update packets can now feed the Java-shaped field. Temporary-exchange remaining seconds, runtime conditioning presence, and Java runtime byte comparison remain unresolved. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_CraftLearnTicketWritesCleanupSealFlagForRemainingSource` | Regression / connection packet serialization | `CraftLearnAction`, `Storage.decreaseByObjectId`, `SM_INVENTORY_UPDATE_ITEM`, `GeneralInfoBlobEntry` | Craft-learn remaining-stack source update carries cleanup/seal flag `3` and `DEC_ITEM_USE`; recipe learn side effect still occurs. | C# packet parsing against reviewed Java full-blob source update shape and static-data predicate. | Does not execute Java runtime, post-consume recipe-add failure edge, or source delete/cube branch. |
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_CubeExpansionTicketConsumesItemAndRefreshesCubeSize` | Regression / connection packet serialization | `ExpandInventoryAction`, `Storage.decreaseByObjectId` | Cube expansion remaining-stack source update carries cleanup/seal flag `3` and existing cube refresh packets still send. | C# packet parsing against reviewed Java mask and static-data predicate. | Does not compare Java runtime bytes or consumed source branch. |
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_WarehouseExpansionTicketConsumesItemAndRefreshesWarehouseInfo` | Regression / connection packet serialization | `ExpandInventoryAction`, `WarehouseService.expand` | Warehouse expansion remaining-stack source update carries cleanup/seal flag `3` and warehouse info refresh still sends. | C# packet parsing against reviewed Java mask and static-data predicate. | Does not compare Java runtime bytes or consumed source branch. |
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_WarehouseExpansionTicketAllowsQuestOffsetLikeJava` | Regression / connection packet serialization | `ExpandInventoryAction`, warehouse quest offset behavior | Existing Java-shaped warehouse quest offset path still sends restricted source update with cleanup/seal flag `3`. | Deterministic C# packet parsing. | No Java runtime bytes. |
| `GamePacketTests.SmInventoryUpdateItem_WritesCleanupSealFlagInItemBlobLikeJava` | Regression / packet serialization | `SM_INVENTORY_UPDATE_ITEM`, `GeneralInfoBlobEntry` | Existing full update packet cleanup/seal wrapper coverage still passes. | Deterministic C# packet parsing. | No Java runtime bytes. |

## Remaining Risks

- Skill/title/emotion source semantics differ materially: Java direct-deletes the source object with default delete mask and cube update, while current C# still allows decrement/update behavior for stackable source items.
- Toy-pet source cleanup/seal wiring is eligible only for remaining-stack `decreaseByObjectId`, but Java scheduling and kisk world-spawn packet order differ enough to require a dedicated unit.
- Covered craft/expansion consumed-source branches still need Java delete/cube-size packet parity.
- `CraftLearnAction` Java consumes before `RecipeService.addRecipe`; current C# persists validation and mutation together, so Java's post-consume failure edge is not modeled.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.

## Summary Metrics

- Total Java artifacts discovered: 10 grouped artifact rows in this unit
- Total artifacts ported: 1 C# connection source full-update family changed for craft/expansion
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 10 grouped rows
- Total blocked artifacts: Java runtime artifact generation, skill/title/emotion direct-delete parity, toy-pet scheduling/source packet convergence, covered consumed-source delete/cube parity, craft post-consume recipe-add failure edge
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: address the skill/title/emotion direct-delete parity gap as a focused packet semantics unit.
- Why: UOW-1428 discovered Java uses direct `inventory.delete(item)` for these actions, not stack decrement semantics; current C# may send remaining-stack full updates or use-delete masks for stackable sources.
- Files:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Skill/title/emotion direct-delete Java packet order | read-only Java action/storage sources | Low | Confirm `inventory.delete` packet mask/cube update and side-effect ordering before changing C#. |
| B | Toy-pet source consume readiness | read-only Java/C# toy-pet/kisk sources | Medium | Map 10s scheduling, cancel observer, completion animation, source consume, kisk spawn/register order before cleanup/seal wiring. |
| C | Armsfusion readiness audit | read-only Java/C# source | Medium | Service planner exists, but live handler availability still needs confirmation. |
| D | Admin/house dye source cleanup audit | read-only Java/C# source | Medium | Candidate source full update callers, but may cross admin/housing seams. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Explorer A | Analyze Java skill/title/emotion direct-delete packet order | Java item-action/storage/packet sources, read-only | all writes, docs, commits |
| Explorer B | Analyze toy-pet Java/C# readiness | Java/C# toy-pet/kisk sources, read-only | all writes, docs, commits |
| Orchestrator | Implement only the selected direct-delete semantics unit after analysis | `GameServerConnection.cs`, `GameServerConnectionInventoryExpansionUseItemTests.cs` | shared docs until validation; unrelated source/test files |

## Do Not Parallelize

- `GameServerConnection.cs` implementation changes.
- `GameServerConnectionInventoryExpansionUseItemTests.cs` shared fixture edits.
- Shared progress/handoff/audit docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1428] Wire craft expansion source cleanup seal metadata`.
- Java source of truth for this unit:
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/CraftLearnAction.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/ExpandInventoryAction.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/SkillLearnAction.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/TitleAddAction.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/EmotionLearnAction.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/ToyPetSpawnAction.java`
  - `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `game-server/src/com/aionemu/gameserver/model/items/storage/PlayerStorage.java`
- C# files changed in this unit:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCleanupSealStaticDataAudit.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCSharpBlobGapAudit.md`
  - `docs/Phase-6AJD-Completion.md`
