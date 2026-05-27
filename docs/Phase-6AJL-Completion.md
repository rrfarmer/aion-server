# Phase 6AJL Completion - AP Extraction Delete/Cube Packet Parity

Date: 2026-05-27
Unit of Work: UOW-1436
Status: Complete after validation.

## Scope

Align AP extraction target/tool consumed-item packet fanout with Java `ApExtractAction.act`. Java remains the source of truth; this unit does not claim runtime byte parity.

## Completed Work

- Audited Java `ApExtractAction`, `Storage.delete`, `Storage.decreaseByObjectId`, and `ItemPacketService.sendItemDeletePacket`.
- Updated C# AP extraction target deletion to use Java default delete mask `0x00` instead of use-delete mask `0x17`.
- Added cube-size snapshots after AP extraction target deletion and after exhausted extraction-tool deletion.
- Kept remaining-stack tool consumption as a full inventory update with cleanup/seal metadata and no cube update.
- Updated existing AP extraction connection tests and added exhausted-tool coverage.
- Spawned and closed two read-only explorers:
  - AP extraction packet analysis confirmed this unit's target/source delete+cubesize changes.
  - Same-item composition analysis confirmed the next recommended regression is Java-reachable.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ApExtractSendsAbyssPointsPlannerPackets|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ApExtractHonorsConfiguredAbyssPointCap|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ApExtractDeletesLastToolWithUseDeleteAndCubeUpdate|FullyQualifiedName~ApExtractServiceTests"`.
- Result: passed 5 tests.

