# Phase 6 Session 2409 Completion - Penalty Refresh Open Registration Packets

## Scope
- Audited Java delayed penalty refresh execution after autogroup penalties.
- Tightened C# scheduler coverage for Java-shaped open-registration packet fanout.
- Confirmed offline object-id refresh execution is a no-op, matching Java `World.getInstance().getPlayer(objectId)` gating.

## Java Source Reviewed
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
  - `penalisePlayerAndScheduleRemoval(int objectId)`
- `game-server/src/com/aionemu/gameserver/services/instance/PeriodicInstanceManager.java`
  - `checkAndSendOpenRegistrations(int objectId)`
  - `checkAndSendOpenRegistrations(Player player)`

## Implemented
- Extended `AutoGroupPenaltyRefreshScheduler_DedupesAndRefreshesOpenRegistrationsLikeJava`.
- The test now asserts:
  - delayed refresh sends one `SM_AUTO_GROUP` entry-icon packet per open eligible mask;
  - packets use window `6` / `SmAutoGroup.EntryIconWindowId`;
  - packets are open-state packets (`IsClosed == false`);
  - refresh execution for an offline object id sends nothing.
- No production code changed in this UOW.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Services.AutoGroupPenaltyRefreshSchedulerService` | Scheduler service | Partial | Regression Tested | Partial Parity | Delayed penalty refresh scheduling and execution are covered for dedupe, delay, online open-registration packets, and offline no-op. |
| `com.aionemu.gameserver.services.instance.PeriodicInstanceManager` | `Aion.GameServer.Services.PeriodicInstanceRegistrationService` | Service | Partial | Unit Tested | Partial Parity | Open-registration packet creation is covered by adjacent service tests for level and cooldown filtering; this UOW pins scheduler execution to entry-icon open packets. |

## Tests Updated
| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `AutoGroupPenaltyRefreshScheduler_DedupesAndRefreshesOpenRegistrationsLikeJava` | Regression | Java scheduled task removes penalty then calls `PeriodicInstanceManager.checkAndSendOpenRegistrations(objectId)`; Java object-id overload no-ops when player is offline. | Online refresh sends open entry-icon packets for open eligible masks; offline object id sends no packets; dedupe and 10000 ms schedule remain covered. | Focused scheduler/connection regression plus adjacent periodic-registration service tests. | The actual scheduled task timer callback is represented through direct `ExecuteRefreshAsync` invocation rather than waiting 10000 ms. |

## Validation Decision
- Changed surface: test-only scheduler/open-registration bridge coverage.
- Specific behavior/contract: scheduled penalty refresh execution sends Java-shaped open-registration entry-icon packets only for online eligible players, and offline object ids no-op.
- Focused C# command: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~PeriodicInstanceRegistrationServiceTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests" --no-restore`
- Focused Java/Maven command: skipped; no targeted Java fixture exists and no Java source changed.
- Broad-validation trigger: none; this UOW changed one focused test.
- Broad .NET decision: skipped.
- Why this scope is sufficient: the edited scheduler regression exercises the delayed refresh bridge, while adjacent periodic-registration tests cover the Java level/cooldown filtering used to build those packets.

## Validation Result
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~PeriodicInstanceRegistrationServiceTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests" --no-restore`
  - Passed: 86 tests, 0 failed, 0 skipped.
  - Pre-existing nullable/analyzer warnings were emitted during the compile-producing run.

## Known Remaining Gaps
- The scheduled timer callback is not observed after a real 10000 ms delay in this focused unit.
- Overall Java `PlayerLeaveWorldService.leaveWorld(...)` ordering remains partial outside autogroup.
- Replacement readiness still needs broader real-client gameplay coverage beyond this autogroup slice.

## Summary Metrics
- Total Java artifacts reviewed in this UOW: 2.
- Total C# artifacts changed in this UOW: 1 test class.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification / partial parity: 2.
- Total blocked artifacts: 0.
- Estimated overall migration completion: Phase 6 remains in progress; no estimate change.
