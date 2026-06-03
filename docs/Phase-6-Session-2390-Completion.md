# Phase 6 Session 2390 Completion - Model Autogroup Penalty Refresh Intents

## Scope
- Ported Java `AutoGroupService.penalisePlayerAndScheduleRemoval(...)` as explicit C# result intent data for registration cancellation and additional-registration cleanup paths.
- Kept this UOW planner/result focused: no live scheduler was introduced.

## Java Source Reviewed
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
  - `penalisePlayerAndScheduleRemoval(int objectId)`
  - `penaliseParty(LookingForParty lfp)`
  - `cancelRegistration(...)`
  - `searchAndRemoveAdditionalRegistrations(int objectId)`
- `game-server/src/com/aionemu/gameserver/services/instance/PeriodicInstanceManager.java`
  - `checkAndSendOpenRegistrations(int objectId)`
- `game-server/src/com/aionemu/gameserver/services/autogroup/AutoGroupUtility.java`
  - confirmed registration guards do not consult the `penalties` set.

## Implemented
- Added `AutoGroupPenaltyRefreshIntent`.
  - Captures the penalized player object id.
  - Captures Java's fixed 10000 ms delay.
  - Captures the Java source breadcrumb for `penalties.remove(objectId)` followed by `PeriodicInstanceManager.checkAndSendOpenRegistrations(objectId)`.
- Added penalty refresh intent lists to:
  - `AutoGroupCancelRegistrationResult`
  - `AutoGroupApplyReadyMatchResult`
  - `AutoGroupOpenQuickEntryAttachment`
- Mapped Java branches:
  - leader cancellation / leader additional cleanup -> penalize every party member;
  - member cancellation / member additional cleanup -> penalize only the removed member;
  - missing cancellation -> no penalty intent.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService` | Service | Partial | Unit Tested | Partial Parity | Java penalty scheduling is now represented as result intent data for cancel-registration, ready-match cleanup, and open quick-entry cleanup. Live 10-second scheduling and open-registration packet refresh remain unwired. |
| `com.aionemu.gameserver.services.instance.PeriodicInstanceManager` | `Aion.GameServer.Services.PeriodicInstanceRegistrationService` | Service | Partial | Unit Tested | Partial Parity | Existing C# open-registration packet creation can serve the Java refresh side effect, but this UOW only records the delayed refresh intent. |

## Tests Updated
| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `AutoGroupLookingPartyRegistrationServiceTests.StartLooking_AttachesOpenQuickEntryAndCleansLeaderAdditionalRegistrationLikeJava` | Unit | Java `searchAndRemoveAdditionalRegistrations` / `penaliseParty` | Open quick-entry leader cleanup exposes refresh intents for every removed party member. | Source-reviewed Java branch with focused C# assertions. | Intent only, not live scheduling. |
| `AutoGroupLookingPartyRegistrationServiceTests.ApplyReadyMatchPlan_RemovesMatchedQueuesAndSendsCleanupBeforeReadyWindowLikeJava` | Unit | Java `createNewInstance` / `searchAndRemoveAdditionalRegistrations` | Ready-match leader cleanup exposes refresh intents for every removed party member. | Source-reviewed Java branch with focused C# assertions. | Intent only, not live scheduling. |
| `AutoGroupLookingPartyRegistrationServiceTests.ApplyReadyMatchPlan_MutatesMemberCleanupAndKeepsLeaderEntryLikeJava` | Unit | Java member cleanup branch | Member cleanup exposes refresh intents only for removed members. | Source-reviewed Java branch with focused C# assertions. | Intent only, not live scheduling. |
| `AutoGroupLookingPartyRegistrationServiceTests.CancelRegistration_LeaderRemovesWholePartyAndSendsCancelWindowLikeJava` | Unit | Java `cancelRegistration` leader branch | Leader cancellation exposes refresh intents for all party members. | Source-reviewed Java branch with focused C# assertions. | Intent only, not live scheduling. |
| `AutoGroupLookingPartyRegistrationServiceTests.CancelRegistration_MemberRemovesOnlyMemberAndSendsCancelWindowLikeJava` | Unit | Java `cancelRegistration` member branch | Member cancellation exposes a refresh intent only for the removed member. | Source-reviewed Java branch with focused C# assertions. | Intent only, not live scheduling. |

## Validation Decision
- Changed surface: non-live service/result shape plus tests.
- Specific behavior/contract: Java `penalisePlayerAndScheduleRemoval` emits a deduplicated 10000 ms delayed open-registration refresh intent for players penalized by cancel/additional-cleanup branches.
- Focused C# command: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests" --no-restore`
- Focused Java/Maven command: skipped; no targeted Java fixture exists and no Java source changed.
- Broad-validation trigger: none.
- Broad .NET decision: skipped; focused service/runtime/connection-adjacent tests compile and exercise the changed result contract.
- Why this scope is sufficient: the edited service is covered directly, and adjacent runtime/connection tests verify call-site compatibility for open quick-entry and cancel-enter paths.

## Validation Result
- Passed: 60 tests, 0 failed, 0 skipped.
- Pre-existing nullable/analyzer warnings remain in unrelated files.

## Known Remaining Gaps
- The C# live scheduler for Java's 10-second `PeriodicInstanceManager.checkAndSendOpenRegistrations(objectId)` refresh remains unwired.
- Java's `penalties` set deduplicates repeated schedules; the C# result intents are distinct and do not yet model live dedupe state.
- `AutoGroupService.onLogout(...)` remains incomplete.

## Summary Metrics
- Total Java artifacts discovered in this UOW: 3.
- Total artifacts ported or extended in this UOW: 2 C# surfaces.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification / partial parity: 2.
- Total blocked artifacts: 0.
- Estimated overall Phase 6 completion: unchanged materially; this is a narrow autogroup planner slice.
