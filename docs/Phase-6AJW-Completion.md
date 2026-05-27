# Phase 6AJW Completion - Toy-Pet Kisk Success Ordering

Date: 2026-05-27
Unit of Work: UOW-1447
Status: Complete after validation.

## Scope

Align the source-present toy-pet/kisk completion path with Java `ToyPetSpawnAction` visible success ordering. Java broadcasts the item-use success end animation before source consumption, kisk spawn, kisk registration, dialog, or bind side effects.

## Completed Work

- Reviewed Java `ToyPetSpawnAction`: delayed completion broadcasts `SM_ITEM_USAGE_ANIMATION(..., 0, 1, 1)`, removes the observer, calls `inventory.decreaseByObjectId`, spawns the kisk, schedules despawn, registers it through `KiskService.regKisk`, then opens bind dialog or binds the player.
- Updated `CompleteToyPetSpawnUseItemAsync` so the source-present path broadcasts the success end animation before source persistence, world insertion cleanup decisions, source packets, runtime registry, NPC visibility, despawn scheduling, and bind side effects.
- Preserved C# rollback semantics after the Java-visible success animation: persistence failure removes provisional world state, clears zone counters, releases the object id, preserves source inventory, skips runtime kisk registration, and skips visibility refresh.
- Tightened persistence-failure tests to assert the success animation is emitted even when the C# persistence boundary rolls back the provisional spawn.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameServerConnectionFlightZoneFanoutTests.CompleteToyPetSpawnUseItemAsync|FullyQualifiedName~PlayerKiskSpawnServiceTests|FullyQualifiedName~PlayerKiskSpawnRestrictionServiceTests|FullyQualifiedName~PlayerKiskUpdateFanoutServiceTests"`.
- Result: passed 13 tests.

## Migration Parity Table - UOW-1447

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.ToyPetSpawnAction` | `GameServerConnection.HandleToyPetSpawnUseItemAsync` / `CompleteToyPetSpawnUseItemAsync` | Item Action / Kisk Spawn | Partial | Regression Tested | Partial Parity | Source-present completion now broadcasts success before consume/spawn/register side effects, matching Java visible ordering. C# still has transaction-first persistence and rollback around provisional world object. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseByObjectId` | `PlayerKiskSpawnService.CreatePlan` / `SaveItemUseSourceMutationAsync` / source packet send in `CompleteToyPetSpawnUseItemAsync` | Storage Source Mutation | Partial | Regression Tested | Needs Verification | Source delete/decrease still happens after C# persistence. Java mutates immediately after success animation. Tests cover last-source delete packet and persistence-failure no runtime mutation. |
| `com.aionemu.gameserver.utils.VisibleObjectSpawner.spawnKisk` | `PlayerKiskSpawnService.CreatePlan` plus `GameWorld.TryAddObject` | Spawn Helper | Partial | Regression Tested | Needs Verification | C# uses `WorldNpc` rather than Java `Kisk`; position/heading/source decrement and world insertion are covered, but Java `Kisk` object/controller construction is not 1:1. |
| `com.aionemu.gameserver.spawnengine.SpawnEngine.bringIntoWorld` | `GameWorld.TryAddObject` plus `RevalidateKiskCreaturePvpZones` | World Spawn | Partial | Regression Tested | Needs Verification | C# explicitly revalidates creature PvP/siege counters after world add. Java world spawn/controller/known-list ordering remains broader than the current C# model. |
| `com.aionemu.gameserver.world.World.spawn` | `GameWorld.TryAddObject`, connection registry NPC visibility refresh | World Visibility | Partial | Regression Tested | Needs Verification | Success path still refreshes NPC visibility after runtime registration; failure path asserts no refresh. Exact Java known-list/update ordering remains unverified. |
| `com.aionemu.gameserver.services.kisk.KiskService.regKisk` | `PlayerKiskRuntimeRegistry.RegisterKisk` | Runtime Registry | Partial | Regression Tested | Needs Verification | Registration still happens only after persistence succeeds; Java registers after spawn without C# DB rollback boundary. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION` | `SmItemUsageAnimation` | Packet | Partial | Regression Tested | Partial Parity | Tests assert success end packet `(0, 1, 1)` on missing-source, source-present success, and persistence-failure rollback paths. Full runtime byte comparison remains blocked. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE_ITEM` / `SM_CUBE_UPDATE` | `SmDeleteItem` / `SmCubeUpdate.CubeSizeSnapshot` | Inventory Packets | Partial | Regression Tested | Needs Verification | Last-source delete still sends use-delete plus cube after successful persistence. Java packet bytes remain unavailable. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CompleteToyPetSpawnUseItemAsync_ClearsCreaturePvpZoneCountersWhenSourceMutationFailsAfterKiskSpawn` | Regression / rollback packet order | `ToyPetSpawnAction` delayed task | Persistence-failure rollback still emits Java success animation, cleans provisional world/zone state, releases id, preserves source, and skips registry/visibility. | Deterministic C# packet and state assertions against reviewed Java visible ordering. | Java has no C# DB persistence boundary; rollback cleanup is C#-specific. |
| `CompleteToyPetSpawnUseItemAsync_ClearsCreatureSiegeZoneCountersWhenSourceMutationFailsAfterKiskSpawn` | Regression / rollback packet order | Same Java delayed task | Same as above for siege-zone counter cleanup. | Deterministic C# packet and state assertions. | Java runtime world/zone behavior is broader than C# counter service. |
| `CompleteToyPetSpawnUseItemAsync_DeletesLastSourceWithUseDeleteAndCubeUpdate` | Existing Regression | `ToyPetSpawnAction`, `Storage.decreaseByObjectId` | Success path still starts with success animation, then source delete/cube, runtime kisk registration, and world object presence. | Existing deterministic C# packet assertions. | No Java runtime bytes. |
| `CompleteToyPetSpawnUseItemAsync_MissingSourceKeepsJavaSuccessEndAndSkipsSpawn` | Existing Regression | Java success-before-decrease behavior | Missing source still emits success end and skips spawn/registry. | Existing deterministic C# packet assertions. | Java source missing race not runtime-captured. |
| `CompleteToyPetSpawnUseItemAsync_RevalidatesCreaturePvpZoneCountersForSpawnedKisk`, `PlayerKiskSpawnServiceTests`, `PlayerKiskSpawnRestrictionServiceTests`, `PlayerKiskUpdateFanoutServiceTests` | Existing Regression / Unit | Toy-pet/kisk spawn and fanout helpers | Regression slice verifies spawn planning, restrictions, counters, and fanout remained stable. | 13-test focused suite passed. | Does not prove Java `Kisk` object/controller parity. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- C# toy-pet spawn still persists source mutation transaction-first after the Java-visible success animation; failure/rollback behavior is C#-specific and should stay explicitly documented.
- C# uses lightweight `WorldNpc` plus `PlayerKiskRuntimeState` instead of Java `Kisk` with controller/effect/known-list internals.
- Exact Java known-list, dialog, bind, and despawn task ordering remains broader than this unit.
- Real-client scheduled item-use ordering remains unvalidated across toy-pet spawn and other item-use actions.

