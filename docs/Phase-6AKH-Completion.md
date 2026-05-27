# Phase 6AKH Completion - Toy-Pet Kisk Lifetime Schedule Observability

Date: 2026-05-27
Unit of Work: UOW-1458
Status: Complete after validation.

## Scope

Add a narrow observability seam for scheduled tasks and use it to verify toy-pet kisk spawn schedules despawn using Java's remaining-lifetime behavior.

## Completed Work

- Added optional `ThreadPoolManager` schedule observations for one-shot and fixed-rate schedules.
- Added `ThreadPoolScheduleObservation` and `ThreadPoolScheduleKind`.
- Added `CompleteToyPetSpawnUseItemAsync_SchedulesKiskDespawnForRemainingLifetime`.
- Extended the flight-zone/kisk workflow test fixture so tests can inject a `ThreadPoolManager`.
- Kept live scheduling, task execution, and kisk despawn logic unchanged.

## Validation

- First focused run failed because the test assumed a concrete kisk object id from `IDFactory([1])`; corrected the assertion to resolve the kisk by owner.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameServerConnectionFlightZoneFanoutTests"`.
- Result: passed 22 tests.
- Ran `git diff --check`.
- Result: only CRLF conversion warnings for touched files.

## Migration Parity Table - UOW-1458

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.ToyPetSpawnAction` | `GameServerConnection.CompleteToyPetSpawnUseItemAsync` | Item Action / Workflow | Partial | Regression Tested | Partial Parity | Test now observes the delayed kisk despawn schedule after toy-pet kisk spawn. The initial 10-second item-use task and observer abort path remain covered elsewhere and not byte-compared to Java in this unit. |
| `com.aionemu.gameserver.model.gameobjects.Kisk` | `PlayerKiskRuntimeState.GetRemainingLifetimeSeconds` | Runtime State | Partial | Regression Tested | Partial Parity | Test asserts scheduled delay is near the Java 7200-second kisk lifetime and matches the registered owner kisk's remaining lifetime. Exact Java wall-clock comparison remains unverified. |
| `com.aionemu.gameserver.model.gameobjects.Kisk.KiskLifeTask` | `GameServerConnection.ScheduleKiskLifetimeDespawn` / `RunKiskLifetimeDespawnAsync` | Scheduled Task | Partial | Regression Tested | Partial Parity | Schedule delay is now observable. Actual delayed task execution/removal is covered by direct `RemoveRuntimeKiskAsync` tests, not by waiting for a real timer. |
| `com.aionemu.gameserver.utils.ThreadPoolManager` | `ThreadPoolManager` / `ThreadPoolScheduleObservation` | Utility / Scheduler | Partial | Regression Tested | Partial Parity | Optional observer records delay/period metadata for tests. Scheduling behavior is unchanged; Java `Future` identity, executor pool semantics, and task ownership remain broader. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CompleteToyPetSpawnUseItemAsync_SchedulesKiskDespawnForRemainingLifetime` | Regression / workflow | `ToyPetSpawnAction.act`, `Kisk.getRemainingLifetime`, `ThreadPoolManager.schedule` | Successful toy-pet kisk spawn registers the owner kisk and schedules one delayed despawn using remaining lifetime metadata. | Deterministic C# schedule observation and kisk runtime assertions from reviewed Java order. | Does not wait for real 7200-second timer or compare Java runtime artifacts. |
| Existing `GameServerConnectionFlightZoneFanoutTests` | Existing Regression | Kisk spawn/removal and creature PVP zone fanout | Existing kisk spawn, failure cleanup, object visibility, and zone counter tests remained stable. | 22-test focused suite passed. | Full Java region visibility and delayed task lifecycle remain partial. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- The new scheduler observer is C# test instrumentation, not a Java behavior.
- Actual delayed despawn execution is not proven by waiting for real kisk lifetime; direct removal tests cover the cleanup path.
- Java `Future` task ownership under `TaskId.DESPAWN` is not modeled on C# kisk runtime state.
- No new serialization behavior was introduced; date/time handling uses C# `DateTimeOffset.UtcNow` with a tolerance around Java's second-based remaining lifetime.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 1 scheduler observability seam plus 1 regression test
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: Java runtime artifact generation, Java `Future`/TaskId ownership, long-delay real timer execution
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: continue kisk lifecycle with a read-only audit of Java `KiskLifeTask`/`KiskController.delete` task ownership versus C# `RunKiskLifetimeDespawnAsync`.
- Alternative: switch to player-owned aggro model design if the next session wants to unblock revive aggro cleanup.

## Suggested Acceptance Criteria

- If continuing kisk lifetime, identify Java `TaskId.DESPAWN` ownership and cancellation/removal behavior before editing production code.
- If adding tests, keep them deterministic; do not wait for a 7200-second timer.
- If designing player aggro, keep it separate from revive live wiring.
- Update the Migration Parity Table for every Java artifact touched.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Kisk lifetime task ownership audit | Java kisk/controller files, C# kisk runtime docs/tests | Medium | Good follow-up to schedule observability. |
| B | Player aggro model design | Java/C# model docs, future service skeleton | High | Separate prerequisite for revive aggro cleanup. |
| C | Kisk revive socket-order hardening | kisk workflow tests | Medium | Sequential only if touching shared fixture again. |

## Do Not Parallelize

- Shared scheduler utility edits.
- Shared kisk/flight-zone workflow fixture edits.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1458] Observe toy-pet kisk lifetime schedule`.
- Files changed in UOW-1458:
  - `dotnetConversion/src/Aion.GameServer/Utils/ThreadPoolManager.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFlightZoneFanoutTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AKH-Completion.md`
