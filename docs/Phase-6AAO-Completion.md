# Phase 6AAO Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1205
Status: Phase 6 continues; bind-point teleport now has parser, action selection, operation/control packet intents, fanout intents, and request-level non-live composition. Live connection dispatch, runtime task/cooldown ownership, scheduled Kinah mutation, persistent known-list parity, and movement remain disabled.

## Session Summary

UOW-1205 added `BindPointTeleportRequestPlanService`, a source-derived non-live composition layer for Java `CM_BIND_POINT_TELEPORT.runImpl` and staged `BindPointTeleportService` branches. It ties client action plans to existing operation/control/fanout planners without sending packets, scheduling tasks, mutating cooldowns, mutating Kinah, or moving players.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportRequestPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportRequestPlanServiceTests.cs`
- `docs/Phase-6-BindPointTeleport-LiveFanout-Audit.md`
- `docs/Phase-6-PricesService-Consumer-Map.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AAO-Completion.md`

## What Changed

- Added request-level composition for Java bind-point client actions:
  - dead player -> no action;
  - unknown action -> no action;
  - action `1` -> missing operation facts, blocked operation, or ready start/cooldown packet and fanout intents;
  - action `2` -> missing control facts, no active task no-op, or ready cancel packet and fanout intent.
- Preserved Java source breadcrumbs for `CM_BIND_POINT_TELEPORT.runImpl`, `BindPointTeleportService.teleport`, `BindPointTeleportService.cancelTeleport`, and `PacketSendUtility.broadcastPacket(..., true)`.
- Kept all behavior non-live and side-effect free.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "BindPointTeleportRequestPlanServiceTests" --nologo` passed 7 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport" --nologo` passed 40 tests.
- No Java runtime comparison was run, so parity remains `Needs Verification`.

