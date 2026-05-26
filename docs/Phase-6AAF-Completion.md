# Phase 6AAF Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1196
Status: Phase 6 continues; bind-point teleport price and requirement planning are staged, but live cooldowns, packets, Kinah mutation, and movement remain incomplete.

## Session Summary

UOW-1196 implemented a pure planner for Java `BindPointTeleportService.checkRequirements`. The planner preserves Java guard ordering for world, race, Kinah, and cooldown facts, recording system-message/audit intent without sending packets or mutating runtime state.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportRequirementsPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportRequirementsPlanServiceTests.cs`
- `docs/Phase-6-PricesService-Consumer-Map.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AAF-Completion.md`

Previous related unit:

- UOW-1195 / commit `60cec509d`: added `BindPointTeleportPricePlanService`.

## What Changed

- Added `BindPointTeleportRequirementsPlanService` and `BindPointTeleportRequirementsPlan`.
- Ported Java bind-point requirement guard order:
  - invalid start world -> no-route system-message key and audit text;
  - invalid race -> audit text only;
  - insufficient Kinah -> not-enough-fee system-message key;
  - active cooldown -> flying-time-not-ready system-message key;
  - missing or expired cooldown -> success.
- Kept the planner non-live (`IsLive=false`).
- Documented that Java cooldown storage/scheduling, `SM_BIND_POINT_TELEPORT`, `PacketSendUtility`, `AuditLogger`, Kinah decrement, and movement are separate follow-up work.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "BindPointTeleportRequirementsPlanServiceTests" --nologo` passed 6 tests.
- No Java runtime comparison was run, so parity remains `Needs Verification`.

