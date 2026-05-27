# Phase 6AJR Completion - Assembly Reward Add Cube Update

Date: 2026-05-27
Unit of Work: UOW-1442
Status: Complete after focused validation.

## Scope

Align assembly added-reward packet fanout with Java `ItemService.addItem -> Storage.add`. Java new reward stacks send `SM_INVENTORY_ADD_ITEM` followed by `SM_CUBE_UPDATE`; stack merges remain update-only.

## Completed Work

- Reviewed Java `AssemblyItemAction.act`, which consumes parts, sends success animation/message, then calls `ItemService.addItem`.
- Updated `SendAssemblyRewardPacketsAsync` to accept the player snapshot and emit `SmCubeUpdate.CubeSizeSnapshot` after each newly added assembly reward.
- Left updated assembly reward stacks unchanged: they still send only `SmInventoryUpdateItem.IncreaseItemCollect`.
- Updated `HandleUseItemAsync_AssemblyAddsRestrictedRewardWithCleanupSealFlag` to assert the trailing cube update after the restricted reward add.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_AssemblyAddsRestrictedRewardWithCleanupSealFlag|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_AssemblyMergesRestrictedRewardWithCleanupSealFlag|FullyQualifiedName~AssemblyItemServiceTests"`.
- Result: passed 5 tests.
- Attempted a broader nearby reward filter including XP extraction and decompose. It failed because `HandleUseItemAsync_ExpExtractAddsRestrictedRewardWithCleanupSealFlag` timed out waiting for its expected packet count. That path was not changed in this unit and is now the recommended next isolated unit.

## Migration Parity Table - UOW-1442

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.AssemblyItemAction` | `GameServerConnection.HandleAssemblyUseItemAsync` / `CompleteAssemblyUseItemAsync` | Item Action / Connection Handler | Partial | Unit + Regression Tested | Partial Parity | Added-reward fanout now follows Java success animation, success system message, add item, cube update order for the covered fixture. Runtime Java bytes and cancel/late-failure timing remain unverified. |
| `com.aionemu.gameserver.services.item.ItemService.addItem` | `AssemblyItemService.CreateMutationPlan` / `GameServerConnection.SendAssemblyRewardPacketsAsync` | Service / Reward Mutation Planner | Partial | Regression Tested | Partial Parity | New assembly reward stack path now emits trailing cube update. Merge path remains update-only. |
| `com.aionemu.gameserver.model.items.storage.Storage.add` | `GameServerConnection.SendAssemblyRewardPacketsAsync` plus `SmInventoryAddItem.CreateItemCollect` | Storage Add / Packet Caller | Partial | Regression Tested | Partial Parity | C# now mirrors Java storage add packet shape for covered assembly rewards. Persistence timing remains C# transaction-first rather than Java dirty-state lifecycle. |
| `com.aionemu.gameserver.model.items.storage.Storage.increaseItemCount` | `InventoryAddService.CreateAddItemPlan` / `SmInventoryUpdateItem.IncreaseItemCollect` | Storage Stack Merge | Partial | Regression Tested | Partial Parity | Existing assembly merge regression still expects update-only behavior and no cube update. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket` | `SmInventoryAddItem.CreateItemCollect` plus `SmCubeUpdate.CubeSizeSnapshot` | Packet Fanout Helper | Partial | Regression Tested | Partial Parity | Assembly added reward now sends item collect add followed by cube count `4` for the covered fixture. Warehouse variants remain outside this unit. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_ADD_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem` | Packet | Partial | Regression Tested | Partial Parity | Test asserts `ITEM_COLLECT` add type and restricted cleanup/seal blob for assembly reward add. Full Java runtime byte comparison remains blocked. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate.CubeSizeSnapshot` | Packet | Partial | Regression Tested | Partial Parity | Test asserts projected cube count after assembly reward add. Expand snapshot bytes remain unverified. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_AssemblyAddsRestrictedRewardWithCleanupSealFlag` | Regression / connection packet serialization | `AssemblyItemAction.act`, `ItemService.addItem`, `Storage.add`, `ItemPacketService.sendStorageUpdatePacket` | Assembly consumed parts update, success animation/message, restricted reward add, and trailing cube update. | C# packet parsing against reviewed Java add/cube fanout. | No Java runtime bytes; fixture covers update-part branch, not deleted-part cube behavior. |
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_AssemblyMergesRestrictedRewardWithCleanupSealFlag` | Existing Regression | `Storage.increaseItemCount` | Regression slice verifies assembly reward merge remains update-only. | Deterministic C# packet assertions from reviewed Java merge branch. | No runtime bytes. |
| `AssemblyItemServiceTests` | Existing Unit | `AssemblyItemAction.canAct` / mutation planning | Regression slice verifies service planning remains stable. | Deterministic C# service assertions. | Does not serialize packets. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Assembly deleted-part cube-update behavior was not broadened in this unit; the covered fixture consumes part stacks with remaining counts.
- XP extraction added-reward validation timed out in a broader nearby run and should be isolated in the next sender-family unit.
- Extraction and decompose added-reward senders likely still need Java add-plus-cube alignment.
- Exact object-id allocation and runtime bytes remain C# deterministic but not Java-runtime compared.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 1 assembly added-reward packet fanout branch changed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: Java runtime artifact generation, assembly deleted-part cube coverage, XP extraction timeout/follow-up, extraction/decompose add-cube senders
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: isolate XP extraction added-reward behavior.
- Why: UOW-1442 broader validation timed out in `HandleUseItemAsync_ExpExtractAddsRestrictedRewardWithCleanupSealFlag`, and UOW-1441 read-only audit found `SendExpExtractRewardPacketsAsync` likely misses Java's trailing cube update for added rewards.
- Candidate files:
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/ExpExtractAction.java`
  - `game-server/src/com/aionemu/gameserver/services/item/ItemService.java`
  - `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`

## Follow-Up Sequential Tasks

- Apply the same add-plus-cube pattern to extraction and decompose added-reward senders one family at a time.
- Keep merge branches update-only; Java `Storage.increaseItemCount` does not emit cube updates.
- Consider a later assembly deleted-part cube regression if Java storage delete fanout needs broader coverage.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | XP extraction add/cube diagnosis and implementation | `GameServerConnection.cs`, XP extraction tests | Medium | Sequential if production sender and shared test fixture both change. |
| B | Extraction/decompose read-only confirmation | Java/C# item-use reward senders | Low | Safe sidecar while XP extraction is implemented. |
| C | Toy-pet internal ordering documentation | toy-pet/kisk docs and source, read-only | Low | Separate from reward sender code. |

## Do Not Parallelize

- Multiple edits to `GameServerConnectionInventoryExpansionUseItemTests.cs`.
- Multiple edits to `GameServerConnection.cs` item-use reward senders.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1442] Add assembly reward cube update`.
- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/AssemblyItemAction.java`
  - `game-server/src/com/aionemu/gameserver/services/item/ItemService.java`
  - `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
- C# files changed in UOW-1442:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AJR-Completion.md`
