# Phase 6AJJ Completion - Composition Consumed Packet Ordering

Date: 2026-05-27
Unit of Work: UOW-1434
Status: Complete after validation.

## Scope

Align composition consumed input packet ordering with Java `CompositionAction`: tool, first stone, then second stone, sending each storage update/delete immediately. Java remains the source of truth; this unit does not claim runtime byte parity.

## Completed Work

- Added ordered `CompositionConsumedItemMutation` descriptors to `CompositionMutationPlan`.
- Preserved existing `UpdatedConsumedItems` and `DeletedConsumedObjectIds` lists for persistence callers.
- Changed `GameServerConnection.CompleteCompositeStonesAsync` to send composition consumed packets from ordered descriptors.
- Added Java-order delete/cube fanout for exhausted composition inputs while preserving cleanup/seal metadata for remaining-stack full updates.
- Added service assertions for ordered consumed mutations.
- Added a focused opcode `208` connection test for mixed delete/update/delete composition input packets.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~CompositionServiceTests|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_CompositeStonesWritesCleanupSealFlagsForRemainingConsumedInputs|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_CompositeStonesSendsConsumedPacketsInJavaOrderForMixedDeletes|FullyQualifiedName~GamePacketTests.ClientPacketFactory_ParsesCompositeStonesPacket"`.
- Result: passed 11 tests.

## Migration Parity Table - UOW-1434

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.CompositionAction` | `Aion.GameServer.Services.CompositionService` / `GameServerConnection.CompleteCompositeStonesAsync` | Item Action / Service / Connection Packet Caller | Partial | Unit + Regression Tested | Partial Parity | C# now records and emits consumed tool/stone packet operations in Java order. Scheduled no-rollback edge where an item disappears after initial validation remains packet-unverified. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseByItemId` | `Aion.GameServer.Services.CompositionConsumedItemMutation` / `CompositionMutationPlan.ConsumedItemMutations` | Storage Mutation Descriptor | Partial | Unit Tested | Partial Parity | Ordered descriptors preserve update/delete packet order while existing final updated/deleted lists remain available for persistence. Same-item repeated consumption and Java runtime comparison remain unverified. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemPacket` / `sendItemDeletePacket` | `GameServerConnection.SendCompositionConsumedItemPacketsAsync` | Packet Fanout Helper | Partial | Regression Tested | Partial Parity | Mixed composition consumed inputs now send delete/cube, update, delete/cube in Java order. Warehouse variants do not apply; runtime byte comparison remains blocked. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE_ITEM` use-delete type | `Aion.GameServer.Network.Aion.ServerPackets.SmDeleteItem.UseDeleteType` | Packet | Complete | Regression Tested | Partial Parity | Composition exhausted consumed inputs use mask `0x17`. No Java runtime byte capture was available. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate.CubeSizeSnapshot` | Packet | Partial | Regression Tested | Partial Parity | Mixed composition test asserts projected cube counts after each consumed delete. Expand field snapshots and Java runtime bytes remain unverified. |
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` cleanup/seal flag | `SmInventoryUpdateItem` via composition remaining consumed update | Serialization Entry | Partial | Regression Tested | Partial Parity | Existing remaining-stack composition consumed update coverage still passes with cleanup/seal flag `3`. Temporary-exchange and runtime conditioning serializer gaps remain. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CompositionServiceTests.CreateMutationPlan_ConsumesInputsAndAddsCalculatedReward` | Unit | `CompositionAction.run`, `Storage.decreaseByItemId` | Mutation plan records ordered consumed operations: update tool, delete first, delete second, while reward planning still succeeds. | Deterministic C# service assertions from reviewed Java sequence. | No packet serialization; same-item repeated consume not covered. |
| `CompositionServiceTests.CreateMutationPlan_ConsumesWhatJavaDecreaseByItemIdCanConsumeWhenSecondStoneIsMissing` | Unit | Java no-rollback sequential consume behavior | Ordered descriptors preserve the successful consumed deletes before missing second-stone failure and no reward. | Deterministic C# service assertions from reviewed Java sequence. | No connection packet ordering for scheduled post-validation disappearance. |
| `GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_CompositeStonesSendsConsumedPacketsInJavaOrderForMixedDeletes` | Regression / connection packet serialization | `CM_COMPOSITE_STONES`, `CompositionAction`, `Storage.decreaseByItemId`, `ItemPacketService` | Opcode `208` sends start animation, tool use-delete/cube, first-stone full update with cleanup/seal, second-stone use-delete/cube, and end animation in Java order. | C# packet parsing against reviewed Java packet fanout order and masks. | No Java runtime bytes; no scheduled disappearing-stone edge. |
| `GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_CompositeStonesWritesCleanupSealFlagsForRemainingConsumedInputs` | Existing Regression | Same Java action, remaining-stack branch | Regression slice verifies all-remaining composition consumed updates still carry cleanup/seal flag `3`. | Deterministic C# packet assertions. | No delete branches in this test. |

## Remaining Risks

- Scheduled no-rollback edge remains: Java can consume tool/first, fail second at completion, add no reward, and still send success end animation.
- Same-item repeated composition consumption may emit update then delete for the same object in Java; C# ordered descriptors are designed for it but lack focused tests.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- AP extraction delete/cube semantics remain unaudited.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 ordered composition consumed packet fanout path changed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: Java runtime artifact generation, scheduled composition no-rollback edge, same-item repeated consume coverage, AP extraction delete/cube audit
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: add connection-level coverage for the scheduled composition no-rollback edge.
- Why: Java validates object ids before scheduling, but at completion it consumes by item id and does not rollback successful earlier consumes if a later consume fails.
- Suggested test approach: submit a valid opcode `208` request, remove or alter the second stone from `player.InventoryItems` before the callback fires, and assert successful earlier consume packets, no reward packet, and success end animation.
- Candidate files:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | AP extraction delete/cube audit | Java/C# AP extraction sources, read-only | Medium | Separate Java action and C# helper; do not mix with composition implementation. |
| B | Toy-pet source consume readiness | Java/C# toy-pet/kisk sources, read-only | Medium | Scheduling/world-spawn packet order still needs careful mapping. |
| C | Same-item composition repeated consume analysis | Java/C# composition sources, read-only | Medium | Check whether real client/static-data can submit same item family and whether packet edge is reachable. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Explorer A | Audit AP extraction delete/cube behavior | Java/C# AP extraction sources, read-only | all writes, docs, commits |
| Orchestrator | Add composition no-rollback edge test/fix if needed | selected composition connection/test files | shared docs until validation; unrelated files |

## Do Not Parallelize

- `GameServerConnection.cs` composition implementation changes.
- Shared item-use test fixture edits.
- Shared progress/handoff/audit docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1434] Align composition consumed packet ordering`.
- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_COMPOSITE_STONES.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/CompositionAction.java`
  - `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
- C# files changed in UOW-1434:
  - `dotnetConversion/src/Aion.GameServer/Services/CompositionService.cs`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/CompositionServiceTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AJJ-Completion.md`
