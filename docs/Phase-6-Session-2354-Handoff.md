# Phase 6 Session 2354 Handoff - AutoGroup Leave Runtime State

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2354-Completion.md`
- `docs/Phase-6-Session-2354-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Prefer production/game parity units. Tests and docs should support concrete parity work, not become standalone evidence/reporting loops.

Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger before execution.

## Current State

Last completed UOW: UOW-2354, added minimal C# autogroup leave runtime state/service.

Relevant completed instance destroy/checker/leave slices:

- `InstanceEmptyInstanceCheckerService` models Java `EmptyInstanceCheckerTask`.
- Portal instance allocation passes the real checker scheduler callback.
- `InstanceRegisteredTeamDisbandService` maps registered team ids to group/alliance runtime membership state.
- `InstanceLeaveMessageService` models Java reset-warning message selection for instance leave.
- `SmSystemMessage` has helpers for `STR_MSG_LEAVE_INSTANCE`, `STR_MSG_LEAVE_INSTANCE_PARTY`, and `STR_MSG_LEAVE_INSTANCE_FORCE`.
- Delayed teleport completion sends the leave-instance reset-warning packet before C# position mutation and spawn packets when world or instance changes.
- Delayed teleport leave handling invokes `IInstanceLifecycleHandler.OnLeaveInstance(Player)` before reset-warning message planning.
- `AutoGroupInstanceLeavePlanService` models Java autogroup leave decisions.
- `AutoGroupInstanceLeaveRuntimeService` can register active autogroup instances, unregister leaving players, remove group/alliance membership for Java PvP subtype cleanup, and remove its registry entry when Java `destroyIfPossible` would destroy.

Still not proven or not implemented:

- Live C# delayed teleport leave wiring into `AutoGroupInstanceLeaveRuntimeService`.
- Java periodic registration refresh packet sends.
- Java quick-entry queue mutation and refill.
- Actual instance destruction through `InstanceDestroyWorkflowService` from autogroup leave.
- Java map leave callback parity: `ConquerorAndProtectorService.getInstance().onLeaveMap(player)`.
- Pet position update and same-map spawn parity in `TeleportService.SpawnTask.run`.

## Commits Made

- `[Phase 6][UOW-2354] Add autogroup leave runtime state`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupInstanceLeaveRuntimeService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupInstanceLeaveRuntimeServiceTests.cs`
- `docs/Phase-6-Session-2354-Completion.md`
- `docs/Phase-6-Session-2354-Handoff.md`

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~AutoGroupInstanceLeavePlanServiceTests" --no-restore
```

Result: passed 9, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Documentation hygiene:

```powershell
git diff --check
```

Result: passed.

Focused Java/Maven validation: skipped because no targeted Java unit fixture exists for autogroup leave runtime behavior; Java source review was used as source-of-truth evidence.

Broad-validation trigger: none.

Broad .NET decision: skipped full project/solution validation because the filtered runtime/planner tests compiled the affected project/dependencies and directly covered the scoped Java behavior.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.AutoGroupService.autoInstances` | `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService` | Runtime Registry | Partial | Unit Tested | Partial Parity | Minimal world/instance keyed registry exists for leave handling. Full queue, creation, quick-entry, and packet refresh runtime remains incomplete. |
| `com.aionemu.gameserver.services.AutoGroupService.onLeaveInstance(Player)` | `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService.OnLeaveInstance(...)` | Runtime Adapter | Partial | Unit Tested | Partial Parity | Planner is consumed and registered player/team cleanup is applied. Connection dispatch, periodic registration packets, quick-entry queue, and instance destruction remain unwired. |
| `com.aionemu.gameserver.model.autogroup.AutoPvpInstance.onLeaveInstance(Player)` | `AutoGroupInstanceLeaveRuntimeService` PvP branch | Runtime Branch | Partial | Unit Tested | Partial Parity | Registered player removal plus group/alliance removal covered against C# runtimes. Packet fanout from group/alliance leave workflows is not sent here. |
| `com.aionemu.gameserver.model.autogroup.AutoPvPFFAInstance.onLeaveInstance(Player)` | `AutoGroupInstanceLeaveRuntimeService` FFA branch | Runtime Branch | Partial | Unit Tested | Partial Parity | Registered player removal and destroy-if-empty registry removal covered. |

## Next Sequential UOW

Recommended next production scope: wire delayed teleport leave handling to `AutoGroupInstanceLeaveRuntimeService` after reset-warning packet selection, preserving Java `InstanceService.onLeaveInstance` order. Keep instance destruction/quick-entry/registration packet fanout as documented gaps unless the wiring can safely call a narrow existing service.

Java artifacts:

- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/services/teleport/TeleportService.java`
- `game-server/src/com/aionemu/gameserver/services/instance/PeriodicInstanceManager.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientSocketServer.cs`
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupInstanceLeaveRuntimeService.cs`
- `dotnetConversion/src/Aion.GameServer/Program.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupInstanceLeaveRuntimeServiceTests.cs`

Focused C# validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInstanceCooldownTests.HandleTeleportAnimationDoneAsync_SendsLeaveInstanceResetWarningBeforeSpawnLikeJavaSpawnTask|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~AutoGroupInstanceLeavePlanServiceTests" --no-restore
```

Before running, narrow the filter to the exact edited test name and closest adjacent test after discovery. If only documentation changes are made, use `git diff --check` instead.

Focused Java/Maven command: not expected unless a targeted Java fixture is added; Java source review is likely the practical evidence.

Broad-validation trigger: live connection dispatch wiring will apply if the next UOW wires `GameServerConnection`. Start with the focused dispatch/runtime tests above; skip full project/solution validation unless focused evidence exposes wider risk or another named trigger applies.

## Safe Candidates

- Wire `AutoGroupInstanceLeaveRuntimeService` into delayed teleport leave handling with focused packet/order/runtime tests.
- Add `PeriodicInstanceManager.checkAndSendOpenRegistrations` packet planner for opened-registration icon refresh.
- Add a narrow handoff from autogroup destroy plans to `InstanceDestroyWorkflowService` if the lifecycle risk can be isolated.
- Port or explicitly model `ConquerorAndProtectorService.onLeaveMap` if supporting C# state exists.
- Review pet position update and same-map spawn behavior in delayed teleport completion.

Avoid:

- Evidence/reporting-only units.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
