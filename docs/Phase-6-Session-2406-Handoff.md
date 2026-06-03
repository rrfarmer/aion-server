# Phase 6 Session 2406 Handoff

## Current Phase
- Phase 6: Port Game Core.

## Last Completed UOW
- `UOW-2406`: Covered leader-party additional-registration cleanup penalty scheduling order.

## Commits Made
- `[Phase 6][UOW-2406] Align leader cleanup penalty scheduling`

## Files Changed
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAutoGroupTests.cs`
- `docs/Phase-6-Session-2406-Completion.md`
- `docs/Phase-6-Session-2406-Handoff.md`

## Java Artifacts Touched
- `com.aionemu.gameserver.services.AutoGroupService`

## C# Artifacts Touched
- `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService`
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Services.AutoGroupPenaltyRefreshSchedulerService`
- `Aion.GameServer.Tests.GameServerConnectionAutoGroupTests`
- Adjacent validation: `Aion.GameServer.Tests.AutoGroupLookingPartyRegistrationServiceTests`

## What Changed
- `ApplyReadyMatchPlanAsync(...)` now offers a `beforeCleanupWindowDeliveryAsync` hook and calls it once per cleanup intent before sending that cleanup's first window packet.
- `GameServerConnection.ApplyAutoGroupReadyMatchPlanAsync(...)` uses the pre-delivery hook for leader-party penalty refresh scheduling.
- Member-cleanup penalty scheduling remains after cancel window delivery and before recursive queue recheck.
- The leader-party logout/recheck regression now asserts schedule events before cancel windows and ready windows.

## Tests Run
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests" --no-restore`
  - First run: compile failed due to test recorder using `SmAutoGroup.InstanceMaskId`; fixed to `SmAutoGroup.MaskId`.
  - Re-run: 69 passed, 0 failed, 0 skipped.
- Java/Maven: skipped because no targeted Java fixture exists and no Java source changed.
- Broad .NET: skipped; focused connection/service filters covered the scoped live dispatch and scheduler-intent risk.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService` | Service | Partial | Unit Tested | Partial Parity | Ready-match cleanup delivery now distinguishes pre-window leader cleanup from post-window member cleanup; broad autogroup parity remains partial. |
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Network.Aion.GameServerConnection` | Connection dispatch | Partial | Regression Tested | Partial Parity | Leader-party cleanup schedules all removed party member penalties before cancel window `2`; member cleanup still schedules after cancel before nested recheck. |
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Services.AutoGroupPenaltyRefreshSchedulerService` | Scheduler service | Partial | Unit Tested | Partial Parity | Scheduler implementation unchanged; dispatch timing now matches the reviewed Java leader/member branch split. |

## Known Gaps
- Recursive recheck stress cases with repeated same-mask cascades remain guarded but not exhaustively modeled.
- Offline-member cleanup and scheduling combinations are not separately pinned.
- Overall Java `PlayerLeaveWorldService.leaveWorld(...)` ordering remains partial outside autogroup.
- Replacement readiness still needs broader real-client gameplay coverage beyond this autogroup slice.

## Next Recommended UOW
- `UOW-2407`: Add targeted service-level stress coverage for recursive ready-recheck same-mask guard behavior so repeated cleanup callbacks cannot dispatch the same mask recursively more than once per apply chain.

## Suggested Discovery For UOW-2407
- Java:
  - `AutoGroupService.searchAndRemoveAdditionalRegistrations(int objectId)`
  - `AutoGroupService.checkQueueForNewMatches(int maskId)`
  - `AutoGroupService.createNewInstance(...)`
- C#:
  - `GameServerConnection.ApplyAutoGroupReadyMatchPlanAsync(...)`
  - `AutoGroupLookingPartyRegistrationService.ApplyReadyMatchPlanAsync(...)`
  - `GameServerConnectionAutoGroupTests`
  - `AutoGroupLookingPartyRegistrationServiceTests`

## Focused Validation Recipe For Next UOW
- Specific behavior to validate: recursive ready recheck for the same mask is guarded within one connection apply chain, while distinct masks may still recheck in Java-observed order.
- Focused C# command:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests" --no-restore`
- Focused Java/Maven command: not expected unless a targeted Java fixture is added; use Java source review by default.
- Broad-validation trigger: live connection dispatch/scheduler intents if connection-level event-order coverage is changed; still start with the focused filters above.

## Safe Candidate UOWs
- Audit open-registration refresh packets after auto-group leave/cancel flows.
- Continue broader `PlayerLeaveWorldService.leaveWorld(...)` ordering slices outside autogroup.
- Add offline-player cleanup delivery coverage for autogroup additional registrations.
