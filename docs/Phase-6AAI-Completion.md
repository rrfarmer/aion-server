# Phase 6AAI Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1199
Status: Phase 6 continues; bind-point teleport cancel/onLogin control intent is staged, but live cooldown/task state, fanout, scheduling, Kinah mutation, and movement remain incomplete.

## Session Summary

UOW-1199 modeled Java `BindPointTeleportService.cancelTeleport` and `onLogin` as pure control planners using the concrete `SmBindPointTeleport` packet from UOW-1198. The planners return task/broadcast intent only and do not touch live task maps, cooldown maps, or connection registries.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportControlPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportControlPlanServiceTests.cs`
- `docs/Phase-6-PricesService-Consumer-Map.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AAI-Completion.md`

Related recent units:

- UOW-1195 / commit `60cec509d`: added `BindPointTeleportPricePlanService`.
- UOW-1196 / commit `f835c9eac`: added `BindPointTeleportRequirementsPlanService`.
- UOW-1197 / commit `7502b7e00`: added `BindPointTeleportOperationPlanService`.
- UOW-1198 / commit `d433d4b40`: added `SmBindPointTeleport`.

## What Changed

- Added `BindPointTeleportControlPlanService` and `BindPointTeleportControlPlan`.
- Modeled Java `cancelTeleport`:
  - no `TaskId.SKILL_USE` -> no action;
  - active task -> cancel task and broadcast `SM_BIND_POINT_TELEPORT(action=2)`.
- Modeled Java `onLogin`:
  - missing or expired cooldown -> no action;
  - active cooldown -> `broadcastPacketAndReceive` with `SM_BIND_POINT_TELEPORT(action=3)`.
- Kept the planner non-live (`IsLive=false`).

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "BindPointTeleportControlPlanServiceTests" --nologo` passed 4 tests.
- No Java runtime comparison was run, so parity remains `Needs Verification`.

## Migration Parity Table - UOW-1199

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.cancelTeleport` | `Aion.GameServer.Services.BindPointTeleportControlPlanService.CreateCancelPlan` | Service / Control Flow | Partial | Unit Tested | Needs Verification | Non-live planner covers Java no-op when `TaskId.SKILL_USE` is absent and cancel/broadcast action `2` intent when present. Live task cancellation and broadcast ordering remain unported. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.onLogin` | `Aion.GameServer.Services.BindPointTeleportControlPlanService.CreateLoginCooldownPlan` | Service / Login Control Flow | Partial | Unit Tested | Needs Verification | Non-live planner covers missing/expired cooldown no-op and active-cooldown action `3` packet intent. Static cooldown map lookup and Java millisecond time-left calculation remain scalar inputs. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_BIND_POINT_TELEPORT` | `Aion.GameServer.Network.Aion.ServerPackets.SmBindPointTeleport` | Packet / Serialization Dependency | Partial | Unit Tested | Needs Verification | Control planner returns concrete packet instances for action `2` and action `3`. Packet bytes are source-derived tested; no live broadcast capture was compared. |
| `com.aionemu.gameserver.model.TaskId.SKILL_USE` | `BindPointTeleportControlStep.CancelSkillUseTask` | Scheduler Dependency | Partial | Unit Tested | Needs Verification | Planner records task-cancel intent only. C# does not inspect or cancel live task state in this unit. Threading and cancellation behavior remain unverified. |

Tests added:

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `BindPointTeleportControlPlanServiceTests.CreateCancelPlan_NoopsWhenSkillUseTaskMissing` | Unit | Java `cancelTeleport` | Validates no-op when the player controller has no `TaskId.SKILL_USE`. | Deterministic source-derived expectation. | No live controller task map. |
| `BindPointTeleportControlPlanServiceTests.CreateCancelPlan_CancelsTaskThenBroadcastsActionTwo` | Unit | Java `cancelTeleport` | Validates cancel task intent and action `2` packet payload. | Deterministic source-derived expectation. | No live task cancellation or fanout. |
| `BindPointTeleportControlPlanServiceTests.CreateLoginCooldownPlan_NoopsWhenCooldownMissingOrExpired` | Unit | Java `onLogin` | Validates no-op for missing or non-positive cooldown facts. | Deterministic source-derived expectation. | Static cooldown map and time math are not modeled. |
| `BindPointTeleportControlPlanServiceTests.CreateLoginCooldownPlan_BroadcastsAndReceivesActionThreeForActiveCooldown` | Unit | Java `onLogin` | Validates active cooldown creates action `3` packet intent with cooldown seconds. | Deterministic source-derived expectation. | No live `broadcastPacketAndReceive`. |

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 1 pure bind-point control intent planner plus 4 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: 3 grouped categories: live cooldown/task state, packet fanout, and scheduler/concurrency behavior
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Static Java cooldown map and `Cooldown.getTimeLeft()` millisecond calculation remain unported.
- Live task cancellation and scheduler behavior remain unported.
- Live `broadcastPacket` / `broadcastPacketAndReceive` fanout remains unported.
- Control planners return packet intent only; they do not interact with a live connection registry.
- No Java runtime comparison was executed. Reflection, date/time, and serialization behavior did not change in this unit; threading remains a future risk.

## Next Work Options

### Recommended Sequential Task

- Task: Add packet-output metadata to `BindPointTeleportOperationPlanService`.
- Why: The main operation planner can now reference concrete `SmBindPointTeleport.Start` and `Cooldown` packets without enabling live fanout.
- Files:
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportOperationPlanService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportOperationPlanServiceTests.cs`
  - `docs/Phase-6-PricesService-Consumer-Map.md`
  - `docs/PHASE-6-PROGRESS.md`
  - next Phase 6 handoff

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Add packet-output metadata to operation planner | operation planner and tests | Medium | Single owner only; touches existing planner files. |
| B | Model scheduled Kinah-decrement failure intent | new planner/test files | Medium | Keep separate from operation planner unless composed by orchestrator. |
| C | Continue `SM_SELL_ITEM` live-readiness fact assembly | new/non-overlapping sell-item adapter files and tests | Medium | Avoid shared dialog routing unless owned exclusively. |
| D | Read-only audit of live fanout insertion points | Java/C# teleport/connection files read-only | Low | Useful before touching live handlers. |

### Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Agent A | Read-only live fanout insertion audit | Java/C# teleport/connection files read-only | all writes |
| Orchestrator | Add operation-plan packet metadata and docs | operation planner/test/docs | connection handlers, live fanout code |

For implementation, keep one writer on `BindPointTeleportOperationPlanService.cs` and its tests.

### Do Not Parallelize

- `GameServerConnection` teleport handlers: live movement, packet order, and scheduling are high-conflict.
- Shared progress/handoff/parity docs: Orchestrator should own final edits.
- `PlayerTeleportService` live routing: broad movement side effects and existing tests make it unsuitable for concurrent edits.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_BIND_POINT_TELEPORT.java`
- Latest completed commits:
  - `d433d4b40 [Phase 6][UOW-1198] Add bind point teleport packet`
  - next commit should be `[Phase 6][UOW-1199] Add bind point teleport control plan`
- Keep bind-point control/planner packet outputs non-live until fanout, cooldown scheduling, Kinah mutation, and movement have their own parity slices.
