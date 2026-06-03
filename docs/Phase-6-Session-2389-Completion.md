# Phase 6 Session 2389 Completion - Refill Autogroup Quick Entries After Leave

## Scope
- Ported the Java `AutoGroupService.onLeaveInstance(...) -> destroyOrAddPlayersFromQuickEntries(autoInstance)` quick-entry refill branch into the live C# teleport leave adapter.
- Kept the UOW focused on the non-destroy, quick-registration-allowed branch where Java checks the queued quick-entry list after a registered autogroup player leaves an instance.

## Java Source Reviewed
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
  - `onLeaveInstance(Player player)`
  - `destroyOrAddPlayersFromQuickEntries(AutoInstance autoInstance)`
  - `checkQueueForQuickEntries(AutoInstance autoInstance)`
- `game-server/src/com/aionemu/gameserver/instance/AutoInstance.java`
  - `destroyIfPossible()`
  - `onLeaveInstance(Player player)`
- `game-server/src/com/aionemu/gameserver/instance/AutoPvpInstance.java`
  - `onLeaveInstance(Player player)`
  - `addLookingForParty(LookingForParty lfp)`

## Implemented
- Updated `GameServerConnection.SendInstanceLeaveMessageIfNeededAsync(...)` so the live leave path now:
  - invokes `AutoGroupInstanceLeaveRuntimeService.OnLeaveInstance(...)`;
  - checks `autoGroupLeave.Plan.WouldCheckQuickEntries`;
  - calls `AutoGroupLookingPartyRegistrationService.TryRefillQueuedQuickEntry(...)` for the leaving instance mask;
  - delegates runtime admission to `AutoGroupInstanceLeaveRuntimeService.TryAddOpenQuickEntry(...)`;
  - sends queued refill window deliveries through the connection registry before the leaving player's open-registration refresh packets.
- Added live teleport-leave coverage proving the queue refill and extra-registration cleanup path.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Network.Aion.GameServerConnection` / `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService` / `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService` | Service | Partial | Unit Tested | Partial Parity | `onLeaveInstance` now unregisters through the runtime planner and refills the first accepted queued quick entry after leave. Java penalty/logout paths and full persistent start-enter state remain incomplete. |
| `com.aionemu.gameserver.instance.AutoInstance` | `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService` | Runtime State | Partial | Unit Tested | Partial Parity | Destroy-if-possible is modeled for registered/online counts. Leave refill uses that planner result, but full Java world-player facts are only represented by the live adapter's `instance.PlayerCount - 1` input. |
| `com.aionemu.gameserver.instance.AutoPvpInstance` | `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService` | Runtime State | Partial | Unit Tested | Partial Parity | Existing quick-entry race/capacity admission is reused for leave refill. Team cleanup and race-capacity branches are covered, but Java subtype behavior is not complete for every arena flavor. |

## Tests Added
| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionInstanceCooldownTests.HandleTeleportAnimationDoneAsync_AutoGroupLeaveRefillsQueuedQuickEntryLikeJavaDestroyOrAdd` | Unit | Java source review of `AutoGroupService.onLeaveInstance`, `destroyOrAddPlayersFromQuickEntries`, and `checkQueueForQuickEntries` | Teleport leave unregisters the leaving player, refills the queued quick entrant, removes the entrant's additional registration, sends ready window `4`, sends cancel window `2`, and preserves normal teleport completion packets. | Focused C# test exercises the live C# leave adapter and shared autogroup runtime/registration services. | No Java runtime fixture; Java/Maven validation skipped. |

## Validation Decision
- Changed surface: live connection dispatch plus existing autogroup runtime/registration services.
- Specific behavior/contract: Java `onLeaveInstance -> destroyOrAddPlayersFromQuickEntries -> checkQueueForQuickEntries` non-destroy quick-entry refill and window delivery ordering.
- Focused C# command: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInstanceCooldownTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests" --no-restore`
- Focused Java/Maven command: skipped; no targeted Java fixture exists for this runtime branch, and no Java source changed.
- Broad-validation trigger: live connection dispatch changed.
- Broad .NET decision: skipped after focused live adapter/runtime/registration coverage passed; no packet primitive, serializer, persistence, scheduler, or shared infrastructure contract changed.
- Why this scope is sufficient: the focused filter covers the edited live leave adapter, the unregister/destroy/refill runtime service, and the queued-registration service that performs Java-style selection and cleanup.

## Validation Result
- Passed: 68 tests, 0 failed, 0 skipped.
- Pre-existing nullable/analyzer warnings remain in unrelated files.

## Known Remaining Gaps
- Java `penalisePlayerAndScheduleRemoval(...)` for cancel-enter remains incomplete.
- Java `onLogout(...)` interactions with start-enter tasks and active auto instances remain incomplete.
- Persistent `LookingForParty.isOnStartEnterTask()` lifecycle is still approximated by C# request timing data.
- Full Java `destroyIfPossible(...)` world-player facts are only partially modeled in the live leave adapter.

## Summary Metrics
- Total Java artifacts discovered in this UOW: 3.
- Total artifacts ported or extended in this UOW: 3 C# surfaces.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification / partial parity: 3.
- Total blocked artifacts: 0.
- Estimated overall Phase 6 completion: still conservative; unchanged materially by this narrow autogroup slice.