## Summary Metrics

- Total Java artifacts discovered: 8 grouped artifact rows in this unit
- Total artifacts ported: 1 toy-pet source-present success-order branch changed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 8 grouped rows
- Total blocked artifacts: Java runtime artifact generation, Java `Kisk` object/controller model parity, known-list/dialog/despawn ordering, real-client scheduled ordering
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: continue toy-pet/kisk parity by auditing and testing post-spawn registry/visibility/bind/despawn ordering.
- Java sequence to preserve as breadcrumbs: `VisibleObjectSpawner.spawnKisk -> SpawnEngine.bringIntoWorld -> World.spawn -> schedule despawn -> cancel item-use task -> KiskService.regKisk -> onDialogRequest/onBind`.
- Candidate files:
  - `game-server/src/com/aionemu/gameserver/utils/VisibleObjectSpawner.java`
  - `game-server/src/com/aionemu/gameserver/spawnengine/SpawnEngine.java`
  - `game-server/src/com/aionemu/gameserver/world/World.java`
  - `game-server/src/com/aionemu/gameserver/services/kisk/KiskService.java`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKiskSpawnService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFlightZoneFanoutTests.cs`
  - docs/progress/handoff docs

## Suggested Acceptance Criteria

- Add a narrow regression for the order of runtime registration, NPC visibility refresh/direct `SmNpcInfo`, despawn scheduling, and bind dialog/bind side effects where current test hooks make this observable.
- Explicitly document any remaining intentional difference caused by C#'s lightweight `WorldNpc`/`PlayerKiskRuntimeState` model.
- Keep Java source breadcrumbs in comments or docs for `VisibleObjectSpawner`, `SpawnEngine`, `World.spawn`, and `KiskService`.
- Do not broaden into a full Java `Kisk` object/controller port unless the required supporting runtime surfaces are already present.

## Follow-Up Sequential Tasks

- Generate Java runtime packet artifacts for toy-pet spawn when Java 25/Maven tooling is available.
- Real-client validate scheduled item-use ordering for toy-pet spawn and the other item-use actions already adjusted in Phase 6.
- Continue broader extraction RNG/drop and enchant-service parity after item-use ordering gaps are closed.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Toy-pet post-spawn ordering tests | `GameServerConnection.cs`, `GameServerConnectionFlightZoneFanoutTests.cs` | Medium | Keep one owner for handler/test changes. |
| B | Extraction RNG/drop audit | Java/C# enchant service sources | Medium | Read-only sidecar is safe and independent. |
| C | Java artifact tooling note | docs/source read-only | Low | Useful only if tooling paths changed; no runtime parity claims without generated artifacts. |

## Do Not Parallelize

- Shared `GameServerConnection.cs` toy-pet changes.
- Multiple edits to `GameServerConnectionFlightZoneFanoutTests.cs`.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1447] Align toy-pet success order`.
- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/ToyPetSpawnAction.java`
  - `game-server/src/com/aionemu/gameserver/utils/VisibleObjectSpawner.java`
  - `game-server/src/com/aionemu/gameserver/spawnengine/SpawnEngine.java`
  - `game-server/src/com/aionemu/gameserver/world/World.java`
  - `game-server/src/com/aionemu/gameserver/services/kisk/KiskService.java`
- C# files changed in UOW-1447:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFlightZoneFanoutTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AJW-Completion.md`
