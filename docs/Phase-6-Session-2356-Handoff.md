# Phase 6 Session 2356 Handoff - AutoGroup Destroy Workflow

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2356-Completion.md`
- `docs/Phase-6-Session-2356-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Prefer production/game parity units. Tests and docs should support concrete parity work, not become standalone evidence/reporting loops.

Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger before execution.

## Current State

Last completed UOW: UOW-2356, wired autogroup destroy decisions to `InstanceDestroyWorkflowService`.

Recent production parity slices:

- Delayed teleport leave sends Java reset-warning packets before position mutation/spawn packets.
- Instance leave handler is invoked before reset-warning message selection.
- Delayed teleport leave invokes autogroup runtime cleanup after reset-warning packet selection and before teleport completion.
- Autogroup leave runtime unregisters players, removes PvP group/alliance membership, removes registry state, and now invokes the existing instance destroy workflow when Java `destroyIfPossible` would destroy.
- Production DI passes shared group/alliance runtimes and `InstanceDestroyWorkflowService.DestroyInstance` into the shared autogroup runtime.

Still not proven or not implemented:

- Java periodic registration refresh packet sends after autogroup leave.
- Java quick-entry queue refill after autogroup leave.
- Full forced-exit packet fanout for instance destruction.
- Java map leave callback parity: `ConquerorAndProtectorService.getInstance().onLeaveMap(player)`.
- Pet position update and same-map spawn parity in `TeleportService.SpawnTask.run`.

## Commits Made

- `ae174019b [Phase 6][UOW-2354] Add autogroup leave runtime state`
- `64d588c7b [Phase 6][UOW-2355] Wire autogroup leave into teleport`
- `[Phase 6][UOW-2356] Invoke autogroup destroy workflow`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupInstanceLeaveRuntimeService.cs`
- `dotnetConversion/src/Aion.GameServer/Program.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupInstanceLeaveRuntimeServiceTests.cs`
- `docs/Phase-6-Session-2356-Completion.md`
- `docs/Phase-6-Session-2356-Handoff.md`

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~GameServerConnectionInstanceCooldownTests.HandleTeleportAnimationDoneAsync_InvokesAutoGroupLeaveAfterResetWarningLikeJavaInstanceService" --no-restore
```

Result: passed 6, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Focused Java/Maven validation: skipped because no targeted Java unit fixture exists for `AutoGroupService.destroyIfPossible`; Java source review was used as source-of-truth evidence.

Broad-validation trigger: live side-effect enabling.

Broad .NET decision: skipped full project/solution validation after focused runtime/connection tests passed and compiled the affected project/dependencies.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.AutoGroupService.destroyIfPossible(AutoInstance)` | `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService.OnLeaveInstance(...)` plus destroy callback | Live Runtime Adapter | Partial | Unit Tested | Partial Parity | C# now removes autogroup registry state before invoking the existing instance destroy workflow when Java would destroy. Quick-entry refill remains missing. |
| `com.aionemu.gameserver.services.instance.InstanceService.destroyInstance(WorldMapInstance)` | `Aion.GameServer.Services.InstanceDestroyWorkflowService.DestroyInstance(...)` via autogroup callback | Service | Partial | Unit Tested | Partial Parity | Existing destroy workflow is now reachable from autogroup leave. Forced-exit packet sends are still modeled by existing workflow plans rather than fully live fanout here. |
| `com.aionemu.gameserver.services.AutoGroupService.onLeaveInstance(Player)` | `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService.OnLeaveInstance(...)` | Live Runtime Adapter | Partial | Regression Tested | Partial Parity | Registered-player cleanup, group/alliance removal, registry removal, and destroy workflow callback are wired. Registration refresh and quick-entry refill remain gaps. |

## Next Sequential UOW

Recommended next production scope: port or model Java `PeriodicInstanceManager.checkAndSendOpenRegistrations(player)` enough for autogroup leave to emit registration-refresh decisions or packets.

Java artifacts:

- `game-server/src/com/aionemu/gameserver/services/instance/PeriodicInstanceManager.java`
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_AUTO_GROUP.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoGroupType.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupInstanceLeavePlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupInstanceLeaveRuntimeService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupInstanceLeaveRuntimeServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`

Focused C# validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~GamePacketTests" --no-restore
```

Before running, narrow `GamePacketTests` to the exact `SM_AUTO_GROUP` packet test if one exists or is added.

Behavior under validation: when autogroup leave reaches the Java refresh step, C# should expose the equivalent registration refresh packet/decision without broadening into queue matching.

Focused Java/Maven command: not expected unless a targeted Java packet fixture is added. If packet bytes are ported, prefer Java source review plus a focused C# packet serialization test.

Broad-validation trigger: packet shape if `SM_AUTO_GROUP` serialization changes. Start with focused packet/runtime tests; do not run full project/solution validation unless focused evidence exposes wider risk or the active notes name a concrete trigger.

## Safe Candidates

- Add/verify `SM_AUTO_GROUP` packet serialization and use it for registration refresh decisions.
- Add a narrow periodic registration refresh planner for `PeriodicInstanceManager.checkAndSendOpenRegistrations`.
- Port or model `ConquerorAndProtectorService.onLeaveMap` if supporting C# state exists.
- Review and port pet position update in delayed teleport completion.
- Review same-map delayed teleport spawn behavior.

Avoid:

- Evidence/reporting-only units.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
