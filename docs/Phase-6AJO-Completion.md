# Phase 6AJO Completion - Toy-Pet Source Consume Packets

Date: 2026-05-27
Unit of Work: UOW-1439
Status: Complete after validation.

## Scope

Align the delayed toy-pet/kisk source-consume packet edge with Java `ToyPetSpawnAction.act`. Java remains the source of truth; this unit does not claim runtime byte parity.

## Completed Work

- Audited Java `ToyPetSpawnAction.act`, `Storage.decreaseByObjectId`, and `ItemPacketService.sendItemDeletePacket`.
- Updated delayed missing-source completion to keep Java's already-broadcast success end animation (`end=1`, trailing value `1`) and skip spawning.
- Added `SM_CUBE_UPDATE` after exhausted toy-pet/kisk source delete.
- Added connection tests for exhausted-source delete/cube packet order and delayed missing-source success/no-spawn behavior.
- Spawned and closed two read-only explorers:
  - Toy-pet source consume readiness confirmed this unit's packet gaps.
  - Composition reward-seam design identified the next deterministic fixture strategy and a likely missing cube update after reward add.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameServerConnectionFlightZoneFanoutTests.CompleteToyPetSpawnUseItemAsync_DeletesLastSourceWithUseDeleteAndCubeUpdate|FullyQualifiedName~GameServerConnectionFlightZoneFanoutTests.CompleteToyPetSpawnUseItemAsync_MissingSourceKeepsJavaSuccessEndAndSkipsSpawn|FullyQualifiedName~GameServerConnectionFlightZoneFanoutTests.CompleteToyPetSpawnUseItemAsync_RevalidatesCreaturePvpZoneCountersForSpawnedKisk|FullyQualifiedName~GameServerConnectionFlightZoneFanoutTests.CompleteToyPetSpawnUseItemAsync_ClearsCreaturePvpZoneCountersWhenSourceMutationFailsAfterKiskSpawn|FullyQualifiedName~PlayerKiskSpawnServiceTests|FullyQualifiedName~PlayerKiskSpawnRestrictionServiceTests"`.
- Result: passed 8 tests.

## Migration Parity Table - UOW-1439

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.ToyPetSpawnAction` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleToyPetSpawnUseItemAsync` / `CompleteToyPetSpawnUseItemAsync` | Item Action / Connection Handler | Partial | Regression Tested | Partial Parity | C# now matches Java delayed missing-source success end animation and exhausted source delete/cube packet fanout. C# still stages kisk world insertion before persistence save and rolls it back on failure, while Java consumes source before spawning. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseByObjectId` | `Aion.GameServer.Services.PlayerKiskSpawnService.CreatePlan` plus toy-pet packet fanout | Storage Mutation Descriptor / Packet Caller | Partial | Unit + Regression Tested | Partial Parity | Single-count toy-pet source now sends use-delete mask `0x17` plus cube update. Remaining-stack source update remains covered by planner tests but connection packet cleanup/seal metadata is not broadened in this unit. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemDeletePacket` | `Aion.GameServer.Network.Aion.ServerPackets.SmDeleteItem` plus `SmCubeUpdate.CubeSizeSnapshot` | Packet Fanout Helper | Partial | Regression Tested | Partial Parity | Focused test asserts final success animation, source use-delete, and cube update order. Warehouse variants do not apply to cube-only kisk source items. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION` | `Aion.GameServer.Network.Aion.ServerPackets.SmItemUsageAnimation` | Packet | Partial | Regression Tested | Partial Parity | Missing-source delayed completion now keeps Java success end state `1` and trailing value `1`, then skips spawn. Cancel/abort path remains separate. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE_ITEM` use-delete type | `Aion.GameServer.Network.Aion.ServerPackets.SmDeleteItem.UseDeleteType` | Packet | Complete | Regression Tested | Partial Parity | Exhausted toy-pet source delete uses mask `0x17`. No Java runtime byte capture was available. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate.CubeSizeSnapshot` | Packet | Partial | Regression Tested | Partial Parity | Exhausted toy-pet source delete now sends projected cube count after source removal. Expand snapshots and Java runtime bytes remain unverified. |
| `com.aionemu.gameserver.services.KiskService.regKisk` / `VisibleObjectSpawner.spawnKisk` | `PlayerKiskSpawnService`, `PlayerKiskRegistry`, `GameServerConnection.RequestOrBindPlayerToKiskAsync` | Service / World Spawn / Registry | Partial | Unit + Regression Tested | Partial Parity | Existing spawn/zone tests still pass. Internal ordering differs: C# adds world object before source persistence and rolls back on save failure; Java consumes first then spawns. Packet-visible source consume branch was the scope of this unit. |
| `com.aionemu.gameserver.model.templates.item.actions.CompositionAction` reward add/update sidecar | `CompositionService` / future connection tests | Item Action / Future Test Dependency | Partial | No Tests in this unit | Needs Verification | Newly refined dependency: deterministic same-object reward connection tests need all possible level-20 reward templates or full seeded merge stacks. Current reward-add path may miss Java's cube update after new reward add. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionFlightZoneFanoutTests.CompleteToyPetSpawnUseItemAsync_DeletesLastSourceWithUseDeleteAndCubeUpdate` | Regression / connection packet serialization | `ToyPetSpawnAction.act`, `Storage.decreaseByObjectId`, `ItemPacketService.sendItemDeletePacket` | Delayed toy-pet completion sends success end animation, source use-delete mask `0x17`, cube update, removes source, and registers/spawns kisk. | C# packet parsing against reviewed Java delayed task order. | No Java runtime bytes; broader known-list fanout is not asserted in this focused packet prefix. |
| `GameServerConnectionFlightZoneFanoutTests.CompleteToyPetSpawnUseItemAsync_MissingSourceKeepsJavaSuccessEndAndSkipsSpawn` | Regression / connection packet serialization | `ToyPetSpawnAction.act` delayed success-before-consume order | Missing source at delayed completion sends Java success end animation and skips kisk spawn/source packets. | Deterministic C# assertion from reviewed Java branch. | No Java runtime bytes; edge reachability depends on external source mutation during the scheduled window. |
| `GameServerConnectionFlightZoneFanoutTests.CompleteToyPetSpawnUseItemAsync_RevalidatesCreaturePvpZoneCountersForSpawnedKisk` | Existing Regression | `VisibleObjectSpawner.spawnKisk` / world zone revalidation | Regression slice confirms normal kisk spawn/zone registration still works. | Existing C# runtime assertions. | No Java runtime bytes. |
| `GameServerConnectionFlightZoneFanoutTests.CompleteToyPetSpawnUseItemAsync_ClearsCreaturePvpZoneCountersWhenSourceMutationFailsAfterKiskSpawn` | Existing Regression | C# rollback policy around Java spawn/source ordering difference | Regression slice confirms C# rollback cleanup still works after packet change. | Existing C# runtime assertions. | Documents intentional internal ordering difference rather than Java parity. |
| `PlayerKiskSpawnServiceTests` / `PlayerKiskSpawnRestrictionServiceTests` | Unit | `ToyPetSpawnAction.canAct`, delayed spawn position/heading | Regression slice verifies guard ordering and spawn plan remain stable. | Deterministic C# service assertions from Java-reviewed behavior. | Does not serialize packets or run Java. |

