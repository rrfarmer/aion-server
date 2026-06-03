# Phase 6 Session 2401 Completion - Logout Ready-Match Member Cleanup

## Scope
- Added connection-level parity coverage for Java `AutoGroupService.onLogout(...)` queue recheck flowing into `createNewInstance(...)` and the non-leader branch of `searchAndRemoveAdditionalRegistrations(int objectId)`.
- Verified that a matched player who is also a non-leader member of another queued party is removed from only that additional registration, receives cancel window `2`, gets the only penalty refresh, and then receives ready window `4` for the matched instance.

## Java Source Reviewed
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
  - `onLogout(Player player)`
  - `createNewInstance(AutoInstance autoInstance, AutoGroupType agt, List<LookingForParty> filteredParties, int maskId)`
  - `searchAndRemoveAdditionalRegistrations(int objectId)`

## Implemented
- Added `LeavePlayerWorldAsync_AutoGroupLogoutQueueRecheckCleansAdditionalMemberRegistrationLikeJava`.
- The test registers a logout-triggered ready match on mask `107` and an additional mask `108` party where ready player `1001` is a non-leader member behind leader `3001`.
- Assertions prove:
  - mask `107` matched queue is removed;
  - mask `108` remains queued with leader `3001`;
  - `1001` is no longer searching on mask `108`;
  - only `1001` receives a penalty refresh;
  - packet order is cancel `1001` mask `108`, then ready windows for `1001`, `1002`, `2001`, and `2002` on mask `107`;
  - the runtime instance is allocated with registered players `[1001, 1002, 2001, 2002]`.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService` | Service | Partial | Unit Tested | Partial Parity | Ready-match additional cleanup now has service-level and connection-level coverage for the non-leader member branch. Broader quick-entry refill and full logout ordering remain partial. |
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Network.Aion.GameServerConnection` | Connection dispatch | Partial | Regression Tested | Partial Parity | Logout queue recheck dispatch now covers matched-player member cleanup, cancel window `2`, ready window `4`, scheduler intent application, and runtime instance registration. |

## Tests Added
| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `LeavePlayerWorldAsync_AutoGroupLogoutQueueRecheckCleansAdditionalMemberRegistrationLikeJava` | Regression | Java source review of `AutoGroupService.onLogout`, `createNewInstance`, and `searchAndRemoveAdditionalRegistrations` | Logout-triggered ready-match dispatch applies the non-leader member cleanup branch, keeps the additional queued leader, sends cancel window `2` only to the removed member, schedules only that member's penalty refresh, and dispatches ready windows for the matched party. | Focused C# connection test with Java-derived packet ordering and state assertions. | Does not cover recursive queue recheck creating another ready match after the member is removed. |

## Validation Decision
- Changed surface: test-only live connection dispatch and scheduler intent coverage.
- Specific behavior/contract: logout-triggered ready-match dispatch applies Java's additional-registration member cleanup branch and preserves the remaining queued leader/party state.
- Focused C# command: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests" --no-restore`
- Focused Java/Maven command: skipped; no targeted Java fixture exists and no Java source changed.
- Broad-validation trigger: live connection dispatch and scheduler intents are covered by focused connection/service filters; no shared infrastructure or packet primitive changed.
- Broad .NET decision: skipped; focused tests provide the parity evidence and compile signal for the affected project.
- Why this scope is sufficient: the edited connection class exercises the live logout dispatch path, while the adjacent service tests keep the underlying Java cleanup contract covered.

## Validation Result
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests" --no-restore`
  - Passed: 21 tests, 0 failed, 0 skipped.
  - Pre-existing nullable/analyzer warnings were emitted during the compile-producing run.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests" --no-restore`
  - Passed: 65 tests, 0 failed, 0 skipped.

## Known Remaining Gaps
- Recursive `checkQueueForNewMatches(maskId)` effects after member cleanup remain modeled but not separately covered at connection level.
- Multiple queued quick-entry candidates and failed quick-entry refill capacity ordering remain untested in the logout/open-runtime path.
- Overall Java `PlayerLeaveWorldService.leaveWorld(...)` ordering remains partial outside autogroup.

## Summary Metrics
- Total Java artifacts reviewed in this UOW: 1.
- Total C# artifacts changed in this UOW: 1.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification / partial parity: 2.
- Total blocked artifacts: 0.
- Estimated overall migration completion: Phase 6 remains in progress; no estimate change.
