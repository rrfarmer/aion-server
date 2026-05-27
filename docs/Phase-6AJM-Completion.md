# Phase 6AJM Completion - Composition Same-Stack Consume Coverage

Date: 2026-05-27
Unit of Work: UOW-1437
Status: Complete after validation.

## Scope

Cover Java-reachable same-object repeated composition consumption after UOW-1434/UOW-1435 aligned composition packet ordering and no-rollback behavior. Java remains the source of truth; this unit does not claim runtime byte parity.

## Completed Work

- Confirmed Java `CM_COMPOSITE_STONES` and `CompositionAction.canAct` do not require distinct first/second stone object ids.
- Added service coverage proving C# records Java-ordered same-object consumed mutations: tool delete, same-stone update, then same-stone delete.
- Added opcode `208` connection coverage proving the same object receives an update packet followed by a use-delete/cube packet in Java order.
- Stabilized composition connection fixtures by replacing level-90/95 fixture stones with level-80/85 stones whose random reward range has no fixture reward template, preventing reward merges from changing consumed-input counts.
- Read-only AP extraction sidecar found the target-delete-success/tool-consume-failure edge is only theoretical under normal same-client Java packet processing; C# remains atomic here and the risk is documented.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~CompositionServiceTests|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_CompositeStonesConsumesSameStoneStackTwiceInJavaOrder|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_CompositeStonesSendsConsumedPacketsInJavaOrderForMixedDeletes|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_CompositeStonesKeepsEarlierConsumesWhenSecondStoneDisappearsBeforeCompletion|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_CompositeStonesWritesCleanupSealFlagsForRemainingConsumedInputs|FullyQualifiedName~GamePacketTests.ClientPacketFactory_ParsesCompositeStonesPacket"`.
- Result: passed 14 tests.

## Migration Parity Table - UOW-1437

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_COMPOSITE_STONES` | `Aion.GameServer.Network.Aion.ClientPackets.CmCompositeStones` / `GameServerConnection.HandleCompositeStonesAsync` | Client Packet / Connection Handler | Partial | Regression Tested | Partial Parity | Same first/second stone object ids are now covered through opcode `208`; C# accepts the repeated object id like Java. Runtime Java bytes remain unavailable. |
| `com.aionemu.gameserver.model.templates.item.actions.CompositionAction` | `Aion.GameServer.Services.CompositionService` / `GameServerConnection.CompleteCompositeStonesAsync` | Item Action / Service / Connection Packet Caller | Partial | Unit + Regression Tested | Partial Parity | C# now covers Java-reachable same-stack double consume with count `2`: tool delete, first stone update, second stone delete/cube, success end animation. Java `canAct` level cap remains represented by existing validation but runtime capture is blocked. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseByItemId` | `CompositionService.DecreaseByItemId` / `CompositionConsumedItemMutation` | Storage Mutation Descriptor | Partial | Unit + Regression Tested | Partial Parity | Ordered descriptors preserve update then delete for the same object id. Final stale updated-consumed state is removed after delete while packet mutations retain Java order. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemPacket` / `sendItemDeletePacket` | `GameServerConnection.SendCompositionConsumedItemPacketsAsync` | Packet Fanout Helper | Partial | Regression Tested | Partial Parity | Connection test asserts same stone object receives full update with cleanup/seal flag `3`, then use-delete mask `0x17` and cube update. Runtime byte comparison remains blocked. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE_ITEM` use-delete type | `Aion.GameServer.Network.Aion.ServerPackets.SmDeleteItem.UseDeleteType` | Packet | Complete | Regression Tested | Partial Parity | Same-object second consume delete uses mask `0x17`. No Java runtime byte capture was available. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate.CubeSizeSnapshot` | Packet | Partial | Regression Tested | Partial Parity | Same-object test asserts projected cube count after tool delete and after stone delete. Expand snapshots and Java runtime bytes remain unverified. |
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` cleanup/seal flag | `SmInventoryUpdateItem` via composition remaining consumed update | Serialization Entry | Partial | Regression Tested | Partial Parity | Same-object first stone consume update carries cleanup/seal flag `3`. Broader temporary-exchange and runtime conditioning serializer gaps remain outside this unit. |
| `com.aionemu.gameserver.model.templates.item.actions.ApExtractAction` partial tool-consume failure edge | `Aion.GameServer.Services.ApExtractService` / AP extraction repository transaction | Item Action / Persistence Boundary | Refactored | Manual Only | Intentional Difference | Newly documented risk: Java's theoretical target-delete-success/tool-consume-failure edge is not reachable through normal same-client packet processing and C# keeps AP extraction atomic unless future Java runtime evidence shows the edge matters. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CompositionServiceTests.CreateMutationPlan_ConsumesSameStoneStackTwiceInJavaOrder` | Unit | `CompositionAction.run`, `Storage.decreaseByItemId` | Same item id consumed twice records tool delete, same-object update, same-object delete, clears stale updated-consumed state, and adds deterministic reward. | Deterministic C# service assertions from reviewed Java sequence. | Does not serialize packets; no Java runtime bytes. |
| `GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_CompositeStonesConsumesSameStoneStackTwiceInJavaOrder` | Regression / connection packet serialization | `CM_COMPOSITE_STONES`, `CompositionAction.run`, `Storage.decreaseByItemId`, `ItemPacketService` | Opcode `208` permits same first/second stone object id and sends tool delete/cube, same-object update, same-object delete/cube, and success end animation in Java order. | C# packet parsing against reviewed Java packet fanout order. | No Java runtime bytes; reward add is intentionally absent because fixture has no generated reward template for level-80 same-stack composition. |
| `GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_CompositeStonesWritesCleanupSealFlagsForRemainingConsumedInputs` | Regression / connection packet serialization | Same Java action, remaining-stack branch | Regression slice verifies deterministic fixture stones still allow all-remaining consumed updates with cleanup/seal flag `3`. | Deterministic C# packet assertions. | No runtime bytes. |
| `GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_CompositeStonesSendsConsumedPacketsInJavaOrderForMixedDeletes` | Regression / connection packet serialization | Same Java action and storage fanout | Regression slice verifies mixed delete/update/delete ordering remains Java-shaped after fixture stabilization. | Deterministic C# packet assertions. | No runtime bytes. |
| `GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_CompositeStonesKeepsEarlierConsumesWhenSecondStoneDisappearsBeforeCompletion` | Regression / connection packet serialization | Same Java action, no-rollback branch | Regression slice verifies disappearing-stone no-rollback behavior remains covered after fixture stabilization. | Deterministic C# packet assertions. | No runtime bytes. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- AP extraction target-delete-success/tool-consume-failure remains an intentional C# atomicity difference unless future Java runtime evidence shows the theoretical edge matters.
- Composition reward add/update packet ordering for a same-object double consume with an actual generated reward template is unit-tested but not connection-tested because the fixture intentionally omits generated reward templates to keep consumed packet assertions deterministic.
- Broader AP rank-change, legion contribution, and rank-limited side effects remain outside the composition unit.

