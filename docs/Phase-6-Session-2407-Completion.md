# Phase 6 Session 2407 Completion - Same-Mask Recursive Recheck Guard

## Scope
- Audited Java recursive ready-recheck behavior from member additional-registration cleanup.
- Added focused C# regression coverage for a same-mask cleanup cascade.
- Confirmed the C# connection guard allows one recursive same-mask recheck in an apply chain but prevents a nested same-mask recheck from dispatching again.

## Java Source Reviewed
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
  - `createNewInstance(AutoInstance autoInstance, AutoGroupType agt, List<LookingForParty> filteredParties, int maskId)`
  - `searchAndRemoveAdditionalRegistrations(int objectId)`
  - `checkQueueForNewMatches(int maskId)`

## Implemented
- Added `LeavePlayerWorldAsync_AutoGroupLogoutMemberCleanupGuardsSameMaskRecursiveRecheckLikeJava`.
- The regression creates a same-mask cascade:
  - logout cleanup prepares the first mask `107` ready match;
  - member cleanup of `1001` triggers one recursive mask `107` recheck;
  - nested member cleanup of `3001` attempts another mask `107` recheck, which is guarded;
  - the would-be third ready pair remains queued.
- No production code changed in this UOW; the existing connection `recheckedMaskIds` guard is now pinned by focused coverage.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Network.Aion.GameServerConnection` | Connection dispatch | Partial | Regression Tested | Partial Parity | Same-mask recursive ready recheck is now guarded in connection dispatch coverage; Java itself recursively calls `checkQueueForNewMatches(maskId)` from member cleanup, so this C# guard remains a defensive async-dispatch adaptation. |
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService` | Service | Partial | Unit Tested | Partial Parity | Service still reports cleanup recheck intent per Java member branch; connection-level recursion policy is covered separately. |

## Tests Updated
| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `LeavePlayerWorldAsync_AutoGroupLogoutMemberCleanupGuardsSameMaskRecursiveRecheckLikeJava` | Regression | Java `searchAndRemoveAdditionalRegistrations` member branch calls `checkQueueForNewMatches(maskId)` after cancel and penalty scheduling. | The first same-mask cleanup recheck dispatches a nested ready match, the nested same-mask cleanup does not dispatch a third match in the same apply chain, and remaining queued parties stay searchable. | Focused connection regression asserts packet order, scheduler observations, runtime instances, and remaining queue state. | Java's direct recursion has no explicit guard; C# guard is an async-dispatch safety adaptation and remains Partial Parity rather than Verified Parity. |

## Validation Decision
- Changed surface: test-only connection regression coverage.
- Specific behavior/contract: same-mask recursive ready recheck is limited to one nested dispatch per connection apply chain, preserving queue state for the would-be second nested same-mask match.
- Focused C# command: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests" --no-restore`
- Focused Java/Maven command: skipped; no targeted Java fixture exists and no Java source changed.
- Broad-validation trigger: none; this UOW changed only one focused test class.
- Broad .NET decision: skipped.
- Why this scope is sufficient: the new regression directly exercises the connection apply-chain guard and adjacent registration-service tests preserve the cleanup intent contract.

## Validation Result
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests" --no-restore`
  - Passed: 70 tests, 0 failed, 0 skipped.
  - Pre-existing nullable/analyzer warnings were emitted during the compile-producing run.

## Known Remaining Gaps
- Distinct-mask recursive cascades are covered by prior nested ready-match behavior, but not exhaustively stress-tested.
- Offline-member cleanup delivery and scheduling combinations are not separately pinned.
- Overall Java `PlayerLeaveWorldService.leaveWorld(...)` ordering remains partial outside autogroup.

## Summary Metrics
- Total Java artifacts reviewed in this UOW: 1.
- Total C# artifacts changed in this UOW: 1 test class.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification / partial parity: 2.
- Total blocked artifacts: 0.
- Estimated overall migration completion: Phase 6 remains in progress; no estimate change.
