# Phase 6 Session 2395 Handoff

## Current Phase
- Phase 6: Port Game Core.

## Last Completed UOW
- `UOW-2395`: Modeled Java autogroup logout start-enter `cancelEnter` delegation as planner/result intents.

## Files Changed
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupLookingPartyRegistrationServiceTests.cs`
- `docs/Phase-6-Session-2395-Completion.md`
- `docs/Phase-6-Session-2395-Handoff.md`

## Java Artifacts Touched
- `com.aionemu.gameserver.services.AutoGroupService`
- `com.aionemu.gameserver.model.autogroup.LookingForParty`

## C# Artifacts Touched
- `Aion.GameServer.Services.AutoGroupLookingPartyRegistration`
- `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService`
- `Aion.GameServer.Tests.AutoGroupLookingPartyRegistrationServiceTests`

## What Changed
- `AutoGroupLookingPartyRegistration` now stores optional `StartEnterTime`.
- `IsOnStartEnterTask(...)` models Java's inclusive 120000 ms start-enter window.
- Logout cleanup accepts active auto-instance mask IDs and emits `AutoGroupLogoutStartEnterCancelIntent` records for start-enter entries.
- Start-enter logout entries are left in the search registry and do not produce leader/member cleanup or queue recheck plans.

## Tests Run
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests" --no-restore`
- Result: 56 passed, 0 failed, 0 skipped.
- Java/Maven: skipped because no targeted Java fixture exists and no Java source changed.
- Broad .NET: skipped because this UOW only modeled planner/result data and did not wire live connection dispatch.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.autogroup.LookingForParty` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistration` | Model | Partial | Unit Tested | Partial Parity | Start-enter timing matches the inclusive 120000 ms Java predicate. |
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService` | Service / Planner | Partial | Unit Tested | Partial Parity | Start-enter logout cleanup now plans cancel-enter calls for active masks and skips normal search cleanup. |

## Known Gaps
- `GameServerConnection.LeavePlayerWorldAsync(...)` does not yet consume `StartEnterCancelIntents`.
- `AutoGroupInstanceLeaveRuntimeService` does not yet expose an active auto-instance mask snapshot for logout planner input.
- Java auto-instance `destroyIfPossible(autoInstance)` on logout remains incomplete.
- Logout-specific additional-registration cleanup during ready-match dispatch still has no separate integration test.

## Next Recommended UOW
- `UOW-2396`: Wire start-enter logout cancel-enter intents into live logout by exposing active auto-instance masks from the runtime service and applying `CancelEnter(...)` for each planned intent.

## Suggested Discovery For UOW-2396
- Java:
  - `AutoGroupService.onLogout(Player player)`
  - `AutoGroupService.cancelEnter(Player player, int instanceMaskId)`
  - `AutoGroupService.getAutoInstance(Player player, int instanceMaskId)`
- C#:
  - `AutoGroupLookingPartyRegistrationService.CleanupSearchEntriesOnLogout(...)`
  - `AutoGroupInstanceLeaveRuntimeService.CancelEnter(...)`
  - `AutoGroupInstanceLeaveRuntimeService` runtime-instance storage/accessors
  - `GameServerConnection.LeavePlayerWorldAsync(...)`

## Focused Validation Recipe For Next UOW
- Specific behavior to validate: a player logging out while in the start-enter window consumes cancel-enter intents, unregisters the player from the matching runtime auto instance, schedules penalty refreshes through existing cancel-enter results, and does not run normal search cleanup for that start-enter entry.
- Focused C# command:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests" --no-restore`
- Focused Java/Maven command: not expected unless a targeted Java fixture is added; use Java source review by default.
- Broad-validation trigger: live connection dispatch and scheduler intents if `LeavePlayerWorldAsync(...)` is wired.

## Safe Candidate UOWs
- Add a logout-specific additional-registration cleanup test for ready-match dispatch.
- Add tests for stop-registration close behavior confirming it sends cancel windows without penalty refresh intents.
- Model Java auto-instance `destroyIfPossible(autoInstance)` logout branch after live cancel-enter wiring.
