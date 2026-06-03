# Phase 6 Session 2404 Completion - Quick-Entry Open Instance Scan

## Scope
- Added parity coverage for Java `AutoGroupService.checkInstancesForOpenQuickEntries(LookingForParty lfp, int maskId)` when multiple open runtime instances exist for the same mask.
- Verified that C# ignores the first matching runtime instance when `AutoPvpInstance.addLookingForParty(...)` rejects the quick-entry party and attaches the player to a later accepting runtime instance.

## Java Source Reviewed
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
  - `checkInstancesForOpenQuickEntries(LookingForParty lfp, int maskId)`
  - `startLooking(Player player, EntryRequestType ert, int maskId)`
- `game-server/src/com/aionemu/gameserver/services/instance/AutoPvpInstance.java`
  - `addLookingForParty(LookingForParty lfp)` behavior was reviewed through the C# runtime parity model and existing capacity rejection tests.

## Implemented
- Added `TryAddOpenQuickEntry_SkipsRejectedOpenInstanceAndAddsLaterMatchLikeJavaCheckInstances`.
- Added `StartLooking_AttachesQuickEntryToLaterOpenRuntimeWhenFirstRejectsLikeJavaCheckInstances`.
- Added `ProcessPacketAsync_AutoGroupQuickEntryAttachesToLaterOpenRuntimeInstanceLikeJava`.
- The tests model a first open mask `107` runtime that is full for the Elyos race and a later mask `107` runtime that accepts the quick-entry player.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService` | Service | Partial | Unit Tested | Partial Parity | Start-looking quick-entry now covers Java's open-instance scan behavior when the first matching runtime rejects and a later runtime accepts. |
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Network.Aion.GameServerConnection` | Connection dispatch | Partial | Regression Tested | Partial Parity | Live quick-entry registration now covers successful-registration fanout, later-runtime attachment, ready window `4`, and additional-registration cleanup windows. |
| `com.aionemu.gameserver.services.instance.AutoPvpInstance` | `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService` | Runtime service | Partial | Unit Tested | Partial Parity | Runtime quick-entry add now has explicit coverage for skipping a rejected open instance and adding to a later matching instance. |

## Tests Added
| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `TryAddOpenQuickEntry_SkipsRejectedOpenInstanceAndAddsLaterMatchLikeJavaCheckInstances` | Unit | Java `checkInstancesForOpenQuickEntries` loop plus AutoPvp capacity behavior | The runtime scans matching open instances, skips the first race-full runtime, and adds the quick-entry player to the later accepting runtime. | Focused runtime assertions tied to Java loop behavior. | Does not cover packet dispatch. |
| `StartLooking_AttachesQuickEntryToLaterOpenRuntimeWhenFirstRejectsLikeJavaCheckInstances` | Unit | Java `startLooking -> checkInstancesForOpenQuickEntries` | Start-looking removes the quick search entry, attaches to the later open runtime, and applies additional-registration cleanup. | Focused service/runtime assertions. | Does not dispatch live client packets. |
| `ProcessPacketAsync_AutoGroupQuickEntryAttachesToLaterOpenRuntimeInstanceLikeJava` | Regression | Java `CM_AUTO_GROUP -> AutoGroupService.startLooking -> checkInstancesForOpenQuickEntries` | Live packet handling sends the successful-registration fanout, attaches the player to the later open runtime, sends ready window `4`, and sends cleanup cancel windows for the additional registration. | Focused connection/runtime/packet assertions. | Uses C# runtime fixture rather than Java runtime execution. |

## Validation Decision
- Changed surface: test-only runtime, service, and connection dispatch coverage.
- Specific behavior/contract: start-looking quick-entry scans multiple open instances for the same mask, skips rejected candidates, and attaches to a later accepting runtime before queue matching.
- Focused C# command: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests" --no-restore`
- Focused Java/Maven command: skipped; no targeted Java fixture exists and no Java source changed.
- Broad-validation trigger: live connection dispatch/runtime quick-entry was exercised by focused connection/service/runtime tests; no product code or shared infrastructure changed.
- Broad .NET decision: skipped after focused validation.
- Why this scope is sufficient: the runtime test proves the open-instance scan, the service test proves Java start-looking state mutation and cleanup, and the connection test proves the live packet ordering.

## Validation Result
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests" --no-restore`
  - Passed: 85 tests, 0 failed, 0 skipped.
  - Pre-existing nullable/analyzer warnings were emitted during the compile-producing run.

## Known Remaining Gaps
- Recursive recheck stress cases with repeated same-mask cascades remain guarded but not exhaustively modeled.
- Penalty refresh scheduling order after nested ready rechecks has not been separately audited.
- Overall Java `PlayerLeaveWorldService.leaveWorld(...)` ordering remains partial outside autogroup.

## Summary Metrics
- Total Java artifacts reviewed in this UOW: 2.
- Total C# artifacts changed in this UOW: 3 test files.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification / partial parity: 3.
- Total blocked artifacts: 0.
- Estimated overall migration completion: Phase 6 remains in progress; no estimate change.
