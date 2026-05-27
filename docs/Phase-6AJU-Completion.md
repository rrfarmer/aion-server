# Phase 6AJU Completion - Decompose Reward Add Cube Update

Date: 2026-05-27
Unit of Work: UOW-1445
Status: Complete after validation.

## Scope

Align normal and selectable decompose added-reward packet fanout with Java `ItemService.addItem -> Storage.add`. New reward stacks send `SM_INVENTORY_ADD_ITEM` followed by `SM_CUBE_UPDATE`; stack merges remain update-only.

## Completed Work

- Reviewed Java `DecomposeAction`: normal and selectable reward branches add rewards through `ItemService.addItem` with decomposable add/update masks.
- Updated shared `SendDecomposeRewardItemsAsync` to accept the player snapshot and emit `SmCubeUpdate.CubeSizeSnapshot` after each new `SmInventoryAddItem.CreateDecomposable`.
- Left decompose reward stack merges unchanged: they still send only `SmInventoryUpdateItem.IncreaseItemCollect`.
- Updated normal decompose, source-delete decompose, restricted reward, selectable reward, encrypted use-item, encrypted select-decompose, and selectable artifact-comparison tests to expect reward-add trailing cube updates.
- Replaced the old selectable artifact comparison that reported Java's trailing cube as a parity gap; the current C# packet shape now includes that cube update.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_DecomposeAddsRestrictedRewardWithCleanupSealFlag|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_DecomposeMergesRestrictedRewardWithCleanupSealFlag|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_DecomposeDeletesLastSourceAndAddsReward|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.RunAsync_EncryptedUseItemFrameSchedulesAndCompletesDecompose|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.RunAsync_EncryptedUseItemFrameDeletesLastSourceAndAddsReward|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleSelectDecomposableAsync_|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.RunAsync_EncryptedSelectDecomposableFrameDispatchesSelection|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.CaptureSelectableDecomposeObservationJson|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.CompareSelectableDecomposeJavaArtifacts|FullyQualifiedName~DecomposeServiceTests"`.
- Result: passed 27 tests.

