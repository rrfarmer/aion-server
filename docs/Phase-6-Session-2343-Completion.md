# Phase 6 Session 2343 Completion - Compose Forced Exit Destination

## Scope

Composed Java instance-exit success and bind-location fallback destination planning for destroyed-instance player forced exits.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/services/teleport/TeleportService.java`
- `game-server/src/com/aionemu/gameserver/dataholders/InstanceExitData.java`
- `game-server/src/com/aionemu/gameserver/dataholders/PlayerInitialData.java`

Java behavior used:

- `destroyInstance(...)` sends `STR_MSG_LEAVE_INSTANCE_FORCE(0)` and calls `moveToExitPoint(player)`.
- `moveToExitPoint(player)` calls `TeleportService.moveToInstanceExit(player, player.getWorldId(), player.getRace())`.
- `moveToInstanceExit(...)` uses the valid instance-exit destination when the exit world instance `1` exists.
- Otherwise Java calls `moveToBindLocation(player)`, which chooses player bind point first and race initial spawn second.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Services/InstanceRuntimeService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRuntimeStateTests.cs`

Implemented:

- Added `CreatePlayerForcedExitTeleportPlans(...)` to compose forced message intent, instance-exit resolution, bind fallback resolution, and final modeled destination.
- Added `InstancePlayerForcedExitTeleportPlan`.
- Added a focused test covering one Java instance-exit destination and one Java bind-point fallback destination in the same destroyed-instance branch.

Known limitations:

- This remains a non-live plan; it does not send packets or mutate player position.
- Bind fallback with missing initial spawn remains a modeled-data gap.
- Live game-server destroy wiring still needs to consume these plans.

## Validation Decision

- Changed surface: non-live instance destroy player teleport destination planner.
- Specific behavior/contract: destroyed-instance forced-exit plans resolve Java `moveToInstanceExit` success to an instance-exit destination and Java fallback to a concrete bind-location destination.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRuntimeStateTests.InstanceRuntimeService_CreatePlayerForcedExitResolutionPlansComposesMessageAndExitLikeJavaDestroy|FullyQualifiedName~WorldMapRuntimeStateTests.InstanceRuntimeService_CreatePlayerForcedExitTeleportPlansComposesBindFallbackDestinationLikeJava|FullyQualifiedName~PlayerTeleportServiceTests.ResolveBindLocation" --no-restore
```

Result: passed 5, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for the combined destroy/teleport/bind branch; Java source review was used as source-of-truth evidence.
- Broad-validation trigger: none. This UOW adds a non-live composition planner only.
- Broad .NET decision: skipped full project/solution validation; the filtered command built the affected project and covered the edited behavior.

Repository hygiene:

```powershell
git diff --check
```

Result: passed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.instance.InstanceService.destroyInstance` player branch | `Aion.GameServer.Services.InstanceRuntimeService.CreatePlayerForcedExitTeleportPlans` | Service Plan | Partial | Unit Tested | Partial Parity | C# now models force message, instance-exit destination, and bind fallback destination for destroyed-instance players. Live packet send, teleport mutation, and destroy-path integration remain pending. |
| `com.aionemu.gameserver.services.teleport.TeleportService.moveToInstanceExit` | `Aion.GameServer.Services.InstanceRuntimeService.CreatePlayerForcedExitTeleportPlans` | Service Plan | Partial | Unit Tested | Partial Parity | C# composes valid exit-world destination with bind fallback. Runtime teleport behavior and bind missing-data exception behavior are not proven. |
| `com.aionemu.gameserver.services.teleport.TeleportService.moveToBindLocation` | `Aion.GameServer.Services.PlayerTeleportService.ResolveBindLocation` | Service Plan | Partial | Unit Tested | Partial Parity | Reused by forced-exit teleport planner. Destination selection is tested; live mutation is pending. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `InstanceRuntimeService_CreatePlayerForcedExitTeleportPlansComposesBindFallbackDestinationLikeJava` | Unit | Java source review | Destroyed-player plans include a direct instance-exit destination when available and a bind-point destination when the exit world instance is missing. | Focused C# test plus Java source review. | Does not send packet or mutate player position. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 3
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; overall completion unchanged conservatively.

## Remaining Gaps

- Live forced-exit packet send and teleport dispatch.
- Live destroy wiring with all cleanup callbacks and handler notifications.
- Walker formation cleanup on instance destroy.
- Empty-instance checker scheduling/cancellation.
- Exact Java missing-initial-data failure behavior.

## Commit

Commit message:

```text
[Phase 6][UOW-2343] Compose forced exit destination
```

