# Phase 6 Session 2403 Completion - Quick-Entry Refill Skip Ordering

## Scope
- Added parity coverage for Java `AutoGroupService.checkQueueForQuickEntries(AutoInstance autoInstance)` when the first queued quick-entry candidate cannot be added to the open runtime instance.
- Verified that C# keeps scanning queued quick-entry registrations, attaches the first addable candidate, and leaves the rejected candidate queued.

## Java Source Reviewed
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
  - `checkQueueForQuickEntries(AutoInstance autoInstance)`
  - `destroyOrAddPlayersFromQuickEntries(AutoInstance autoInstance)`
  - `checkInstancesForOpenQuickEntries(LookingForParty lfp, int maskId)`
- `game-server/src/com/aionemu/gameserver/services/instance/AutoPvpInstance.java`
  - `addLookingForParty(LookingForParty lfp)` behavior was reviewed through the C# runtime parity model and existing rejection tests.

## Implemented
- Added `TryRefillQueuedQuickEntry_SkipsRejectedQuickPartyAndKeepsItQueuedLikeJavaCheckQueueForQuickEntries`.
- Extended `HandleTeleportAnimationDoneAsync_AutoGroupLeaveRefillsQueuedQuickEntryLikeJavaDestroyOrAdd` with a rejected Asmodian quick-entry party before the accepted Elyos quick-entry party.
- The rejected party exceeds the open runtime's per-race capacity, remains queued, receives no ready window, and is not added to the runtime snapshot.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService` | Service | Partial | Unit Tested | Partial Parity | Quick-entry refill scan now has explicit coverage for skipping a rejected queued quick party and keeping it queued while attaching a later addable party. |
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Network.Aion.GameServerConnection` | Connection dispatch | Partial | Regression Tested | Partial Parity | Auto-group leave refill dispatch now covers Java-style skipped candidate state, runtime attachment, and ready/cancel packet delivery for the accepted candidate. |
| `com.aionemu.gameserver.services.instance.AutoPvpInstance` | `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService` | Runtime service | Partial | Unit Tested | Partial Parity | Existing runtime tests cover race-capacity rejection; this UOW consumes that rejection through service and connection refill scans. |

## Tests Added Or Updated
| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `TryRefillQueuedQuickEntry_SkipsRejectedQuickPartyAndKeepsItQueuedLikeJavaCheckQueueForQuickEntries` | Unit | Java source review of `checkQueueForQuickEntries` loop and AutoPvp capacity rejection behavior | A rejected quick-entry party is skipped and left queued; the next addable quick-entry party is attached and receives ready window `4`. | Focused C# service/runtime assertions tied to Java loop behavior. | Does not dispatch live packets. |
| `HandleTeleportAnimationDoneAsync_AutoGroupLeaveRefillsQueuedQuickEntryLikeJavaDestroyOrAdd` | Regression | Java `InstanceService.onLeaveInstance -> AutoGroupService.onLeaveInstance -> destroyOrAddPlayersFromQuickEntries -> checkQueueForQuickEntries` | Live leave-instance dispatch skips an over-capacity quick candidate, keeps it queued, attaches the next candidate, and sends the accepted candidate's ready/cancel windows. | Focused connection test with runtime snapshot, search-state, and packet assertions. | Uses C# runtime fixture rather than Java runtime execution. |

## Validation Decision
- Changed surface: test-only service and connection dispatch coverage.
- Specific behavior/contract: open-runtime quick-entry refill scans queued quick registrations in Java order, skips candidates whose runtime add fails, keeps rejected candidates queued, and attaches the first addable candidate.
- Focused C# command: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInstanceCooldownTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests" --no-restore`
- Focused Java/Maven command: skipped; no targeted Java fixture exists and no Java source changed.
- Broad-validation trigger: live connection dispatch/runtime refill was exercised by focused connection/service/runtime tests; no product code or shared infrastructure changed.
- Broad .NET decision: skipped after focused validation.
- Why this scope is sufficient: the service test proves the Java loop edge directly, the connection test proves the same state through live leave-instance dispatch, and existing runtime tests cover the rejection reason consumed by the refill scan.

## Validation Result
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~GameServerConnectionInstanceCooldownTests" --no-restore`
  - Passed: 64 tests, 0 failed, 0 skipped.
  - Pre-existing nullable/analyzer warnings were emitted during the compile-producing run.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInstanceCooldownTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests" --no-restore`
  - Passed: 79 tests, 0 failed, 0 skipped.

## Known Remaining Gaps
- Start-looking quick-entry over multiple open runtime instances still needs explicit ordering coverage.
- Recursive recheck stress cases with repeated same-mask cascades remain guarded but not exhaustively modeled.
- Overall Java `PlayerLeaveWorldService.leaveWorld(...)` ordering remains partial outside autogroup.

## Summary Metrics
- Total Java artifacts reviewed in this UOW: 2.
- Total C# artifacts changed in this UOW: 2 test files.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification / partial parity: 3.
- Total blocked artifacts: 0.
- Estimated overall migration completion: Phase 6 remains in progress; no estimate change.
