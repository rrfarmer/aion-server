# Phase 6AAG Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1197
Status: Phase 6 continues; bind-point teleport price, requirements, and non-live operation intent are staged, but packet serialization, live cooldowns, Kinah mutation, and movement remain incomplete.

## Session Summary

UOW-1197 composed the bind-point price and requirements planners into a pure operation planner for Java `BindPointTeleportService.teleport`. The planner records Java branch and operation intent without sending packets, scheduling tasks, mutating Kinah, storing cooldowns, or moving the player.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportOperationPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportOperationPlanServiceTests.cs`
- `docs/Phase-6-PricesService-Consumer-Map.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AAG-Completion.md`

Related recent units:

- UOW-1195 / commit `60cec509d`: added `BindPointTeleportPricePlanService`.
- UOW-1196 / commit `f835c9eac`: added `BindPointTeleportRequirementsPlanService`.

## What Changed

- Added `BindPointTeleportOperationPlanService` and `BindPointTeleportOperationPlan`.
- Modeled Java `BindPointTeleportService.teleport` operation intent:
  - invalid hotspot branch stops immediately with audit/no-route metadata;
  - requirements failure branch stops before `SM_BIND_POINT_TELEPORT(1, ...)` and task scheduling;
  - ready branch records start broadcast, 10s skill-use task, Kinah decrement attempt, scheduled not-enough-fee fallback, cooldown add, cooldown broadcast, and final 1s teleport task.
- Kept the planner non-live (`IsLive=false`).

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "BindPointTeleportOperationPlanServiceTests" --nologo` passed 3 tests.
- No Java runtime comparison was run, so parity remains `Needs Verification`.

## Migration Parity Table - UOW-1197

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService` | `Aion.GameServer.Services.BindPointTeleportOperationPlanService` | Service / Movement | Partial | Unit Tested | Needs Verification | Non-live operation intent now covers invalid hotspot, requirements failure, and success sequencing metadata. Missing methods/logic: live `PacketSendUtility`, `ThreadPoolManager`, static cooldown map mutation, real `tryDecreaseKinah`, death/about-to-die recheck, concrete `SM_BIND_POINT_TELEPORT`, and final movement. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_BIND_POINT_TELEPORT` | step metadata in `Aion.GameServer.Services.BindPointTeleportOperationPlanService` | Packet Dependency | Not Started | No Tests | Unknown | Operation plan records action intent only. Concrete opcode, payload fields, broadcast/receive behavior, and serializer tests are not implemented. |
| `com.aionemu.gameserver.model.TaskId.SKILL_USE` | `BindPointTeleportOperationStep.ScheduleSkillUseTask` | Scheduler Dependency | Partial | Unit Tested | Needs Verification | Plan records the 10s skill-use scheduling intent. C# does not add/cancel live tasks in this unit. Threading and cancellation behavior remain unverified. |
| `com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType.DEC_KINAH_FLY` | `BindPointTeleportOperationStep.TryDecreaseKinahFly` | Inventory Mutation Dependency | Partial | Unit Tested | Needs Verification | Plan records the intended Kinah decrement step and scheduled failure message, but no inventory mutation or packet update is executed. |
| `com.aionemu.gameserver.services.teleport.TeleportService.teleportTo` | `BindPointTeleportOperationStep.ScheduleFinalTeleport` | Movement Dependency | Not Started | No Tests | Unknown | Plan records the final 1s teleport scheduling intent. Death/about-to-die recheck and actual movement remain unported. |