## Remaining Risks

- C# still adds the kisk world object before source persistence succeeds and rolls it back on failure; Java consumes source first and only then spawns. Packet-visible success path is closer, but internal ordering remains an intentional safety/rollback boundary needing broader documentation if exposed.
- Remaining-stack toy-pet source update connection serialization is not newly covered with cleanup/seal metadata in this unit.
- Full scheduled `HandleToyPetSpawnUseItemAsync` 10-second path and real-client broadcast visibility remain broader readiness items.
- Same-object composition reward add/update connection coverage remains pending; sidecar found a deterministic fixture strategy and a likely missing cube update after reward add.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.

## Summary Metrics

- Total Java artifacts discovered: 8 grouped artifact rows in this unit
- Total artifacts ported: 2 toy-pet delayed completion packet branches changed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 8 grouped rows
- Total blocked artifacts: Java runtime artifact generation, toy-pet internal spawn/persistence ordering, scheduled real-client validation, composition reward-add cube update, broader kisk known-list fanout
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: implement or test the deterministic composition same-object reward connection seam.
- Why: UOW-1437 covered consumed packet order without a generated reward template. The UOW-1439 sidecar found a safe fixture strategy for reward update coverage and exposed a likely missing cube update after reward add.
- Candidate files:
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/CompositionAction.java`
  - `game-server/src/com/aionemu/gameserver/services/item/ItemService.java`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/CompositionServiceTests.cs`

## Alternative Sequential Task

- Task: document toy-pet internal spawn/persistence ordering as an intentional C# safety difference.
- Why: C# currently adds the kisk world object before persistence and rolls back if save fails, while Java consumes source first and then spawns. Packet-visible behavior is closer after UOW-1439, but internal ordering remains different.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Composition reward-update connection test design | composition tests/static data, read-only first | Medium | Seed all possible level-20 reward stacks to avoid random add/update ambiguity. |
| B | Composition reward-add cube update audit | Java `ItemService.addItem` and C# reward packet send path, read-only | Medium | Sidecar suggests C# may miss Java cube update after new reward add. |
| C | Toy-pet internal ordering audit | toy-pet/kisk docs and source, read-only | Low | Documentation-only candidate if not changing behavior. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Explorer A | Audit composition reward-add cube update behavior | Java item service/storage and C# composition reward send path, read-only | all writes, docs, commits |
| Orchestrator | Add deterministic reward-update connection coverage if selected | selected composition tests only unless bug exposed | shared docs until validation; unrelated files |

## Do Not Parallelize

- Shared item-use test fixture edits.
- `GameServerConnection.cs` item-use packet fanout edits.
- Shared progress/handoff/audit docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1439] Align toy-pet source consume packets`.
- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/ToyPetSpawnAction.java`
  - `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
  - `game-server/src/com/aionemu/gameserver/services/KiskService.java`
  - `game-server/src/com/aionemu/gameserver/spawnengine/VisibleObjectSpawner.java`
- C# files changed in UOW-1439:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFlightZoneFanoutTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AJO-Completion.md`
