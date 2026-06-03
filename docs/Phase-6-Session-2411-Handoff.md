# Phase 6 Session 2411 Handoff

## Current Phase
- Phase 6: Port Game Core.

## Last Completed UOW
- `UOW-2411`: Added leave-world dead-player revive branch coverage and production wiring.

## Commits Made
- `[Phase 6][UOW-2411] Align dead-player leave-world revive`

## Files Changed
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerReviveRestoreService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionKiskReviveWorkflowTests.cs`
- `docs/Phase-6-Session-2411-Completion.md`
- `docs/Phase-6-Session-2411-Handoff.md`

## Java Artifacts Touched
- `com.aionemu.gameserver.services.player.PlayerLeaveWorldService`
- `com.aionemu.gameserver.services.player.PlayerReviveService`
- `com.aionemu.gameserver.services.teleport.TeleportService`

## C# Artifacts Touched
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Services.PlayerReviveRestoreService`
- `Aion.GameServer.Services.PlayerTeleportService`
- `Aion.GameServer.Tests.GameServerConnectionKiskReviveWorkflowTests`
- Adjacent validation:
  - `Aion.GameServer.Tests.PlayerReviveRestoreServiceTests`
  - `Aion.GameServer.Tests.PlayerReviveCleanupAdapterServiceTests`
  - `Aion.GameServer.Tests.PlayerTeleportServiceTests`

## What Changed
- Leave-world now revives dead players immediately after saving offline kisk binding, matching the local Java ordering slice.
- Open-world dead logout uses bind revive restore and bind-location movement.
- Instance-map dead logout uses runtime instance start position when present.
- The special `400030000` route is represented but still falls back to bind movement when the runtime map is not an instance type.
- Bind and instance revive restore constants/helpers were added next to the existing kisk revive restore helper.

## Tests Run
- Initial focused command failed during compile due a test-only `Assert.NotNull(...)` expression mistake; fixed in the same UOW.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionKiskReviveWorkflowTests|FullyQualifiedName~PlayerReviveRestoreServiceTests|FullyQualifiedName~PlayerReviveCleanupAdapterServiceTests" --no-restore`
  - Result after fix: 24 passed, 0 failed, 0 skipped.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionKiskReviveWorkflowTests|FullyQualifiedName~PlayerReviveRestoreServiceTests|FullyQualifiedName~PlayerReviveCleanupAdapterServiceTests|FullyQualifiedName~PlayerTeleportServiceTests" --no-restore`
  - Result: 30 passed, 0 failed, 0 skipped.
- Java/Maven: skipped because no targeted Java fixture exists and no Java source changed.
- Broad .NET: skipped after focused connection/revive/teleport tests passed; broad trigger was production connection/world-state behavior, but focused evidence isolated the change.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.player.PlayerLeaveWorldService` | `Aion.GameServer.Network.Aion.GameServerConnection` | Connection dispatch | Partial | Regression Tested | Partial Parity | Dead-player logout branch is wired after kisk offline binding. Full logout ordering and duel-loss branch remain partial. |
| `com.aionemu.gameserver.services.player.PlayerReviveService` | `Aion.GameServer.Services.PlayerReviveRestoreService` | Service | Partial | Unit Tested / Regression Tested | Partial Parity | Bind/instance 25% restore semantics are covered; soul sickness, instance handler consume, event/prison/Panesterra/vortex routing, and full packet fanout remain incomplete. |
| `com.aionemu.gameserver.services.teleport.TeleportService` | `Aion.GameServer.Services.PlayerTeleportService` | Service | Partial | Unit Tested / Regression Tested | Partial Parity | Logout branch uses modeled bind-location and instance-start position mutations. Full Java teleport lifecycle remains partial. |

## Known Gaps
- Full Java `PlayerLeaveWorldService.leaveWorld(...)` ordering remains partial.
- `PlayerReviveService.instanceRevive` `InstanceHandler.onReviveEvent` is not modeled in the logout branch.
- Soul sickness and several special revive destinations remain incomplete.
- Logout duel-loss branch and later inventory/warehouse owner cleanup are still unpinned.
- Replacement readiness still needs broader real-client gameplay coverage beyond this workflow slice.

## Next Recommended UOW
- `UOW-2412`: Continue leave-world ordering with the non-dead duel-loss branch: Java calls `DuelService.loseDuel(player)` when the player is not dead and is currently dueling.

## Suggested Discovery For UOW-2412
- Java:
  - `PlayerLeaveWorldService.leaveWorld(Player player)`
  - `DuelService.isDueling(Player player)`
  - `DuelService.loseDuel(Player player)`
- C#:
  - `GameServerConnection.LeavePlayerWorldAsync(...)`
  - Existing duel request/response/runtime services and tests.
  - Any player duel state fields or pending duel request cleanup currently modeled on `Player`.

## Focused Validation Recipe For Next UOW
- Specific behavior to validate: non-dead leave-world duel-loss branch follows Java by invoking the modeled duel-loss cleanup only when `DuelService.isDueling(player)` would be true; dead players should take the revive branch instead.
- Focused C# command:
  - Start with `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionKiskReviveWorkflowTests|FullyQualifiedName~Duel" --no-restore`
  - If the duel test class names differ or the filter is too broad, narrow to the edited logout workflow test plus the closest duel runtime/service test class.
- Focused Java/Maven command: not expected unless a targeted Java fixture is added; use Java source review by default.
- Broad-validation trigger: production connection/runtime-state changes may apply if live duel cleanup is wired; still start focused.

## Safe Candidate UOWs
- Add FindGroup logout cleanup connection wiring if source review identifies a narrow already-modeled service hook.
- Audit owner/null cleanup for inventory, warehouse, and account warehouse at the end of Java leave-world.
- Continue revive logout parity for instance-handler `onReviveEvent` only if a narrow modeled handler hook exists.