## Migration Parity Table - UOW-1445

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.DecomposeAction` | `GameServerConnection.HandleDecomposeUseItemAsync` / `HandleSelectDecomposableAsync` | Item Action / Connection Handler | Partial | Unit + Regression Tested | Partial Parity | Normal and selectable added-reward fanout now includes Java-shaped reward add plus trailing cube update. Runtime Java bytes and scheduled real-client timing remain unverified. |
| `com.aionemu.gameserver.services.item.ItemService.addItem` | `DecomposeService` reward planning / `GameServerConnection.SendDecomposeRewardItemsAsync` | Service / Reward Mutation Planner | Partial | Regression Tested | Partial Parity | New decompose reward stack path now emits trailing cube update for normal and selectable decompose. Merge path remains update-only. |
| `com.aionemu.gameserver.model.items.storage.Storage.add` | `SendDecomposeRewardItemsAsync` plus `SmInventoryAddItem.CreateDecomposable` | Storage Add / Packet Caller | Partial | Regression Tested | Partial Parity | C# now mirrors Java storage add packet shape for covered normal/selectable decompose rewards, including source-update and source-delete cases. Persistence timing remains C# transaction-first. |
| `com.aionemu.gameserver.model.items.storage.Storage.increaseItemCount` | `InventoryAddService.CreateAddItemPlan` / `SmInventoryUpdateItem.IncreaseItemCollect` | Storage Stack Merge | Partial | Regression Tested | Partial Parity | Existing decompose merge regression still expects update-only behavior and no reward cube update. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket` | `SmInventoryAddItem.CreateDecomposable` plus `SmCubeUpdate.CubeSizeSnapshot` | Packet Fanout Helper | Partial | Regression Tested | Partial Parity | Decompose added rewards now send decomposable add followed by cube counts for ordinary, full-cube-overflow, source-delete, selectable, and encrypted-frame paths. Warehouse variants remain outside this unit. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_ADD_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem` | Packet | Partial | Regression Tested | Partial Parity | Tests assert decomposable add type and restricted cleanup/seal blob for covered reward adds. Full Java runtime byte comparison remains blocked. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate.CubeSizeSnapshot` | Packet | Partial | Regression Tested | Partial Parity | Tests assert projected cube counts after source delete when applicable and after reward adds. Expand snapshot bytes remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_FIRST_SHOW_DECOMPOSABLE` / `SM_SECONDARY_SHOW_DECOMPOSABLE` | `SmFirstShowDecomposable` / `SmSecondaryShowDecomposable` | Packet | Partial | Regression Tested | Partial Parity | Selectable reward artifact projection now includes reward-add trailing cube in the expected packet class sequence. Java artifact files are still unavailable locally. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleUseItemAsync_DecomposeAddsRestrictedRewardWithCleanupSealFlag` | Regression / connection packet serialization | `DecomposeAction.run`, `ItemService.addItem`, `Storage.add` | Normal decompose source update, restricted decomposable reward add, and reward cube update. | C# packet parsing against reviewed Java add/cube fanout. | No Java runtime bytes. |
| `HandleUseItemAsync_DecomposeDeletesLastSourceAndAddsReward` | Regression / connection packet serialization | Same Java action/storage path | Source use-delete/cube, reward add, and reward cube update. | C# packet parsing against reviewed Java add/cube fanout. | No Java runtime bytes. |
| `HandleUseItemAsync_DecomposeMergesRestrictedRewardWithCleanupSealFlag` | Existing Regression | `Storage.increaseItemCount` | Decompose reward merge remains update-only. | Deterministic C# packet assertions from reviewed Java merge branch. | No runtime bytes. |
| `HandleSelectDecomposableAsync_*` and `RunAsync_EncryptedSelectDecomposableFrameDispatchesSelection` | Regression / selectable packet serialization | `CM_SELECT_DECOMPOSABLE`, selectable decompose reward handling, `Storage.add` | Selectable source update/delete, secondary-show packet, reward add, and trailing cube update. | C# packet parsing and observation JSON comparison shape. | Java selectable artifact files are still unavailable. |
| `CaptureSelectableDecomposeObservationJson_ProjectsContractComparablePackets` / `CompareSelectableDecomposeJavaArtifacts_*` | Artifact projection / regression | Selectable Java artifact contract | Packet class sequences and mapped fields now include reward-add trailing cube update. | Deterministic C# artifact projection; mapped synthetic Java comparison. | Live Java artifact generation remains blocked. |
| `RunAsync_EncryptedUseItemFrameSchedulesAndCompletesDecompose` / `RunAsync_EncryptedUseItemFrameDeletesLastSourceAndAddsReward` | Encrypted frame regression | `CM_USE_ITEM`, normal decompose storage fanout | Encrypted use-item frames now assert reward-add cube update for source-update and source-delete cases. | C# encrypted frame dispatch assertions. | No Java runtime encrypted frames. |
| `DecomposeServiceTests` | Existing Unit | `DecomposeAction.canAct` / reward planning | Regression slice verifies reward planning remains stable. | Deterministic C# service assertions. | Does not serialize packets. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Normal and selectable decompose final success-animation ordering still reflects the current C# implementation and existing tests; Java normal decompose sends reward add before final success animation, while C# sends final success before reward packets. This needs a separate ordering audit before changing packet order.
- Exact object-id allocation and runtime bytes remain C# deterministic but not Java-runtime compared.
- Scheduled real-client item-use ordering remains unvalidated.
- The item-use add-plus-cube sender batch is now covered for composition, assembly, XP extraction, extraction, and decompose, but broader warehouse/house-object reward add variants remain outside this batch.

## Summary Metrics

- Total Java artifacts discovered: 8 grouped artifact rows in this unit
- Total artifacts ported: 1 shared decompose added-reward packet fanout branch changed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 8 grouped rows
- Total blocked artifacts: Java runtime artifact generation, decompose final-success ordering audit, live selectable artifacts, real-client scheduled ordering
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: audit decompose normal/selectable final success-animation ordering, or document toy-pet internal spawn/persistence ordering.
- Why: The add-plus-cube sender batch is closed, and the next visible risks are ordering/documentation items rather than another obvious missing add/cube sender.
- Candidate files:
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/DecomposeAction.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/ToyPetSpawnAction.java`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
  - docs/progress/handoff docs

## Follow-Up Sequential Tasks

- Real-client validate scheduled item-use ordering for decompose, assembly, XP extraction, composition, extraction, and AP extraction once Java runtime tooling is available.
- Consider broader Java `EnchantService.breakItem` RNG/drop table parity after packet fanout gaps are closed.
- Consider source-delete branch coverage for XP extraction if Java storage delete fanout needs broader coverage.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Decompose final-success ordering audit | Java/C# decompose sources and tests | Medium | Start read-only; production reorder needs dedicated tests. |
| B | Toy-pet internal ordering documentation | toy-pet/kisk docs and source, read-only | Low | Documentation-only candidate from earlier handoffs. |
| C | Extraction RNG/drop audit | Java/C# enchant service sources | Medium | Read-only sidecar only; avoid mixing with ordering changes. |

## Do Not Parallelize

- Shared `GameServerConnection.cs` packet-order changes.
- Multiple edits to `GameServerConnectionInventoryExpansionUseItemTests.cs`.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1445] Add decompose reward cube update`.
- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/DecomposeAction.java`
  - `game-server/src/com/aionemu/gameserver/services/item/ItemService.java`
  - `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
- C# files changed in UOW-1445:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AJU-Completion.md`
