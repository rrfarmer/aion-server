# Phase 6 Session 2341 Completion - Compose Destroy Player Exit Resolution

## Scope

Composed the existing forced-leave message plan with the modeled instance-exit destination/fallback plan for Java `InstanceService.destroyInstance(...)`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/services/teleport/TeleportService.java`
- `game-server/src/com/aionemu/gameserver/dataholders/InstanceExitData.java`

Java behavior used:

- `destroyInstance(...)` iterates objects in the destroyed instance.
- For each `Player`, Java sends `SM_SYSTEM_MESSAGE.STR_MSG_LEAVE_INSTANCE_FORCE(0)`.
- Java then calls `moveToExitPoint(player)`, which calls `TeleportService.moveToInstanceExit(player, player.getWorldId(), player.getRace())`.
- `moveToInstanceExit(...)` either teleports to a valid instance-exit destination or falls back to bind location.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Services/InstanceRuntimeService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRuntimeStateTests.cs`

Implemented:

- Added `CreatePlayerForcedExitResolutionPlans(...)` to compose each destroyed-instance player's force-message plan with Java-shaped exit resolution.
- Added `InstancePlayerForcedExitResolutionPlan` to carry the forced message, exit resolution, and Java source breadcrumb.
- Added a focused test proving player filtering, forced-message id/parameter, successful destination resolution, and bind fallback composition.

Known limitations:

- This remains a non-live plan; it does not send packets or mutate player positions.
- Bind-location coordinate resolution is still not modeled.
- Live destroy wiring still needs to apply temporary cleanup, non-player cleanup, player forced exit, handler notification, and walker cleanup through the real server path.

## Validation Decision

- Changed surface: non-live instance destroy player-exit planner.
- Specific behavior/contract: destroyed-instance players get Java's force-leave message and an exit-resolution decision equivalent to `moveToInstanceExit`; players in other instance ids are excluded.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRuntimeStateTests.InstanceRuntimeService_CreatePlayerForcedExitPlansSendsForceMessageAndMoveToInstanceExitLikeJava|FullyQualifiedName~WorldMapRuntimeStateTests.InstanceRuntimeService_ResolveInstanceExitUsesRaceExitAndFallsBackToBindLikeJavaTeleportService|FullyQualifiedName~WorldMapRuntimeStateTests.InstanceRuntimeService_CreatePlayerForcedExitResolutionPlansComposesMessageAndExitLikeJavaDestroy" --no-restore
```

Result: passed 3, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for this destroy-player branch; Java source review was used as source-of-truth evidence.
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
| `com.aionemu.gameserver.services.instance.InstanceService.destroyInstance` player branch | `Aion.GameServer.Services.InstanceRuntimeService.CreatePlayerForcedExitResolutionPlans` | Service Plan | Partial | Unit Tested | Partial Parity | C# now composes Java's force-leave packet intent with Java-shaped exit destination/fallback planning for players in the destroyed instance. Live packet send and teleport mutation remain pending. |
| `com.aionemu.gameserver.services.teleport.TeleportService.moveToInstanceExit` | `Aion.GameServer.Services.InstanceRuntimeService.ResolveInstanceExit` | Service Plan | Partial | Unit Tested | Partial Parity | Reused from UOW-2340. Destination/fallback branch is tested, but live teleport and bind-location coordinate resolution remain pending. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `InstanceRuntimeService_CreatePlayerForcedExitResolutionPlansComposesMessageAndExitLikeJavaDestroy` | Unit | Java source review | Only players in the destroyed instance are planned; each plan has message id `1400046`; ELYOS resolves to destination; ASMODIANS falls back when exit-world instance `1` is unavailable. | Focused C# test plus Java source review. | Does not dispatch packet or perform teleport mutation. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 2
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 2
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; overall completion unchanged conservatively.

## Remaining Gaps

- Live forced-exit packet send and teleport dispatch.
- Bind-location coordinate resolution.
- Live destroy wiring with all cleanup callbacks and handler notifications.
- Walker formation cleanup on instance destroy.
- Empty-instance checker scheduling/cancellation.

## Commit

Commit message:

```text
[Phase 6][UOW-2341] Compose destroy player exit resolution
```

