# Phase 6 Session 2355 Handoff - AutoGroup Leave Live Wiring

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2355-Completion.md`
- `docs/Phase-6-Session-2355-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Prefer production/game parity units. Tests and docs should support concrete parity work, not become standalone evidence/reporting loops.

Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger before execution.

## Current State

Last completed UOW: UOW-2355, wired delayed teleport leave handling into `AutoGroupInstanceLeaveRuntimeService`.

Recent production parity slices:

- Delayed teleport leave sends Java reset-warning packets before position mutation/spawn packets.
- Instance leave handler is invoked before reset-warning message selection.
- Autogroup leave planning/runtime state can unregister players, remove PvP group/alliance membership, and remove registry state when Java `destroyIfPossible` would destroy.
- Delayed teleport leave now invokes the autogroup runtime after reset-warning selection and before teleport completion.
- Production DI registers one shared autogroup runtime using shared group/alliance runtimes; `GameClientSocketServer` passes it into each connection.

Still not proven or not implemented:

- Java periodic registration refresh packet sends after autogroup leave.
- Java quick-entry queue refill after autogroup leave.
- Actual instance destruction through `InstanceDestroyWorkflowService` from autogroup leave.
- Java map leave callback parity: `ConquerorAndProtectorService.getInstance().onLeaveMap(player)`.
- Pet position update and same-map spawn parity in `TeleportService.SpawnTask.run`.

## Commits Made

- `ae174019b [Phase 6][UOW-2354] Add autogroup leave runtime state`
- `[Phase 6][UOW-2355] Wire autogroup leave into teleport`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientSocketServer.cs`
- `dotnetConversion/src/Aion.GameServer/Program.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`
- `docs/Phase-6-Session-2355-Completion.md`
- `docs/Phase-6-Session-2355-Handoff.md`

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInstanceCooldownTests.HandleTeleportAnimationDoneAsync_InvokesAutoGroupLeaveAfterResetWarningLikeJavaInstanceService|FullyQualifiedName~GameServerConnectionInstanceCooldownTests.HandleTeleportAnimationDoneAsync_SendsLeaveInstanceResetWarningBeforeSpawnLikeJavaSpawnTask|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~AutoGroupInstanceLeavePlanServiceTests" --no-restore
```

Result: passed 11, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Documentation hygiene:

```powershell
git diff --check
```

Result: passed. Git emitted CRLF working-copy warnings only.

Focused Java/Maven validation: skipped because no targeted Java unit fixture exists for this teleport/autogroup path; Java source review was used as source-of-truth evidence.

Broad-validation trigger: live connection dispatch wiring.

Broad .NET decision: skipped full project/solution validation after focused dispatch/runtime tests passed and compiled the affected project/dependencies.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.instance.InstanceService.onLeaveInstance(Player)` | `Aion.GameServer.Network.Aion.GameServerConnection.SendInstanceLeaveMessageIfNeededAsync(...)` | Live Adapter | Partial | Regression Tested | Partial Parity | C# now preserves handler, reset-warning packet, and autogroup cleanup order for delayed teleport leave. `ConquerorAndProtectorService.onLeaveMap` and full instance destroy side effects remain missing. |
| `com.aionemu.gameserver.services.AutoGroupService.onLeaveInstance(Player)` | `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService.OnLeaveInstance(...)` via `GameServerConnection` | Live Runtime Adapter | Partial | Regression Tested | Partial Parity | Live delayed teleport leave now invokes registered-player cleanup and PvP group/alliance removal. Quick-entry refill, periodic registration refresh packets, and actual instance destruction remain unwired. |
| `com.aionemu.gameserver.services.teleport.TeleportService.SpawnTask.run` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleTeleportAnimationDoneAsync(Player)` | Teleport Dispatch | Partial | Regression Tested | Partial Parity | Leave-side autogroup cleanup occurs before C# position mutation and spawn packets. Pet position update and same-map spawn branch remain separate gaps. |
| `com.aionemu.gameserver.model.autogroup.AutoPvpInstance.onLeaveInstance(Player)` | `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService` PvP branch via connection | Runtime Branch | Partial | Unit Tested | Partial Parity | Leaving autogroup player is removed from group/alliance runtime when delayed teleport leaves the instance. Packet fanout is not fully modeled here. |

## Next Sequential UOW

Recommended next production scope: wire autogroup destroy decisions to the existing `InstanceDestroyWorkflowService` when Java `AutoGroupService.destroyIfPossible` would destroy the instance. Keep quick-entry refill and registration refresh packets as separate gaps unless a narrow existing service already supports them.

Java artifacts:

- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java` `destroyInstance`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoInstance.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupInstanceLeaveRuntimeService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/InstanceDestroyWorkflowService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupInstanceLeaveRuntimeServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`

Focused C# validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~GameServerConnectionInstanceCooldownTests.HandleTeleportAnimationDoneAsync_InvokesAutoGroupLeaveAfterResetWarningLikeJavaInstanceService" --no-restore
```

Behavior under validation: when autogroup leave unregisters the final registered player and no players remain online inside, C# should remove autogroup registry state and, if wired in the next UOW, call the existing instance destroy workflow in Java order.

Focused Java/Maven command: not expected unless a targeted Java fixture is added; Java source review is likely the practical evidence.

Broad-validation trigger: likely live side-effect enabling if the next UOW invokes instance destruction. Start with focused runtime/connection tests; run broader validation only if focused evidence exposes wider risk or the active notes name a concrete trigger.

## Safe Candidates

- Wire autogroup destroy plans to `InstanceDestroyWorkflowService` with a focused runtime/connection test.
- Add a narrow periodic registration refresh packet planner for `PeriodicInstanceManager.checkAndSendOpenRegistrations`.
- Port or model `ConquerorAndProtectorService.onLeaveMap` if supporting C# state exists.
- Review and port pet position update in delayed teleport completion.
- Review same-map delayed teleport spawn behavior.

Avoid:

- Evidence/reporting-only units.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
