# Phase 6 Session 2339 Completion - Plan Instance Destroy Player Forced Exit

## Scope

Ported a modeled player forced-exit plan for Java `InstanceService.destroyInstance(...)` without enabling live teleport dispatch.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/services/teleport/TeleportService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
- `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`

Java behavior used:

- For each player in a destroyed instance, Java sends `SM_SYSTEM_MESSAGE.STR_MSG_LEAVE_INSTANCE_FORCE(0)`.
- Java then calls `TeleportService.moveToInstanceExit(player, player.getWorldId(), player.getRace())`.
- `moveToInstanceExit` resolves `InstanceExitData`; if no valid exit exists, it falls back to bind-location movement.

This UOW only models the forced-exit packet plus move-to-instance-exit request. Destination resolution and live teleport/send dispatch remain pending.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Services/InstanceRuntimeService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRuntimeStateTests.cs`

Implemented:

- Added `SmSystemMessage.LeaveInstanceForce(int)` for Java message id `1400046`.
- Exposed read-only `SmSystemMessage.Parameters` so focused tests can verify Java message parameters without packet serialization.
- Added `InstanceRuntimeService.CreatePlayerForcedExitPlans(...)` to select players in the target map instance and produce Java-shaped forced-exit plans.

Known limitations:

- The plan does not yet resolve `InstanceExitData`.
- The plan does not send packets or mutate player positions.
- Live destroy wiring still needs to compose forced-exit plans with temporary cleanup and non-player cleanup.

## Validation Decision

- Changed surface: modeled instance destroy player-exit planning and system-message packet factory.
- Specific behavior/contract: players in the destroyed instance receive a forced-leave message id `1400046` with parameter `0`, plus a `moveToInstanceExit` request.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRuntimeStateTests.InstanceRuntimeService_CreatePlayerForcedExitPlansSendsForceMessageAndMoveToInstanceExitLikeJava" --no-restore
```

Result: passed 1, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for `InstanceService.destroyInstance(...)` or `TeleportService.moveToInstanceExit(...)`; Java source review was used as source-of-truth evidence.
- Broad-validation trigger: none. This is a modeled planning slice and does not enable live teleport dispatch.
- Broad .NET decision: skipped full project/solution validation; the filtered test built the affected project and covered the edited behavior.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.instance.InstanceService.destroyInstance` player branch | `Aion.GameServer.Services.InstanceRuntimeService.CreatePlayerForcedExitPlans` | Service Plan | Partial | Unit Tested | Partial Parity | C# selects players in the destroyed instance and plans force-message plus move-to-instance-exit request. Live packet send and teleport mutation remain pending. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_MSG_LEAVE_INSTANCE_FORCE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.LeaveInstanceForce` | Packet Factory | Complete | Unit Tested | Partial Parity | Message id and parameter are verified in focused plan test. Full byte-level golden serialization for this specific factory was not added. |
| `com.aionemu.gameserver.services.teleport.TeleportService.moveToInstanceExit` | `InstancePlayerForcedExitPlan.MoveToInstanceExitJavaSource` | Teleport Boundary | Not Started | No Tests | Needs Verification | C# records the required move request only. Instance-exit data lookup, bind-location fallback, and live teleport dispatch remain pending. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `InstanceRuntimeService_CreatePlayerForcedExitPlansSendsForceMessageAndMoveToInstanceExitLikeJava` | Unit | Java source review | Only players in the destroyed instance are planned; message id `1400046`, parameter `0`, and move-to-instance-exit request are present. | Focused C# test plus Java source review. | Does not resolve destinations or dispatch packets/teleports. |

## Remaining Gaps

- Instance-exit destination lookup and bind-location fallback.
- Live forced-exit packet send and teleport dispatch.
- Live destroy wiring with all cleanup callbacks.
- Empty-instance checker scheduling/cancellation.
- Walker formation cleanup on instance destroy.

## Commit

Commit message:

```text
[Phase 6][UOW-2339] Plan destroy player forced exit
```

