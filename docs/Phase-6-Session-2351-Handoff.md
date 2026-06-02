# Phase 6 Session 2351 Handoff - Delayed Teleport Leave Warning

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2351-Completion.md`
- `docs/Phase-6-Session-2351-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Prefer production/game parity units. Tests and docs should support concrete parity work, not become standalone evidence/reporting loops.

Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger before execution.

## Current State

Last completed UOW: UOW-2351, wired the Java delayed teleport leave-instance reset-warning side effect into C# `HandleTeleportAnimationDoneAsync`.

Relevant completed instance destroy/checker/leave slices:

- `InstanceEmptyInstanceCheckerService` models Java `EmptyInstanceCheckerTask`.
- Portal instance allocation passes the real checker scheduler callback.
- `InstanceRegisteredTeamDisbandService` maps registered team ids to group/alliance runtime membership state.
- `InstanceLeaveMessageService` models Java reset-warning message selection for instance leave.
- `SmSystemMessage` has helpers for `STR_MSG_LEAVE_INSTANCE`, `STR_MSG_LEAVE_INSTANCE_PARTY`, and `STR_MSG_LEAVE_INSTANCE_FORCE`.
- Delayed teleport completion now sends the leave-instance reset-warning packet before C# position mutation and spawn packets when world or instance changes.

Still not proven or not implemented:

- Java map leave callback parity: `ConquerorAndProtectorService.getInstance().onLeaveMap(player)`.
- Java `InstanceService.onLeaveInstance` handler/autogroup callbacks beyond reset-warning packet selection.
- Pet position update and same-map spawn parity in `TeleportService.SpawnTask.run`.
- Other runtime instance creation call sites need real scheduler callback review.
- Live forced-exit packet send and teleport mutation.
- Dynamic handler/auto-group destroy call sites invoking `InstanceDestroyWorkflowService`.

## Commits Made

- `[Phase 6][UOW-2351] Send instance leave reset warning on teleport`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`
- `docs/Phase-6-Session-2351-Completion.md`
- `docs/Phase-6-Session-2351-Handoff.md`

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

Focused Java/Maven validation: skipped because no targeted Java unit fixture exists for `TeleportService.SpawnTask.run`; Java source review was used as source-of-truth evidence.

Broad-validation trigger: live side-effect and connection dispatch wiring.

Broad .NET decision: skipped full project/solution validation because the focused dispatch test compiled the affected project/dependencies and directly covered the scoped packet-order risk.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_TELEPORT_ANIMATION_DONE.runImpl()` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleTeleportAnimationDoneAsync(Player)` | Client Packet Handler | Partial | Regression Tested | Partial Parity | Delayed teleport completion now sends leave reset-warning before C# position mutation and spawn packets. Java task/exception behavior remains incomplete. |
| `com.aionemu.gameserver.services.teleport.TeleportService.SpawnTask.run()` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleTeleportAnimationDoneAsync(Player)` / `Aion.GameServer.Services.PlayerTeleportService.CompletePendingTeleport(Player)` | Teleport Runtime | Partial | Regression Tested | Partial Parity | World/instance-change reset-warning ordering is covered. Java map leave, pet position update, same-map spawn path, and handler callbacks remain incomplete. |
| `com.aionemu.gameserver.services.instance.InstanceService.onLeaveInstance(Player)` | `Aion.GameServer.Services.InstanceLeaveMessageService.CreateLeaveMessagePlan(...)` plus `GameServerConnection.SendInstanceLeaveMessageIfNeededAsync(...)` | Service Planner / Runtime Wiring | Partial | Unit Tested / Regression Tested | Partial Parity | Reset-warning branch order and delayed-teleport wiring are covered. Java instance handler and autogroup leave callbacks remain unwired. |

## Next Sequential UOW

Recommended next production scope: inspect and port the next concrete Java `TeleportService.SpawnTask.run` side effect that can be safely isolated. Good first candidate is map leave callback parity or pet/same-map spawn parity, depending on which C# runtime services already exist.

Java artifacts:

- `game-server/src/com/aionemu/gameserver/services/teleport/TeleportService.java`
- `game-server/src/com/aionemu/gameserver/services/conquerorAndProtectorSystem/ConquerorAndProtectorService.java`
- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/Player.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerTeleportService.cs`
- any existing conquest/protector/map-leave service equivalents found by discovery
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerTeleportServiceTests.cs`

Focused C# validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInstanceCooldownTests|FullyQualifiedName~PlayerTeleportServiceTests" --no-restore
```

Before running, narrow the filter to the exact edited test name and closest adjacent test after discovery. If only documentation changes are made, use `git diff --check` instead.

Focused Java/Maven command: not expected unless a targeted Java fixture is added; Java source review is likely the practical evidence.

Broad-validation trigger: none until the next UOW changes shared position mutation, connection dispatch, scheduler, persistence, packet primitives, or live handler wiring. If such a trigger applies, write it into the active completion/handoff notes before any broad .NET run.

## Safe Candidates

- Port or explicitly model `ConquerorAndProtectorService.onLeaveMap` if supporting C# state exists.
- Add a narrow adapter for Java `InstanceService.onLeaveInstance` handler/autogroup callbacks if C# handler runtime has enough shape.
- Review pet position update and same-map spawn behavior in delayed teleport completion.
- Wire another runtime instance creation call site to the checker callback.
- Add a narrow live forced-exit adapter if packet-send and teleport mutation boundaries are ready.

Avoid:

- Evidence/reporting-only units.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