Tests added:

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `BindPointTeleportOperationPlanServiceTests.CreatePlan_InvalidHotspotStopsBeforePriceAndRequirements` | Unit | Java `BindPointTeleportService.teleport` invalid hotspot branch | Validates invalid hotspot audit/no-route intent and no price/requirements dependency. | Deterministic source-derived expectation. | No live `AuditLogger` or packet send. |
| `BindPointTeleportOperationPlanServiceTests.CreatePlan_RequirementFailureStopsBeforeBroadcastAndScheduling` | Unit | Java `if (!checkRequirements(...)) return` branch | Validates failed requirements stop before start broadcast and task scheduling while preserving price warning and requirement metadata. | Deterministic source-derived expectation. | No live packet/scheduler behavior. |
| `BindPointTeleportOperationPlanServiceTests.CreatePlan_ReadyPlanRecordsJavaSuccessOperationOrder` | Unit | Java success branch in `teleport` | Validates ordered success intent: start broadcast, 10s task, Kinah decrement, failure fallback, cooldown add, cooldown broadcast, and final 1s teleport schedule. | Deterministic source-derived expectation. | No real timing, cooldown mutation, packet serializer, death recheck, or movement. |

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 pure bind-point teleport operation planner plus 3 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: 4 grouped categories: packet serialization/fanout, live cooldown scheduling, inventory mutation, and final movement/death checks
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- `SM_BIND_POINT_TELEPORT` remains unported; action ids `1`, `2`, and `3`, payload shape, opcode, and broadcast variants need isolated packet work.
- Static Java cooldown map and scheduler semantics are not modeled.
- The scheduled `tryDecreaseKinah` race/failure branch is only metadata.
- Final teleport skips Java's live death/about-to-die recheck.
- No Java runtime comparison was executed. Reflection, serialization, and date/time behavior did not change; threading remains a major future risk for scheduler/cooldown work.

## Next Work Options

### Recommended Sequential Task

- Task: Analyze and port `SM_BIND_POINT_TELEPORT` packet/opcode shape in isolation with byte-shape tests.
- Why: Operation intent now references the packet in three branches; concrete packet parity is the next prerequisite before any live broadcast wiring.
- Files:
  - likely `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmBindPointTeleport.cs`
  - likely `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs` or a dedicated packet test file
  - `docs/Phase-6-PricesService-Consumer-Map.md`
  - `docs/PHASE-6-PROGRESS.md`
  - next Phase 6 handoff

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Read-only `SM_BIND_POINT_TELEPORT` opcode/payload analysis | Java packet/opcode files read-only | Low | Best first step if packet layout is not obvious. |
| B | Port `SmBindPointTeleport` serializer and tests | packet file and packet test file | Medium | Avoid live connection/teleport handlers. |
| C | Model bind-point `cancelTeleport` / `onLogin` intent planners | new planner files and tests | Medium | Can be separate from packet serializer if files do not overlap. |
| D | Continue `SM_SELL_ITEM` live-readiness fact assembly | new/non-overlapping sell-item adapter files and tests | Medium | Avoid shared dialog routing unless owned exclusively. |

### Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Agent A | Read-only `SM_BIND_POINT_TELEPORT` packet/opcode analysis | Java packet/opcode files read-only | all writes |
| Agent B | Port packet serializer/tests after analysis | `SmBindPointTeleport.cs`, selected packet test file | shared docs, connection handlers, teleport services |
| Orchestrator | Integrate, test, update docs, commit | shared docs and final review | do not overlap with Agent B files until handoff |

If sub-agent tooling is unavailable or packet work is small, do the packet unit sequentially.

### Do Not Parallelize

- `GameServerConnection` teleport handlers: live movement, packet order, and scheduling are high-conflict.
- Shared progress/handoff/parity docs: Orchestrator should own final edits.
- `PlayerTeleportService` live routing: broad movement side effects and existing tests make it unsuitable for concurrent edits.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_BIND_POINT_TELEPORT.java`
  - Java opcode registration for `SM_BIND_POINT_TELEPORT`
- Latest completed commits:
  - `60cec509d [Phase 6][UOW-1195] Add bind point teleport price plan`
  - `f835c9eac [Phase 6][UOW-1196] Add bind point teleport requirements plan`
  - next commit should be `[Phase 6][UOW-1197] Add bind point teleport operation plan`
- Keep bind-point planners non-live until packet fanout, cooldown scheduling, Kinah mutation, and movement have their own parity slices.
