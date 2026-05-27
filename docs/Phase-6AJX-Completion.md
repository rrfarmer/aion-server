# Phase 6AJX Completion - Toy-Pet Direct Post-Spawn Order

Date: 2026-05-27
Unit of Work: UOW-1448
Status: Complete after validation.

## Scope

Lock down the observable direct no-registry toy-pet/kisk post-spawn sequence after UOW-1447 aligned the success animation. This unit is test/documentation-only.

## Completed Work

- Reviewed the C# direct-send path after a successful kisk spawn: source delete/cube packets are sent, runtime kisk state is registered, direct `SmNpcInfo` is sent when no connection registry is available, despawn is scheduled, then the multi-member bind dialog is requested.
- Updated `CompleteToyPetSpawnUseItemAsync_DeletesLastSourceWithUseDeleteAndCubeUpdate` to assert the full packet sequence: success animation, source delete, cube update, `SmNpcInfo`, `SmQuestionWindow`.
- Added assertions that `PendingKiskBindRequest` is populated with the spawned kisk object id and `SmQuestionWindow.RegisterBindstone`.
- No production code changed.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameServerConnectionFlightZoneFanoutTests.CompleteToyPetSpawnUseItemAsync_DeletesLastSourceWithUseDeleteAndCubeUpdate|FullyQualifiedName~GameServerConnectionFlightZoneFanoutTests.CompleteToyPetSpawnUseItemAsync|FullyQualifiedName~PlayerKiskSpawnServiceTests|FullyQualifiedName~PlayerKiskUpdateFanoutServiceTests"`.
- Result: passed 11 tests.

## Migration Parity Table - UOW-1448

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.ToyPetSpawnAction` | `GameServerConnection.CompleteToyPetSpawnUseItemAsync` | Item Action / Kisk Spawn | Partial | Regression Tested | Partial Parity | Direct no-registry success path now asserts the full visible order through NPC info and bind dialog. No production code changed. |
| `com.aionemu.gameserver.utils.VisibleObjectSpawner.spawnKisk` | `PlayerKiskSpawnService.CreatePlan` / `GameWorld.TryAddObject` | Spawn Helper | Partial | Regression Tested | Needs Verification | Test observes downstream direct `SmNpcInfo` after source consume packets. Java `Kisk` object construction remains not 1:1. |
| `com.aionemu.gameserver.spawnengine.SpawnEngine.bringIntoWorld` | `GameWorld.TryAddObject` | World Spawn | Partial | Regression Tested | Needs Verification | World object presence is asserted before runtime/bind checks, but exact Java `bringIntoWorld` internals remain broader than C# model. |
| `com.aionemu.gameserver.world.World.spawn` | `SmNpcInfo` direct send / connection registry visibility refresh | World Visibility | Partial | Regression Tested | Needs Verification | Direct no-registry path asserts `SmNpcInfo` precedes bind question. Registry-backed visibility ordering still needs richer instrumentation. |
| `com.aionemu.gameserver.services.kisk.KiskService.regKisk` | `PlayerKiskRuntimeRegistry.RegisterKisk` | Runtime Registry | Partial | Regression Tested | Needs Verification | Test asserts runtime kisk state exists and pending bind request references the spawned object id. Exact Java service internals remain unverified. |
| `com.aionemu.gameserver.controllers.KiskController.onDialogRequest` | `RequestOrBindPlayerToKiskAsync` / `PendingKiskBindRequest` | Dialog / Bind Request | Partial | Regression Tested | Partial Parity | Multi-member kisk direct path now asserts `SmQuestionWindow.RegisterBindstone` after NPC info and pending request metadata. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_NPC_INFO` | `SmNpcInfo` | Packet | Partial | Regression Tested | Needs Verification | Packet class order is asserted, but Java bytes and known-list fanout remain unavailable. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW` | `SmQuestionWindow` | Packet | Partial | Regression Tested | Partial Parity | Bind dialog packet class and pending request id are asserted for multi-member kisk path. Full packet fields are not byte-compared to Java. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CompleteToyPetSpawnUseItemAsync_DeletesLastSourceWithUseDeleteAndCubeUpdate` | Regression / packet order | `ToyPetSpawnAction`, `VisibleObjectSpawner.spawnKisk`, `KiskService.regKisk`, `KiskController.onDialogRequest` | Full direct-send sequence: success animation, source delete, cube update, NPC info, bind question; pending bind request uses kisk object id and register-bindstone question id. | Deterministic C# packet order and state assertions from reviewed Java sequence. | Direct path only; registry-backed known-list order still needs richer hooks. |
| `CompleteToyPetSpawnUseItemAsync_*`, `PlayerKiskSpawnServiceTests`, `PlayerKiskUpdateFanoutServiceTests` | Existing Regression / Unit | Toy-pet/kisk helpers | Focused regression slice remained stable. | 11-test focused suite passed. | Does not prove Java `Kisk` object/controller parity. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Registry-backed NPC visibility order is not fully observable with the current `CapturingConnectionRegistry`; a richer ordering hook may be needed.
- C# still uses lightweight `WorldNpc` and `PlayerKiskRuntimeState`, not Java `Kisk`/controller/effect/known-list internals.
- Despawn scheduling order is not directly asserted because the test thread-pool hook does not expose task order metadata here.
- Real-client scheduled item-use ordering remains unvalidated.

## Summary Metrics

- Total Java artifacts discovered: 8 grouped artifact rows in this unit
- Total artifacts ported: 0 production artifacts changed in this unit
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 8 grouped rows
- Total blocked artifacts: Java runtime artifact generation, registry-backed known-list ordering, despawn task ordering, Java `Kisk` object/controller model parity
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: continue toy-pet/kisk parity with registry-backed visibility/bind/despawn observability if a narrow test hook is available; otherwise pivot to the safe read-only extraction RNG/drop audit.
- Candidate files for registry observability:
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFlightZoneFanoutTests.cs`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `game-server/src/com/aionemu/gameserver/world/World.java`
  - `game-server/src/com/aionemu/gameserver/services/kisk/KiskService.java`

## Suggested Acceptance Criteria

- If staying on toy-pet/kisk, add the smallest possible ordering hook/test for registry-backed `RefreshNpcVisibilityAsync` relative to bind dialog or scheduled despawn.
- If pivoting to extraction RNG/drop audit, keep it read-only first and produce a parity risk table before changing code.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Registry-backed toy-pet visibility hook | `GameServerConnectionFlightZoneFanoutTests.cs`, maybe test doubles only | Medium | Keep one owner if editing shared test doubles. |
| B | Extraction RNG/drop audit | Java/C# enchant service sources | Medium | Read-only sidecar is safe and independent. |
| C | Java artifact tooling note | docs/source read-only | Low | No runtime parity claims without generated artifacts. |

## Do Not Parallelize

- Shared `GameServerConnection.cs` toy-pet changes.
- Multiple edits to `GameServerConnectionFlightZoneFanoutTests.cs`.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1448] Cover toy-pet direct post-spawn order`.
- C# files changed in UOW-1448:
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFlightZoneFanoutTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AJX-Completion.md`
