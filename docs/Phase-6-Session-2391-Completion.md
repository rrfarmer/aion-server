# Phase 6 Session 2391 Completion - Schedule Autogroup Penalty Refreshes

## Scope
- Ported Java `AutoGroupService.penalisePlayerAndScheduleRemoval(...)` into a live C# scheduler/dedupe adapter.
- Wired existing penalty refresh intents from cancellation, ready-match cleanup, quick-entry cleanup, and cancel-enter paths into `GameServerConnection`.
- Reused `PeriodicInstanceRegistrationService` for the delayed Java `PeriodicInstanceManager.checkAndSendOpenRegistrations(objectId)` packet refresh.

## Java Source Reviewed
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
  - `penalisePlayerAndScheduleRemoval(int objectId)`
  - `penaliseParty(LookingForParty lfp)`
  - `cancelRegistration(...)`
  - `cancelEnter(...)`
  - `searchAndRemoveAdditionalRegistrations(int objectId)`
- `game-server/src/com/aionemu/gameserver/services/instance/PeriodicInstanceManager.java`
  - `checkAndSendOpenRegistrations(int objectId)`
  - `checkAndSendOpenRegistrations(Player player)`

## Implemented
- Added `AutoGroupPenaltyRefreshSchedulerService`.
  - Dedupes pending delayed refreshes by player object id, matching Java's `penalties.add(objectId)` gate.
  - Uses Java's 10000 ms delay from `AutoGroupPenaltyRefreshIntent`.
  - Resolves the online player through `IGameClientConnectionRegistry.ForEachOnlinePlayer(...)`.
  - Sends current open-registration icon packets using `PeriodicInstanceRegistrationService.CreateOpenRegistrationPackets(...)`.
- Registered the scheduler in server DI and passed it through `GameClientSocketServer` into `GameServerConnection`.
- Connected penalty refresh scheduling in:
  - cancel registration window 101;
  - ready-match queue cleanup;
  - open quick-entry cleanup;
  - cancel-enter window 103;
  - teleport-leave quick-entry refill cleanup.
- Extended cancel-enter runtime results to expose the Java penalty refresh intent on successful unregister and none on no-op.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Services.AutoGroupPenaltyRefreshSchedulerService` | Service | Partial | Unit Tested | Partial Parity | Java penalty scheduling/dedupe is now live for C# autogroup paths that expose refresh intents. |
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Network.Aion.GameServerConnection` | Packet Handler | Partial | Integration Tested | Partial Parity | Connection paths now schedule refreshes from cancel, ready-match cleanup, quick-entry cleanup, and cancel-enter results. |
| `com.aionemu.gameserver.services.instance.PeriodicInstanceManager` | `Aion.GameServer.Services.PeriodicInstanceRegistrationService` | Service | Partial | Unit Tested | Partial Parity | Existing C# packet creator is used for the delayed open-registration refresh. |

## Tests Updated
| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionAutoGroupTests.ProcessPacketAsync_AutoGroupCancelRegistrationSchedulesPenaltyRefreshLikeJava` | Integration | Java `cancelRegistration` -> `penaliseParty` / `penalisePlayerAndScheduleRemoval` | Cancel registration schedules one 10000 ms delayed refresh and still sends the cancel window. | Source-reviewed Java branch with live C# scheduler observation. | Does not wait 10 real seconds. |
| `GameServerConnectionAutoGroupTests.AutoGroupPenaltyRefreshScheduler_DedupesAndRefreshesOpenRegistrationsLikeJava` | Unit | Java `penalties.add(objectId)` and `PeriodicInstanceManager.checkAndSendOpenRegistrations` | Duplicate schedules are deduped while pending, and refresh execution sends open-registration icon packets. | Source-reviewed Java branch with direct scheduler and packet assertions. | Uses direct execution for packet refresh rather than waiting on the delayed task. |
| `AutoGroupInstanceLeaveRuntimeServiceTests.CancelEnter_UnregistersPlayerLikeJavaAutoGroupService` | Unit | Java `cancelEnter` -> `penalisePlayerAndScheduleRemoval` | Successful cancel-enter exposes one 10000 ms penalty refresh intent. | Source-reviewed Java branch with focused result assertion. | Runtime service emits intent only; connection test coverage handles live scheduling. |
| `AutoGroupInstanceLeaveRuntimeServiceTests.CancelEnter_MissingMaskOrUnregisteredPlayerIsNoOpLikeJavaGetAutoInstanceNull` | Unit | Java `getAutoInstance(player, mask) == null` guard | No-op cancel-enter emits no penalty refresh intent. | Source-reviewed Java branch with focused result assertion. | None for this branch. |

## Validation Decision
- Changed surface: live scheduler/connection dispatch plus result contract tests.
- Specific behavior/contract: Java-style penalty refresh scheduling dedupes by player for 10000 ms and eventually refreshes open-registration icon packets for an online player.
- Focused C# command: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~PeriodicInstanceRegistrationServiceTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests" --no-restore`
- Focused Java/Maven command: skipped; no targeted Java fixture exists and no Java source changed.
- Broad-validation trigger: live scheduler/connection dispatch.
- Broad .NET decision: skipped after focused validation; the targeted slice compiles the game server and covers scheduler, connection dispatch, periodic-registration packet creation, and cancel-enter result shape.
- Why this scope is sufficient: the changed live adapter is covered directly, and its primary connection entry point is exercised through packet processing.

## Validation Result
- Passed: 77 tests, 0 failed, 0 skipped.
- Pre-existing nullable/analyzer warnings remain in unrelated files.

## Known Remaining Gaps
- Java `AutoGroupService.onLogout(...)` search-entry cleanup remains incomplete.
- The delayed task is validated through scheduler observation and direct refresh execution, not by waiting 10 real seconds.
- Some C# quick-entry / ready-match cleanup branches rely on existing service tests for intent creation and this UOW's connection hooks for scheduling.

## Summary Metrics
- Total Java artifacts discovered in this UOW: 2.
- Total artifacts ported or extended in this UOW: 5 C# surfaces.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification / partial parity: 3.
- Total blocked artifacts: 0.
- Estimated overall Phase 6 completion: unchanged materially; this is a narrow autogroup live-scheduler slice.
