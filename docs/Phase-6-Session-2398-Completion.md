# Phase 6 Session 2398 Completion - Logout Autogroup Quick-Entry Refill Coverage

## Scope
- Added logout-specific integration evidence for Java `AutoGroupService.onLogout(...)` start-enter cancellation flowing into `cancelEnter(...)`.
- Verified that logout-driven cancel-enter refills one queued quick-entry party through the shared C# helper, sends ready window `4`, removes the quick-entry leader's additional registrations, and schedules Java penalty-refresh cleanup.
- Tightened the existing no-refill logout test to assert no registry `SM_AUTO_GROUP` fanout occurs when no queued quick-entry party can be attached.

## Java Source Reviewed
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
  - `onLogout(Player player)`
  - `cancelEnter(Player player, int instanceMaskId)`
  - `destroyOrAddPlayersFromQuickEntries(AutoInstance autoInstance)`
  - `checkQueueForQuickEntries(AutoInstance autoInstance)`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoPvpInstance.java`
  - `addLookingForParty(LookingForParty lookingForParty)`

## Implemented
- Added `LeavePlayerWorldAsync_StartEnterLogoutRefillsQueuedQuickEntryLikeJava`.
- The new test drives `LeavePlayerWorldAsync(...)` through a start-enter logout cleanup intent, unregisters the logging-out player from the open runtime instance, attaches a queued quick-entry player, sends the quick-entry ready window, removes the quick-entry leader's separate queued party, and schedules penalty-refresh timers for both cancel-enter and additional-registration cleanup.
- Strengthened `LeavePlayerWorldAsync_StartEnterLogoutConsumesCancelEnterIntentsLikeJava` so the no-refill logout path must leave registry fanout empty.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.player.PlayerLeaveWorldService` | `Aion.GameServer.Network.Aion.GameServerConnection` | Connection / Logout Handler | Partial | Integration Tested | Partial Parity | Logout start-enter cleanup now has direct evidence that it invokes the shared cancel-enter quick-entry refill path. Overall leave-world ordering remains partial. |
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Network.Aion.GameServerConnection` / `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService` / `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService` | Service / Runtime | Partial | Integration Tested | Partial Parity | `cancelEnter -> destroyOrAddPlayersFromQuickEntries -> checkQueueForQuickEntries` is covered for logout-driven start-enter cancellation. Other autogroup logout branches remain partial. |
| `com.aionemu.gameserver.model.autogroup.AutoPvpInstance` | `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService` | Runtime Model | Partial | Integration Tested | Partial Parity | PVP race capacity and quick-entry attachment are exercised through the logout refill scenario. Broader subclass-specific behavior remains partial. |

## Tests Updated
| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `LeavePlayerWorldAsync_StartEnterLogoutRefillsQueuedQuickEntryLikeJava` | Integration | Java `AutoGroupService.onLogout`, `cancelEnter`, `destroyOrAddPlayersFromQuickEntries`, `checkQueueForQuickEntries`, and `AutoPvpInstance.addLookingForParty` | Logout-driven start-enter cancel-enter refills an open quick-entry slot, sends window `4`, removes additional registrations, and schedules 10-second penalty refreshes. | Source-reviewed Java branch with C# connection/runtime/search/scheduler assertions. | Does not cover multiple queued quick-entry candidates or failed capacity refill. |
| `LeavePlayerWorldAsync_StartEnterLogoutConsumesCancelEnterIntentsLikeJava` | Integration | Java `cancelEnter` no-refill branch after start-enter logout cleanup | No quick-entry candidate means no registry fanout while the logging-out player still receives cancel window `2` and penalty refresh. | Source-reviewed Java branch with stricter C# no-fanout assertion. | Does not cover quick-entry refill; covered by the new test above. |

## Validation Decision
- Changed surface: connection-level autogroup logout tests only.
- Specific behavior/contract: logout-driven start-enter `cancelEnter(...)` must run the Java quick-entry refill branch when an open runtime instance still has players and queued quick-entry capacity, and must be a no-op fanout when no refill is available.
- Focused C# command: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests" --no-restore`
- Focused Java/Maven command: skipped; no targeted Java fixture exists and no Java source changed.
- Broad-validation trigger: live connection dispatch and scheduler intents, but the change is test-only and the focused command covers connection, registration, and runtime surfaces.
- Broad .NET decision: skipped after focused validation.
- Why this scope is sufficient: the added integration test directly exercises the Java-reviewed branch through `LeavePlayerWorldAsync(...)`, and adjacent registration/runtime tests guard the helper behavior it relies on.

## Validation Result
- Passed: 78 tests, 0 failed, 0 skipped.
- A narrower connection-class precheck also passed: 19 tests, 0 failed, 0 skipped.
- Pre-existing nullable/analyzer warnings appeared on the first compile-producing run; no new warnings were introduced by this test-only change.

## Known Remaining Gaps
- Logout-specific additional-registration cleanup during ready-match dispatch remains a separate branch from quick-entry refill cleanup.
- Multiple queued quick-entry candidates and failed capacity-refill ordering are not covered by this logout test.
- Overall Java `PlayerLeaveWorldService.leaveWorld(...)` ordering remains partial.

## Summary Metrics
- Total Java artifacts reviewed in this UOW: 2.
- Total C# artifacts changed in this UOW: 1 test file.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification / partial parity: 3.
- Total blocked artifacts: 0.
