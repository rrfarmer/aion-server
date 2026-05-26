# Phase 6AAJ Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1200
Status: Phase 6 continues; bind-point teleport operation plans now include concrete packet intents, but live fanout, cooldown scheduling, Kinah mutation, and movement remain incomplete.

## Session Summary

UOW-1200 refined `BindPointTeleportOperationPlanService` so non-live success plans carry concrete `SmBindPointTeleport` packet intents for Java's start and cooldown broadcasts. This keeps the bind-point flow staged while making future live fanout wiring less ambiguous.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportOperationPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportOperationPlanServiceTests.cs`
- `docs/Phase-6-PricesService-Consumer-Map.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AAJ-Completion.md`

Related recent units:

- UOW-1195 / commit `60cec509d`: added `BindPointTeleportPricePlanService`.
- UOW-1196 / commit `f835c9eac`: added `BindPointTeleportRequirementsPlanService`.
- UOW-1197 / commit `7502b7e00`: added initial `BindPointTeleportOperationPlanService`.
- UOW-1198 / commit `d433d4b40`: added `SmBindPointTeleport`.
- UOW-1199 / commit `4b60343c1`: added `BindPointTeleportControlPlanService`.

## What Changed

- Updated `BindPointTeleportOperationPlanService.CreatePlan` to accept `playerObjectId`.
- Added `PacketIntents` to `BindPointTeleportOperationPlan`.
- Ready success plans now include:
  - `SmBindPointTeleport.Start(playerObjectId, locId)`;
  - `SmBindPointTeleport.Cooldown(playerObjectId, locId, 600)`.
- Invalid-hotspot and requirements-failure plans still include no packet intents, matching Java's early returns before bind-point broadcast.
- Tests now parse the concrete packet-intent payloads.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "BindPointTeleportOperationPlanServiceTests" --nologo` passed 3 tests.
- No Java runtime comparison was run, so parity remains `Needs Verification`.

## Migration Parity Table - UOW-1200

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` | `Aion.GameServer.Services.BindPointTeleportOperationPlanService` | Service / Movement | Partial | Unit Tested | Needs Verification | Non-live success plans now carry concrete action `1` and action `3` `SmBindPointTeleport` packet intents in Java order. Invalid-hotspot and requirements-failure plans still carry no packet intents. Live fanout, scheduler, cooldown map, Kinah mutation, death recheck, and movement remain unported. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_BIND_POINT_TELEPORT` | `Aion.GameServer.Network.Aion.ServerPackets.SmBindPointTeleport` | Packet / Serialization Dependency | Partial | Unit Tested | Needs Verification | Operation planner now composes concrete packet instances, and tests parse their payloads. No live Java/C# packet capture or broadcast comparison was run. |
| `com.aionemu.gameserver.model.TaskId.SKILL_USE` | `BindPointTeleportOperationStep.ScheduleSkillUseTask` / packet intent order | Scheduler Dependency | Partial | Unit Tested | Needs Verification | Planner records the start packet before scheduling and cooldown packet after Kinah/cooldown intent, matching Java order. Real task creation and delay timing remain unported. |

Tests added or updated:

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `BindPointTeleportOperationPlanServiceTests.CreatePlan_InvalidHotspotStopsBeforePriceAndRequirements` | Unit | Java `BindPointTeleportService.teleport` invalid hotspot branch | Validates invalid hotspot still has no packet intents. | Deterministic source-derived expectation. | No live audit or packet send. |
| `BindPointTeleportOperationPlanServiceTests.CreatePlan_RequirementFailureStopsBeforeBroadcastAndScheduling` | Unit | Java `if (!checkRequirements(...)) return` branch | Validates failed requirements still have no packet intents. | Deterministic source-derived expectation. | No live packet/scheduler behavior. |
| `BindPointTeleportOperationPlanServiceTests.CreatePlan_ReadyPlanRecordsJavaSuccessOperationOrder` | Unit | Java success branch in `teleport`; Java `SM_BIND_POINT_TELEPORT.writeImpl` | Validates success step order and parses concrete action `1` and action `3` packet intents. | Deterministic source-derived expectation. | No live fanout, timing, cooldown mutation, death recheck, or movement. |

## Summary Metrics

- Total Java artifacts discovered: 3 grouped artifact rows in this unit
- Total artifacts ported: 1 operation-planner packet-intent refinement plus focused test updates
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 3 grouped rows
- Total blocked artifacts: 4 grouped categories: live packet fanout, cooldown scheduling, inventory mutation, and movement/death checks
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Live `PacketSendUtility.broadcastPacket` order is still not exercised against a connection registry.
- Static cooldown map and scheduler semantics are not modeled.
- Scheduled `tryDecreaseKinah` failure remains metadata.
- Final teleport death/about-to-die recheck and movement remain unported.
- No Java runtime comparison was executed. Reflection, date/time, and threading behavior did not change in this unit.

## Next Work Options

### Recommended Sequential Task

- Task: Perform a read-only live bind-point fanout insertion audit across C# connection/teleport services.
- Why: The bind-point flow now has staged price, requirement, operation, control, and packet pieces; live wiring should not start until insertion points, packet order, and task/cooldown ownership are mapped.
- Files:
  - read-only first: `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
  - read-only first: `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_BIND_POINT_TELEPORT.java`
  - read-only first: `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - read-only first: `dotnetConversion/src/Aion.GameServer/Services/PlayerTeleportService.cs`
  - docs for next unit

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Read-only live fanout insertion audit | Java/C# teleport/connection files read-only | Low | Best next step before live mutation. |
| B | Model scheduled Kinah-decrement failure intent | new planner/test files | Medium | Keep separate from live inventory mutation. |
| C | Continue `SM_SELL_ITEM` live-readiness fact assembly | new/non-overlapping sell-item adapter files and tests | Medium | Avoid shared dialog routing unless owned exclusively. |
| D | Analyze CM_BIND_POINT_TELEPORT parser parity | Java/C# client packet files read-only first | Low/Medium | Implementation may touch shared connection packet switch. |

### Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Agent A | Read-only bind-point live fanout insertion audit | Java/C# teleport/connection files read-only | all writes |
| Agent B | Read-only `CM_BIND_POINT_TELEPORT` parser audit | Java/C# client packet files read-only | all writes |
| Orchestrator | Integrate findings and choose next implementation UOW | docs only after audits | live connection handlers until audit is complete |

If sub-agent tooling is unavailable, do the audits sequentially.

### Do Not Parallelize

- `GameServerConnection` teleport handlers: live movement, packet order, and scheduling are high-conflict.
- Shared progress/handoff/parity docs: Orchestrator should own final edits.
- `PlayerTeleportService` live routing: broad movement side effects and existing tests make it unsuitable for concurrent edits.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_BIND_POINT_TELEPORT.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_BIND_POINT_TELEPORT.java`
- Latest completed commits:
  - `4b60343c1 [Phase 6][UOW-1199] Add bind point teleport control plan`
  - next commit should be `[Phase 6][UOW-1200] Add bind point teleport packet intents`
- Keep bind-point operation packet intents non-live until fanout, cooldown scheduling, Kinah mutation, and movement have their own parity slices.
