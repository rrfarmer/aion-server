# Phase 6 Session 2402 Handoff

## Current Phase
- Phase 6: Port Game Core.

## Last Completed UOW
- `UOW-2402`: Implemented recursive member-cleanup queue recheck during ready-match dispatch.

## Commits Made
- `[Phase 6][UOW-2402] Apply recursive ready-match recheck`

## Files Changed
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAutoGroupTests.cs`
- `docs/Phase-6-Session-2402-Completion.md`
- `docs/Phase-6-Session-2402-Handoff.md`

## Java Artifacts Touched
- `com.aionemu.gameserver.services.AutoGroupService`

## C# Artifacts Touched
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService`
- `Aion.GameServer.Tests.GameServerConnectionAutoGroupTests`
- Adjacent validation: `Aion.GameServer.Tests.AutoGroupLookingPartyRegistrationServiceTests`

## What Changed
- Added an optional cleanup-delivery hook to `ApplyReadyMatchPlanAsync(...)`.
- The connection ready-match helper now recursively rechecks cleanup masks that Java would pass to `checkQueueForNewMatches(maskId)`.
- Nested rechecks use the same static auto-group and instance-cooltime data as the original dispatch.
- Added a visited-mask guard for one dispatch chain.
- Added a connection regression proving cancel window `2` for the removed member is followed by nested mask `108` ready windows before the original mask `107` ready windows continue.

## Tests Run
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests" --no-restore`
  - Result: 22 passed, 0 failed, 0 skipped.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests" --no-restore`
  - Result: 66 passed, 0 failed, 0 skipped.
- Java/Maven: skipped because no targeted Java fixture exists and no Java source changed.
- Broad .NET: skipped after focused validation; the broad trigger was live connection/scheduler/runtime dispatch and was covered by the focused connection/service tests.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService` | Service | Partial | Unit Tested | Partial Parity | Ready-match apply now exposes cleanup delivery timing needed for Java-style nested rechecks. |
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Network.Aion.GameServerConnection` | Connection dispatch | Partial | Regression Tested | Partial Parity | Logout ready-match dispatch now covers initial ready match, leader cleanup, member cleanup, and recursive member-cleanup recheck ordering. |

## Known Gaps
- Multiple queued quick-entry candidates and failed capacity-refill ordering for open runtime quick-entry refill remain untested.
- Recursive recheck stress cases with repeated same-mask cascades are guarded but not exhaustively modeled.
- Overall Java `PlayerLeaveWorldService.leaveWorld(...)` ordering remains partial outside autogroup.
- Replacement readiness still needs broader real-client gameplay coverage beyond this autogroup slice.

## Next Recommended UOW
- `UOW-2403`: Cover open-runtime quick-entry refill ordering when multiple queued quick-entry candidates exist and the first candidate cannot be added because of capacity/race constraints.

## Suggested Discovery For UOW-2403
- Java:
  - `AutoGroupService.destroyOrAddPlayersFromQuickEntries(AutoInstance autoInstance)`
  - `AutoGroupService.checkQueueForQuickEntries(AutoInstance autoInstance)`
  - `AutoPvpInstance.addLookingForParty(...)`
- C#:
  - `AutoGroupLookingPartyRegistrationService.TryRefillQueuedQuickEntry(...)`
  - `AutoGroupInstanceLeaveRuntimeService.LeaveInstance(...)`
  - `GameServerConnection.ApplyAutoGroupCancelEnterAsync(...)`
  - `GameServerConnectionAutoGroupTests`
  - `AutoGroupLookingPartyRegistrationServiceTests`

## Focused Validation Recipe For Next UOW
- Specific behavior to validate: after a player leaves an open auto-group runtime instance, queued quick-entry refill scans Java-style order, skips candidates that cannot fit the current runtime, attaches the first addable candidate, sends ready window `4`, applies additional cleanup, and keeps skipped candidates queued.
- Focused C# command:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests" --no-restore`
- Focused Java/Maven command: not expected unless a targeted Java fixture is added; use Java source review by default.
- Broad-validation trigger: live connection dispatch/runtime refill if a connection-level test is added; still start with the focused filters above.

## Safe Candidate UOWs
- Add service-level quick-entry refill coverage first if the connection scenario needs too much fixture setup.
- Continue broader `PlayerLeaveWorldService.leaveWorld(...)` ordering slices outside autogroup.
- Audit penalty refresh scheduling order now that nested rechecks can occur before the original ready-match dispatch resumes.
