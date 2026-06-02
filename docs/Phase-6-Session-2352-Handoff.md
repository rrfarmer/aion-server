# Phase 6 Session 2352 Handoff - Instance Leave Handler Hook

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2352-Completion.md`
- `docs/Phase-6-Session-2352-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Prefer production/game parity units. Tests and docs should support concrete parity work, not become standalone evidence/reporting loops.

Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger before execution.

## Current State

Last completed UOW: UOW-2352, added the C# instance leave handler hook and invoked it from delayed teleport leave handling before reset-warning message selection.

Relevant completed instance destroy/checker/leave slices:

- `InstanceEmptyInstanceCheckerService` models Java `EmptyInstanceCheckerTask`.
- Portal instance allocation passes the real checker scheduler callback.
- `InstanceRegisteredTeamDisbandService` maps registered team ids to group/alliance runtime membership state.
- `InstanceLeaveMessageService` models Java reset-warning message selection for instance leave.
- `SmSystemMessage` has helpers for `STR_MSG_LEAVE_INSTANCE`, `STR_MSG_LEAVE_INSTANCE_PARTY`, and `STR_MSG_LEAVE_INSTANCE_FORCE`.
- Delayed teleport completion sends the leave-instance reset-warning packet before C# position mutation and spawn packets when world or instance changes.
- Delayed teleport leave handling now invokes `IInstanceLifecycleHandler.OnLeaveInstance(Player)` before reset-warning message planning.

Still not proven or not implemented:

- Java `AutoGroupService.onLeaveInstance(player)` callback.
- Java map leave callback parity: `ConquerorAndProtectorService.getInstance().onLeaveMap(player)`.
- Pet position update and same-map spawn parity in `TeleportService.SpawnTask.run`.
- Other runtime instance creation call sites need real scheduler callback review.
- Live forced-exit packet send and teleport mutation.
- Dynamic handler/auto-group destroy call sites invoking `InstanceDestroyWorkflowService`.

## Commits Made

- `[Phase 6][UOW-2352] Invoke instance leave handler on teleport`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/World/IInstanceLifecycleHandler.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`
- `docs/Phase-6-Session-2352-Completion.md`
- `docs/Phase-6-Session-2352-Handoff.md`

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInstanceCooldownTests.HandleTeleportAnimationDoneAsync_SendsLeaveInstanceResetWarningBeforeSpawnLikeJavaSpawnTask|FullyQualifiedName~InstanceLeaveMessageServiceTests" --no-restore
```

Result: passed 2, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Documentation hygiene:

```powershell
git diff --check
```

Result: passed. Git reported line-ending normalization warnings only.

Focused Java/Maven validation: skipped because no targeted Java unit fixture exists for `InstanceService.onLeaveInstance`; Java source review was used as source-of-truth evidence.

Broad-validation trigger: live side-effect and connection dispatch wiring.

Broad .NET decision: skipped full project/solution validation because the focused dispatch test compiled the affected project/dependencies and directly covered the scoped handler-order risk.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.instance.handlers.InstanceHandler.onLeaveInstance(Player)` | `Aion.GameServer.World.IInstanceLifecycleHandler.OnLeaveInstance(Player)` | Interface Hook | Partial | Regression Tested | Partial Parity | Hook is represented and covered on delayed teleport leave; broader handler runtime and implementations remain incomplete. |
| `com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler.onLeaveInstance(Player)` | `Aion.GameServer.World.GeneralInstanceLifecycleHandler.OnLeaveInstance(Player)` | Handler | Complete | Regression Tested | Verified Parity | Java and C# default handlers are no-ops. |
| `com.aionemu.gameserver.services.instance.InstanceService.onLeaveInstance(Player)` | `Aion.GameServer.Network.Aion.GameServerConnection.SendInstanceLeaveMessageIfNeededAsync(...)` plus `InstanceLeaveMessageService.CreateLeaveMessagePlan(...)` | Runtime Wiring / Planner | Partial | Unit Tested / Regression Tested | Partial Parity | Handler callback and reset-warning packet selection are wired for delayed teleport. Java autogroup callback remains unwired. |

## Next Sequential UOW

Recommended next production scope: inspect and port the next smallest Java `InstanceService.onLeaveInstance` callback that has enough C# runtime shape. The likely candidate is an autogroup leave planner or narrow runtime adapter if existing `PlayerGroupRuntime`/`AutoGroupTable` state can model Java `AutoGroupService.onLeaveInstance`.

Java artifacts:

- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoInstance.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoInstanceHandler.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/PlayerGroupRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/AutoGroupTable.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- any existing autogroup planner/service found by discovery
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerGroupRuntimeTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`

Focused C# validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInstanceCooldownTests.HandleTeleportAnimationDoneAsync_SendsLeaveInstanceResetWarningBeforeSpawnLikeJavaSpawnTask|FullyQualifiedName~PlayerGroupRuntimeTests" --no-restore
```

Before running, narrow the filter to the exact edited test name and closest adjacent test after discovery. If only documentation changes are made, use `git diff --check` instead.

Focused Java/Maven command: not expected unless a targeted Java fixture is added; Java source review is likely the practical evidence.

Broad-validation trigger: none until the next UOW changes shared position mutation, connection dispatch, scheduler, persistence, packet primitives, or live handler wiring. If such a trigger applies, write it into the active completion/handoff notes before any broad .NET run.

## Safe Candidates

- Add a narrow autogroup leave planner or runtime adapter if C# state can safely represent Java `AutoGroupService.onLeaveInstance`.
- Port or explicitly model `ConquerorAndProtectorService.onLeaveMap` if supporting C# state exists.
- Review pet position update and same-map spawn behavior in delayed teleport completion.
- Wire another runtime instance creation call site to the checker callback.
- Add a narrow live forced-exit adapter if packet-send and teleport mutation boundaries are ready.

Avoid:

- Evidence/reporting-only units.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
