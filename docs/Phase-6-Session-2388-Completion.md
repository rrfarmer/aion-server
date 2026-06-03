# Phase 6 Session 2388 Completion - Refill Autogroup Quick Entries After Cancel-Enter

## Scope
- Ported the first Java `destroyOrAddPlayersFromQuickEntries(autoInstance)` / `checkQueueForQuickEntries(autoInstance)` refill caller for the cancel-enter path.
- Kept the unit focused on queued quick-entry refill after a player cancels an active autogroup enter prompt.

## Java Source Reviewed
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/instance/AutoInstance.java`
- `game-server/src/com/aionemu/gameserver/instance/AutoPvpInstance.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/LookingForParty.java`

## Implemented
- Added `AutoGroupLookingPartyRegistrationService.TryRefillQueuedQuickEntry(...)`.
  - Scans queued parties for the mask in current list order.
  - Skips non-quick entries.
  - Reuses the open quick-entry runtime gate.
  - Removes the accepted quick entry from search.
  - Plans Java window `4` for the accepted leader.
  - Cleans additional leader registrations and plans Java cancel window `2` for those players.
- Wired `CM_AUTO_GROUP` window `103` cancel-enter handling to attempt a queued quick-entry refill before sending the cancelling player Java cancel window `2`.
- Added service and packet-bridge coverage for Java-style refill behavior.

## Parity Notes
| Java | C# | Status | Notes |
| --- | --- | --- | --- |
| `AutoGroupService.cancelEnter(...)` | `GameServerConnection.HandleAutoGroupAsync(...)` / `AutoGroupInstanceLeaveRuntimeService.CancelEnter(...)` | Partial, unit tested | Unregisters, then attempts a quick-entry refill before sending cancel window `2` when the modeled instance still has registered players and quick registration is allowed. Penalty scheduling and full destroy behavior remain incomplete. |
| `AutoGroupService.destroyOrAddPlayersFromQuickEntries(...)` | `GameServerConnection` cancel branch + `AutoGroupLookingPartyRegistrationService.TryRefillQueuedQuickEntry(...)` | Partial, unit tested | Cancel-enter refill is wired. Leave-instance refill is not wired yet. Java `destroyIfPossible(...)` is approximated with the available registered-count snapshot. |
| `AutoGroupService.checkQueueForQuickEntries(...)` | `AutoGroupLookingPartyRegistrationService.TryRefillQueuedQuickEntry(...)` | Partial, unit tested | Scans queue in order, attaches the first accepted quick entry, removes search entry, sends window `4`, and removes additional leader registrations. Persistent `isOnStartEnterTask` lifecycle is still partial. |
| `AutoPvpInstance.addLookingForParty(...)` | `AutoGroupInstanceLeaveRuntimeService.TryAddOpenQuickEntry(...)` | Partial, unit tested | Existing race/capacity/join-window gate is reused for refill. |

## Tests Added
| Test | Coverage |
| --- | --- |
| `AutoGroupLookingPartyRegistrationServiceTests.TryRefillQueuedQuickEntry_AttachesFirstQueuedQuickPartyLikeJavaCheckQueueForQuickEntries` | Verifies Java queue scan order, non-quick skip, first accepted quick attach, additional-registration cleanup, and window delivery planning. |
| `GameServerConnectionAutoGroupTests.ProcessPacketAsync_AutoGroupCancelEnterRefillsQueuedQuickEntryLikeJava` | Verifies live `CM_AUTO_GROUP` cancel-enter unregisters the cancelling player, refills a queued quick entrant, cleans additional registration, sends ready window `4`, and sends cancel window `2`. |

## Validation
- Passed:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests" --no-restore`
- Result: 60 passed, 0 failed, 0 skipped.
- Java/Maven validation skipped: no targeted Java fixture exists for this runtime queue-refill branch; parity was checked by source review.
- Broad .NET solution validation skipped: the change is constrained to autogroup registration/runtime connection tests, and the focused filter covers the touched service and packet dispatch surface.

## Known Remaining Gaps
- Java `destroyIfPossible(autoInstance)` is not fully modeled in cancel-enter because the C# cancel-enter path currently lacks online-inside-player facts. This UOW conservatively refills only when the modeled registered-player count remains above zero after cancel.
- `AutoGroupService.onLeaveInstance(...)` still needs the same destroy-or-refill parity behavior.
- Java penalty scheduling and delayed removal after cancel-enter remain incomplete.
- Full persistent start-enter task state on `LookingForParty` remains partial.
