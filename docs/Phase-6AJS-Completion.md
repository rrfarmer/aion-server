# Phase 6AJS Completion - XP Extraction Reward Add Cube Update

Date: 2026-05-27
Unit of Work: UOW-1443
Status: Complete after validation.

## Scope

Diagnose the XP extraction added-reward timeout from UOW-1442 and align XP extraction added-reward packet fanout with Java `ItemService.addItem -> Storage.add`. Java new reward stacks send `SM_INVENTORY_ADD_ITEM` followed by `SM_CUBE_UPDATE`; stack merges remain update-only.

## Completed Work

- Diagnosed the prior timeout: `HandleUseItemAsync_ExpExtractAddsRestrictedRewardWithCleanupSealFlag` already waited for 7 packets, but C# only sent 6 because the reward-add cube update was missing.
- Reviewed Java `ExpExtractAction.act`, which consumes the source, lowers EXP, calls `ItemService.addItem`, sends the EXP extraction system message, then sends final success animation.
- Updated `SendExpExtractRewardPacketsAsync` to accept the player snapshot and emit `SmCubeUpdate.CubeSizeSnapshot` after each newly added XP extraction reward.
- Left updated XP extraction reward stacks unchanged: they still send only `SmInventoryUpdateItem.IncreaseItemCollect`.
- Updated `HandleUseItemAsync_ExpExtractAddsRestrictedRewardWithCleanupSealFlag` to assert the trailing cube update after the restricted reward add.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ExpExtractAddsRestrictedRewardWithCleanupSealFlag|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag|FullyQualifiedName~ExpExtractServiceTests"`.
- Result: passed 5 tests.

## Migration Parity Table - UOW-1443

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.ExpExtractAction` | `GameServerConnection.HandleExpExtractUseItemAsync` / `CompleteExpExtractUseItemAsync` | Item Action / Connection Handler | Partial | Unit + Regression Tested | Partial Parity | Added-reward fanout now follows reviewed Java order: EXP stat update, item collect add, cube update, EXP extraction system message, success animation for the covered fixture. Runtime Java bytes and scheduled real-client timing remain unverified. |
| `com.aionemu.gameserver.services.item.ItemService.addItem` | `ExpExtractService.CreateMutationPlan` / `GameServerConnection.SendExpExtractRewardPacketsAsync` | Service / Reward Mutation Planner | Partial | Regression Tested | Partial Parity | New XP extraction reward stack path now emits trailing cube update. Merge path remains update-only. |
| `com.aionemu.gameserver.model.items.storage.Storage.add` | `GameServerConnection.SendExpExtractRewardPacketsAsync` plus `SmInventoryAddItem.CreateItemCollect` | Storage Add / Packet Caller | Partial | Regression Tested | Partial Parity | C# now mirrors Java storage add packet shape for covered XP extraction rewards. Persistence timing remains C# transaction-first rather than Java dirty-state lifecycle. |
| `com.aionemu.gameserver.model.items.storage.Storage.increaseItemCount` | `InventoryAddService.CreateAddItemPlan` / `SmInventoryUpdateItem.IncreaseItemCollect` | Storage Stack Merge | Partial | Regression Tested | Partial Parity | Existing XP extraction merge regression still expects update-only behavior and no cube update. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket` | `SmInventoryAddItem.CreateItemCollect` plus `SmCubeUpdate.CubeSizeSnapshot` | Packet Fanout Helper | Partial | Regression Tested | Partial Parity | XP extraction added reward now sends item collect add followed by cube count `2` for the covered fixture. Warehouse variants remain outside this unit. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_ADD_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem` | Packet | Partial | Regression Tested | Partial Parity | Test asserts `ITEM_COLLECT` add type and restricted cleanup/seal blob for XP extraction reward add. Full Java runtime byte comparison remains blocked. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate.CubeSizeSnapshot` | Packet | Partial | Regression Tested | Partial Parity | Test asserts projected cube count after XP extraction reward add. Expand snapshot bytes remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_STATS_INFO` / EXP update equivalent | `Aion.GameServer.Network.Aion.ServerPackets.SmStatUpdateExp` | Packet | Partial | Regression Tested | Partial Parity | Existing EXP stat packet remains in the Java-shaped position before reward packets. Exact Java byte comparison remains unavailable. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ExpExtractAddsRestrictedRewardWithCleanupSealFlag` | Regression / connection packet serialization | `ExpExtractAction.act`, `ItemService.addItem`, `Storage.add`, `ItemPacketService.sendStorageUpdatePacket` | Source update, EXP update, restricted reward add, trailing cube update, EXP extraction message, and final success animation. | C# packet parsing against reviewed Java add/cube fanout. | No Java runtime bytes; fixture covers remaining-source update, not source delete. |
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag` | Existing Regression | `Storage.increaseItemCount` | Regression slice verifies XP extraction reward merge remains update-only. | Deterministic C# packet assertions from reviewed Java merge branch. | No runtime bytes. |
| `ExpExtractServiceTests` | Existing Unit | `ExpExtractAction.canAct` / required EXP and mutation planning | Regression slice verifies service planning remains stable. | Deterministic C# service assertions. | Does not serialize packets. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- XP extraction source-delete branch was not broadened in this unit; the covered fixture consumes from a remaining source stack.
- Extraction and decompose added-reward senders likely still need Java add-plus-cube alignment.
- Exact object-id allocation, EXP stat bytes, and runtime bytes remain C# deterministic but not Java-runtime compared.
- Scheduled real-client item-use ordering remains unvalidated.

## Summary Metrics

- Total Java artifacts discovered: 8 grouped artifact rows in this unit
- Total artifacts ported: 1 XP extraction added-reward packet fanout branch changed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 8 grouped rows
- Total blocked artifacts: Java runtime artifact generation, XP extraction source-delete coverage, extraction/decompose add-cube senders, real-client scheduled ordering
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: align extraction added-reward cube update behavior.
- Why: UOW-1441 read-only audit found `SendExtractRewardPacketsAsync` likely misses Java's trailing cube update for added extraction rewards. UOW-1440 through UOW-1443 now provide the composition, assembly, and XP extraction reference pattern.
- Candidate files:
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/ExtractAction.java`
  - `game-server/src/com/aionemu/gameserver/services/item/ItemService.java`
  - `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`

## Follow-Up Sequential Tasks

- Apply the same add-plus-cube pattern to decompose added-reward senders after extraction.
- Keep merge branches update-only; Java `Storage.increaseItemCount` does not emit cube updates.
- Consider source-delete branch coverage for XP extraction if Java storage delete fanout needs broader coverage.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Extraction add/cube implementation | `GameServerConnection.cs`, extraction tests | Medium | Sequential if production sender and shared test fixture both change. |
| B | Decompose read-only confirmation | Java/C# decompose sender paths | Low | Safe sidecar while extraction is implemented. |
| C | Toy-pet internal ordering documentation | toy-pet/kisk docs and source, read-only | Low | Separate from reward sender code. |

## Do Not Parallelize

- Multiple edits to `GameServerConnectionInventoryExpansionUseItemTests.cs`.
- Multiple edits to `GameServerConnection.cs` item-use reward senders.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1443] Add XP extraction reward cube update`.
- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/ExpExtractAction.java`
  - `game-server/src/com/aionemu/gameserver/services/item/ItemService.java`
  - `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
- C# files changed in UOW-1443:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AJS-Completion.md`
