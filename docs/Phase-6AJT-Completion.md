# Phase 6AJT Completion - Extraction Reward Add Cube Update

Date: 2026-05-27
Unit of Work: UOW-1444
Status: Complete after validation.

## Scope

Align extraction added-reward packet fanout with Java storage add behavior. New reward stacks send `SM_INVENTORY_ADD_ITEM` followed by `SM_CUBE_UPDATE`; stack merges remain update-only.

## Completed Work

- Reviewed Java `ExtractAction.act`, which delegates the delayed result to `EnchantService.breakItem`.
- Updated `SendExtractRewardPacketsAsync` to accept the player snapshot and emit `SmCubeUpdate.CubeSizeSnapshot` after each newly added extraction reward.
- Left updated extraction reward stacks unchanged: they still send only `SmInventoryUpdateItem.IncreaseItemCollect`.
- Updated both added-reward extraction regressions:
  - source-update branch expects reward add plus cube count `2`
  - last-source-delete branch expects reward add plus cube count `1`

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ExtractAddsRestrictedRewardWithCleanupSealFlag|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ExtractMergesRestrictedRewardWithCleanupSealFlag|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ExtractDeletesLastSourceWithUseDeleteAndCubeUpdate|FullyQualifiedName~EnchantServiceTests"`.
- Result: passed 25 tests.

## Migration Parity Table - UOW-1444

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.ExtractAction` | `GameServerConnection.HandleExtractUseItemAsync` / `CompleteExtractUseItemAsync` | Item Action / Connection Handler | Partial | Unit + Regression Tested | Partial Parity | Added-reward fanout now includes Java-shaped cube update for source-update and source-delete variants. Runtime Java bytes and scheduled real-client timing remain unverified. |
| `com.aionemu.gameserver.services.EnchantService.breakItem` | `Aion.GameServer.Services.EnchantService.CreateBreakItemPlan` / `GameServerConnection.SendExtractRewardPacketsAsync` | Service / Reward Mutation Planner | Partial | Unit + Regression Tested | Partial Parity | Focused validation covers reward add/merge packet fanout around existing break-item plans. Broader enchant RNG/drop calculations remain outside this unit. |
| `com.aionemu.gameserver.services.item.ItemService.addItem` | `EnchantService.CreateBreakItemPlan` / `GameServerConnection.SendExtractRewardPacketsAsync` | Service / Reward Mutation Planner | Partial | Regression Tested | Partial Parity | New extraction reward stack path now emits trailing cube update. Merge path remains update-only. |
| `com.aionemu.gameserver.model.items.storage.Storage.add` | `GameServerConnection.SendExtractRewardPacketsAsync` plus `SmInventoryAddItem.CreateItemCollect` | Storage Add / Packet Caller | Partial | Regression Tested | Partial Parity | C# now mirrors Java storage add packet shape for covered extraction rewards, including last-source-delete case. Persistence timing remains C# transaction-first. |
| `com.aionemu.gameserver.model.items.storage.Storage.increaseItemCount` | `InventoryAddService.CreateAddItemPlan` / `SmInventoryUpdateItem.IncreaseItemCollect` | Storage Stack Merge | Partial | Regression Tested | Partial Parity | Existing extraction merge regression still expects update-only behavior and no reward cube update. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket` | `SmInventoryAddItem.CreateItemCollect` plus `SmCubeUpdate.CubeSizeSnapshot` | Packet Fanout Helper | Partial | Regression Tested | Partial Parity | Extraction added rewards now send item collect add followed by cube count `2` or `1`, depending on source retention. Warehouse variants remain outside this unit. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_ADD_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem` | Packet | Partial | Regression Tested | Partial Parity | Tests assert `ITEM_COLLECT` add type and restricted cleanup/seal blob for extraction reward add. Full Java runtime byte comparison remains blocked. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate.CubeSizeSnapshot` | Packet | Partial | Regression Tested | Partial Parity | Tests assert projected cube counts after target delete, source delete when applicable, and reward add. Expand snapshot bytes remain unverified. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ExtractAddsRestrictedRewardWithCleanupSealFlag` | Regression / connection packet serialization | `ExtractAction.act`, `EnchantService.breakItem`, `Storage.add` | Target delete/cube, source update, restricted reward add, reward cube update, and final success animation. | C# packet parsing against reviewed Java add/cube fanout. | No Java runtime bytes; fixture uses deterministic reward plan. |
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ExtractDeletesLastSourceWithUseDeleteAndCubeUpdate` | Regression / connection packet serialization | Same Java action/service/storage path | Target delete/cube, source use-delete/cube, restricted reward add, reward cube update, and final success animation. | C# packet parsing against reviewed Java add/cube fanout. | No Java runtime bytes. |
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ExtractMergesRestrictedRewardWithCleanupSealFlag` | Existing Regression | `Storage.increaseItemCount` | Regression slice verifies extraction reward merge remains update-only. | Deterministic C# packet assertions from reviewed Java merge branch. | No runtime bytes. |
| `EnchantServiceTests` | Existing Unit | `EnchantService.breakItem` planning | Regression slice verifies break-item planning remains stable. | Deterministic C# service assertions. | Does not serialize packets. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Decompose added-reward sender likely still needs Java add-plus-cube alignment.
- Broader Java `EnchantService.breakItem` RNG/drop table parity remains outside this packet-fanout unit.
- Exact object-id allocation and runtime bytes remain C# deterministic but not Java-runtime compared.
- Scheduled real-client item-use ordering remains unvalidated.

## Summary Metrics

- Total Java artifacts discovered: 8 grouped artifact rows in this unit
- Total artifacts ported: 1 extraction added-reward packet fanout branch changed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 8 grouped rows
- Total blocked artifacts: Java runtime artifact generation, decompose add-cube sender, broader enchant RNG/drop parity, real-client scheduled ordering
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: align decompose added-reward cube update behavior.
- Why: UOW-1441 read-only audit found `SendDecomposeRewardItemsAsync` likely misses Java's trailing cube update for added decompose rewards. UOW-1440 through UOW-1444 now provide the composition, assembly, XP extraction, and extraction reference pattern.
- Candidate files:
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/DecomposeAction.java`
  - `game-server/src/com/aionemu/gameserver/services/item/ItemService.java`
  - `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`

## Follow-Up Sequential Tasks

- Keep decompose merge branches update-only; Java `Storage.increaseItemCount` does not emit cube updates.
- Consider broader Java `EnchantService.breakItem` RNG/drop table parity after packet fanout gaps are closed.
- Consider source-delete branch coverage for XP extraction if Java storage delete fanout needs broader coverage.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Decompose add/cube implementation | `GameServerConnection.cs`, decompose tests | Medium | Sequential if production sender and shared test fixture both change. |
| B | Toy-pet internal ordering documentation | toy-pet/kisk docs and source, read-only | Low | Separate from reward sender code. |
| C | Extraction RNG/drop audit | Java/C# enchant service sources | Medium | Read-only sidecar only; do not mix with decompose sender edits. |

## Do Not Parallelize

- Multiple edits to `GameServerConnectionInventoryExpansionUseItemTests.cs`.
- Multiple edits to `GameServerConnection.cs` item-use reward senders.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1444] Add extraction reward cube update`.
- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/ExtractAction.java`
  - `game-server/src/com/aionemu/gameserver/services/EnchantService.java`
  - `game-server/src/com/aionemu/gameserver/services/item/ItemService.java`
  - `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
- C# files changed in UOW-1444:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AJT-Completion.md`
