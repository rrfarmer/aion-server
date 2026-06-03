# Phase 6 Session 2410 Handoff

## Current Phase
- Phase 6: Port Game Core.

## Last Completed UOW
- `UOW-2410`: Added leave-world kisk offline-binding connection coverage.

## Commits Made
- `[Phase 6][UOW-2410] Cover leave-world kisk offline binding`

## Files Changed
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionKiskReviveWorkflowTests.cs`
- `docs/Phase-6-Session-2410-Completion.md`
- `docs/Phase-6-Session-2410-Handoff.md`

## Java Artifacts Touched
- `com.aionemu.gameserver.services.player.PlayerLeaveWorldService`
- `com.aionemu.gameserver.services.KiskService`

## C# Artifacts Touched
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Services.PlayerKiskRegistry`
- `Aion.GameServer.Tests.GameServerConnectionKiskReviveWorkflowTests`
- Adjacent validation: `Aion.GameServer.Tests.PlayerKiskRegistryTests`

## What Changed
- Added a connection-level leave-world regression for `KiskService.onLogout` parity.
- The test registers a runtime kisk, logs out a player bound to it, restores the binding on a returning player, and asserts the offline binding is consumed.
- No production code changed.

## Tests Run
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionKiskReviveWorkflowTests|FullyQualifiedName~PlayerKiskRegistryTests" --no-restore`
  - Result: 18 passed, 0 failed, 0 skipped.
- Java/Maven: skipped because no targeted Java fixture exists and no Java source changed.
- Broad .NET: skipped because this was a test-only focused regression with no broad-validation trigger.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.player.PlayerLeaveWorldService` | `Aion.GameServer.Network.Aion.GameServerConnection` | Connection dispatch | Partial | Regression Tested | Partial Parity | Leave-world now covers the kisk offline-binding side effect; full logout sequence remains partial. |
| `com.aionemu.gameserver.services.KiskService` | `Aion.GameServer.Services.PlayerKiskRegistry` | Service | Partial | Unit Tested / Regression Tested | Partial Parity | Offline binding registration and one-shot restore are covered from registry and connection paths. |

## Known Gaps
- Full Java `PlayerLeaveWorldService.leaveWorld(...)` ordering remains partial.
- Kisk logout ordering relative to dead-player revive handling is not pinned.
- Many Java logout services remain unported or only partially modeled in C#.
- Replacement readiness still needs broader real-client gameplay coverage beyond this workflow slice.

## Next Recommended UOW
- `UOW-2411`: Continue leave-world ordering with the dead-player logout branch: Java revives dead players after kisk/GM/instance logout service calls, using instance revive for instances or map `400030000`, otherwise bind revive.

## Suggested Discovery For UOW-2411
- Java:
  - `PlayerLeaveWorldService.leaveWorld(Player player)`
  - `PlayerReviveService.instanceRevive(Player player)`
  - `PlayerReviveService.bindRevive(Player player)`
- C#:
  - `GameServerConnection.LeavePlayerWorldAsync(...)`
  - `PlayerReviveService` / revive workflow services in `dotnetConversion/src/Aion.GameServer/Services`
  - `GameServerConnectionKiskReviveWorkflowTests`
  - Any revive service tests adjacent to logout/death behavior.

## Focused Validation Recipe For Next UOW
- Specific behavior to validate: dead-player leave-world revive branch follows Java rules for instance/map `400030000` versus bind revive, or document the current missing production gap if not yet implemented.
- Focused C# command:
  - Start with `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionKiskReviveWorkflowTests|FullyQualifiedName~PlayerKiskReviveServiceTests|FullyQualifiedName~PlayerReviveCleanupAdapterServiceTests" --no-restore`
  - If class names differ, use the edited revive/logout test class plus closest revive service tests.
- Focused Java/Maven command: not expected unless a targeted Java fixture is added; use Java source review by default.
- Broad-validation trigger: production leave-world/revive changes may cross world-state and player-state surfaces; still start with focused revive/logout tests.

## Safe Candidate UOWs
- Add FindGroup logout cleanup connection wiring if source review identifies a narrow already-modeled service hook.
- Audit owner/null cleanup for inventory, warehouse, and account warehouse at the end of Java leave-world.
- Continue autogroup edge coverage only if a concrete missing Java branch is found.