## Summary Metrics

- Total Java artifacts discovered: 8 grouped artifact rows in this unit
- Total artifacts ported: 0 production artifacts changed; 1 service test and 1 connection test added
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: Java runtime artifact generation, same-object runtime byte comparison, reward-template connection variant, broader AP side effects
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: make an explicit AP extraction atomicity decision/audit note, or move to toy-pet source consume readiness.
- Why: the AP sidecar found Java's partial AP extraction failure edge is theoretical under normal same-client packet processing. If keeping C# atomic, document the intentional difference in the relevant audit/progress docs; otherwise design a fault-injection seam.
- Candidate files:
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/ApExtractAction.java`
  - `dotnetConversion/src/Aion.GameServer/Services/ApExtractService.cs`
  - `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
  - `docs/PHASE-6-PROGRESS.md`

## Alternative Sequential Task

- Task: toy-pet source consume readiness analysis.
- Why: previous handoffs continue to list toy-pet/kisk source consumption as a nearby scheduled/world-spawn packet family needing Java packet-order mapping.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | AP extraction atomicity documentation | AP extraction docs/audit plus read-only source review | Low | Mostly documentation unless a new test seam is chosen. |
| B | Toy-pet source consume readiness | Java/C# toy-pet/kisk sources, read-only | Medium | Independent future-work analysis. |
| C | Composition reward-template connection variant design | composition tests/static data, read-only first | Medium | Needs deterministic reward seam before adding packet assertions. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Explorer A | Analyze toy-pet source consume readiness | Java/C# toy-pet/kisk sources, read-only | all writes, docs, commits |
| Orchestrator | Document AP extraction atomicity decision if chosen | selected docs only unless tests are requested | unrelated files |

## Do Not Parallelize

- Shared item-use test fixture edits.
- Shared progress/handoff/audit docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1437] Cover composition same stack consume`.
- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_COMPOSITE_STONES.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/CompositionAction.java`
  - `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/ApExtractAction.java`
- C# files changed in UOW-1437:
  - `dotnetConversion/tests/Aion.GameServer.Tests/CompositionServiceTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AJM-Completion.md`
