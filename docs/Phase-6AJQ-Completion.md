# Phase 6AJQ Completion - Composition Reward Merge No-Cube Regression

Date: 2026-05-27
Unit of Work: UOW-1441
Status: Complete after validation.

## Scope

Add a dedicated connection regression for Java's composition reward stack-merge path. Java `Storage.increaseItemCount` sends an inventory update only; unlike `Storage.add`, it does not send `SM_CUBE_UPDATE`.

## Completed Work

- Added deterministic input-only enchantment-stone fixture templates `166001020` and `166001030` with levels 20 and 30.
- Added `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate`.
- Seeded every possible generated reward stack for the level-20/30 composition range so random reward selection always merges into an existing stack.
- Asserted consumed input delete/cube packets still use Java order while the reward merge sends `SM_INVENTORY_UPDATE_ITEM` with `IncreaseItemCollect` and no reward cube update before final success animation.
- Ran a read-only sidecar audit of adjacent item-use reward senders. The audit found likely add-plus-cube gaps in assembly, XP extraction, extraction, and decompose added-reward branches.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_CompositeStonesAddsRewardWithJavaCubeUpdate|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_CompositeStonesConsumesSameStoneStackTwiceInJavaOrder|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_CompositeStonesSendsConsumedPacketsInJavaOrderForMixedDeletes|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_CompositeStonesWritesCleanupSealFlagsForRemainingConsumedInputs|FullyQualifiedName~CompositionServiceTests|FullyQualifiedName~GamePacketTests.ClientPacketFactory_ParsesCompositeStonesPacket"`.
- Result: passed 15 tests.

## Migration Parity Table - UOW-1441

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.CompositionAction` | `Aion.GameServer.Services.CompositionService` / `GameServerConnection.CompleteCompositeStonesAsync` | Item Action / Service / Connection Packet Caller | Partial | Unit + Regression Tested | Partial Parity | Dedicated merge regression now covers Java's reward-update path for a level-20/30 composition pair. Runtime Java bytes and scheduled real-client timing remain unavailable. |
| `com.aionemu.gameserver.services.item.ItemService.addItem` | `CompositionService.CreateMutationPlan` / `GameServerConnection.SendCompositionRewardPacketsAsync` | Service / Reward Mutation Planner | Partial | Regression Tested | Partial Parity | Seeded every possible random reward stack so the equivalent add operation must take the merge branch regardless of random reward id. No object-id runtime comparison against Java artifacts. |
| `com.aionemu.gameserver.model.items.storage.Storage.increaseItemCount` | `InventoryAddService.CreateAddItemPlan` / `SmInventoryUpdateItem.IncreaseItemCollect` | Storage Stack Merge | Partial | Regression Tested | Partial Parity | New regression asserts reward stack merge sends only `SM_INVENTORY_UPDATE_ITEM`; no cube update follows the merge before final success. Cleanup/seal blob is asserted with plain item mask `0`. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUpdatePacket` | `GameServerConnection.SendCompositionRewardPacketsAsync` plus `SmInventoryUpdateItem` | Packet Fanout Helper | Partial | Regression Tested | Partial Parity | Test validates update type `0x19` and absence of trailing cube update. Full Java runtime byte comparison remains blocked. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` | Packet | Partial | Regression Tested | Partial Parity | Merge path serializes `IncreaseItemCollect`; broader blob parity, object-id mapping, and Java runtime bytes remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate.CubeSizeSnapshot` | Packet | Partial | Regression Tested | Partial Parity | Consumed item deletes still emit cube snapshots; reward merge intentionally does not emit a cube snapshot per reviewed Java `Storage.increaseItemCount`. Expand snapshot bytes remain unverified. |
| `com.aionemu.gameserver.model.templates.item.actions.AssemblyItemAction` | `GameServerConnection.SendAssemblyRewardPacketsAsync` | Item Action / Future Sender | Partial | Existing Regression Tested | Needs Verification | Read-only sidecar found added assembly rewards likely miss Java's trailing cube update after `SM_INVENTORY_ADD_ITEM`; merge tests should remain update-only. |
| `com.aionemu.gameserver.model.templates.item.actions.ExpExtractAction` | `GameServerConnection.SendExpExtractRewardPacketsAsync` | Item Action / Future Sender | Partial | Existing Regression Tested | Needs Verification | Read-only sidecar found added XP extraction rewards likely miss Java's trailing cube update. Needs focused follow-up. |
| `com.aionemu.gameserver.model.templates.item.actions.ExtractAction` | `GameServerConnection.SendExtractRewardPacketsAsync` | Item Action / Future Sender | Partial | Existing Regression Tested | Needs Verification | Read-only sidecar found added extraction rewards likely miss Java's trailing cube update. Needs focused follow-up. |
| `com.aionemu.gameserver.model.templates.item.actions.DecomposeAction` | `GameServerConnection.SendDecomposeRewardItemsAsync` | Item Action / Future Sender | Partial | Existing Regression Tested | Needs Verification | Read-only sidecar found added decompose rewards likely miss Java's trailing cube update; existing Java artifact comparison already names this gap. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` | Regression / connection packet serialization | `CompositionAction.run`, `ItemService.addItem`, `Storage.increaseItemCount`, `ItemPacketService.sendItemUpdatePacket` | Opcode `208` consumes tool/input stones, merges any possible generated level-20/30 reward into a seeded stack, sends `SM_INVENTORY_UPDATE_ITEM` with `IncreaseItemCollect`, and does not send a reward cube update. | C# packet parsing against reviewed Java merge fanout. | No Java runtime bytes; random reward id is constrained by seeded stacks rather than fixed by Java artifact. |
| `GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_CompositeStonesAddsRewardWithJavaCubeUpdate` | Existing Regression | `CompositionAction.run`, `Storage.add` | Regression slice verifies reward add still sends add plus cube after adding input-only fixture templates. | Deterministic C# packet assertions. | No runtime bytes. |
| `CompositionServiceTests` | Existing Unit | `CompositionAction.run`, reward calculation and mutation planning | Regression slice verifies service planning remains stable. | Deterministic C# service assertions from reviewed Java logic. | Does not serialize packets. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Composition object-id allocation and exact runtime bytes remain C# deterministic but not Java-runtime compared.
- Broader scheduled item-use ordering for real clients remains unvalidated.
- Adjacent item-use added-reward senders likely still need Java add-plus-cube alignment: assembly, XP extraction, extraction, and decompose.
- House object reward add may have a similar cube-update gap but is adjacent to, not inside, this item-use unit.

