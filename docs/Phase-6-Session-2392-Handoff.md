# Phase 6 Session 2392 Handoff

## Current Phase
- Phase 6: Port Game Core.

## Last Completed UOW
- `UOW-2392`: Modeled Java autogroup logout search-entry cleanup.

## Files Changed
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupLookingPartyRegistrationServiceTests.cs`
- `docs/Phase-6-Session-2392-Completion.md`
- `docs/Phase-6-Session-2392-Handoff.md`

## Java Artifacts Touched
- `com.aionemu.gameserver.services.AutoGroupService`
- `com.aionemu.gameserver.model.autogroup.LookingForParty`

## C# Artifacts Touched
- `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService`
- `Aion.GameServer.Services.AutoGroupLookingPartyRegistration`
- `Aion.GameServer.Tests.AutoGroupLookingPartyRegistrationServiceTests`

## What Changed
- Added a non-live C# logout cleanup method for queued autogroup search registrations.
- Leader-only logout removes the entry.
- Leader logout with remaining members promotes the first remaining member and keeps the old leader in the member list, matching Java's `setLeaderObjId` behavior.
- Non-leader logout removes that member and emits a queue recheck plan for the affected mask.
- Missing search entries are no-ops.

## Tests Run
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests" --no-restore`
- Result: 41 passed, 0 failed, 0 skipped.
- Java/Maven: skipped because no targeted Java fixture exists and no Java source changed.
- Broad .NET: skipped; no broad-validation trigger for this non-live service/result slice.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService` | Service | Partial | Unit Tested | Partial Parity | Java `onLogout` search-entry cleanup is modeled. Live logout/disconnect dispatch, start-enter cancel-enter delegation, and auto-instance destroy-if-possible remain incomplete. |
| `com.aionemu.gameserver.model.autogroup.LookingForParty` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistration` | DTO | Partial | Unit Tested | Partial Parity | Leader promotion and member unregister semantics are covered; queued start-enter timing is not represented. |

## Known Gaps
- Live logout/disconnect path does not yet invoke autogroup logout cleanup.
- Java start-enter logout cancellation and auto-instance `destroyIfPossible` branches remain incomplete.
- Queue recheck after member logout is planned through result data but not applied into live ready-match dispatch in this UOW.
- Java `HashMap` member ordering for leader promotion is not deterministic; C# uses registration order.

## Next Recommended UOW
- `UOW-2393`: Wire autogroup search-entry logout cleanup into `GameServerConnection.LeavePlayerWorldAsync(...)` without enabling the start-enter/auto-instance destroy branches yet, or add a narrow live-dispatch adapter if the connection path needs a separate bridge.

## Suggested Discovery For UOW-2393
- Java:
  - `AutoGroupService.onLogout(Player player)`
  - logout call sites that invoke `AutoGroupService.getInstance().onLogout(player)`
  - `AutoGroupService.cancelEnter(Player player, int instanceMaskId)`
- C#:
  - `GameServerConnection.LeavePlayerWorldAsync(...)`
  - `GameServerConnection.DisposeAsync(...)` / `LeaveActivePlayerAsync(...)`
  - `AutoGroupLookingPartyRegistrationService.CleanupSearchEntriesOnLogout(...)`
  - `AutoGroupLookingPartyRegistrationService.ApplyReadyMatchPlanAsync(...)` if queue rechecks are applied live

## Focused Validation Recipe For Next UOW
- Specific behavior to validate: live player logout invokes autogroup queued-search cleanup once, preserving Java leader/member semantics without sending cancel windows or penalty refreshes for search cleanup.
- Focused C# command:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests" --no-restore`
- Focused Java/Maven command: not expected unless a targeted Java fixture is added; use Java source review by default.
- Broad-validation trigger: connection logout/disconnect dispatch if live wiring is enabled.

## Safe Candidate UOWs
- Add tests for stop-registration close behavior confirming it sends cancel windows without penalty refresh intents.
- Model Java `onLogout` start-enter `cancelEnter` delegation as a planner before live wiring.
- Improve cancel-enter destroy-if-possible modeling if online-inside-player facts become available.
