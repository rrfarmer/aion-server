# Phase 6AKI Completion - Kisk Despawn Task Ownership

Date: 2026-05-27
Unit of Work: UOW-1459
Status: Complete after validation.

## Scope

Model Java `TaskId.DESPAWN` ownership for toy-pet kisk lifetime tasks without porting the full `CreatureController` task map.

## Completed Work

- Audited Java `ToyPetSpawnAction.act`, `Kisk.KiskLifeTask`, `CreatureController.addTask(TaskId.DESPAWN)`, `CreatureController.onDelete`, `VisibleObjectController.delete`, and `KiskService.removeKisk`.
- Added stored despawn task ownership to `PlayerKiskRuntimeState`.
- Added Java-style replacement cancellation for an existing kisk despawn task.
- Updated `GameServerConnection.ScheduleKiskLifetimeDespawn` to store the scheduled task on the runtime kisk state.
- Updated `RemoveRuntimeKiskAsync` and `PlayerKiskLifetimeService.DespawnExpiredKisk` so non-scheduled removal paths cancel the stored despawn task.
- Preserved scheduled callback behavior by allowing `RunKiskLifetimeDespawnAsync` to skip cancelling its own task token.

## Validation

- First focused run exposed one old `PlayerKiskDespawnResult.Removed` helper call; updated it with explicit `cancelledDespawnTask: false`.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~PlayerKiskLifetimeServiceTests|FullyQualifiedName~PlayerKiskRemovalCleanupServiceTests|FullyQualifiedName~GameServerConnectionFlightZoneFanoutTests"`.
- Result: passed 28 tests.
- Ran `git diff --check`.
- Result: only CRLF conversion warnings for touched files.

## Migration Parity Table - UOW-1459

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.ToyPetSpawnAction` | `GameServerConnection.ScheduleKiskLifetimeDespawn` | Item Action / Scheduler Caller | Partial | Regression Tested | Partial Parity | C# now stores the scheduled lifetime task after toy-pet kisk spawn, matching Java's `kisk.getController().addTask(TaskId.DESPAWN, task)` ownership intent. Initial item-use observer lifecycle remains broader. |
| `com.aionemu.gameserver.model.gameobjects.Kisk` | `PlayerKiskRuntimeState` | Runtime State | Partial | Unit Tested | Partial Parity | Runtime state now owns a scheduled despawn task reference and exposes replacement/cancel behavior. Java `Kisk` still has richer controller/object lifecycle, known-list, and death state behavior. |
| `com.aionemu.gameserver.controllers.CreatureController` | `PlayerKiskRuntimeState.SetDespawnTask` / `CancelDespawnTask` | Controller / Task Ownership | Partial | Unit Tested | Partial Parity | Models `addTask(TaskId.DESPAWN)` replacement cancellation and `onDelete -> cancelAllTasks` for the kisk despawn task only. Other controller task ids and warning/log behavior remain outside this unit. |
| `com.aionemu.gameserver.controllers.VisibleObjectController` | `PlayerKiskLifetimeService.DespawnExpiredKisk` | Controller / World Removal | Partial | Unit Tested | Partial Parity | C# removal still uses world/registry services rather than a full controller hierarchy, but non-scheduled removal now cancels the stored despawn task before world cleanup. |
| `com.aionemu.gameserver.services.KiskService` | `PlayerKiskLifetimeService` / `PlayerKiskRemovalRuntimeCleanupService` | Service / Cleanup | Partial | Regression Tested | Partial Parity | Existing removal cleanup stayed stable. New `CancelledDespawnTask` metadata records Java-style task cancellation for removal paths. Exact Java online/offline world interactions remain partially modeled. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `DespawnExpiredKiskRemovesRegistryWorldObjectReleasesIdAndCancelsDespawnTask` | Unit / service | `Kisk.KiskLifeTask`, `CreatureController.onDelete`, `KiskService.removeKisk` | Non-scheduled C# kisk removal removes registry/world/id state and cancels the stored despawn task. | Deterministic C# assertion from reviewed Java task ownership flow. | Does not compare Java runtime output or full controller deletion chain. |
| `DespawnExpiredKiskCanSkipCancellingCurrentScheduledTask` | Unit / service | `Kisk.KiskLifeTask.run -> controller.delete` | Scheduled callback path can remove the kisk without cancelling its own task token. | Deterministic C# assertion preserving Java `Future.cancel(false)` non-interruption intent. | C# skip flag is an implementation seam; Java does not need it because `cancel(false)` does not interrupt a running task. |
| `SetDespawnTaskReplacesAndCancelsPreviousTaskLikeJavaControllerAddTask` | Unit / runtime state | `CreatureController.addTask(TaskId.DESPAWN)` | Setting a second despawn task cancels/replaces the previous task. | Deterministic C# assertion from reviewed Java replacement behavior. | Does not assert Java warning log message. |
| Existing `PlayerKiskRemovalCleanupServiceTests` | Existing Unit | `KiskService.removeKisk` | Synthetic removal-plan tests compile with explicit cancellation metadata and remain stable. | Focused test suite passed. | Cleanup recipient/packet byte details remain broader. |
| Existing `GameServerConnectionFlightZoneFanoutTests` | Existing Regression | `ToyPetSpawnAction`, kisk spawn/removal, zone fanout | Toy-pet/kisk workflow and schedule observability remained stable. | Focused suite passed. | Full Java delayed task lifecycle remains partial. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- C# models only the kisk DESPAWN ownership slice, not the full Java `CreatureController` task map.
- Threading differs intentionally at the scheduled callback boundary: Java `Future.cancel(false)` does not interrupt a running task; C# uses `cancelScheduledDespawnTask: false` to avoid cancelling the current callback token.
- Java warning log for replacing a DESPAWN task is not ported.
- Date/time behavior remains second-based via `DateTimeOffset.UtcNow` and existing lifetime tests; no Java runtime wall-clock comparison exists.
- Serialization behavior was not changed in this unit.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 kisk DESPAWN task ownership slice
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime artifact generation, full Java controller task map, Java runtime delayed-task comparison
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: continue kisk lifecycle with socket-order hardening for kisk removal/visibility refresh.
- Alternative: start a dedicated player-owned aggro model design UOW to unblock revive aggro cleanup.

## Suggested Acceptance Criteria

- If continuing kisk removal, inspect Java `Kisk.resurrectionUsed`, `KiskController.delete`, `World.removeObject`, and `KiskService.removeKisk` ordering before adding tests.
- Keep shared kisk revive and flight-zone fixtures sequential.
- If designing player aggro, keep it separate from revive live wiring.
- Update the Migration Parity Table for every Java artifact touched.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Kisk removal socket-order audit/test | kisk workflow or flight-zone tests | Medium | Sequential if touching shared fixtures. |
| B | Player aggro model design | docs/model design, future service skeleton | High | Do not combine with revive live wiring. |
| C | Read-only Java controller task-map audit | Java controller files only | Low | Safe read-only sub-agent candidate if tools are available. |

## Do Not Parallelize

- Shared `GameServerConnection.cs` kisk removal/revive edits.
- Shared kisk workflow or flight-zone fixtures.
- Shared kisk runtime state files.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1459] Track kisk despawn task ownership`.
- Files changed in UOW-1459:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKiskLifetimeService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKiskRegistry.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerKiskLifetimeServiceTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerKiskRemovalCleanupServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AKI-Completion.md`
