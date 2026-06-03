# Phase 6 Session 2387 Handoff - Wire Autogroup Open Quick-Entry Start-Looking

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2387-Completion.md`
- `docs/Phase-6-Session-2387-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Prefer production/game parity units. Use focused validation by default; full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger before execution. A passing filtered `dotnet test` command is compile evidence for the affected project/dependencies.

## Current State

Last completed UOW: UOW-2387, wired open quick-entry runtime attachment into `StartLooking(...)` and live `CM_AUTO_GROUP` window `100` dispatch.

Recent production parity slices:

- `CM_AUTO_GROUP` window `100` start-looking queue registration.
- `CM_AUTO_GROUP` window `100` duplicate already-registered system message.
- `CM_AUTO_GROUP` window `100` successful registration fanout.
- Multi-member online filtering evidence for successful registration fanout.
- `CM_AUTO_GROUP` window `100` battleground registration announcement after successful periodic group registration.
- Non-live queue ordering and periodic PvP match readiness planning for `checkQueueForNewMatches`.
- Successful `StartLooking` results expose the queue match plan after registration/announcement planning.
- Ready queue matches expose `CreateReadyMatchPlan(...)` with matched parties, window `4` recipients, and additional-registration cleanup intents.
- Ready queue matches apply live matched queue removal, additional-registration queue cleanup, cleanup window `2`, and ready-enter window `4`.
- Ready queue matches register auto-instance runtime entries for matched players before window `4` delivery.
- Live ready queue matches allocate a modeled `WorldMapInstanceRuntimeState` id and notify instance creation before runtime registration.
- Ready queue match runtime registrations carry `ReadyEnterStartTime`, matching Java `LookingForParty.setStartEnterTime()`.
- Ready queue match runtime registrations carry `StartInstanceTime`, matching Java `AutoInstance.onInstanceCreate(instance)`.
- Ready queue match allocation passes Java-derived `AutoGroupType.getDifficultId()` into modeled world instance state.
- C# auto-group summaries expose Java-derived `AutoGroupType.getMaximumJoinTime()` as `MaximumJoinTimeMilliseconds`.
- Runtime registrations carry `MaximumJoinTimeMilliseconds`, total max players, and member race facts.
- Runtime open quick-entry gating can accept/reject quick-entry requests like Java `checkInstancesForOpenQuickEntries(...)` and `AutoPvpInstance.addLookingForParty(...)` for the modeled facts.
- `StartLooking(...)` now calls the runtime open quick-entry gate before queue matching when a callback is provided.
- Live `CM_AUTO_GROUP` window `100` quick entries can attach to existing runtime auto-instances, send window `4`, remove the just-added queue entry, and clean leader additional registrations.
- Runtime rejection falls back to the normal queue-match path.
- `CM_AUTO_GROUP` window `102` press-enter can resolve matched players from the ready-match runtime bridge and send window `5`.
- `CM_AUTO_GROUP` window `101` cancel queued registration.
- `CM_AUTO_GROUP` window `103` cancel-enter runtime unregister.
- `CM_AUTO_GROUP` window `104` periodic request-icon click handling.
- `CM_AUTO_GROUP` top-level disabled config guard using `gameserver.autogroup.enable`.
- `CM_AUTO_GROUP` window `105` explicit no-op.

Still not proven or not implemented:

- Java `AutoGroupService.checkQueueForQuickEntries(autoInstance)` refill after cancel/leave remains missing.
- Open quick-entry accepted start-enter time is exposed as an attachment result, but the C# model still lacks the full Java `LookingForParty` lifecycle after attachment.
- Java `AutoInstance.isRegistrationDisabled(lfp)` score-ended branch remains missing.
- Java `AutoPvpInstance.onPressEnter(...)` port-to-start-position behavior remains missing.
- Java `SpawnEngine.spawnInstance(...)` and event spawn behavior are not wired into autogroup ready-match allocation.
- Java penalties, delayed cancel removal, and full auto-instance lifecycle remain missing.
- Java periodic registration cron callbacks and real scheduled close task creation/cancellation remain missing.

## Commits Made

- `[Phase 6][UOW-2387] Wire autogroup open quick-entry start-looking`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupLookingPartyRegistrationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAutoGroupTests.cs`
- `docs/Phase-6-Session-2387-Completion.md`
- `docs/Phase-6-Session-2387-Handoff.md`

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests" --no-restore
```

Result: passed 58, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Documentation hygiene:

```powershell
git diff --check
```

Run after this handoff is created and before commit.

Focused Java/Maven validation: skipped because no targeted Java fixture exists for this start-looking/open quick-entry bridge. Java source review identified the authoritative branch behavior.

Broad-validation trigger: live connection dispatch and shared autogroup runtime state integration changed.

