# Phase 6 Session 2339 Handoff - Plan Instance Destroy Player Forced Exit

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2339-Completion.md`
- `docs/Phase-6-Session-2339-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

Last completed UOW: UOW-2339, modeled instance destroy now has a player forced-exit plan for Java's force-message and move-to-instance-exit request.

Relevant completed portal/instance/spawn/destroy slices:

- Fresh solo/group allocation paths spawn instance objects and notify `onInstanceCreate` after spawning.
- Portal difficulty is preserved on top-level allowed plans and team plans.
- Instance spawning filters by nonzero difficulty id and uses the allocated runtime instance id.
- Static doors and static handler objects are materialized for instance spawning.
- Temporary spawn tracking is per `(spawn, instanceId)`.
- Modeled destroy order includes map removal, optional temporary cleanup, optional non-player object cleanup, and destroy handler notification.
- Player forced-exit planning selects players in the destroyed instance and plans `STR_MSG_LEAVE_INSTANCE_FORCE(0)` plus `moveToInstanceExit`.

Still not proven or not implemented:

- Instance-exit destination lookup and bind-location fallback.
- Live forced-exit packet send and teleport dispatch.
- Live game-server destroy path wiring with all cleanup callbacks.
- Empty-instance checker scheduling and cancellation.
- Walker formation cleanup.
- Dynamic Java AI/instance handler discovery and per-map/per-AI handler class selection.

## Commits Made

- `[Phase 6][UOW-2339] Plan destroy player forced exit`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Services/InstanceRuntimeService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRuntimeStateTests.cs`
- `docs/Phase-6-Session-2339-Completion.md`
- `docs/Phase-6-Session-2339-Handoff.md`

Unrelated dirty file to preserve:

- `docs/parity-verification.md` was already dirty and should remain unstaged unless the user explicitly asks to edit it.

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRuntimeStateTests.InstanceRuntimeService_CreatePlayerForcedExitPlansSendsForceMessageAndMoveToInstanceExitLikeJava" --no-restore
```

Result: passed 1, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Focused Java/Maven validation: skipped because no targeted Java fixture exists for `InstanceService.destroyInstance(...)` or `TeleportService.moveToInstanceExit(...)`. Java source review was used as source-of-truth evidence.

Broad-validation trigger: none.

Broad .NET decision: skipped full project/solution validation because focused tests compiled the affected project and directly covered the changed service behavior.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.instance.InstanceService.destroyInstance` player branch | `Aion.GameServer.Services.InstanceRuntimeService.CreatePlayerForcedExitPlans` | Service Plan | Partial | Unit Tested | Partial Parity | C# selects players in the destroyed instance and plans force-message plus move-to-instance-exit request. Live packet send and teleport mutation remain pending. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_MSG_LEAVE_INSTANCE_FORCE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.LeaveInstanceForce` | Packet Factory | Complete | Unit Tested | Partial Parity | Message id and parameter are verified in focused plan test. Full byte-level golden serialization for this specific factory was not added. |
| `com.aionemu.gameserver.services.teleport.TeleportService.moveToInstanceExit` | `InstancePlayerForcedExitPlan.MoveToInstanceExitJavaSource` | Teleport Boundary | Not Started | No Tests | Needs Verification | C# records the required move request only. Instance-exit data lookup, bind-location fallback, and live teleport dispatch remain pending. |

## Next Sequential UOW

Recommended next production scope: port instance-exit destination lookup and bind-location fallback planning for Java `TeleportService.moveToInstanceExit(...)`, if C# static data for instance exits or bind locations is ready.

Java artifacts:

- `game-server/src/com/aionemu/gameserver/services/teleport/TeleportService.java`
- `game-server/src/com/aionemu/gameserver/dataholders/InstanceExitData.java`
- `game-server/src/com/aionemu/gameserver/model/templates/portal/InstanceExit.java`
- bind-location artifacts discovered during Work Discovery

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Dataholders/*InstanceExit*` if present or needing a small model
- `dotnetConversion/src/Aion.GameServer/Services/InstanceRuntimeService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerTeleportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRuntimeStateTests.cs`
- focused static-data parser tests if an instance-exit table is added

Focused C# validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRuntimeStateTests.InstanceRuntimeService_CreatePlayerForcedExitPlansSendsForceMessageAndMoveToInstanceExitLikeJava|FullyQualifiedName~PlayerTeleportServiceTests" --no-restore
```

Narrow after Work Discovery to exact instance-exit lookup tests. Do not run a full project/solution test unless a broad trigger is named first.

Focused Java/Maven command: not expected unless a targeted Java fixture is added; Java source review is likely the practical evidence.

Broad-validation trigger: none unless live teleport dispatch, packet fanout, or shared static-data loader changes are enabled.

## Safe Candidates

- Instance-exit lookup/fallback planning.
- Live forced-exit send/teleport dispatch if all required boundaries are already modeled.
- Live destroy wiring with temporary cleanup, player forced-exit planning, non-player cleanup, and handler notification.
- Walker formation cleanup on instance destroy.

Avoid:

- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Evidence/reporting-only units.
- Updating `docs/PHASE-6-PROGRESS.md`.

