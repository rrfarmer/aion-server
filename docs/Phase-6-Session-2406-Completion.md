# Phase 6 Session 2406 Completion - Leader Cleanup Penalty Scheduling Order

## Scope
- Audited Java leader-party additional-registration cleanup order during ready-match dispatch.
- Added a pre-cleanup-window callback so C# can schedule leader-party penalty refreshes before cancel window delivery.
- Extended connection-level autogroup coverage to assert scheduler and packet ordering for leader-party cleanup.

## Java Source Reviewed
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
  - `searchAndRemoveAdditionalRegistrations(int objectId)`
  - `penaliseParty(LookingForParty lfp)`
  - `penalisePlayerAndScheduleRemoval(int objectId)`
  - `createNewInstance(AutoInstance autoInstance, AutoGroupType agt, List<LookingForParty> filteredParties, int maskId)`

## Implemented
- `AutoGroupLookingPartyRegistrationService.ApplyReadyMatchPlanAsync(...)` accepts an optional `beforeCleanupWindowDeliveryAsync` callback.
- Cleanup intents are now correlated with window deliveries before packet send, and the pre-delivery callback fires once per cleanup intent.
- `GameServerConnection.ApplyAutoGroupReadyMatchPlanAsync(...)` schedules leader-party cleanup penalties through the pre-delivery callback.
- Non-leader member cleanup still schedules through the after-delivery callback before recursive queue rechecks.
- `LeavePlayerWorldAsync_AutoGroupLogoutQueueRecheckCleansAdditionalRegistrationsLikeJava` now records schedule/packet events and asserts Java-style order: two penalty schedules, two cancel windows, then ready windows.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService` | Service | Partial | Unit Tested | Partial Parity | Apply-ready cleanup delivery now exposes pre-window and post-window timing hooks so leader and member cleanup can follow separate Java ordering. Broader autogroup behavior remains partial. |
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Network.Aion.GameServerConnection` | Connection dispatch | Partial | Regression Tested | Partial Parity | Leader-party cleanup now schedules all removed party member penalties before cancel window `2`; member cleanup keeps after-cancel scheduling before nested recheck. |
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Services.AutoGroupPenaltyRefreshSchedulerService` | Scheduler service | Partial | Unit Tested | Partial Parity | Scheduler behavior unchanged; this UOW changes connection dispatch timing for leader-party cleanup scheduling. |

## Tests Updated
| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `LeavePlayerWorldAsync_AutoGroupLogoutQueueRecheckCleansAdditionalRegistrationsLikeJava` | Regression | Java `searchAndRemoveAdditionalRegistrations` leader branch calls `penaliseParty(lfp)` before sending cancel window `2` to removed party members. | Scheduler events for removed leader-party members occur before cancel windows, and matched parties still receive ready window `4` afterward. | Focused connection event-order assertions using packet sends and scheduler observations. | Does not exhaustively model repeated cleanup cascades or offline member packet omission. |

## Validation Decision
- Changed surface: production service callback contract, connection dispatch scheduling order, and connection regression test.
- Specific behavior/contract: leader-party additional cleanup schedules penalty refreshes for all removed party members before cancel window `2` deliveries while preserving member cleanup after-cancel order from UOW-2405.
- Focused C# command: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests" --no-restore`
- Focused Java/Maven command: skipped; no targeted Java fixture exists and no Java source changed.
- Broad-validation trigger: live connection dispatch/scheduler intents; covered with focused connection/service filters.
- Broad .NET decision: skipped after focused validation.
- Why this scope is sufficient: the edited service contract and connection dispatch are exercised by the connection autogroup regression plus adjacent registration-service coverage tied to reviewed Java branches.

## Validation Result
- First focused C# run failed at compile because the new test recorder referenced `SmAutoGroup.InstanceMaskId`; corrected to the existing `SmAutoGroup.MaskId` accessor.
- Re-run: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests" --no-restore`
  - Passed: 69 tests, 0 failed, 0 skipped.
  - Pre-existing nullable/analyzer warnings were emitted during the compile-producing run.

## Known Remaining Gaps
- Recursive same-mask ready-recheck stress cases remain guarded but not exhaustively modeled.
- Offline-member cleanup delivery and penalty scheduling combinations are not separately pinned.
- Overall Java `PlayerLeaveWorldService.leaveWorld(...)` ordering remains partial outside autogroup.

## Summary Metrics
- Total Java artifacts reviewed in this UOW: 1.
- Total C# artifacts changed in this UOW: 3.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification / partial parity: 3.
- Total blocked artifacts: 0.
- Estimated overall migration completion: Phase 6 remains in progress; no estimate change.
