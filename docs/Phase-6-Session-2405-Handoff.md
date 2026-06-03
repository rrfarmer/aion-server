# Phase 6 Session 2405 Handoff

## Current Phase
- Phase 6: Port Game Core.

## Last Completed UOW
- `UOW-2405`: Moved nested ready-recheck member-cleanup penalty refresh scheduling before nested ready dispatch and covered packet/schedule order.

## Commits Made
- `[Phase 6][UOW-2405] Align nested recheck penalty scheduling`

## Files Changed
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAutoGroupTests.cs`
- `docs/Phase-6-Session-2405-Completion.md`
- `docs/Phase-6-Session-2405-Handoff.md`

## Java Artifacts Touched
- `com.aionemu.gameserver.services.AutoGroupService`

## C# Artifacts Touched
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService`
- `Aion.GameServer.Services.AutoGroupPenaltyRefreshSchedulerService`
- `Aion.GameServer.Tests.GameServerConnectionAutoGroupTests`
- Adjacent validation: `Aion.GameServer.Tests.AutoGroupLookingPartyRegistrationServiceTests`

## What Changed
- `ApplyAutoGroupReadyMatchPlanAsync(...)` now schedules penalty refreshes for cleanup intents when the cleanup callback fires.
- The connection dedupes immediately scheduled cleanup refreshes before scheduling any remaining apply-result refreshes.
- The nested ready-match regression now records packet/schedule events and asserts Java-style order: cancel window, penalty schedule, nested ready windows, original ready windows.
- `RecordingConnectionRegistry` in `GameServerConnectionAutoGroupTests` accepts an optional send callback for local event-order assertions.

## Tests Run
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests" --no-restore`
  - Result: 69 passed, 0 failed, 0 skipped.
- Java/Maven: skipped because no targeted Java fixture exists and no Java source changed.
- Broad .NET: skipped; focused connection/service filters covered the scoped live dispatch and scheduler-intent risk.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Network.Aion.GameServerConnection` | Connection dispatch | Partial | Regression Tested | Partial Parity | Non-leader member cleanup now schedules after cancel window `2` and before nested ready recheck dispatch. |
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService` | Service | Partial | Unit Tested | Partial Parity | Cleanup intent contract remains service-level; exact leader-party pre-cancel scheduling needs separate coverage. |
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Services.AutoGroupPenaltyRefreshSchedulerService` | Scheduler service | Partial | Unit Tested | Partial Parity | Scheduler dedupe is unchanged; connection now calls it earlier for callback-observed cleanup intents. |

## Known Gaps
- Leader-party cleanup penalty scheduling is not yet pinned to Java's exact pre-cancel ordering.
- Recursive recheck stress cases with repeated same-mask cascades remain guarded but not exhaustively modeled.
- Overall Java `PlayerLeaveWorldService.leaveWorld(...)` ordering remains partial outside autogroup.
- Replacement readiness still needs broader real-client gameplay coverage beyond this autogroup slice.

## Next Recommended UOW
- `UOW-2406`: Cover leader-party additional-registration cleanup penalty scheduling order, where Java calls `penaliseParty(lfp)` before sending any cancel windows to that removed party.

## Suggested Discovery For UOW-2406
- Java:
  - `AutoGroupService.searchAndRemoveAdditionalRegistrations(int objectId)`
  - `AutoGroupService.penaliseParty(LookingForParty lfp)`
  - `AutoGroupService.createNewInstance(...)`
- C#:
  - `AutoGroupLookingPartyRegistrationService.ApplyReadyMatchPlanAsync(...)`
  - `GameServerConnection.ApplyAutoGroupReadyMatchPlanAsync(...)`
  - `GameServerConnectionAutoGroupTests`
  - `AutoGroupLookingPartyRegistrationServiceTests`

## Focused Validation Recipe For Next UOW
- Specific behavior to validate: leader-party additional cleanup schedules penalty refreshes for all removed party members before cancel window `2` deliveries, while still sending ready window `4` for the matched party in Java order.
- Focused C# command:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests" --no-restore`
- Focused Java/Maven command: not expected unless a targeted Java fixture is added; use Java source review by default.
- Broad-validation trigger: live connection dispatch/scheduler intents if connection-level event-order coverage is added; still start with the focused filters above.

## Safe Candidate UOWs
- Add targeted service-level stress coverage for recursive recheck same-mask guard behavior.
- Audit open-registration refresh packets after auto-group leave/cancel flows.
- Continue broader `PlayerLeaveWorldService.leaveWorld(...)` ordering slices outside autogroup.
