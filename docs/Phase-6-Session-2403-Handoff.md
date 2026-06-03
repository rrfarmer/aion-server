# Phase 6 Session 2403 Handoff

## Current Phase
- Phase 6: Port Game Core.

## Last Completed UOW
- `UOW-2403`: Added quick-entry refill coverage for skipping rejected queued candidates and attaching the next addable candidate.

## Commits Made
- `[Phase 6][UOW-2403] Cover quick-entry refill skip ordering`

## Files Changed
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupLookingPartyRegistrationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`
- `docs/Phase-6-Session-2403-Completion.md`
- `docs/Phase-6-Session-2403-Handoff.md`

## Java Artifacts Touched
- `com.aionemu.gameserver.services.AutoGroupService`
- `com.aionemu.gameserver.services.instance.AutoPvpInstance`

## C# Artifacts Touched
- `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService`
- `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService`
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Tests.AutoGroupLookingPartyRegistrationServiceTests`
- `Aion.GameServer.Tests.GameServerConnectionInstanceCooldownTests`
- Adjacent validation: `Aion.GameServer.Tests.AutoGroupInstanceLeaveRuntimeServiceTests`

## What Changed
- Added a service-level refill test where a queued Asmodian quick-entry party is rejected by per-race capacity and remains queued.
- Extended the live leave-instance refill regression with the same rejected candidate before the accepted Elyos quick-entry party.
- Verified accepted candidate packet delivery remains ready window `4` followed by additional-registration cleanup windows, while the rejected candidate is not registered and receives no packet.

## Tests Run
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~GameServerConnectionInstanceCooldownTests" --no-restore`
  - Result: 64 passed, 0 failed, 0 skipped.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInstanceCooldownTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests" --no-restore`
  - Result: 79 passed, 0 failed, 0 skipped.
- Java/Maven: skipped because no targeted Java fixture exists and no Java source changed.
- Broad .NET: skipped; focused service/connection/runtime filters covered the scoped risk.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService` | Service | Partial | Unit Tested | Partial Parity | Quick-entry refill scan now covers rejected candidate skip/keep-queued behavior. |
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Network.Aion.GameServerConnection` | Connection dispatch | Partial | Regression Tested | Partial Parity | Live leave-instance refill dispatch now covers skipped candidate state plus accepted candidate packets/runtime attachment. |
| `com.aionemu.gameserver.services.instance.AutoPvpInstance` | `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService` | Runtime service | Partial | Unit Tested | Partial Parity | Existing race-capacity rejection is consumed by the new service/connection refill tests. |

## Known Gaps
- Start-looking quick-entry over multiple existing open instances is not yet covered.
- Recursive recheck stress cases with repeated same-mask cascades remain guarded but not exhaustively modeled.
- Overall Java `PlayerLeaveWorldService.leaveWorld(...)` ordering remains partial outside autogroup.
- Replacement readiness still needs broader real-client gameplay coverage beyond this autogroup slice.

## Next Recommended UOW
- `UOW-2404`: Cover start-looking quick-entry when multiple open runtime instances exist for the same mask and the first matching runtime rejects the party but a later runtime accepts it.

## Suggested Discovery For UOW-2404
- Java:
  - `AutoGroupService.checkInstancesForOpenQuickEntries(LookingForParty lfp, int maskId)`
  - `AutoPvpInstance.addLookingForParty(...)`
- C#:
  - `AutoGroupInstanceLeaveRuntimeService.TryAddOpenQuickEntry(...)`
  - `AutoGroupLookingPartyRegistrationService.StartLooking(...)`
  - `GameServerConnection.HandleAutoGroupAsync(...)`
  - `GameServerConnectionAutoGroupTests`
  - `AutoGroupInstanceLeaveRuntimeServiceTests`
  - `AutoGroupLookingPartyRegistrationServiceTests`

## Focused Validation Recipe For Next UOW
- Specific behavior to validate: Java `checkInstancesForOpenQuickEntries` iterates open auto instances for a mask, ignores rejected runtime candidates, attaches to a later accepting runtime, removes the search entry, sends ready window `4`, and applies additional-registration cleanup.
- Focused C# command:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests" --no-restore`
- Focused Java/Maven command: not expected unless a targeted Java fixture is added; use Java source review by default.
- Broad-validation trigger: live connection dispatch/runtime quick-entry if a connection-level start-looking test is added; still start with the focused filters above.

## Safe Candidate UOWs
- Add service-level multiple-open-instance quick-entry coverage first if the live connection setup becomes too broad.
- Audit penalty refresh scheduling order now that nested ready rechecks can occur before the original ready-match dispatch resumes.
- Continue broader `PlayerLeaveWorldService.leaveWorld(...)` ordering slices outside autogroup.