## Migration Parity Table - UOW-1436

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.ApExtractAction` | `Aion.GameServer.Services.ApExtractService` / `GameServerConnection.HandleApExtractUseItemAsync` | Item Action / Service / Connection Packet Caller | Partial | Unit + Regression Tested | Partial Parity | C# now mirrors Java packet-visible AP extraction target delete before tool consume. Java runtime byte comparison remains unavailable, and AP rank-change broadcast/legion side effects beyond local player packets are not fully traced in this unit. |
| `com.aionemu.gameserver.model.items.storage.Storage.delete` | `GameServerConnection.SendApExtractConsumedItemPacketsAsync` | Storage / Packet Caller | Partial | Regression Tested | Partial Parity | AP extraction target deletion now uses Java default delete mask `0x00` and sends a cube-size snapshot after target removal. Repository timing and runtime bytes remain unverified. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseByObjectId` | `Aion.GameServer.Services.ApExtractService.DecreaseItemCount` plus AP extraction packet fanout | Storage Mutation Descriptor / Packet Caller | Partial | Unit + Regression Tested | Partial Parity | Remaining tool stacks still send `SM_INVENTORY_UPDATE_ITEM` with cleanup/seal metadata; exhausted tool stacks now send use-delete mask `0x17` plus cube update. Java tool-consume failure after target delete is still not modeled as a separate packet edge. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemDeletePacket` | `Aion.GameServer.Network.Aion.ServerPackets.SmDeleteItem` plus `SmCubeUpdate.CubeSizeSnapshot` | Packet Fanout Helper | Partial | Regression Tested | Partial Parity | Focused AP extraction tests assert target default delete/cube and exhausted source use-delete/cube in Java order. Warehouse variants do not apply to cube-only AP extraction. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE_ITEM` default and use delete types | `Aion.GameServer.Network.Aion.ServerPackets.SmDeleteItem` | Packet | Complete | Regression Tested | Partial Parity | Tests assert target mask `0x00` and exhausted tool mask `0x17`. No Java runtime byte capture was available. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate.CubeSizeSnapshot` | Packet | Partial | Regression Tested | Partial Parity | Tests assert projected cube counts after target removal and after exhausted tool removal. Expand fields are represented-player snapshots; Java runtime bytes remain blocked. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_COMPOSITE_STONES` / `CompositionAction` same-object consume edge | `Aion.GameServer.Services.CompositionService` / `GameServerConnection.HandleCompositeStonesAsync` | Client Packet / Item Action | Partial | No Tests in this unit | Needs Verification | Newly discovered dependency: same first/second stone object ids are Java-reachable for stackable enchantment stones. C# ordered descriptors appear able to send update then delete for the same object, but no focused regression exists yet. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ApExtractSendsAbyssPointsPlannerPackets` | Regression / connection packet serialization | `ApExtractAction.act`, `Storage.delete`, `Storage.decreaseByObjectId`, `ItemPacketService.sendItemDeletePacket` | Stacked AP extraction tool path sends target default delete mask `0`, cube update, remaining tool full update with cleanup/seal flag `3`, then AP packets. | C# packet parsing against reviewed Java packet fanout. | No Java runtime bytes; AP rank-change broadcast/legion side effects outside local packet list not traced. |
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ApExtractHonorsConfiguredAbyssPointCap` | Regression / connection packet serialization | Same Java action plus AP add cap planning | Capped AP path keeps the same target default delete/cube and remaining tool update packet order, then emits capped AP packets. | Deterministic C# packet assertions from Java-reviewed behavior. | No Java runtime bytes. |
| `GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_ApExtractDeletesLastToolWithUseDeleteAndCubeUpdate` | Regression / connection packet serialization | `ApExtractAction.act`, `Storage.delete`, `Storage.decreaseByObjectId`, `ItemPacketService.sendItemDeletePacket` | Single-count AP extraction tool sends target default delete/cube, exhausted tool use-delete mask `0x17`, second cube update, then AP packets. | C# packet parsing against reviewed Java storage packet fanout. | No Java runtime bytes; Java target-delete-success/tool-consume-failure edge not modeled. |
| `ApExtractServiceTests` | Unit | `ApExtractAction.canAct + act` | Regression slice verifies AP extraction mutation planning still handles can-act, AP math, target delete, and source decrement/delete state. | Deterministic C# service assertions. | Service tests do not serialize packets or compare Java runtime output. |

## Remaining Risks

- Java deletes the AP extraction target before attempting tool consume; C# planning still treats the operation as an atomic success path and does not model a target-delete-success/tool-consume-failure packet edge.
- AP rank-change broadcast, legion contribution, and deeper rank-limited equipment/skill side effects remain broader AP-service parity risks outside this focused local-player packet unit.
- Same-object composition first/second stone consumption is now confirmed reachable and needs focused regression coverage.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 1 AP extraction consumed-item packet fanout branch changed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: Java runtime artifact generation, AP extraction tool-consume failure edge, AP rank-change secondary side effects, same-object composition regression coverage
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: add same-object composition repeated consume regression coverage.
- Why: Java allows first and second stone object ids to be equal. With a count-2 stack, Java should emit a remaining-stack update for the first stone consume followed by use-delete/cube for the second consume, then reward and success end animation.
- Candidate files:
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_COMPOSITE_STONES.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/CompositionAction.java`
  - `dotnetConversion/src/Aion.GameServer/Services/CompositionService.cs`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/CompositionServiceTests.cs`

## Alternative Sequential Task

- Task: model or explicitly document AP extraction's Java target-delete-success/tool-consume-failure edge.
- Why: Java deletes the target before consuming the extraction tool. If tool consume fails after target delete, Java does not add AP and does not roll back the target delete. C# currently plans only an atomic success/failure path.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Same-object composition connection regression | composition service/connection tests | Medium | Shared test fixture; keep implementation single-owner. |
| B | AP extraction failure-edge analysis | Java AP extraction/storage and C# AP extraction service/repository sources, read-only | Medium | Determine whether a practical C# test seam exists without unsafe partial persistence. |
| C | Toy-pet source consume readiness | Java/C# toy-pet/kisk sources, read-only | Medium | Scheduling/world-spawn packet order still needs careful mapping. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Explorer A | Analyze AP extraction target-delete-success/tool-consume-failure edge | Java AP extraction/storage sources and C# AP extraction persistence sources, read-only | all writes, docs, commits |
| Orchestrator | Add same-object composition repeated consume coverage | selected composition tests and production only if test exposes a bug | shared docs until validation; unrelated files |

## Do Not Parallelize

- `GameServerConnection.cs` item-use packet fanout changes.
- Shared item-use test fixture edits.
- Shared progress/handoff/audit docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1436] Align AP extraction delete cube packets`.
- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/ApExtractAction.java`
  - `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_COMPOSITE_STONES.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/CompositionAction.java`
- C# files changed in UOW-1436:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AJL-Completion.md`
