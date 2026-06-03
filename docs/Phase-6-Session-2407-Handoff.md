# Phase 6 Session 2407 Handoff

## Current Phase
- Phase 6: Port Game Core.

## Last Completed UOW
- `UOW-2407`: Added same-mask recursive ready-recheck guard regression coverage.

## Commits Made
- `[Phase 6][UOW-2407] Cover same-mask recursive recheck guard`

## Files Changed
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAutoGroupTests.cs`
- `docs/Phase-6-Session-2407-Completion.md`
- `docs/Phase-6-Session-2407-Handoff.md`

## Java Artifacts Touched
- `com.aionemu.gameserver.services.AutoGroupService`

## C# Artifacts Touched
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService`
- `Aion.GameServer.Tests.GameServerConnectionAutoGroupTests`
- Adjacent validation: `Aion.GameServer.Tests.AutoGroupLookingPartyRegistrationServiceTests`

## What Changed
- Added a connection-level same-mask cascade regression:
  - logout cleanup creates the first mask `107` ready match;
  - cleanup of `1001` triggers one nested mask `107` ready match;
  - cleanup of `3001` attempts another mask `107` recheck but the existing guard prevents a third dispatch;
  - the remaining parties `5001` and `6001` stay queued.
- No production code changed.

## Tests Run
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests" --no-restore`
  - Result: 70 passed, 0 failed, 0 skipped.
- Java/Maven: skipped because no targeted Java fixture exists and no Java source changed.
- Broad .NET: skipped because this was a test-only focused regression with no broad-validation trigger.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Network.Aion.GameServerConnection` | Connection dispatch | Partial | Regression Tested | Partial Parity | Same-mask recursive ready recheck guard is covered at the connection boundary; C# guard is a defensive async-dispatch adaptation around Java's recursive member cleanup call. |
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService` | Service | Partial | Unit Tested | Partial Parity | Cleanup intent contract remains unchanged; recursive dispatch behavior is governed by the connection apply chain. |

## Known Gaps
- Offline-member cleanup delivery and scheduling combinations are not separately pinned.
- Open-registration refresh packets after auto-group leave/cancel flows still need audit.
- Overall Java `PlayerLeaveWorldService.leaveWorld(...)` ordering remains partial outside autogroup.
- Replacement readiness still needs broader real-client gameplay coverage beyond this autogroup slice.

## Next Recommended UOW
- `UOW-2408`: Add offline-player additional-registration cleanup coverage for autogroup ready-match cleanup so cancel-window delivery failures do not block penalty scheduling, queue cleanup, or ready-window dispatch for online members.

## Suggested Discovery For UOW-2408
- Java:
  - `AutoGroupService.searchAndRemoveAdditionalRegistrations(int objectId)`
  - `AutoGroupUtility.sendWindowToPlayerIfOnline(...)`
  - `AutoGroupService.penaliseParty(LookingForParty lfp)`
  - `AutoGroupService.penalisePlayerAndScheduleRemoval(int objectId)`
- C#:
  - `AutoGroupLookingPartyRegistrationService.ApplyReadyMatchPlanAsync(...)`
  - `GameServerConnection.ApplyAutoGroupReadyMatchPlanAsync(...)`
  - `GameServerConnectionAutoGroupTests`
  - `AutoGroupLookingPartyRegistrationServiceTests`

## Focused Validation Recipe For Next UOW
- Specific behavior to validate: cleanup intents still remove/search/schedule by Java rules when some cleanup-window recipients are offline and `SendPacketToPlayerAsync` returns false.
- Focused C# command:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests" --no-restore`
- Focused Java/Maven command: not expected unless a targeted Java fixture is added; use Java source review by default.
- Broad-validation trigger: none if the UOW stays test-only; live connection dispatch/scheduler intents if production timing changes are required.

## Safe Candidate UOWs
- Audit open-registration refresh packets after auto-group leave/cancel flows.
- Continue broader `PlayerLeaveWorldService.leaveWorld(...)` ordering slices outside autogroup.
- Add distinct-mask recursive cascade stress coverage if source review finds a concrete missing branch.
