# Phase 6AJP Completion - Composition Reward Add Cube Update

Date: 2026-05-27
Unit of Work: UOW-1440
Status: Complete after validation.

## Scope

Align composition reward-add packet fanout with Java `ItemService.addItem` / `Storage.add` after UOW-1437 covered consumed-item ordering. Java remains the source of truth; this unit does not claim runtime byte parity.

## Completed Work

- Audited Java `CompositionAction.run`, `ItemService.addItem`, `Storage.add`, and `ItemPacketService.sendStorageUpdatePacket`.
- Confirmed Java sends `SM_INVENTORY_ADD_ITEM` followed by `SM_CUBE_UPDATE` for new composition reward items.
- Confirmed reward stack merges send `SM_INVENTORY_UPDATE_ITEM` only; no cube update is sent for merge.
- Updated C# composition reward add fanout to emit `SmCubeUpdate.CubeSizeSnapshot` after each `SmInventoryAddItem`.
- Added a deterministic opcode `208` connection test that supplies every possible random reward template for a level-20/30 composition pair and asserts consumed packets, reward add, cube update, and success end animation.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_CompositeStonesAddsRewardWithJavaCubeUpdate|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_CompositeStonesConsumesSameStoneStackTwiceInJavaOrder|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_CompositeStonesSendsConsumedPacketsInJavaOrderForMixedDeletes|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_CompositeStonesWritesCleanupSealFlagsForRemainingConsumedInputs|FullyQualifiedName~CompositionServiceTests|FullyQualifiedName~GamePacketTests.ClientPacketFactory_ParsesCompositeStonesPacket"`.
- Result: passed 14 tests.

## Migration Parity Table - UOW-1440

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.CompositionAction` | `Aion.GameServer.Services.CompositionService` / `GameServerConnection.CompleteCompositeStonesAsync` | Item Action / Service / Connection Packet Caller | Partial | Unit + Regression Tested | Partial Parity | New reward-add packet fanout now includes Java cube update after `SM_INVENTORY_ADD_ITEM`. Reward merge path remains update-only as Java `Storage.increaseItemCount` does not send cube update. Runtime Java bytes remain unavailable. |
| `com.aionemu.gameserver.services.item.ItemService.addItem` | `CompositionService.CreateMutationPlan` / `GameServerConnection.SendCompositionRewardPacketsAsync` | Service / Reward Mutation Planner | Partial | Regression Tested | Partial Parity | Deterministic connection test covers every possible random reward id for a level-20/30 pair by adding fixture templates; reward object id remains C# allocated and not Java-runtime compared. |
| `com.aionemu.gameserver.model.items.storage.Storage.add` | `GameServerConnection.SendCompositionRewardPacketsAsync` plus `SmInventoryAddItem.CreateItemCollect` | Storage Add / Packet Caller | Partial | Regression Tested | Partial Parity | C# now mirrors Java `Storage.add -> sendStorageUpdatePacket` add/cube sequence for composition reward adds. Persistence timing remains C# transaction-first rather than Java dirty-state lifecycle. |
| `com.aionemu.gameserver.model.items.storage.Storage.increaseItemCount` | `InventoryAddService.CreateAddItemPlan` / `SmInventoryUpdateItem.IncreaseItemCollect` | Storage Stack Merge | Partial | Existing Regression Tested | Partial Parity | Reward stack merges remain update-only with no cube update, matching reviewed Java path. This unit did not add a dedicated merge no-cube test. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket` | `SmInventoryAddItem.CreateItemCollect` plus `SmCubeUpdate.CubeSizeSnapshot` | Packet Fanout Helper | Partial | Regression Tested | Partial Parity | Test asserts `ITEM_COLLECT` add packet followed by cube update and final success animation. Warehouse variants are outside composition reward scope. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_ADD_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem` | Packet | Partial | Regression Tested | Partial Parity | Test asserts add type `ITEM_COLLECT` (`0x19`) for composition reward add. Full Java runtime byte comparison and object-id mapping remain blocked. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate.CubeSizeSnapshot` | Packet | Partial | Regression Tested | Partial Parity | Test asserts cube count after consumed inputs are removed and reward is added. Expand snapshots and Java runtime bytes remain unverified. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_CompositeStonesAddsRewardWithJavaCubeUpdate` | Regression / connection packet serialization | `CompositionAction.run`, `ItemService.addItem`, `Storage.add`, `ItemPacketService.sendStorageUpdatePacket` | Opcode `208` consumes tool/stone inputs, adds any possible generated level-20/30 reward as `ITEM_COLLECT`, sends `SM_CUBE_UPDATE`, then success end animation. | C# packet parsing against reviewed Java add/cube packet fanout. | No Java runtime bytes; random reward id is constrained by fixture templates rather than fixed by Java artifact. |
| `GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_CompositeStonesConsumesSameStoneStackTwiceInJavaOrder` | Existing Regression | `CompositionAction.run`, `Storage.decreaseByItemId` | Regression slice verifies same-stack consumed packet order still passes after reward add cube change. | Deterministic C# packet assertions. | No runtime bytes. |
| `CompositionServiceTests` | Existing Unit | `CompositionAction.run`, reward calculation and consumed mutation planning | Regression slice verifies service planning remains stable. | Deterministic C# service assertions from reviewed Java logic. | Does not serialize packets. |

## Remaining Risks

- This unit does not add a dedicated reward-merge no-cube regression; existing merge behavior was source-reviewed and left unchanged.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Object-id mapping for generated rewards remains C# deterministic but not compared to Java runtime artifacts.
- Broader composition real-client scheduled ordering remains a readiness item.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 1 composition reward-add packet fanout branch changed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: Java runtime artifact generation, reward object-id runtime mapping, reward-merge dedicated no-cube coverage, real-client scheduled ordering
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: add a dedicated composition reward-merge no-cube regression.
- Why: Java `Storage.increaseItemCount` sends `SM_INVENTORY_UPDATE_ITEM` only for reward merges. UOW-1440 source-reviewed and preserved this, but did not add focused coverage.
- Candidate files:
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/CompositionAction.java`
  - `game-server/src/com/aionemu/gameserver/services/item/ItemService.java`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`

## Alternative Sequential Task

- Task: document toy-pet internal spawn/persistence ordering as an intentional C# safety difference.
- Why: UOW-1439 aligned packet-visible toy-pet source consume behavior, but C# still spawns/rolls back around persistence differently from Java's consume-then-spawn order.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Composition reward-merge no-cube regression | composition connection test fixture | Medium | Seed every possible level-20/30 reward stack to force merge regardless of random result. |
| B | Toy-pet internal ordering documentation | toy-pet/kisk docs and source, read-only | Low | Documentation-only candidate. |
| C | Broader item add cube audit | Java `ItemService.addItem` callers and C# item-use reward senders, read-only | Medium | Search for other new-add paths missing cube updates. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Explorer A | Audit other item-use reward add cube update callers | Java item action/item service and C# item-use reward send paths, read-only | all writes, docs, commits |
| Orchestrator | Add reward-merge no-cube regression if selected | `GameServerConnectionInventoryExpansionUseItemTests.cs` only unless test exposes a bug | shared docs until validation; unrelated files |

## Do Not Parallelize

- Shared item-use test fixture edits.
- `GameServerConnection.cs` item-use packet fanout edits.
- Shared progress/handoff/audit docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1440] Add composition reward cube update`.
- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/CompositionAction.java`
  - `game-server/src/com/aionemu/gameserver/services/item/ItemService.java`
  - `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
- C# files changed in UOW-1440:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AJP-Completion.md`