## Summary Metrics

- Total Java artifacts discovered: 10 grouped artifact rows in this unit, including 4 newly discovered follow-up sender gaps
- Total artifacts ported: 0 production artifacts changed; 1 connection regression and 2 fixture templates added
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 10 grouped rows
- Total blocked artifacts: Java runtime artifact generation, composition runtime byte/object-id comparison, scheduled real-client validation, adjacent item-use add/cube senders
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: align assembly added-reward cube update behavior.
- Why: Java `AssemblyItemAction` rewards added through `ItemService.addItem -> Storage.add` send `SM_INVENTORY_ADD_ITEM` followed by `SM_CUBE_UPDATE`; read-only audit found C# added assembly rewards send only `SmInventoryAddItem.CreateItemCollect`.
- Candidate files:
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/AssemblyItemAction.java`
  - `game-server/src/com/aionemu/gameserver/services/item/ItemService.java`
  - `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`

## Follow-Up Sequential Tasks

- Apply the same add-plus-cube audit/fix pattern to `ExpExtractAction`, `ExtractAction`, and `DecomposeAction` one sender family at a time.
- Keep merge branches update-only; Java `Storage.increaseItemCount` does not emit cube updates.
- Keep toy-pet internal spawn/persistence ordering documentation as a separate low-risk documentation unit.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Assembly add/cube implementation | `GameServerConnection.cs`, assembly tests | Medium | Sequential if production sender and shared test fixture both change. |
| B | XP/extraction/decompose read-only confirmation | Java/C# item-use reward senders | Low | Safe sidecar while assembly is implemented. |
| C | Toy-pet internal ordering documentation | toy-pet/kisk docs and source, read-only | Low | Separate from reward sender code. |

## Do Not Parallelize

- Multiple edits to `GameServerConnectionInventoryExpansionUseItemTests.cs`.
- Multiple edits to `GameServerConnection.cs` item-use reward senders.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1441] Cover composition reward merge`.
- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/CompositionAction.java`
  - `game-server/src/com/aionemu/gameserver/services/item/ItemService.java`
  - `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
- C# files changed in UOW-1441:
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AJQ-Completion.md`
