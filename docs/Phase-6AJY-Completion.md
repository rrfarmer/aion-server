# Phase 6AJY Completion - Toy-Pet Registry Visibility Order

Date: 2026-05-27
Unit of Work: UOW-1449
Status: Complete after validation.

## Scope

Lock down the observable registry-backed toy-pet/kisk post-spawn sequence after direct no-registry packet order was covered in UOW-1448. This unit is test/documentation-only.

## Completed Work

- Added a combined operation-order hook to the local `GameServerConnectionFlightZoneFanoutTests` fixture.
- The hook records registry broadcasts, direct sent packets, and registry NPC visibility refreshes in one sequence without changing production code.
- Extended `CompleteToyPetSpawnUseItemAsync_RevalidatesCreaturePvpZoneCountersForSpawnedKisk` to assert: success animation broadcast, source delete, cube update, `RefreshNpcVisibilityAsync`, then bind question.
- Added assertions that the refreshed NPC and pending bind request both target the spawned kisk object id, with `SmQuestionWindow.RegisterBindstone`.
- Spawned and closed a read-only extraction RNG/drop audit sidecar.
- The sidecar found core extraction RNG/drop mapping is close, but highlighted missing focused coverage for all five stone thresholds, inventory-full-after-consumption behavior, missing reward-template behavior, and scheduled race behavior.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameServerConnectionFlightZoneFanoutTests.CompleteToyPetSpawnUseItemAsync_RevalidatesCreaturePvpZoneCountersForSpawnedKisk|FullyQualifiedName~GameServerConnectionFlightZoneFanoutTests.CompleteToyPetSpawnUseItemAsync|FullyQualifiedName~PlayerKiskSpawnServiceTests|FullyQualifiedName~PlayerKiskUpdateFanoutServiceTests"`.
- Result: passed 11 tests.

## Migration Parity Table - UOW-1449

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.ToyPetSpawnAction` | `GameServerConnection.CompleteToyPetSpawnUseItemAsync` | Item Action / Kisk Spawn | Partial | Regression Tested | Partial Parity | Registry-backed success path now asserts visible ordering through refresh and bind question. No production code changed. |
| `com.aionemu.gameserver.world.World.spawn` | `CapturingConnectionRegistry.RefreshNpcVisibilityAsync` / `GameWorld.TryAddObject` | World Visibility | Partial | Regression Tested | Needs Verification | Test observes NPC visibility refresh after source packets and before bind dialog. Java known-list internals remain broader than C# registry refresh. |
| `com.aionemu.gameserver.services.kisk.KiskService.regKisk` | `PlayerKiskRuntimeRegistry.RegisterKisk` | Runtime Registry | Partial | Regression Tested | Needs Verification | Test asserts runtime kisk state exists before pending bind request is inspected, but exact Java service internals remain unverified. |
| `com.aionemu.gameserver.controllers.KiskController.onDialogRequest` | `RequestOrBindPlayerToKiskAsync` / `PendingKiskBindRequest` | Dialog / Bind Request | Partial | Regression Tested | Partial Parity | Registry-backed multi-member kisk path now asserts bind question follows visibility refresh and targets the spawned kisk. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION` | `SmItemUsageAnimation` | Packet | Partial | Regression Tested | Partial Parity | Combined order hook asserts success animation broadcast remains first. Full Java byte comparison remains blocked. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE_ITEM` / `SM_CUBE_UPDATE` | `SmDeleteItem` / `SmCubeUpdate.CubeSizeSnapshot` | Inventory Packets | Partial | Regression Tested | Needs Verification | Combined order hook asserts source delete/cube precede registry visibility refresh. Java bytes remain unavailable. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW` | `SmQuestionWindow` | Packet | Partial | Regression Tested | Partial Parity | Bind dialog packet order and pending request metadata are asserted for registry-backed path. Full packet fields remain unverified. |
| `com.aionemu.gameserver.services.EnchantService.breakItem` | `EnchantService.CreateBreakItemPlan` | Service / Extraction Planner | Partial | Manual Only | Needs Verification | Read-only sidecar found C# mirrors Java threshold IDs and count ranges via injectable RNG, but threshold-table, inventory-full-after-consumption, missing-template, and scheduled race coverage remain thin. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CompleteToyPetSpawnUseItemAsync_RevalidatesCreaturePvpZoneCountersForSpawnedKisk` | Regression / packet and registry order | `ToyPetSpawnAction`, `World.spawn`, `KiskService.regKisk`, `KiskController.onDialogRequest` | Combined registry-backed sequence: success animation, source delete, cube update, NPC visibility refresh, bind question; refreshed NPC and pending bind request use the spawned kisk id. | Deterministic C# operation-order assertions from reviewed Java sequence. | Despawn scheduling remains unobserved; Java known-list internals and packet bytes are unavailable. |
| `CompleteToyPetSpawnUseItemAsync_*`, `PlayerKiskSpawnServiceTests`, `PlayerKiskUpdateFanoutServiceTests` | Existing Regression / Unit | Toy-pet/kisk helpers | Focused regression slice remained stable. | 11-test focused suite passed. | Does not prove Java `Kisk` object/controller parity. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Despawn scheduling order is still not directly asserted because the current test thread-pool hook does not expose scheduling metadata.
- C# still uses lightweight `WorldNpc` and `PlayerKiskRuntimeState`, not Java `Kisk`/controller/effect/known-list internals.
- Extraction RNG/drop sidecar found concrete follow-up coverage gaps: all five stone thresholds, inventory-full-after-consumption behavior, missing reward template behavior, and scheduled race behavior.
- Real-client scheduled item-use ordering remains unvalidated.

## Summary Metrics

- Total Java artifacts discovered: 8 grouped artifact rows in this unit
- Total artifacts ported: 0 production artifacts changed in this unit
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 8 grouped rows
- Total blocked artifacts: Java runtime artifact generation, despawn task ordering, Java `Kisk` object/controller model parity, extraction missing-template/race parity
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: add a small extraction edge parity test slice for threshold mapping and inventory-full behavior.
- If staying on toy-pet/kisk, proceed only if a small despawn-scheduling observability hook is available without production churn.
- Candidate extraction files:
  - `game-server/src/com/aionemu/gameserver/services/EnchantService.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/ExtractAction.java`
  - `dotnetConversion/src/Aion.GameServer/Services/EnchantService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/*Enchant*`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`

## Suggested Acceptance Criteria

- Add threshold-table tests for Alpha/Beta/Gamma/Delta/Epsilon selection from Java `EnchantService.breakItem`.
- Add one inventory-full extraction completion case that proves target/tool consumption plus dice inventory error behavior.
- Keep source/delete/reward packet order and RNG/drop count behavior tied directly to Java `EnchantService.breakItem`.
- If no safe extraction code change emerges, document the audit as a parity-risk UOW with a complete parity table and next recommendation.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Extraction RNG/drop audit follow-up | Java/C# enchant source and tests | Medium | Read-only first; code changes likely touch shared connection tests. |
| B | Toy-pet despawn scheduling observability | `GameServerConnectionFlightZoneFanoutTests.cs` | Medium | Only if a tiny fixture hook is enough. |
| C | Java artifact tooling note | docs/source read-only | Low | No runtime parity claims without generated artifacts. |

## Do Not Parallelize

- Shared `GameServerConnection.cs` item-use changes.
- Multiple edits to `GameServerConnectionFlightZoneFanoutTests.cs`.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1449] Cover toy-pet registry visibility order`.
- C# files changed in UOW-1449:
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFlightZoneFanoutTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AJY-Completion.md`