Broad .NET decision: skipped full project/solution validation. The focused command compiled the affected project and directly covered the edited service, runtime gate, and live packet boundary.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.AutoGroupService.startLooking(...)` | `GameServerConnection.HandleAutoGroupAsync(...)` / `AutoGroupLookingPartyRegistrationService.StartLooking(...)` | Service/Connection | Partial | Unit Tested | Partial Parity | Window `100` now tries open quick-entry runtime attachment before queue matching after successful registration. Penalties and full scheduled lifecycle remain partial. |
| `com.aionemu.gameserver.services.AutoGroupService.checkInstancesForOpenQuickEntries(...)` | `AutoGroupLookingPartyRegistrationService.TryAttachOpenQuickEntry(...)` / `AutoGroupInstanceLeaveRuntimeService.TryAddOpenQuickEntry(...)` | Service/Runtime | Partial | Unit Tested | Partial Parity | Accepted quick entries remove the search entry, plan window `4`, and clean leader additional registrations. Queue refill caller remains missing. |
| `com.aionemu.gameserver.services.AutoGroupService.searchAndRemoveAdditionalRegistrations(...)` | `AutoGroupLookingPartyRegistrationService.ApplyAdditionalRegistrationCleanup(...)` | Service | Partial | Unit Tested | Partial Parity | Leader cleanup windows are exercised for open quick-entry attachment. Penalty side effects remain intent-only, and member cleanup recheck remains only modeled. |
| `com.aionemu.gameserver.model.autogroup.LookingForParty` | `AutoGroupLookingPartyRegistration` / `AutoGroupOpenQuickEntryAttachment` | Model | Partial | Unit Tested | Partial Parity | Start-enter time is represented on the attachment result for accepted open quick entries, but not as a persistent full lifecycle model. |

## Next Sequential UOW

Next sequential production slice: implement queued quick-entry refill after cancel-enter or leave, matching Java `AutoGroupService.destroyOrAddPlayersFromQuickEntries(autoInstance)` and `checkQueueForQuickEntries(autoInstance)`.

Recommended scope:

- Reuse the open quick-entry runtime gate for queued quick-entry candidates after `CancelEnter(...)` or `OnLeaveInstance(...)` unregisters a player.
- Add a service-level refill planner that scans queued parties for the same mask, picks the first Java-eligible quick-entry party, removes it, records start-enter time, plans leader window `4`, and cleans additional registrations.
- Keep live packet dispatch focused on `CM_AUTO_GROUP` window `103` cancel-enter first; defer leave-instance live dispatch if it needs broader world/player-state plumbing.
- Preserve existing destroy-if-empty behavior; if the runtime instance is destroyed, do not refill.

Java artifacts to inspect:

- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoInstance.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoPvpInstance.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/LookingForParty.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupInstanceLeaveRuntimeService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupLookingPartyRegistrationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupInstanceLeaveRuntimeServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAutoGroupTests.cs`

Expected Java behavior to model next:

- After cancel-enter unregisters a runtime player, Java calls `destroyOrAddPlayersFromQuickEntries(autoInstance)`.
- If the instance is not destroyed and quick registration is allowed, Java scans queued parties for a quick-entry party not on start-enter task.
- The first accepted queued quick-entry party is removed from the queue, has start-enter time set, receives window `4` to the leader, and triggers additional-registration cleanup for the leader.
- If no queued quick-entry party is accepted, no refill window is sent.

Focused validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests" --no-restore
```

Specific behavior this command should prove: cancel-enter or leave-triggered quick-entry refill removes a queued quick-entry party, attaches it to the existing runtime instance, sends window `4`, cleans additional registrations, and preserves existing start-looking, cancel-enter, leave, and press-enter behavior.

Java/Maven is not expected unless a targeted Java fixture is added. Broad-validation trigger: live connection dispatch or shared autogroup runtime state if the next UOW wires refill into packet handling; start focused and add only directly related tests.

## Safe Candidates

- Implement queued quick-entry refill after cancel-enter.
- Implement queued quick-entry refill after instance leave if the live boundary is already narrow enough.
- Add instance score-ended registration rejection when score runtime state exists.
- Add Java penalty scheduling as explicit delayed runtime state once refill behavior is stable.
- Add a press-enter destination/port-to-start-position plan for `AutoPvpInstance.onPressEnter(...)` after a C# handler abstraction exists.

Avoid:

- Evidence/reporting-only units.
- Claiming full `checkInstancesForOpenQuickEntries(...)` parity before refill, penalties, and lifecycle behavior are wired.
- Claiming full `AutoInstance.isRegistrationDisabled(...)` parity before score-ended rejection is modeled.
- Claiming full `createNewInstance(...)` parity before spawn content, handler subclasses, and start-position behavior are represented.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