## Migration Parity Table - UOW-1196

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService` | `Aion.GameServer.Services.BindPointTeleportRequirementsPlanService` | Service / Movement | Partial | Unit Tested | Needs Verification | `checkRequirements` guard ordering is staged and source-derived tested. Missing methods/logic: hotspot lookup, cooldown storage and mutation, `onLogin`, `teleport`, `cancelTeleport`, live `AuditLogger`, live packet sends, delayed task scheduling, Kinah decrement, death checks, and final `TeleportService.teleportTo`. |
| `com.aionemu.gameserver.model.Race` | string race inputs to `Aion.GameServer.Services.BindPointTeleportRequirementsPlanService` | Enum / Runtime Fact | Partial | Unit Tested | Needs Verification | `PC_ALL` bypass and exact player/hotspot race match are covered. Java enum identity is approximated with ordinal string comparison; invalid/null string behavior is not Java-representable and remains a C# staging risk. |
| `com.aionemu.gameserver.model.templates.hotspot.HotspotTemplate` | scalar inputs to `Aion.GameServer.Services.BindPointTeleportRequirementsPlanService` | DTO / Static Data Dependency | Partial | Unit Tested | Needs Verification | Planner consumes hotspot id, world id, and race as scalar facts. Static-data lookup and XML normalization remain outside this unit. |
| `com.aionemu.gameserver.model.gameobjects.player.Player` | scalar inputs to `Aion.GameServer.Services.BindPointTeleportRequirementsPlanService` | Model / Runtime Fact Source | Partial | Unit Tested | Needs Verification | Planner consumes world id, race, current Kinah, and cooldown seconds. Java uses live player world/race/inventory/task state. Threading/live object identity remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | string system-message keys in `Aion.GameServer.Services.BindPointTeleportRequirementsPlanService` | Packet / Message Dependency | Partial | Unit Tested | Needs Verification | Planner records message intent for no-route, not-enough-fee, and cooldown-not-ready branches. Concrete packet serialization/send order is not added here. |
| `com.aionemu.gameserver.utils.audit.AuditLogger` | audit-message strings in `Aion.GameServer.Services.BindPointTeleportRequirementsPlanService` | Audit Dependency | Partial | Unit Tested | Needs Verification | Planner records audit intent for invalid world/race branches. Live staff audit routing, formatting, and persistence are unported. |

Tests added:

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `BindPointTeleportRequirementsPlanServiceTests.CreatePlan_RejectsInvalidStartWorldBeforeRaceKinahAndCooldown` | Unit | Java `BindPointTeleportService.checkRequirements` | Validates world mismatch is first, produces no-route message intent, and records audit text. | Deterministic source-derived expectation. | No live packet or audit logger. |
| `BindPointTeleportRequirementsPlanServiceTests.CreatePlan_RejectsInvalidRaceWithoutSystemMessage` | Unit | Java race guard in `checkRequirements` | Validates race mismatch returns false with audit intent and no system-message send. | Deterministic source-derived expectation. | Java enum identity is represented by strings. |
| `BindPointTeleportRequirementsPlanServiceTests.CreatePlan_AllowsPcAllPlayerRaceLikeJava` | Unit | Java `player.getRace() == Race.PC_ALL` bypass | Validates `PC_ALL` bypasses hotspot-race mismatch. | Deterministic source-derived expectation. | Live players normally use Elyos/Asmodians; branch exists in Java. |
| `BindPointTeleportRequirementsPlanServiceTests.CreatePlan_RejectsNotEnoughKinahBeforeCooldown` | Unit | Java Kinah guard order | Validates insufficient Kinah wins before active cooldown and records not-enough-fee message intent. | Deterministic source-derived expectation. | No live inventory read/mutation. |
| `BindPointTeleportRequirementsPlanServiceTests.CreatePlan_RejectsActiveCooldownAfterKinahPasses` | Unit | Java cooldown guard | Validates active cooldown returns flying-time-not-ready message intent after Kinah succeeds. | Deterministic source-derived expectation. | Static cooldown map and time calculation are scalar facts. |
| `BindPointTeleportRequirementsPlanServiceTests.CreatePlan_AllowsMissingOrExpiredCooldown` | Unit | Java `cooldown == null` / `getTimeLeft() <= 0` success branches | Validates missing and zero-second cooldowns pass. | Deterministic source-derived expectation. | Millisecond clock and negative cooldown calculations are not modeled. |

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 pure bind-point teleport requirements planner plus 6 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: 3 grouped categories: live bind-point cooldown scheduling, packet fanout/order, and movement/inventory mutation
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Static Java cooldown map behavior, `addCooldown`, `onLogin`, `cancelTeleport`, and delayed scheduler task ordering remain unported.
- `SM_BIND_POINT_TELEPORT` packet shape and broadcast order remain unknown in C#.
- This planner stores system-message/audit intent strings only; no live `PacketSendUtility` or `AuditLogger` integration exists.
- Java race enum behavior is approximated with exact strings.
- No Java runtime comparison was executed. Reflection, serialization, and date/time behavior did not change; threading remains a future risk for cooldown storage/scheduling.

## Next Work Options

### Recommended Sequential Task

- Task: Compose a non-live bind-point teleport operation plan from price + requirements.
- Why: UOW-1195 and UOW-1196 now provide the two pure inputs needed to model the high-level success/failure decision before packet/scheduler work.
- Files:
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportOperationPlanService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportOperationPlanServiceTests.cs`
  - `docs/Phase-6-PricesService-Consumer-Map.md`
  - `docs/PHASE-6-PROGRESS.md`
  - next Phase 6 handoff

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Read-only `SM_BIND_POINT_TELEPORT` packet/opcode analysis | Java packet/opcode files read-only | Low | Can run beside operation-plan work if sub-agent tooling is available. |
| B | Compose non-live bind-point operation plan | new operation-plan service and tests | Medium | Do not send packets or schedule tasks. |
| C | Continue `SM_SELL_ITEM` live-readiness fact assembly | new/non-overlapping sell-item adapter files and tests | Medium | Avoid shared dialog routing unless owned exclusively. |
| D | Broker rollback/order analysis | read-only Java/C# broker handler/repository files | Low/Medium | Implementation touches shared connection handler and should be sequential. |

### Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Agent A | Read-only `SM_BIND_POINT_TELEPORT` packet/opcode analysis | Java packet/opcode files read-only | all writes |
| Agent B | Implement non-live bind-point operation plan | `BindPointTeleportOperationPlanService.cs`, `BindPointTeleportOperationPlanServiceTests.cs` | shared docs, packet files, connection handlers |
| Orchestrator | Integrate, test, update docs, commit | shared docs and final review | do not overlap with Agent B files until handoff |

If sub-agent tooling is unavailable or the scope remains small, do the operation planner sequentially.

### Do Not Parallelize

- `GameServerConnection` teleport handlers: live movement, packet order, and scheduling are high-conflict.
- Shared progress/handoff/parity docs: Orchestrator should own final edits.
- `PlayerTeleportService` live routing: broad movement side effects and existing tests make it unsuitable for concurrent edits.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
  - `game-server/src/com/aionemu/gameserver/utils/PositionUtil.java`
- Latest completed commits:
  - `60cec509d [Phase 6][UOW-1195] Add bind point teleport price plan`
  - next commit should be `[Phase 6][UOW-1196] Add bind point teleport requirements plan`
- Keep both bind-point planners non-live until packet fanout, cooldown scheduling, Kinah mutation, and movement have their own parity slices.