## Migration Parity Table - UOW-1205

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_BIND_POINT_TELEPORT.runImpl` | `Aion.GameServer.Services.BindPointTeleportRequestPlanService` / `Aion.GameServer.Services.BindPointTeleportClientActionPlanService` | Client Packet / Handler Planner | Partial | Unit Tested | Needs Verification | Request composition preserves Java dead-player early return, action `1`, action `2`, and default no-op branching. Live `GameServerConnection` dispatch remains unwired. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` | `Aion.GameServer.Services.BindPointTeleportRequestPlanService` / `Aion.GameServer.Services.BindPointTeleportOperationPlanService` / `Aion.GameServer.Services.BindPointTeleportFanoutPlanService` | Service / Movement Dependency | Partial | Unit Tested | Needs Verification | Action `1` composition records missing hotspot/price/requirement facts, blocked operation, or ready start/cooldown packet fanout intents. Scheduler, cooldown map, scheduled Kinah decrement, death/about-to-die recheck, and final movement remain unported. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.cancelTeleport` | `Aion.GameServer.Services.BindPointTeleportRequestPlanService` / `Aion.GameServer.Services.BindPointTeleportControlPlanService` / `Aion.GameServer.Services.BindPointTeleportFanoutPlanService` | Service / Control Dependency | Partial | Unit Tested | Needs Verification | Action `2` composition records missing `TaskId.SKILL_USE` facts, no active task no-op, or ready cancel packet/fanout intent. Live task lookup/cancel and live fanout remain unported. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_BIND_POINT_TELEPORT.readImpl` | `Aion.GameServer.Network.Aion.ClientPackets.CmBindPointTeleport` feeding `Aion.GameServer.Services.BindPointTeleportRequestPlanService` | Client Packet / Parser Dependency | Complete | Unit Tested | Needs Verification | Parser output can feed request composition, but no Java-generated client packet capture or live C# handler dispatch was compared. |
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastPacket(Player,AionServerPacket,boolean)` | `Aion.GameServer.Services.BindPointTeleportFanoutPlanService` used by `Aion.GameServer.Services.BindPointTeleportRequestPlanService` | Fanout Utility / Planner Dependency | Partial | Unit Tested | Needs Verification | Request composition creates include-source fanout intents for action `1` start/cooldown and action `2` cancel. Live C# registry send and Java persistent known-list membership remain unverified. |

Tests added:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `BindPointTeleportRequestPlanServiceTests.CreatePlan_DeadPlayerStopsBeforeTeleportComposition` | Dead-player early return prevents action `1` operation/fanout composition. | Source-derived only. |
| `BindPointTeleportRequestPlanServiceTests.CreatePlan_UnknownActionNoopsWithoutFanout` | Unknown actions produce no packet/fanout intents. | Source-derived only. |
| `BindPointTeleportRequestPlanServiceTests.CreatePlan_ActionOneReadyComposesOperationPacketsAndFanout` | Ready action `1` composes start/cooldown packets and include-source fanout intents. | Source-derived only. |
| `BindPointTeleportRequestPlanServiceTests.CreatePlan_ActionOneRequirementsFailureDoesNotCreateFanout` | Failed action `1` operation stops before broadcast/schedule. | Source-derived only. |
| `BindPointTeleportRequestPlanServiceTests.CreatePlan_ActionOneMissingOperationFactsRecordsGap` | Missing hotspot/price/requirement facts are explicit before live use. | C# staging guard only. |
| `BindPointTeleportRequestPlanServiceTests.CreatePlan_ActionTwoActiveSkillUseTaskComposesCancelFanout` | Action `2` with active skill task composes cancel packet and fanout intent. | Source-derived only. |
| `BindPointTeleportRequestPlanServiceTests.CreatePlan_ActionTwoMissingSkillUseTaskDoesNotCreateFanout` | Action `2` with no active skill task remains no-op. | Source-derived only. |

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live request composition planner plus focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 5 grouped categories: live connection dispatch, task/cooldown state, persistent known-list parity, Kinah mutation, and final movement/death checks
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Live `GameServerConnection` still does not dispatch `CmBindPointTeleport`.
- Request composition consumes externally supplied operation/control plans; live hotspot lookup, static data projection, price/requirement fact assembly, and `TaskId.SKILL_USE` facts remain separate.
- C# fanout remains non-live and distance-based when eventually wired; Java persistent `KnownList.forEachPlayer` membership remains unverified.
- Static cooldown map, scheduler timing/cancellation, scheduled Kinah decrement race, final death/about-to-die recheck, and movement remain unported.
- No Java runtime comparison was executed. Reflection, serialization, date/time, threading, persistence, and movement parity remain unverified for live bind-point teleport.

## Next Work Options

### Recommended Sequential Task

- Task: Audit/model a bind-point runtime state owner for Java `TaskId.SKILL_USE` and the static cooldown map, still without live scheduling or movement.
- Why: Request-level composition now exposes where action `1` scheduling and action `2` cancellation need runtime facts. A focused state-owner plan can pin down ownership, time math, and cancellation semantics before any live `ThreadPoolManager` callback or `GameServerConnection` branch is enabled.
- Required behavior:
  - represent scheduled bind-point skill-use task ownership by player/object id;
  - represent cooldown map facts by player/locId/remaining time;
  - model action `2` cancellation lookup without cancelling a real task;
  - model login cooldown fact extraction without sending packets;
  - no live scheduler callback, no cooldown mutation, no Kinah mutation, no packet send, and no movement.
- Suggested files:
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportRuntimeStatePlanService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportRuntimeStatePlanServiceTests.cs`
  - docs for next unit

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Runtime task/cooldown owner plan | new service/test files | Medium | Best next step before live scheduling. |
| B | Scheduled Kinah-decrement failure intent | new planner/test files | Medium | Keep separate from inventory mutation. |
| C | Audit `PlayerController` task-map equivalents for bind-point `TaskId.SKILL_USE` | Java/C# scheduler files read-only | Low/Medium | Useful before state-owner implementation. |
| D | Continue `SM_SELL_ITEM` live-readiness fact assembly | new/non-overlapping sell-item adapter files and tests | Medium | Avoid shared dialog routing unless owned exclusively. |

### Do Not Parallelize

- `GameServerConnection` live bind-point dispatch: wait until runtime task/cooldown ownership exists.
- Shared progress/handoff/parity docs: Orchestrator should own final edits.
- `PlayerTeleportService` movement wiring: defer until task/cooldown ownership and scheduled Kinah failure are modeled.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_BIND_POINT_TELEPORT.java`
  - `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
  - `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`
  - `game-server/src/com/aionemu/gameserver/model/TaskId.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_BIND_POINT_TELEPORT.java`
- Latest completed commits:
  - `d6f7ebce3 [Phase 6][UOW-1204] Add bind point teleport fanout plan`
  - next commit should be `[Phase 6][UOW-1205] Add bind point teleport request plan`
- Keep live bind-point behavior disabled until runtime state ownership, persistent known-list/fanout semantics, cooldown/task scheduling, Kinah mutation, and final movement each have focused parity slices.
