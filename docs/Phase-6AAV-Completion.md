# Phase 6AAV Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1212
Status: Phase 6 continues; bind-point teleport now has parser, action selection, operation/control packet intents, fanout intents, request-level composition, runtime task/cooldown state semantics, scheduled Kinah decrement/failure intent, final movement gate intent, scheduled callback composition metadata, a `TeleportService.teleportTo` side-effect audit, a non-live teleport side-effect planner, and callback-side side-effect metadata composition. Live connection dispatch, real scheduler/cooldown mutation, live inventory mutation, persistent known-list parity, and movement remain disabled.

## Session Summary

UOW-1212 updated `BindPointTeleportScheduledCallbackPlanService` so the Java delayed callback can carry optional teleport side-effect metadata after final movement intent. This preserves Java order: scheduled Kinah success, cooldown insert, action `3` fanout, final movement gate, final movement intent, then concrete `TeleportService.teleportTo` side-effect metadata. Existing callers remain stable when no side-effect plan is supplied.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportScheduledCallbackPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportScheduledCallbackPlanServiceTests.cs`
- `docs/Phase-6-BindPointTeleport-TeleportTo-Audit.md`
- `docs/Phase-6-BindPointTeleport-LiveFanout-Audit.md`
- `docs/Phase-6-PricesService-Consumer-Map.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AAV-Completion.md`

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "BindPointTeleportScheduledCallbackPlanServiceTests" --nologo` passed 4 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport" --nologo` passed 63 tests.
- No Java runtime comparison was run, so parity remains `Needs Verification`.

## Migration Parity Table - UOW-1212

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` delayed `TaskId.SKILL_USE` callback | `Aion.GameServer.Services.BindPointTeleportScheduledCallbackPlanService` | Service / Scheduled Callback Planner | Partial | Unit Tested | Needs Verification | Non-live callback composition now carries optional concrete teleport side-effect metadata after final movement intent. No live scheduler, inventory mutation, cooldown mutation, packet send, or movement executes. |
| `com.aionemu.gameserver.services.teleport.TeleportService.teleportTo(Player,int,float,float,float)` | `Aion.GameServer.Services.BindPointTeleportTeleportToSideEffectPlanService` composed by `BindPointTeleportScheduledCallbackPlanService` | Service / Movement Planner Dependency | Partial | Unit Tested | Needs Verification | Side-effect plan is now part of callback metadata when supplied and movement is allowed. Live `teleportTo` execution remains unported. |
| `com.aionemu.gameserver.services.teleport.TeleportService.sendLoc` / `SpawnTask` | `Aion.GameServer.Services.BindPointTeleportTeleportToSideEffectPlanService` composed by `BindPointTeleportScheduledCallbackPlanService` | Packet / Movement Dependency | Partial | Unit Tested | Needs Verification | Callback metadata can carry `TeleportAnimation.NONE` side-effect ordering, including no `SM_TELEPORT_LOC`. No packets are emitted and no world state changes. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.addCooldown` | `Aion.GameServer.Services.BindPointTeleportRuntimeStatePlanService` composed by `BindPointTeleportScheduledCallbackPlanService` | Runtime State Dependency | Partial | Unit Tested | Needs Verification | Existing cooldown insert intent remains before cooldown fanout and final movement metadata. Static map mutation and threading remain unported. |
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastPacket(Player,AionServerPacket,boolean)` | `Aion.GameServer.Services.BindPointTeleportFanoutPlanService` composed by `BindPointTeleportScheduledCallbackPlanService` | Fanout Dependency | Partial | Unit Tested | Needs Verification | Existing action `3` include-source fanout intent remains before final movement metadata. Live known-list fanout remains unverified. |

Tests added:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `BindPointTeleportScheduledCallbackPlanServiceTests.CreatePlan_KinahSuccessCanCarryTeleportSideEffectMetadataAfterFinalMovementIntent` | Side-effect metadata is carried after final movement intent and not before Kinah/cooldown/fanout ordering. | Source-derived only. |

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live callback/side-effect composition update plus focused test coverage
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 6 grouped categories: live connection dispatch, live scheduler/cooldown ownership, live inventory mutation/packets, live teleport adapter, persistent known-list parity, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Live `GameServerConnection` still does not dispatch `CmBindPointTeleport`.
- The bind-point pipeline still has no live scheduler, inventory mutation, cooldown-map mutation, packet fanout, or final movement.
- Side-effect metadata is composed, but action aborts, private-store closure, current-skill cancellation, target clearing, ride-mode removal, pet movement, world despawn/spawn, protection/effect/zone callbacks, instance/leave-map callbacks, and legion refresh remain unported or unverified.
- No Java runtime comparison was executed. Reflection behavior did not change; serialization, threading, date/time, movement, known-list, and persistence parity remain unverified for live bind-point teleport.

## Next Work Options

### Recommended Sequential Task

- Task: Add a live-adapter readiness checklist for bind-point teleport.
- Why: Non-live request, scheduler, Kinah, cooldown, fanout, final movement, side-effect, and callback composition metadata now exists. The next step should identify the smallest remaining prerequisite before any live adapter work.
- Suggested files:
  - `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
  - existing progress/handoff docs

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Live-adapter readiness checklist | new doc only | Low/Medium | Best next step before touching `GameServerConnection`. |
| B | Concrete system-message packet support audit for bind-point failure messages | read-only packet/system-message files plus optional doc | Low/Medium | Useful before live sends. |
| C | Handler-level no-op composition bridge sketch | new service/test pair only | Medium | Must remain non-live and avoid `GameServerConnection` dispatch. |
| D | Continue `SM_SELL_ITEM` live-readiness fact assembly | new/non-overlapping sell-item adapter files and tests | Medium | Avoid shared dialog routing unless owned exclusively. |

### Do Not Parallelize

- `GameServerConnection` live bind-point dispatch: wait until readiness checklist and missing live prerequisites are settled.
- Shared progress/handoff/parity docs: Orchestrator should own final edits.
- `PlayerTeleportService` live bind-point movement wiring: defer until packet order and unsupported side effects are ready for execution.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
  - `game-server/src/com/aionemu/gameserver/services/teleport/TeleportService.java`
- C# surfaces:
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportScheduledCallbackPlanService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportTeleportToSideEffectPlanService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportScheduledCallbackPlanServiceTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportTeleportToSideEffectPlanServiceTests.cs`
- Latest completed commits:
  - `2c6d62ae7 [Phase 6][UOW-1211] Add bind point teleport side effect plan`
  - next commit should be `[Phase 6][UOW-1212] Compose bind point teleport side effect metadata`
- Keep live bind-point behavior disabled until teleport side-effect ordering, cooldown mutation/fanout execution, live inventory mutation/packets, live movement packet ordering, and live known-list fanout each have focused parity slices.
